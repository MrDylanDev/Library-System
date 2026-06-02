package com.libromagico.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "prestamo")
@Getter
@Setter
public class PrestamoConfig {
    private int dias = 15;
    private BigDecimal multaMonto = new BigDecimal("10.00");
}