package com.libromagico.controller;

import com.libromagico.dto.PrestamoRequest;
import com.libromagico.exception.AccesoDenegadoException;
import com.libromagico.exception.RecursoNoEncontradoException;
import com.libromagico.model.Prestamo;
import com.libromagico.model.RolUsuario;
import com.libromagico.model.Usuario;
import com.libromagico.repository.UsuarioRepository;
import com.libromagico.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<Page<Prestamo>> listarTodos(@PageableDefault(sort = "id", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {
        return ResponseEntity.ok(prestamoService.listarTodos(pageable));
    }

    @PostMapping
    public ResponseEntity<Prestamo> prestar(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody PrestamoRequest request) {
        var usuario = usuarioAutenticado(userDetails);
        if (usuario.getRol() == RolUsuario.USER && !request.usuarioId().equals(usuario.getId())) {
            throw new AccesoDenegadoException("Solo puedes solicitar préstamos para tu propia cuenta");
        }

        var prestamo = prestamoService.prestar(request.usuarioId(), request.libroIsbn());
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamo);
    }

    @PutMapping("/{id}/devolucion")
    public ResponseEntity<Prestamo> devolver(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        var usuario = usuarioAutenticado(userDetails);
        if (usuario.getRol() == RolUsuario.USER) {
            var prestamo = prestamoService.buscarPorId(id);
            if (!prestamo.getUsuario().getId().equals(usuario.getId())) {
                throw new AccesoDenegadoException("No puedes devolver un préstamo que no te pertenece");
            }
        }

        return ResponseEntity.ok(prestamoService.devolver(id));
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<Page<Prestamo>> historial(@AuthenticationPrincipal UserDetails userDetails,
                                                    @PathVariable Long usuarioId,
                                                    @PageableDefault(sort = "id", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {
        var usuario = usuarioAutenticado(userDetails);
        if (usuario.getRol() == RolUsuario.USER && !usuarioId.equals(usuario.getId())) {
            throw new AccesoDenegadoException("Solo puedes ver tu propio historial de préstamos");
        }

        return ResponseEntity.ok(prestamoService.historialPorUsuario(usuarioId, pageable));
    }

    private Usuario usuarioAutenticado(UserDetails userDetails) {
        return usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + userDetails.getUsername()));
    }
}
