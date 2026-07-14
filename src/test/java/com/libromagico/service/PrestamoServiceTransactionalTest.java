package com.libromagico.service;

import com.libromagico.model.*;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testtx-libromagico;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class PrestamoServiceTransactionalTest {

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @SpyBean
    private PrestamoRepository prestamoRepository;

    @Test
    @DisplayName("prestar() rolls back all writes when save fails mid-method")
    void prestar_rollsBackOnFailure() {
        var user = new Usuario();
        user.setNombre("Test User");
        user.setEmail("txuser@test.com");
        user.setContrasena("pass123");
        user.setDni("87654321");
        user.setTelefono("+5491111111111");
        usuarioRepository.save(user);

        var libro = new Libro();
        libro.setIsbn("9780201633610");
        libro.setTitulo("Design Patterns");
        libro.setAutor("Gang of Four");
        libro.setCopiasDisponibles(3);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        doThrow(new RuntimeException("DB failure"))
                .when(prestamoRepository)
                .save(any(Prestamo.class));

        assertThrows(RuntimeException.class,
                () -> prestamoService.prestar(user.getId(), libro.getIsbn()));

        var libroActual = libroRepository.findById(libro.getIsbn()).orElseThrow();
        assertEquals(3, libroActual.getCopiasDisponibles(),
                "Libro copias should be unchanged after rollback");
    }
}
