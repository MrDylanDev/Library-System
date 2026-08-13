# ADR-001: Monolito Spring Boot que sirve API y SPA

- **Estado:** Aceptado
- **Fecha estimada:** 2024-Q2
- **Relacionados:** [ADR-009](ADR-009-sin-redis.md), [ADR-010](ADR-010-spa-vanilla-js.md)

## Contexto

LibroMágico es un sistema de gestión bibliotecaria desarrollado por un equipo pequeño. Necesita exponer una API REST para el catálogo y las operaciones de préstamo, y servir una interfaz web. El equipo quiere un despliegue simple, sin orquestación compleja ni múltiples piezas que versionar y operar por separado.

## Decisión

Usar un **monolito Spring Boot 3.2.5 (Java 17, Maven)**, artefacto único `com.libromagico:libromagico:0.1.0`, que sirve a la vez:

- la **API REST** bajo `/api/**`, y
- la **SPA** de la interfaz (ver [ADR-010](ADR-010-spa-vanilla-js.md)) desde el mismo contexto de aplicación (`/`, `/index.html`, `/css/**`, `/js/**`).

Todo el código vive en el paquete raíz `com.libromagico`. Existe un único `.jar` desplegable con su propio `Dockerfile` multi-etapa.

## Alternativas consideradas

- **Node/Express + frontend separado:** divide el artefacto en dos, obliga a un paso de build de frontend y a decidir el mecanismo de servido estático, sin aportar beneficio para el tamaño del equipo.
- **Microservicios:** separación por dominio (catálogo, préstamos, usuarios) con comunicación por red; coste de despliegue, observabilidad y coordinación desproporcionado para la escala del proyecto.

## Consecuencias

- **Despliegue simple:** un único jar/contenedor que además sirve la interfaz; un solo ciclo de versionado y de pruebas end-to-end.
- **Acoplamiento de módulos:** las capas (`controller → service → repository → model`) conviven en el mismo proceso; el disciplinado de paquetes y las reglas de seguridad por URL son lo que mantiene el orden.
- **Escalabilidad vertical:** para crecer hay que dar más recursos a la misma instancia; el escalado horizontal requeriría revisar las decisiones locales (ver [ADR-009](ADR-009-sin-redis.md)).