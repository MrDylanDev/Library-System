package com.libromagico.config;

import com.libromagico.model.Libro;
import com.libromagico.model.EstadoLibro;
import com.libromagico.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final LibroRepository libroRepository;

    @Override
    public void run(String... args) {
        if (libroRepository.count() > 0) {
            log.info("Datos de prueba ya existen, omitiendo seed");
            return;
        }

        var libros = List.of(
                createLibro("9780132350884", "Clean Code", "Robert C. Martin", "Software", 2008, "Prentice Hall"),
                createLibro("9780201633610", "Design Patterns", "Gang of Four", "Software", 1994, "Addison-Wesley"),
                createLibro("9780134685991", "Effective Java", "Joshua Bloch", "Software", 2018, "Addison-Wesley"),
                createLibro("9781492052203", "Kubernetes: Up and Running", "Kelsey Hightower", "DevOps", 2019, "O'Reilly"),
                createLibro("9781492090717", "Fluent Python", "Luciano Ramalho", "Software", 2022, "O'Reilly"),
                createLibro("9780134494166", "Clean Architecture", "Robert C. Martin", "Software", 2017, "Prentice Hall"),
                createLibro("9781617296208", "Spring in Action", "Craig Walls", "Software", 2022, "Manning"),
                createLibro("9781449355739", "Learning Python", "Mark Lutz", "Software", 2013, "O'Reilly")
        );

        libroRepository.saveAll(libros);
        log.info("Seed completado: {} libros insertados", libros.size());
    }

    private Libro createLibro(String isbn, String titulo, String autor, String categoria, int añoPub, String editorial) {
        var libro = new Libro();
        libro.setIsbn(isbn);
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setCategoria(categoria);
        libro.setAñoPub(añoPub);
        libro.setEditorial(editorial);
        libro.setCopiasDisponibles(3);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        return libro;
    }
}
