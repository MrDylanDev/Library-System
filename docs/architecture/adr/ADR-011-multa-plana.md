# ADR-011: Multa plana de devolución tardía

- **Estado:** Aceptado
- **Fecha estimada:** 2025-Q3
- **Relacionados:** [ADR-001](ADR-001-monolito-spring-boot.md)

## Contexto

Una devolución fuera de plazo debe tener una consecuencia económica para incentivar la devolución a tiempo. El sistema gestiona el vencimiento marcando los préstamos como `ATRASADO` de forma programada (sin acumular deuda durante el retraso), así que la multa se decide en el momento real de la devolución.

## Decisión

Aplicar una **multa plana** al devolver tarde:

- Importe fijo de **$10** (`prestamo.multa.monto`), **configurable** en `PrestamoConfig` — no acumula por día.
- La multa (estado `PENDIENTE`) se genera **solo cuando la devolución real supera la fecha de devolución** del préstamo.
- El estado `ATRASADO` del préstamo **no genera multa progresiva**: un préstamo que sigue atrasado no incrementa su deuda.
- La multa queda asociada al préstamo (relación `prestamos` 1—* `multas`), con monto `DECIMAL(10,2)`.
- El pago se gestiona por el propio usuario (self-service) o por el administrador — ver [business-flows.md](../business-flows.md).

## Alternativas consideradas

- **Multa diaria proporcional:** penaliza mejor el retraso prolongado, pero es más compleja de calcular, de configurar y de comunicar al usuario; descartada por simplicidad.
- **Suspensión de cuenta (bloqueo de nuevos préstamos):** complementa la multa como medida, pero no sustituye la penalización económica; no se adoptó como mecanismo principal.

## Consecuencias

- **Simple y predecible:** regla de un solo importe, fácil de explicar y de verificar en tests.
- **Seed de deuda de demostración:** la base de datos de ejemplo incluye multas pendientes para ejercitar el flujo de pago.
- **Configuración centralizada:** cambiar el importe es tocar una propiedad de `PrestamoConfig`, sin tocar lógica.
- **Alcance acotado:** la multa se genera exclusivamente en la devolución tardía, alineada con el flujo descrito en [business-flows.md](../business-flows.md).