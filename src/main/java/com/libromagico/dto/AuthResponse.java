package com.libromagico.dto;

public record AuthResponse(String token, Long id, String email, String rol) {}
