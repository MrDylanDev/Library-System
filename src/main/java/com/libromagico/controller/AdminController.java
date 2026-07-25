package com.libromagico.controller;

import com.libromagico.dto.UpdateEstadoRequest;
import com.libromagico.dto.UpdateRolRequest;
import com.libromagico.dto.UsuarioResponse;
import com.libromagico.model.Multa;
import com.libromagico.model.Usuario;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.service.MultaService;
import com.libromagico.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioService usuarioService;
    private final MultaService multaService;

    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<?> actualizarRol(@PathVariable Long id, @Valid @RequestBody UpdateRolRequest request) {
        RolUsuario nuevoRol;
        try {
            nuevoRol = RolUsuario.valueOf(request.rol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("Rol inválido: " + request.rol() + ". Usar USER, LIBRARIAN o ADMIN");
        }

        var usuario = usuarioService.actualizarRol(id, nuevoRol);
        return ResponseEntity.ok(toResponse(usuario));
    }

    @PutMapping("/usuarios/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @Valid @RequestBody UpdateEstadoRequest request) {
        EstadoUsuario nuevoEstado;
        try {
            nuevoEstado = EstadoUsuario.valueOf(request.estado().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("Estado inválido: " + request.estado() + ". Usar ACTIVO o BLOQUEADO");
        }

        var usuario = usuarioService.actualizarEstado(id, nuevoEstado);
        return ResponseEntity.ok(toResponse(usuario));
    }

    @GetMapping("/multas")
    public ResponseEntity<Page<Multa>> listarMultas(@PageableDefault(sort = "id", size = 20) Pageable pageable) {
        return ResponseEntity.ok(multaService.listarMultas(pageable));
    }

    @PutMapping("/multas/{id}/pagar")
    public ResponseEntity<?> pagarMulta(@PathVariable Long id) {
        var multa = multaService.pagarMulta(id);
        return ResponseEntity.ok(multa);
    }

    private static UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getDni(), u.getTelefono(), u.getRol(), u.getEstado());
    }
}
