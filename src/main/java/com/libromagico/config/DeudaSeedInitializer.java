package com.libromagico.config;

import com.libromagico.model.EstadoLibro;
import com.libromagico.model.EstadoPrestamo;
import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.Prestamo;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.LibroRepository;
import com.libromagico.repository.PrestamoRepository;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.MultaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@Profile("!test")
public class DeudaSeedInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;
    private final MultaService multaService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (prestamoRepository.count() > 0) {
            log.info("Préstamos de prueba ya existen, omitiendo seed de deudas");
            return;
        }

        var moroso = usuarioRepository.findByEmail("moroso@libromagico.com").orElseGet(() -> {
            var u = new Usuario();
            u.setNombre("Moroso");
            u.setEmail("moroso@libromagico.com");
            u.setContrasena(passwordEncoder.encode("moroso123"));
            u.setDni("00000004");
            u.setTelefono("+5491144444444");
            u.setRol(RolUsuario.USER);
            u.setEstado(EstadoUsuario.ACTIVO);
            var saved = usuarioRepository.save(u);
            log.info("Usuario moroso creado para seed de deudas");
            return saved;
        });

        // Préstamos devueltos con demora -> generan multa PENDIENTE
        var libros = List.of(
                libroRepository.findById("9781492090717"), // Fluent Python
                libroRepository.findById("9780134494166")  // Clean Architecture
        );

        var monto = java.math.BigDecimal.TEN;
        libros.stream().filter(l -> l.isPresent() && l.get().getEstado() == EstadoLibro.DISPONIBLE)
                .forEach(libro -> {
                    var prestamo = new Prestamo();
                    prestamo.setUsuario(moroso);
                    prestamo.setLibro(libro.get());
                    prestamo.setFechaPrestamo(LocalDate.now().minusDays(30));
                    prestamo.setFechaDevolucion(LocalDate.now().minusDays(16));
                    prestamo.setFechaEntregaReal(LocalDate.now().minusDays(10));
                    prestamo.setEstado(EstadoPrestamo.DEVUELTO);
                    var saved = prestamoRepository.save(prestamo);
                    multaService.crearMulta(saved, monto);
                });

        log.info("Seed de deudas completado: 2 multas PENDIENTE para moroso@libromagico.com");
    }
}
