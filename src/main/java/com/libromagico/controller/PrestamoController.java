package com.libromagico.controller;

import com.libromagico.dto.PrestamoRequest;
import com.libromagico.model.Prestamo;
import com.libromagico.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @GetMapping
    public ResponseEntity<List<Prestamo>> listarTodos() {
        return ResponseEntity.ok(prestamoService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Prestamo> prestar(@Valid @RequestBody PrestamoRequest request) {
        var prestamo = prestamoService.prestar(request.usuarioId(), request.libroIsbn());
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamo);
    }

    @PutMapping("/{id}/devolucion")
    public ResponseEntity<Prestamo> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolver(id));
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<Prestamo>> historial(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(prestamoService.historialPorUsuario(usuarioId));
    }
}
