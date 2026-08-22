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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRevocationService tokenRevocationService;

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

    @Transactional
    public Optional<ResetPasswordInvitation> forgotPassword(String email) {
        var usuario = usuarioRepository.findByEmail(email);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }

        var u = usuario.get();
        String tokenCrudo = UUID.randomUUID().toString();
        u.setResetTokenHash(hash(tokenCrudo));
        u.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(u);

        // Solo el hash se persiste; el token crudo viaja en el email. Si la base
        // se ve comprometida, el hash no permite restablecer la contraseña.
        return Optional.of(new ResetPasswordInvitation(u.getEmail(), u.getNombre(), tokenCrudo));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        var usuario = usuarioRepository.findByResetTokenHash(hash(token))
                .orElseThrow(() -> new OperacionInvalidaException("Token inválido o expirado"));

        if (usuario.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new OperacionInvalidaException("Token inválido o expirado");
        }
        if (usuario.getResetTokenHash() == null
                || !usuario.getResetTokenHash().equals(hash(token))) {
            throw new OperacionInvalidaException("Token inválido o expirado");
        }

        usuario.setContrasena(passwordEncoder.encode(newPassword));
        usuario.setResetTokenHash(null);
        usuario.setResetTokenExpiry(null);
        usuarioRepository.save(usuario);

        // Invalida TODAS las sesiones previas del usuario al cambiar la contraseña.
        tokenRevocationService.revocarTokensDeUsuario(usuario.getEmail());
        log.info("Sesiones previas revocadas tras cambio de contraseña: usuario={}", usuario.getId());
        log.info("Contraseña restablecida: usuario={}", usuario.getId());
    }

    public static String hashToken(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private String hash(String token) {
        return hashToken(token);
    }

    /**
     * Datos para enviar la invitación de restablecimiento de contraseña.
     * El token es el valor crudo (solo viaja en el email); en la base se
     * persiste únicamente su hash.
     */
    public record ResetPasswordInvitation(String email, String nombre, String token) {
    }

    @Transactional
    public Usuario actualizarRol(Long id, RolUsuario nuevoRol) {
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        boolean cambiaPrivilegios = usuario.getRol() != nuevoRol;
        usuario.setRol(nuevoRol);
        var saved = usuarioRepository.save(usuario);
        log.info("Rol actualizado: usuario={}, rol={}", id, nuevoRol);

        // Si cambian los privilegios, los tokens ya emitidos siguen llevando el
        // rol viejo en el claim. Se revocan para que el usuario deba re-autenticarse
        // y obtener tokens con el rol nuevo.
        if (cambiaPrivilegios) {
            tokenRevocationService.revocarTokensDeUsuario(usuario.getEmail());
        }
        return saved;
    }

    @Transactional
    public Usuario actualizarEstado(Long id, EstadoUsuario nuevoEstado) {
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        boolean seBloquea = usuario.getEstado() != EstadoUsuario.BLOQUEADO
                && nuevoEstado == EstadoUsuario.BLOQUEADO;
        usuario.setEstado(nuevoEstado);
        var saved = usuarioRepository.save(usuario);
        log.info("Estado actualizado: usuario={}, estado={}", id, nuevoEstado);

        // Al bloquear, se revocan todos los tokens vigentes: un usuario con una
        // sesión activa (token emitido antes del bloqueo) no debe seguir operando
        // hasta que el token expire o se cierre sesión.
        if (seBloquea) {
            tokenRevocationService.revocarTokensDeUsuario(usuario.getEmail());
        }
        return saved;
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }

    public Page<Usuario> listarTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }
}
