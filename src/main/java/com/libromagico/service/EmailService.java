package com.libromagico.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class EmailService {

    public void notificarDevolucionTardia(String email, String tituloLibro, BigDecimal monto) {
        log.info("Email enviado a {}: Devolución tardía del libro '{}'. Multa: ${}",
                email, tituloLibro, monto);
    }
}
