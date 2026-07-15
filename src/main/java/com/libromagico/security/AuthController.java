package com.libromagico.security;

import com.libromagico.dto.AuthResponse;
import com.libromagico.dto.ForgotPasswordRequest;
import com.libromagico.dto.LoginRequest;
import com.libromagico.dto.RegisterRequest;
import com.libromagico.dto.ResetPasswordRequest;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.EmailService;
import com.libromagico.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.contrasena()));

        var usuario = usuarioRepository.findByEmail(request.email()).orElseThrow();
        String token = tokenProvider.generateToken(usuario.getEmail(), usuario.getRol().name());

        return ResponseEntity.ok(new AuthResponse(token, usuario.getId(), usuario.getEmail(), usuario.getRol().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        var usuario = usuarioService.register(request);

        String token = tokenProvider.generateToken(usuario.getEmail(), usuario.getRol().name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, usuario.getId(), usuario.getEmail(), usuario.getRol().name()));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserDetails userDetails) {
        var usuario = usuarioRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol().name()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        var usuario = usuarioService.forgotPassword(request.email());

        usuario.ifPresent(u -> {
            String resetLink = baseUrl + "/#/reset-password/" + u.getResetToken();
            emailService.enviarResetPassword(u.getEmail(), u.getNombre(), resetLink);
        });

        return ResponseEntity.ok(Map.of("message", "Si el email existe, recibirás un enlace de recuperación"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        usuarioService.resetPassword(request.token(), request.newPassword());

        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }
}
