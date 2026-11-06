package com.petsalud.backendpetsalud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.petsalud.backendpetsalud.dto.RegistroRequest;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;

/**
 * Pruebas del registro: la contraseña se guarda con BCrypt y el rol siempre es CLIENTE.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    private static final String PASSWORD_PLANA = "Secreta123";

    @Mock
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioServiceImpl(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("El registro guarda la contraseña como hash BCrypt y asigna el rol CLIENTE")
    void registroGuardaHashYRolCliente() {
        when(usuarioRepository.findByEmail("ana@petsalud.test")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario guardado = usuarioService.registrarUsuario(solicitud());

        assertThat(guardado.getPassword()).isNotEqualTo(PASSWORD_PLANA);
        assertThat(guardado.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD_PLANA, guardado.getPassword())).isTrue();
        assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("No permite registrar un email que ya existe")
    void rechazaEmailDuplicado() {
        when(usuarioRepository.findByEmail("ana@petsalud.test")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> usuarioService.registrarUsuario(solicitud()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(usuarioRepository, never()).save(any());
    }

    private static RegistroRequest solicitud() {
        return new RegistroRequest("Ana", "Pérez", "ana@petsalud.test", "+56912345678",
                PASSWORD_PLANA, "Av. Siempre Viva 123");
    }
}
