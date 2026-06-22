package com.libromagico.controller;

import com.libromagico.dto.UpdateEstadoRequest;
import com.libromagico.dto.UpdateRolRequest;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoMulta;
import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;
import com.libromagico.repository.MultaRepository;
import com.libromagico.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final MultaRepository multaRepository;

    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<?> actualizarRol(@PathVariable Long id, @Valid @RequestBody UpdateRolRequest request) {
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        RolUsuario nuevoRol;
        try {
            nuevoRol = RolUsuario.valueOf(request.rol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("Rol inválido: " + request.rol() + ". Usar USER, LIBRARIAN o ADMIN");
        }

        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/usuarios/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @Valid @RequestBody UpdateEstadoRequest request) {
        var usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        EstadoUsuario nuevoEstado;
        try {
            nuevoEstado = EstadoUsuario.valueOf(request.estado().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("Estado inválido: " + request.estado() + ". Usar ACTIVO o BLOQUEADO");
        }

        usuario.setEstado(nuevoEstado);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/multas")
    public ResponseEntity<List<?>> listarMultas() {
        var multas = multaRepository.findAllWithPrestamoAndUsuario();
        return ResponseEntity.ok(multas);
    }

    @PutMapping("/multas/{id}/pagar")
    public ResponseEntity<?> pagarMulta(@PathVariable Long id) {
        var multa = multaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));

        if (multa.getEstado() == EstadoMulta.PAGADO) {
            throw new OperacionInvalidaException("La multa ya fue pagada");
        }

        multa.setEstado(EstadoMulta.PAGADO);
        multaRepository.save(multa);
        return ResponseEntity.ok(multa);
    }
}
