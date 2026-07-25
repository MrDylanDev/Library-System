package com.libromagico.controller;

import com.libromagico.model.Libro;
import com.libromagico.service.LibroService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogController {

    private final LibroService libroService;

    @GetMapping
    public ResponseEntity<Page<Libro>> listarTodos(@PageableDefault(sort = "titulo", size = 20) Pageable pageable) {
        return ResponseEntity.ok(libroService.listarTodos(pageable));
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<Libro>> buscar(@RequestParam String q, @PageableDefault(sort = "titulo", size = 20) Pageable pageable) {
        return ResponseEntity.ok(libroService.buscarGeneral(q, pageable));
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<Libro> detalle(@PathVariable String isbn) {
        return ResponseEntity.ok(libroService.buscarPorIsbn(isbn));
    }
}
