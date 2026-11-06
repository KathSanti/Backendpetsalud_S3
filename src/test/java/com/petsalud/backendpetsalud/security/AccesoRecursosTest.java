package com.petsalud.backendpetsalud.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.ExpedienteMedicoRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.NotificacionRepository;
import com.petsalud.backendpetsalud.repository.RecompensaRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;

/**
 * Pruebas del control de propiedad (prevención de IDOR):
 * un cliente solo accede a sus propios recursos.
 */
@ExtendWith(MockitoExtension.class)
class AccesoRecursosTest {

    @Mock private MascotaRepository mascotas;
    @Mock private CitaRepository citas;
    @Mock private ExpedienteMedicoRepository expedientes;
    @Mock private VentaRepository ventas;
    @Mock private RecompensaRepository recompensas;
    @Mock private NotificacionRepository notificaciones;

    private AccesoRecursos acceso;

    private final Usuario duena = Usuario.builder().id(1L).email("duena@petsalud.test").rol(Rol.CLIENTE).build();
    private final Usuario otroCliente = Usuario.builder().id(2L).email("otro@petsalud.test").rol(Rol.CLIENTE).build();
    private final Mascota mascotaDeDuena = Mascota.builder().id(10L).nombre("Firulais").propietario(duena).build();

    @BeforeEach
    void setUp() {
        acceso = new AccesoRecursos(mascotas, citas, expedientes, ventas, recompensas, notificaciones);
    }

    @Test
    @DisplayName("Un usuario solo es dueño de su propio id")
    void usuarioSoloAccedeASuPropioId() {
        assertThat(acceso.usuario(autenticado(duena), 1L)).isTrue();
        assertThat(acceso.usuario(autenticado(duena), 2L)).isFalse();
        assertThat(acceso.usuario(null, 1L)).isFalse();
    }

    @Test
    @DisplayName("Solo la dueña accede a su mascota")
    void soloLaDuenaAccedeASuMascota() {
        when(mascotas.findById(10L)).thenReturn(Optional.of(mascotaDeDuena));

        assertThat(acceso.mascota(autenticado(duena), 10L)).isTrue();
        assertThat(acceso.mascota(autenticado(otroCliente), 10L)).isFalse();
    }

    @Test
    @DisplayName("Una mascota que no existe no le pertenece a nadie")
    void mascotaInexistenteDeniegaAcceso() {
        when(mascotas.findById(99L)).thenReturn(Optional.empty());

        assertThat(acceso.mascota(autenticado(duena), 99L)).isFalse();
    }

    @Test
    @DisplayName("Una cita pertenece al dueño de la mascota")
    void citaPerteneceAlDuenoDeLaMascota() {
        Cita cita = Cita.builder().id(20L).mascota(mascotaDeDuena).build();
        when(citas.findById(20L)).thenReturn(Optional.of(cita));

        assertThat(acceso.cita(autenticado(duena), 20L)).isTrue();
        assertThat(acceso.cita(autenticado(otroCliente), 20L)).isFalse();
    }

    @Test
    @DisplayName("Una venta solo la ve el cliente que compró")
    void ventaSoloLaVeElComprador() {
        Venta venta = Venta.builder().id(30L).usuario(duena).build();
        when(ventas.findById(30L)).thenReturn(Optional.of(venta));

        assertThat(acceso.venta(autenticado(duena), 30L)).isTrue();
        assertThat(acceso.venta(autenticado(otroCliente), 30L)).isFalse();
    }

    private static Authentication autenticado(Usuario usuario) {
        UsuarioDetails details = new UsuarioDetails(usuario);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
