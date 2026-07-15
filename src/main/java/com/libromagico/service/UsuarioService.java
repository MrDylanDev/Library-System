package com.libromagico.service;

import com.libromagico.dto.RegisterRequest;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new OperacionInvalidaException("El email ya está registrado");
        }
        if (request.dni() != null && usuarioRepository.existsByDni(request.dni())) {
            throw new OperacionInvalidaException("El DNI ya está registrado");
        }

        var usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setDni(request.dni());
        usuario.setTelefono(request.telefono());
        usuario.setRol(RolUsuario.USER);

        var saved = usuarioRepository.save(usuario);
        log.info("Usuario registrado: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    public Optional<Usuario> forgotPassword(String email) {
        var usuario = usuarioRepository.findByEmail(email);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }

        var u = usuario.get();
        u.setResetToken(UUID.randomUUID().toString());
        u.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(u);

        return Optional.of(u);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        var usuario = usuarioRepository.findByResetToken(token)
                .orElseThrow(() -> new OperacionInvalidaException("Token inválido o expirado"));

        if (usuario.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new OperacionInvalidaException("Token inválido o expirado");
        }

        usuario.setContrasena(passwordEncoder.encode(newPassword));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiry(null);
        usuarioRepository.save(usuario);
        log.info("Contraseña restablecida: usuario={}", usuario.getId());
    }

    @Transactional
    public Usuario actualizarRol(Long id, RolUsuario nuevoRol) {
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        usuario.setRol(nuevoRol);
        var saved = usuarioRepository.save(usuario);
        log.info("Rol actualizado: usuario={}, rol={}", id, nuevoRol);
        return saved;
    }

    @Transactional
    public Usuario actualizarEstado(Long id, EstadoUsuario nuevoEstado) {
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        usuario.setEstado(nuevoEstado);
        var saved = usuarioRepository.save(usuario);
        log.info("Estado actualizado: usuario={}, estado={}", id, nuevoEstado);
        return saved;
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }
}
