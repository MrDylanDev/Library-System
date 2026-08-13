# Estrategia de Pruebas — LibroMágico

**Proyecto:** LibroMágico
**Alcance:** Pirámide de testing, cobertura y gate de CI, comandos y requisitos de entorno.

## Resumen

LibroMágico se prueba con una **pirámide de tres capas**: pruebas unitarias de servicios (rápidas), pruebas de integración con MockMvc sobre H2 (matrices de autorización, seguridad, controllers) y pruebas **E2E con Python + Playwright** contra el stack real en Docker (Postgres). La barrera de calidad se aplica en CI con **JaCoCo ≥ 70% de cobertura por instrucciones** mediante el perfil `coverage`.

```
        E2E (Playwright, Docker+Postgres)     ← pocos, lentos, de extremo a extremo
       /                                       /
  Integración (MockMvc, H2+Flyway)   ← autorización, seguridad, controllers
       /
  Unitarias (servicios, mocks)      ← lógica de negocio aislada
```

| Capa | Velocidad | Qué valida | Cuándo corre |
|---|---|---|---|
| Unit | ms por test | Lógica de servicios con mocks | `./mvnw test` |
| Integración | s por suite | RBAC, seguridad, controllers, flujos con BD | `./mvnw test` |
| E2E | min por suite | Flujo completo contra stack real | `python -m pytest` (manual o CI) |

## Capa unitaria

- **Framework:** JUnit 5, mocks de dependencias (Mockito), sin contexto Spring ni BD.
- **Qué cubre:** la lógica pura de negocio de los servicios. Ejemplos reales: `PrestamoServiceUnitTest` (reglas de préstamo, devolución, multa) y `DtoValidationTest` (validaciones de entrada: DNI de 8 dígitos, formato de email, campos obligatorios).
- **Objetivo:** decidir en milisegundos si un cambio rompió una regla de negocio, aislado de infraestructura.

## Capa de integración

- **Framework:** `@SpringBootTest` + `@AutoConfigureMockMvc`, base de datos **H2** con migraciones **Flyway** (mismo esquema versionado que prod).
- **Qué cubre:**
  - **Matrices RBAC:** `AuthorizationIntegrationTest` recorre los pares (recurso × rol) y verifica `200/401/403` esperados (ver la [matriz RBAC](../security/rbac.md#matriz-rbac)).
  - **Seguridad:** rate limiting (429), revocación de tokens (logout, reset de contraseña), headers de seguridad (CSP), CSRF.
  - **Controllers:** contratos de endpoints y flujos de negocio completos contra BD real (H2).
  - Datos: `@DataJpaTest` para la capa de repositorio.
- **Objetivo:** validar que las reglas de seguridad y los contratos HTTP se cumplen con el contexto real de Spring y el esquema de BD.

## Capa E2E

- **Stack:** Python + **Playwright** (Chromium) contra el stack **Docker real** (app Spring Boot + **PostgreSQL** 16).
- **Alcance:** 9 archivos de tests (`test_01` … `test_09`) con **66 tests** que ejercitan la app como lo haría un usuario real en el navegador.
- **Característica clave:** los E2E **mutan la base de datos** (crean usuarios, libros, préstamos, multas). Por eso entre ejecuciones hay que **bajar el stack y borrar los volúmenes** (`docker compose down -v`) para partir de un estado limpio.

### Cobertura por archivo de test

| Archivo | Qué cubre |
|---|---|
| `test_01_*` | Autenticación: login, registro, logout, recuperación de contraseña |
| `test_02_*` | Catálogo: búsqueda, detalle de libro, préstamo desde catálogo |
| `test_03_*` | Préstamos: flujo completo (crear, listar, devolver) |
| `test_04_*` | Multas: pago como usuario y como admin |
| `test_05_*` | Administración: dashboard, CRUD de libros, toggles de rol/estado |
| `test_06_*` | Navegación y flujos generales de la SPA |
| `test_07_*` | Conflictos: préstamo duplicado, ISBN duplicado |
| `test_08_*` | Paginación del catálogo |
| `test_09_*` | Visibilidad de acciones de administración en la vista de detalle |

> El contenido de cada archivo puede variar; lo relevante es que los 66 tests en conjunto cubren los flujos listados. La suite es **mutante** y **no aislada entre sí** por diseño: requiere el reset de Docker entre runs.

## Cobertura y gate de CI

- **Herramienta:** JaCoCo, activado con el perfil Maven `coverage`.
- **Barrera:** ≥ **70% de cobertura por instrucciones**, verificada con `./mvnw -Pcoverage verify`. Si baja del umbral, el build **falla**.
- **Gate en CI:** el pipeline (`.github/workflows/ci.yml`) ejecuta `mvn -B -Pcoverage verify` — la barrera no es solo local, **defiende en cada PR** — y publica el reporte como artefacto.
- **E2E en CI:** la suite Playwright corre contra el stack Docker del CI (incluye reset de volumen entre jobs cuando corresponde).

## Comandos y requisitos de entorno

### Unitarias + integración (backend)

```bash
./mvnw test
```

Cobertura con barrera JaCoCo:

```bash
./mvnw -Pcoverage verify
```

- **Requisitos:** JDK 17+, Maven Wrapper (`./mvnw`) ya incluido. No necesita Docker ni BD externa (usa H2 en memoria + Flyway).

### E2E (Python + Playwright)

```bash
# 1. Levantar el stack real (app + Postgres)
docker compose up -d

# 2. Dependencias de Python y navegador de Playwright
pip install -r requirements.txt
python -m playwright install chromium --with-deps

# 3. Correr la suite
python -m pytest
```

**IMPORTANTE — reset entre runs:** los tests mutan la base. Antes de cada ejecución:

```bash
docker compose down -v
docker compose up -d
```

- **Requisitos:** Docker Compose, Python 3, la app construida/imagen disponible, navegador Chromium instalado por Playwright.
- **Configuración:** las URLs y credenciales del entorno E2E se toman del stack local (ver README del proyecto para el detalle de arranque).

## Qué se prueba en cada PR (regla práctica)

1. Un cambio en **lógica de negocio** exige cobertura unitaria nueva.
2. Un cambio en **rutas o autorización** exige casos en `AuthorizationIntegrationTest` (y actualizar [critical-cases.md](critical-cases.md)).
3. Un cambio de **flujo visible al usuario** exige ajustar/agregar un E2E.
4. Ningún cambio puede hacer bajar la cobertura por debajo de 70% (gate de CI).
