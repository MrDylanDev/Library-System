package com.libromagico.security;

import com.libromagico.dto.AuthResponse;
import com.libromagico.dto.ForgotPasswordRequest;
import com.libromagico.dto.LoginRequest;
import com.libromagico.dto.RegisterRequest;
import com.libromagico.dto.ResetPasswordRequest;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.EmailService;
import com.libromagico.service.TokenRevocationService;
import com.libromagico.service.UsuarioService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final EmailService emailService;
    private final TokenRevocationService tokenRevocationService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private ResponseCookie jwtCookie(String token) {
        return ResponseCookie.from("AuthToken", token)
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .secure(cookieSecure)
                .maxAge(Duration.ofMillis(jwtExpiration))
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.contrasena()));

        var usuario = usuarioRepository.findByEmail(request.email()).orElseThrow();
        String token = tokenProvider.generateToken(usuario.getEmail(), usuario.getRol().name());

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie(token).toString());
        return ResponseEntity.ok(new AuthResponse(usuario.getId(), usuario.getEmail(), usuario.getRol().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        var usuario = usuarioService.register(request);

        String token = tokenProvider.generateToken(usuario.getEmail(), usuario.getRol().name());
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie(token).toString());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(usuario.getId(), usuario.getEmail(), usuario.getRol().name()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie delete = ResponseCookie.from("AuthToken", "")
                .httpOnly(true).path("/").sameSite("Strict").secure(cookieSecure).maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, delete.toString());

        revocarTokenDelRequest(request);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada"));
    }

    private void revocarTokenDelRequest(HttpServletRequest request) {
        try {
            String token = extraerToken(request);
            if (token == null || !tokenProvider.validateToken(token)) {
                return;
            }
            String jti = tokenProvider.getTokenIdFromToken(token);
            String email = tokenProvider.getEmailFromToken(token);
            LocalDateTime expiraEn = tokenProvider.getExpirationFromToken(token);
            tokenRevocationService.revocarToken(jti, email, expiraEn);
        } catch (JwtException | IllegalArgumentException e) {
            // El token del request puede estar corrupto: el logout igual borra
            // la cookie y responde OK.
            log.warn("Token de logout inválido, se omite la revocación: {}", e.getMessage());
        }
    }

    private String extraerToken(HttpServletRequest request) {
        // Cookie httpOnly (SPA) tiene prioridad sobre el header Bearer.
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JwtAuthenticationFilter.AUTH_COOKIE.equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }

        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
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
