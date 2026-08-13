# Contribuir a LibroMágico

Los cambios se entregan mediante el flujo **issue aprobado → rama → pull request**, con CI obligatorio y una única etiqueta `type:*`. Un PR que no siga este flujo no podrá mergearse.

## Flujo completo

1. **Buscar o crear un issue.** Abrir uno nuevo con el formato del repositorio: **Problema**, **Reproducción**, **Resultado esperado** y **Ambiente**. Los issues de mejora (gaps) llevan una etiqueta `type:*` (por ejemplo `type:docs`).

2. **Esperar la aprobación del issue.** No se trabaja sobre un issue hasta que tenga la etiqueta `status:approved`.

3. **Crear la rama desde `main`.** Convención de nombres: `type/descripcion`, donde `type` es uno de `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `revert`.

   ```
   ^(feat|fix|chore|docs|style|refactor|perf|test|build|ci|revert)\/[a-z0-9._-]+$
   ```

   Ejemplos: `feat/catalog-pagination`, `fix/loan-status`, `docs/readme`.

   ```bash
   git checkout -b docs/readme main
   ```

4. **Escribir los commits en formato Conventional Commits:** `tipo(alcance): descripción` (el alcance es opcional). Por ejemplo:

   ```
   docs(readme): add project quickstart and contributing guide
   ```

   Reglas:

   - `tipo` ∈ {`feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `revert`}; `!` al final indica breaking change.
   - NO se agregan trailers de atribución como `Co-Authored-By` ni menciones de IA.

5. **Subir la rama y abrir el pull request** hacia `main`:

   - El cuerpo del PR debe incluir `Closes #N` (o `Fixes #N` / `Resolves #N`) referenciando el issue aprobado.
   - Agregar **exactamente una** etiqueta `type:*` según la tabla de mapeo:

   | Tipo de cambio | Rama | Label del PR |
   |---|---|---|
   | `feat` (funcionalidad) | `feat/...` | `type:feature` |
   | `perf` (rendimiento) | `perf/...` | `type:feature` |
   | `fix` (corrección) | `fix/...` | `type:bug` |
   | `revert` (revertir cambio) | `revert/...` | `type:bug` |
   | `docs` (documentación) | `docs/...` | `type:docs` |
   | `refactor` (reestructurar) | `refactor/...` | `type:refactor` |
   | `chore`, `test`, `build`, `ci`, `style` | Ej. `chore/...` | `type:chore` |
   | `feat!` / `fix!` (breaking change) | `feat/...` / `fix/...` | `type:breaking-change` |

   > Nota: el repositorio tiene actualmente las etiquetas `type:feature`, `type:bug`, `type:chore`, `type:docs` y `type:refactor`. La etiqueta `type:breaking-change` todavía no existe: ante un cambio con breaking change, coordinar la etiqueta con los mantenedores.

6. **Esperar que el CI quede verde** (los 3 jobs: Backend tests, E2E, Backup/Restore). Un approve puede mergear cuando el CI pasa.

## Checklist del PR

- [ ] Vincula un issue aprobado (`status:approved`) con `Closes #N`, `Fixes #N` o `Resolves #N`.
- [ ] Tiene exactamente una etiqueta `type:*`.
- [ ] Los commits respetan Conventional Commits (`tipo(alcance): descripción`).
- [ ] La documentación está actualizada si el cambio modifica comportamiento.
- [ ] Sin trailers de atribución (`Co-Authored-By`, etc.).

## Estándares de calidad

- **Backend**: la suite de tests debe pasar y la cobertura no puede bajar del 70 % de instrucciones (JaCoCo).
- **Frontend / UI**: los cambios de UI deben dejar la suite E2E en verde.
- **Idioma**: la interfaz y los mensajes de la aplicación están en español (neutro); mantenerlo en los cambios.
- **Seguridad**: no se suben secretos al repositorio ni se loguean contraseñas o tokens en el código.

## Comandos útiles

| Comando | Uso |
|---|---|
| `./mvnw test` | Tests de backend |
| `./mvnw -Pcoverage verify` | Tests + reporte y barrera de cobertura JaCoCo (≥ 70 %) |
| `cd tests/e2e && python -m pytest` | Suite E2E (requiere el stack dev levantado) |
| `docker compose down -v && docker compose up -d --build` | Reset del stack E2E (datos frescos entre corridas) |

Instalación inicial de la suite E2E (una sola vez):

```bash
cd tests/e2e
pip install -r requirements.txt
python -m playwright install chromium --with-deps
```