package com.libromagico.dto;

public record DashboardResponse(
    long totalLibros,
    long librosDisponibles,
    long librosPrestados,
    long librosPerdidos,
    long totalUsuarios,
    long prestamosActivos,
    long prestamosAtrasados,
    long multasPendientes,
    long multasPagadas
) {}
