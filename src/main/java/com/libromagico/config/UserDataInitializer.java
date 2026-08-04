package com.libromagico.config;

import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@Profile("!test")
public class UserDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("Usuarios de prueba ya existen, omitiendo seed");
            return;
        }

        var usuarios = List.of(
                createUsuario("Admin", "admin@libromagico.com", "admin123", "00000001", "+5491111111111", RolUsuario.ADMIN),
                createUsuario("Librarian", "librarian@libromagico.com", "librarian123", "00000002", "+5491122222222", RolUsuario.LIBRARIAN),
                createUsuario("Usuario", "usuario@libromagico.com", "usuario123", "00000003", "+5491133333333", RolUsuario.USER)
        );

        usuarioRepository.saveAll(usuarios);
        log.info("Seed completado: {} usuarios insertados", usuarios.size());
    }

    private Usuario createUsuario(String nombre, String email, String contrasena, String dni, String telefono, RolUsuario rol) {
        var usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuario.setDni(dni);
        usuario.setTelefono(telefono);
        usuario.setRol(rol);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        return usuario;
    }
}
