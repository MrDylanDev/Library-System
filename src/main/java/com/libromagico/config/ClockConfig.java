package com.libromagico.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuración del reloj inyectable del sistema.
 *
 * <p>Vive en una clase propia (y no dentro de {@code SecurityConfig}) para
 * evitar una dependencia circular: {@code SecurityConfig} depende del filtro
 * de rate limiting, que depende de {@code AuthRateLimiter}, que depende de
 * {@code Clock}. Algunos tests inyectan un {@code Clock} fijo para controlar
 * el tiempo.</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}