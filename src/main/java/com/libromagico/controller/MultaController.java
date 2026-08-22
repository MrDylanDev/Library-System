package com.libromagico.controller;

import com.libromagico.dto.MultaResponse;
import com.libromagico.exception.OperacionInvalidaException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoMulta;
import com.libromagico.repository.MultaRepository;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.MultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multas")
@RequiredArgsConstructor
public class MultaController {

    private final MultaService multaService;
    private final UsuarioRepository usuarioRepository;
    private final MultaRepository multaRepository;

    @GetMapping("/mis-multas")
    public ResponseEntity<Page<MultaResponse>> getMisMultas(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {
        var usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + userDetails.getUsername()));
        return ResponseEntity.ok(
                multaService.obtenerMultasPorUsuarioPaginado(usuario.getId(), EstadoMulta.PENDIENTE, pageable));
    }

    @PutMapping("/{id}/pagar")
    public ResponseEntity<?> pagarMulta(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + userDetails.getUsername()));

        var multa = multaRepository.findByIdWithPrestamoAndUsuario(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada: " + id));

        if (!multa.getPrestamo().getUsuario().getId().equals(usuario.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.of(HttpStatus.FORBIDDEN,
                            "No tenés permiso para pagar esta multa"));
        }

        try {
            multaService.pagarMulta(id);
        } catch (OperacionInvalidaException e) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of(HttpStatus.BAD_REQUEST, e.getMessage()));
        }

        var response = new MultaResponse(
                multa.getId(), multa.getMonto(), EstadoMulta.PAGADO,
                multa.getPrestamo().getId(),
                multa.getPrestamo().getLibro().getTitulo(),
                multa.getPrestamo().getLibro().getIsbn(),
                multa.getPrestamo().getFechaPrestamo(),
                multa.getPrestamo().getFechaDevolucion()
        );
        return ResponseEntity.ok(response);
    }
}
