package com.libromagico.controller;

import com.libromagico.model.Libro;
import com.libromagico.service.LibroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogController {

    private final LibroService libroService;

    @GetMapping
    public ResponseEntity<List<Libro>> listarTodos() {
        return ResponseEntity.ok(libroService.listarTodos());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Libro>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(libroService.buscarGeneral(q));
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<Libro> detalle(@PathVariable String isbn) {
        return ResponseEntity.ok(libroService.buscarPorIsbn(isbn));
    }
}
