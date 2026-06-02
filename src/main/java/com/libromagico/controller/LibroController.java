package com.libromagico.controller;

import com.libromagico.model.Libro;
import com.libromagico.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class LibroController {
    private final LibroRepository libroRepository;
    
    @GetMapping
    public ResponseEntity<List<Libro>> getAllLibros() {
        return ResponseEntity.ok(libroRepository.findAll());
    }
    
    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Libro> createLibro(@RequestBody Libro libro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroRepository.save(libro));
    }
}