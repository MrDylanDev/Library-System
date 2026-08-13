# Roadmap — LibroMágico

Hoja de ruta del producto por fases. El **MVP está entregado y operativo** en producción hoy; las fases restantes son propuestas a futuro y pueden ajustarse según feedback y prioridades del negocio.

| Fase | Estado |
|---|---|
| [MVP](#fase-mvp-entregado) | ✅ **ENTREGADO — operativo hoy** |
| [Fase 1](#fase-1-siguiente) | Propuesta |
| [Fase 2](#fase-2-mm) | Propuesta |
| [Fase 3](#fase-3-futuro) | Propuesta |

## Fase MVP (ENTREGADO) ✅

**Objetivo**: cubrir el ciclo operativo completo de una biblioteca pequeña/mediana con autoservicio y control administrativo.

Está **en producción hoy**: se despliega con Docker Compose (`docker-compose.prod.yml`), los entornos de desarrollo y producción están documentados en el [README](../../README.md) y existe backup diario con restore probado (`docs/ops/backup-restore.md`).

| Feature | Evidencia en el repo |
|---|---|
| Autenticación JWT (cookie `httpOnly`), registro, login, logout, CSRF y rate limiting | README (Características); backend en `src/main/java/com/libromagico/security/`; tests E2E `tests/e2e/test_01_auth.py` |
| Catálogo público con búsqueda multi-campo y paginación | README; SPA `static/js/components/catalog.js`; E2E `test_02_catalog.py` y `test_08_pagination.py` |
| Préstamos (15 días) y devoluciones con detección de atraso | README; E2E `test_03_loans.py`; estados ATRASADO vía scheduler horario |
| Multas planas de $10 con pago (autoservicio y admin) | README; E2E `test_04_multas.py` y `test_07_conflicts.py` |
| Administración: usuarios (rol/estado), libros (CRUD + Perdido), préstamos y multas | E2E `test_05_admin.py` y `test_09_admin_detail.py` |
| Dashboard con estadísticas | README (Panel de administración); SPA `static/js/components/admin.js` |
| Seguridad: CSP, denylist JWT, Habeas Data | README (Características) |
| Recuperación de contraseña por email | README (Emails transaccionales) |
| CI/CD: tests, E2E y backup/restore | `.github/workflows/ci.yml` (3 jobs: Maven + E2E + Backup/Restore) |
| Cobertura ≥ 70 % (barrera JaCoCo) | README (Ejecutar los tests) |
| Backups diarios (RPO 24 h, RTO < 30 min) | `docs/ops/backup-restore.md` |

**Valor de negocio**: eliminación del proceso manual, visibilidad total del catálogo y control automático de morosidad.

## Fase 1 (siguiente)

**Objetivo**: expandir la experiencia del lector y la capacidad de análisis del staff.

| Feature | Descripción |
|---|---|
| Reservas de libros | Reserva de un libro prestado y asignación al liberarse; requiere flujo nuevo, hoy el estado `RESERVADO` existe solo como valor del enum sin funcionalidad |
| Historial detallado | Registro histórico de préstamos, devoluciones y multas por usuario y por libro |
| Notificaciones por email de vencimiento | Aviso automático antes del vencimiento y al quedar atrasado |
| Reportes y exportación CSV | Exportación de catálogo, préstamos y multas para análisis y control |

**Dependencias**: el historial y las notificaciones requieren extender el modelo de datos; la exportación depende de la funcionalidad de reportes.

**Valor de negocio**: reduce la morosidad (avisos proactivos), mejora la retención de lectores (reservas) y da al staff datos accionables.

## Fase 2 (mm)

**Objetivo**: escalar el modelo de negocio y la recaudación.

| Feature | Descripción |
|---|---|
| Multas progresivas por día | Reemplaza la multa plana de $10 por acumulación diaria hasta un tope configurable |
| Pasarela de pago real | Cobro efectivo de multas (hoy el pago es registro interno, sin dinero real) |
| App móvil / PWA | Acceso desde móvil con la misma experiencia de la SPA |
| Multi-sucursal | Soporte para varias sedes con inventario y reportes por sucursal |
| Importación masiva de libros | Alta de catálogo desde archivos (hoy el alta es libro por libro) |

**Dependencias**: la pasarela de pago requiere decisión de proveedor y cumplimiento local; multi-sucursal es un cambio estructural en el modelo de datos.

**Valor de negocio**: recaudación real, alcance móvil y crecimiento a redes de bibliotecas.

## Fase 3 (futuro)

**Objetivo**: convertir LibroMágico en una comunidad de lectura con ecosistema abierto.

| Feature | Descripción |
|---|---|
| Recomendaciones | Sugerencias de lectura basadas en historial y preferencias |
| Comunidad | Reseñas, valoraciones y listas de lectura por usuarios |
| API pública | Exposición documentada del catálogo para integraciones externas |

**Dependencias**: construye sobre el historial detallado (Fase 1) y requiere diseño de API pública y modelo de moderación.

**Valor de negocio**: diferenciación, engagement de lectores y apertura a integraciones de terceros.

## Resumen de priorización

1. **MVP** — operativo: base sólida, en producción y con calidad garantizada (CI + cobertura + backups).
2. **Fase 1** — mejoras de retención y análisis, bajo riesgo, alto valor.
3. **Fase 2** — crecimiento y monetización, con dependencias externas (pagos) y estructurales (multi-sucursal).
4. **Fase 3** — visión de largo plazo, depende de las fases previas.

> Todas las fases posteriores al MVP son **propuestas** y deben validarse con el negocio antes de comprometer esfuerzo.