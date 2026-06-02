package com.libromagico.service;

import com.libromagico.config.PrestamoConfig;
import com.libromagico.model.Prestamo;
import com.libromagico.repository.MultaRepository;
import com.libromagico.repository.PrestamoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {
    @Mock
    private PrestamoRepository prestamoRepository;
    
    @Mock
    private MultaRepository multaRepository;
    
    @Mock
    private PrestamoConfig config;
    
    @InjectMocks
    private PrestamoService prestamoService;
    
    @Test
    void registrarDevolucion_Tardia_GeneraMulta() {
        // Configurar préstamo con fecha_devolucion = ayer
        Prestamo prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setFechaDevolucion(LocalDate.now().minusDays(1));
        
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(config.getMultaMonto()).thenReturn(new BigDecimal("10.00"));
        when(multaRepository.existsByPrestamoId(1L)).thenReturn(false);
        
        // Ejecutar
        prestamoService.registrarDevolucion(1L);
        
        // Verificar
        verify(multaRepository, times(1)).save(any());
    }
}