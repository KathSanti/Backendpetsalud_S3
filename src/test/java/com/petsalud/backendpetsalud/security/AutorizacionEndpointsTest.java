package com.petsalud.backendpetsalud.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.petsalud.backendpetsalud.controller.MascotaController;
import com.petsalud.backendpetsalud.controller.ProductoController;
import com.petsalud.backendpetsalud.controller.UsuarioController;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.monitoreo.SuspiciousActivityService;
import com.petsalud.backendpetsalud.service.MascotaService;
import com.petsalud.backendpetsalud.service.ProductoService;
import com.petsalud.backendpetsalud.service.UsuarioService;

/**
 * Pruebas de autorización por rol y por propiedad sobre los endpoints reales,
 * con la configuración de seguridad real y los servicios simulados (sin base de datos).
 */
@WebMvcTest(controllers = {ProductoController.class, MascotaController.class, UsuarioController.class})
@Import({SecurityConfig.class, SuspiciousActivityService.class})
class AutorizacionEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ProductoService productoService;
    @MockBean private MascotaService mascotaService;
    @MockBean private UsuarioService usuarioService;
    @MockBean private JwtService jwtService;
    @MockBean private UsuarioDetailsService usuarioDetailsService;
    @MockBean(name = "accesoRecursos") private AccesoRecursos accesoRecursos;

    // ---------- Autenticación ----------

    @Test
    @DisplayName("Sin token, un endpoint protegido responde 401")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Roles: catálogo de productos ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("Un cliente puede ver el catálogo")
    void clienteVeProductos() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("Un cliente no puede crear productos")
    void clienteNoCreaProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Collar\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("Un empleado puede crear productos")
    void empleadoCreaProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Collar\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("Un empleado no puede eliminar productos")
    void empleadoNoEliminaProductos() throws Exception {
        mockMvc.perform(delete("/api/productos/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("El gerente puede eliminar productos")
    void gerenteEliminaProductos() throws Exception {
        mockMvc.perform(delete("/api/productos/1"))
                .andExpect(status().isNoContent());
    }

    // ---------- Roles: administración de usuarios ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("Un cliente no puede listar usuarios")
    void clienteNoListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("Un empleado no puede listar usuarios")
    void empleadoNoListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    @DisplayName("El gerente puede listar usuarios")
    void gerenteListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    // ---------- Propiedad de recursos (IDOR) ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("Un cliente no puede ver la mascota de otra persona")
    void clienteNoVeMascotaAjena() throws Exception {
        when(accesoRecursos.mascota(any(), eq(5L))).thenReturn(false);

        mockMvc.perform(get("/api/mascotas/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("Un cliente sí puede ver su propia mascota")
    void clienteVeSuMascota() throws Exception {
        when(accesoRecursos.mascota(any(), eq(5L))).thenReturn(true);
        when(mascotaService.obtenerMascotaPorId(5L))
                .thenReturn(Optional.of(Mascota.builder().id(5L).nombre("Firulais").build()));

        mockMvc.perform(get("/api/mascotas/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Firulais"));
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("El personal ve cualquier mascota sin revisar la propiedad")
    void empleadoVeCualquierMascota() throws Exception {
        when(mascotaService.obtenerMascotaPorId(5L))
                .thenReturn(Optional.of(Mascota.builder().id(5L).nombre("Firulais").build()));

        mockMvc.perform(get("/api/mascotas/5"))
                .andExpect(status().isOk());
        verify(accesoRecursos, never()).mascota(any(), any());
    }

    // ---------- Registro público y validación de entrada ----------

    @Test
    @DisplayName("El registro es público, responde 201 y nunca devuelve la contraseña")
    void registroPublicoSinPassword() throws Exception {
        when(usuarioService.registrarUsuario(any())).thenReturn(Usuario.builder()
                .id(7L).nombre("Ana").apellido("Pérez").email("ana@petsalud.test")
                .password("$2a$10$hashDePrueba").rol(Rol.CLIENTE).build());

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Ana", "Secreta123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("XSS: el registro rechaza etiquetas HTML en el nombre")
    void registroRechazaScript() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("<script>alert(1)</script>", "Secreta123")))
                .andExpect(status().isBadRequest());
        verify(usuarioService, never()).registrarUsuario(any());
    }

    @Test
    @DisplayName("El registro rechaza contraseñas de menos de 8 caracteres")
    void registroRechazaPasswordCorta() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Ana", "123")))
                .andExpect(status().isBadRequest());
        verify(usuarioService, never()).registrarUsuario(any());
    }

    private static String registroJson(String nombre, String password) {
        return """
                {"nombre":"%s","apellido":"Pérez","email":"ana@petsalud.test",
                 "telefono":"+56912345678","password":"%s","direccion":"Av. Siempre Viva 123"}
                """.formatted(nombre, password);
    }
}
