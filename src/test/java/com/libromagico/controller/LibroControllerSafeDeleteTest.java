package com.libromagico.controller;

import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.service.LibroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LibroControllerSafeDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LibroService libroService;

    @Test
    @DisplayName("DELETE /api/libros/{isbn} returns 409 when book has active loans")
    void eliminar_returns409_whenActiveLoansExist() throws Exception {
        doThrow(new OperacionInvalidaException("No se puede eliminar el libro porque tiene préstamos activos"))
                .when(libroService).eliminar("9780132350884");

        mockMvc.perform(delete("/api/libros/9780132350884"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No se puede eliminar el libro porque tiene préstamos activos"));
    }

    @Test
    @DisplayName("DELETE /api/libros/{isbn} returns 204 when book has no active loans")
    void eliminar_returns204_whenNoActiveLoans() throws Exception {
        doNothing().when(libroService).eliminar("9780201633610");

        mockMvc.perform(delete("/api/libros/9780201633610"))
                .andExpect(status().isNoContent());
    }
}
