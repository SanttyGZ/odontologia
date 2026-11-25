# Design

## Context

Ver `proposal.md`. C-09 y su corrección están archivados. La especificación actual `appointment-self-management` ya contiene AG-05 y el escenario «Reprogramación pierde una carrera por el horario». El trabajo de este seguimiento no cambia requisitos ni código de producción.

## Goals / Non-Goals

**Goals:**

- Mantener separada la evidencia de implementación de la aprobación CRITICAL del dato adicional.
- Registrar la confirmación reportada sobre PB-06 y el límite operativo que sigue pendiente.
- Informar el alcance exacto de las pruebas: PostgreSQL real cuando esté disponible, dobles de C-04 cuando corresponda y ausencia de evidencia persistente cuando una prueba no pueda ejecutarse.

**Non-Goals:**

- Aprobar por inferencia la devolución del email de recuperación desde `CredencialesGestion.validar`.
- Cambiar la credencial, las rutas protegidas, la respuesta `409 MANAGEMENT_WINDOW_CLOSED` o el alcance de C-09.
- Cambiar el estado de C-10 o reabrir el cierre de C-12.

## Decisions

### Aprobación CRITICAL

La aprobación explícita de D-07 en el diseño archivado de C-09 cubre solo el UUID opaco del enlace en el resultado interno de validación; el mismo registro deja cualquier ampliación fuera de alcance. La aprobación previa de C-04 para la recuperación y persistencia no cubre el nuevo campo de email expuesto a C-09. Se registra la revisión humana pendiente y no se modifica el mecanismo de credenciales.

**Addendum de cierre (2026-11-25):** esa revisión pendiente se resolvió cuando el usuario autorizó terminar C-09 tras conocer el alcance exacto. La autorización, limitada al uso interno del email confirmado para avisos futuros, quedó registrada en el [diseño de correcciones C-09](../2026-11-25-correcciones-c09-autogestion-turnos-api/design.md) y en su [verificación](../2026-11-25-correcciones-c09-autogestion-turnos-api/verification.md).

### PB-06

Santiago informó el 2026-11-25 que Fernando confirmó que PB-06 puede quedar para C-14. C-09 mantiene el rechazo `409 MANAGEMENT_WINDOW_CLOSED`. El canal operativo concreto todavía debe acordarse antes de implementar C-14; no se agregan excepciones ni operaciones manuales.

### Estados del roadmap

En `ramaSanty`, C-10 figura `[ ]` y no se encontró su change archivado. Santiago indicó conservar el estado local pendiente y registrar la discrepancia. C-12 permanece `[x]` con sus enlaces de cierre existentes. C-09 conserva la referencia a la implementación archivada y muestra por separado la revisión CRITICAL pendiente y la verificación de integración no disponible.

### Evidencia de disputa por horario

AG-05 ya define el resultado y la conservación del original. La prueba unitaria existente simula dependencias y no demuestra persistencia PostgreSQL. La prueba de integración de C-09 usa PostgreSQL real para sus escenarios cubiertos, pero reemplaza `CredencialesGestion` de C-04 por un doble; esto no valida internamente C-04. No se afirmará cobertura PostgreSQL de la disputa por horario sin ejecutar una prueba que lo compruebe.

### UI

Decision: no (no UI).

## Risks / Trade-offs

- [Riesgo] No poder ejecutar PostgreSQL en el entorno Windows → registrar el comando y el error observado y mantener el escenario AG-05 pendiente de evidencia real.
- [Riesgo] Confundir la autorización de C-04 con la ampliación del resultado interno de validación → citar aprobaciones por alcance y mantener pendiente la revisión CRITICAL adicional.
- [Riesgo] PB-06 quede sin canal al iniciar C-14 → documentar el acuerdo operativo como prerrequisito de C-14 y conservar el rechazo existente de C-09.
