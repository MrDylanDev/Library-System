package com.libromagico.dto;

import com.libromagico.model.EstadoUsuario;
import com.libromagico.model.RolUsuario;

public record UsuarioResponse(
    Long id,
    String nombre,
    String email,
    String dni,
    String telefono,
    RolUsuario rol,
    EstadoUsuario estado
) {}
