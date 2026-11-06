package com.petsalud.backendpetsalud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password}")
    private String passwordInicial;

    @Override
    public void run(String... args) {
        crearSiNoExiste("cliente@petsalud.test", "Carla", "Cliente", Rol.CLIENTE);
        crearSiNoExiste("empleado@petsalud.test", "Esteban", "Empleado", Rol.EMPLEADO);
        crearSiNoExiste("gerente@petsalud.test", "Gabriela", "Gerente", Rol.GERENTE);
    }

    private void crearSiNoExiste(String email, String nombre, String apellido, Rol rol) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        usuarioRepository.save(Usuario.builder()
                .email(email)
                .nombre(nombre)
                .apellido(apellido)
                .password(passwordEncoder.encode(passwordInicial))
                .rol(rol)
                .build());
        log.info("Usuario de prueba creado: {} ({})", email, rol);
    }
}