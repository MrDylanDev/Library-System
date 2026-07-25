package com.libromagico.service;

import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoLibro;
import com.libromagico.model.EstadoPrestamo;
import com.libromagico.model.Libro;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;

    public Page<Libro> listarTodos(Pageable pageable) {
        return libroRepository.findAll(pageable);
    }

    public Libro buscarPorIsbn(String isbn) {
        return libroRepository.findById(isbn)
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado: " + isbn));
    }

    public Page<Libro> buscarPorAutor(String autor, Pageable pageable) {
        return libroRepository.findByAutorContainingIgnoreCase(autor, pageable);
    }

    public Page<Libro> buscarPorTitulo(String titulo, Pageable pageable) {
        return libroRepository.findByTituloContainingIgnoreCase(titulo, pageable);
    }

    public Page<Libro> buscarGeneral(String q, Pageable pageable) {
        if (q == null || q.isBlank()) return listarTodos(pageable);
        return libroRepository.buscarGeneral(q.trim(), pageable);
    }

    public Libro crear(Libro libro) {
        libro.setEstado(EstadoLibro.DISPONIBLE);
        return libroRepository.save(libro);
    }

    public Libro actualizar(String isbn, Libro datos) {
        var libro = buscarPorIsbn(isbn);
        libro.setTitulo(datos.getTitulo());
        libro.setAutor(datos.getAutor());
        libro.setCategoria(datos.getCategoria());
        libro.setAñoPub(datos.getAñoPub());
        libro.setEditorial(datos.getEditorial());
        return libroRepository.save(libro);
    }

    public void eliminar(String isbn) {
        var libro = buscarPorIsbn(isbn);
        if (prestamoRepository.existsByLibroAndEstado(libro, EstadoPrestamo.ACTIVO)) {
            throw new OperacionInvalidaException(
                    "No se puede eliminar el libro porque tiene préstamos activos");
        }
        libroRepository.deleteById(isbn);
    }

    public void actualizarDisponibilidad(Libro libro, EstadoLibro estado, int deltaCopias) {
        libro.setEstado(estado);
        libro.setCopiasDisponibles(libro.getCopiasDisponibles() + deltaCopias);
        libroRepository.save(libro);
    }
}
