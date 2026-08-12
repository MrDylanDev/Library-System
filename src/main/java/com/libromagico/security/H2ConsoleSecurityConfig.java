package com.libromagico.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuración exclusiva del perfil {@code dev} para la consola H2.
 *
 * <p>La consola H2 solo debe existir en desarrollo. En producción este bean no se
 * registra (perfil no activo), por lo que {@code /h2-console/**} cae en el chain
 * principal y exige autenticación — no existe ningún {@code permitAll} global para
 * ella. La consola necesita un chain propio porque usa frames y formularios sin
 * token CSRF; aquí se ignoran ambos matchers únicamente bajo el perfil dev.</p>
 *
 * <p>IMPORTANTE: se usa {@link AntPathRequestMatcher} (no {@code MvcRequestMatcher})
 * porque la consola H2 se registra como servlet, no como handler MVC, y
 * {@code MvcRequestMatcher} no matchea rutas de servlet — la petición caería al
 * chain principal y recibiría 401.</p>
 */
@Configuration
@Profile("dev")
public class H2ConsoleSecurityConfig {

    /** Matcher de servlet para la consola H2 (no usa el introspector MVC). */
    private static final AntPathRequestMatcher H2_CONSOLE = new AntPathRequestMatcher("/h2-console/**");

    /**
     * Chain dedicado que matchea primero (Order 1) solo cuando el perfil dev está
     * activo. En prod el bean no existe y la consola no queda expuesta.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(H2_CONSOLE)
            .authorizeHttpRequests(auth -> auth.requestMatchers(H2_CONSOLE).permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers(H2_CONSOLE))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}