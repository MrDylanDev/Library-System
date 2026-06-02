package com.libromagico.controller;

import com.libromagico.dto.PrestamoDTO;
import com.libromagico.model.Prestamo;
import com.libromagico.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {
    private final PrestamoService prestamoService;
    
    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Prestamo> registrarPrestamo(@RequestBody PrestamoDTO dto) {
        Prestamo prestamo = prestamoService.registrarPrestamo(dto.getUsuarioId(), dto.getIsbn());
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamo);
    }
    
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Void> registrarDevolucion(@PathVariable Long id) {
        prestamoService.registrarDevolucion(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<List<Prestamo>> getHistorialPrestamos(@PathVariable Long id) {
        List<Prestamo> historial = prestamoService.getHistorialPrestamos(id);
        return ResponseEntity.ok(historial);
    }
}