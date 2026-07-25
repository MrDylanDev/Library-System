package com.libromagico.controller;

import com.libromagico.dto.UsuarioResponse;
import com.libromagico.model.Usuario;
import com.libromagico.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listarTodos(@PageableDefault(sort = "id", size = 20) Pageable pageable) {
        Page<Usuario> usuarios = usuarioService.listarTodos(pageable);
        Page<UsuarioResponse> response = usuarios.map(UsuarioController::toResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        var usuario = usuarioService.buscarPorId(id);
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
