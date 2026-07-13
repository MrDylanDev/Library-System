package com.libromagico.controller;

import com.libromagico.dto.UsuarioResponse;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.Usuario;
import com.libromagico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        var usuarios = usuarioRepository.findAll().stream()
                .map(UsuarioController::toResponse)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        return ResponseEntity.ok(toResponse(usuario));
    }

    private static UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNombre(),
                u.getEmail(),
                u.getDni(),
                u.getTelefono(),
                u.getRol(),
                u.getEstado()
        );
    }
}
