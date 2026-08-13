# Seguridad y Protección de Datos — LibroMágico

**Proyecto:** LibroMágico
**Alcance:** Modelo de autorización (RBAC), tratamiento de datos personales y cumplimiento normativo.

Esta sección documenta cómo LibroMágico controla el acceso a sus recursos y cómo trata los datos personales de sus usuarios. Es documentación técnica del proyecto: **no constituye asesoría legal ni un documento normativo vinculante** (ver el aviso completo en [data-protection.md](data-protection.md)).

## Qué cubre esta sección

| Tema | Documento | Para quién |
|---|---|---|
| **RBAC y autorización** — matriz de roles (USER/LIBRARIAN/ADMIN), reglas de acceso por endpoint, control por ownership, cómo se implementa | [rbac.md](rbac.md) | Backend, contribuyentes, revisores de seguridad |
| **Protección de datos personales** — qué datos se recolectan y quién los ve, medidas técnicas de seguridad, estado frente a la Ley 1581 de 2012 (Habeas Data, Colombia) | [data-protection.md](data-protection.md) | Equipo, DPO/legal, operaciones |

## Qué NO cubre esta sección

- No es una política de privacidad para usuarios finales (eso es un documento legal que el proyecto no tiene aún; ver "Brechas para cumplimiento completo" en [data-protection.md](data-protection.md)).
- No es asesoría legal ni un análisis de cumplimiento jurídicamente verificable.
- El modelo de datos completo y los flujos de negocio viven en la [documentación de arquitectura](../architecture/README.md).

## Puntos de partida rápidos

1. **¿Puede un rol hacer X?** → abre la [matriz RBAC](rbac.md#matriz-rbac).
2. **¿Quién puede ver un dato personal?** → tabla de datos en [data-protection.md](data-protection.md#datos-personales-que-procesa-el-sistema).
3. **¿Agrego un endpoint nuevo?** → [Guía de cambios](rbac.md#guia-de-cambios) explica dónde se define el acceso.

> **Nota:** este documento describe el comportamiento del código en `main`. Si la configuración de seguridad cambia (p. ej. nuevas reglas en `SecurityConfig`), actualiza estos documentos en el mismo cambio.
