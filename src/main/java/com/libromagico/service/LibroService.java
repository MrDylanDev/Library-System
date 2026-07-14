package com.libromagico.service;

import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoLibro;
import com.libromagico.model.EstadoPrestamo;
import com.libromagico.model.Libro;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;

    public List<Libro> listarTodos() {
        return libroRepository.findAll();
    }

    public Libro buscarPorIsbn(String isbn) {
        return libroRepository.findById(isbn)
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado: " + isbn));
    }

    public List<Libro> buscarPorAutor(String autor) {
        return libroRepository.findByAutorContainingIgnoreCase(autor);
    }

    public List<Libro> buscarPorTitulo(String titulo) {
        return libroRepository.findByTituloContainingIgnoreCase(titulo);
    }

    public List<Libro> buscarGeneral(String q) {
        if (q == null || q.isBlank()) return listarTodos();
        return libroRepository.buscarGeneral(q.trim());
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
