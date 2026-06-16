package com.libromagico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String nombre,
        @NotBlank @Email String email,
        @NotBlank String contrasena,
        @Pattern(regexp = "^[0-9]{8}$") String dni,
        @Pattern(regexp = "^\\+[0-9]{10,15}$") String telefono
) {}
