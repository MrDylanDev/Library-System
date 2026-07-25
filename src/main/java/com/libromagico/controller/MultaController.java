package com.libromagico.controller;

import com.libromagico.dto.MultaResponse;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.EstadoMulta;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.MultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multas")
@RequiredArgsConstructor
public class MultaController {

    private final MultaService multaService;
    private final UsuarioRepository usuarioRepository;

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
}
