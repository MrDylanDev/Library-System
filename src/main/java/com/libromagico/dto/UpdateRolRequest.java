package com.libromagico.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRolRequest(@NotNull String rol) {}
