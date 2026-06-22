package com.libromagico.security;

import com.libromagico.dto.AuthResponse;
import com.libromagico.dto.LoginRequest;
import com.libromagico.dto.RegisterRequest;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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
        if (usuarioRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado"));
        }
        if (request.dni() != null && usuarioRepository.existsByDni(request.dni())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El DNI ya está registrado"));
        }

        var usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setDni(request.dni());
        usuario.setTelefono(request.telefono());
        usuario.setRol(RolUsuario.USER);

        usuarioRepository.save(usuario);

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
}
