package com.libromagico.service;

import com.libromagico.model.*;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresión: dos préstamos concurrentes sobre la última copia no deben
 * agotarla dos veces (double-booking). El alta serializa el decremento de
 * {@code copiasDisponibles} con bloqueo pesimista en la fila del libro.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testtx-concurrency;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class PrestamoConcurrencyTest {

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Test
    @DisplayName("prestar() concurrente sobre la última copia no hace double-booking")
    void prestar_concurrente_noHaceDoubleBooking() throws Exception {
        var user1 = crearUsuario("conc1@test.com");
        var user2 = crearUsuario("conc2@test.com");

        var libro = new Libro();
        libro.setIsbn("9780451524934");
        libro.setTitulo("1984");
        libro.setAutor("George Orwell");
        libro.setCopiasDisponibles(1);
        libro.setEstado(EstadoLibro.DISPONIBLE);
        libroRepository.save(libro);

        int N = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(N);
        AtomicInteger exito = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(N);
        try {
            for (var user : new Usuario[]{user1, user2}) {
                pool.submit(() -> {
                    try {
                        start.await();
                        prestamoService.prestar(user.getId(), libro.getIsbn());
                        exito.incrementAndGet();
                    } catch (Exception e) {
                        // Se espera que uno de los dos falle por falta de copias.
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(15, TimeUnit.SECONDS), "Los hilos no terminaron a tiempo");
        } finally {
            pool.shutdownNow();
        }

        // Con bloqueo pesimista, exactamente un préstamo debería lograrse.
        assertEquals(1, exito.get(), "Debe poder prestarse exactamente una copia");

        var libroActual = libroRepository.findById(libro.getIsbn()).orElseThrow();
        assertTrue(libroActual.getCopiasDisponibles() >= 0,
                "Las copias disponibles no deben quedar en negativo");
    }

    private Usuario crearUsuario(String email) {
        var user = new Usuario();
        user.setNombre("User " + email);
        user.setEmail(email);
        user.setContrasena("pass123");
        user.setDni(String.valueOf(10000000L + (Math.abs(email.hashCode()) % 89999999L)));
        user.setTelefono("+5491112223333");
        return usuarioRepository.save(user);
    }
}
