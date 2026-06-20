package com.libromagico.controller;

import com.libromagico.model.Libro;
import com.libromagico.service.LibroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    @GetMapping
    public ResponseEntity<List<Libro>> listarTodos() {
        return ResponseEntity.ok(libroService.listarTodos());
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<Libro> buscarPorIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(libroService.buscarPorIsbn(isbn));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Libro>> buscar(
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String titulo) {
        if (autor != null) {
            return ResponseEntity.ok(libroService.buscarPorAutor(autor));
        }
        if (titulo != null) {
            return ResponseEntity.ok(libroService.buscarPorTitulo(titulo));
        }
        return ResponseEntity.ok(libroService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<Libro> crear(@Valid @RequestBody Libro libro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crear(libro));
    }

    @PutMapping("/{isbn}")
    public ResponseEntity<Libro> actualizar(@PathVariable String isbn, @Valid @RequestBody Libro datos) {
        return ResponseEntity.ok(libroService.actualizar(isbn, datos));
    }

    @DeleteMapping("/{isbn}")
    public ResponseEntity<Void> eliminar(@PathVariable String isbn) {
        libroService.eliminar(isbn);
        return ResponseEntity.noContent().build();
    }
}
