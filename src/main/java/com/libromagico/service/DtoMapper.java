package com.libromagico.service;

import com.libromagico.dto.LibroResponse;
import com.libromagico.dto.MultaAdminResponse;
import com.libromagico.dto.PrestamoResponse;
import com.libromagico.dto.UsuarioResponse;
import com.libromagico.model.Libro;
import com.libromagico.model.Multa;
import com.libromagico.model.Prestamo;
import com.libromagico.model.Usuario;

import java.util.function.Function;

/**
 * Mapea entidades JPA a DTOs de respuesta.
 *
 * <p>Los controllers no deben exponer entidades directamente: serializan
 * campos internos (por ejemplo el hash de contraseña anidado en un
 * {@code Prestamo}). Este mapeo centraliza la conversión y garantiza que solo
 * los campos públicos llegan al cliente.</p>
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static <T, R> Function<T, R> mapper(Function<T, R> fn) {
        return fn;
    }

    public static LibroResponse from(Libro libro) {
        if (libro == null) {
            return null;
        }
        return new LibroResponse(
                libro.getIsbn(),
                libro.getTitulo(),
                libro.getAutor(),
                libro.getCategoria(),
                libro.getAñoPub(),
                libro.getEditorial(),
                libro.getCopiasDisponibles(),
                libro.getEstado());
    }

    public static UsuarioResponse from(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getDni(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getEstado());
    }

    public static PrestamoResponse from(Prestamo prestamo) {
        if (prestamo == null) {
            return null;
        }
        return new PrestamoResponse(
                prestamo.getId(),
                from(prestamo.getUsuario()),
                from(prestamo.getLibro()),
                prestamo.getFechaPrestamo(),
                prestamo.getFechaDevolucion(),
                prestamo.getFechaEntregaReal(),
                prestamo.getEstado());
    }

    public static MultaAdminResponse from(Multa multa) {
        if (multa == null) {
            return null;
        }
        return new MultaAdminResponse(
                multa.getId(),
                multa.getMonto(),
                multa.getEstado(),
                from(multa.getPrestamo()));
    }
}
