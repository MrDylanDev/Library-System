package com.libromagico.controller;

import com.libromagico.dto.PrestamoRequest;
import com.libromagico.model.Prestamo;
import com.libromagico.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @GetMapping
    public ResponseEntity<Page<Prestamo>> listarTodos(@PageableDefault(sort = "id", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {
        return ResponseEntity.ok(prestamoService.listarTodos(pageable));
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
    public ResponseEntity<Page<Prestamo>> historial(@PathVariable Long usuarioId, @PageableDefault(sort = "id", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {
        return ResponseEntity.ok(prestamoService.historialPorUsuario(usuarioId, pageable));
    }
}
