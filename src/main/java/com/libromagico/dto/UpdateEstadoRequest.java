package com.libromagico.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateEstadoRequest(@NotNull String estado) {}
