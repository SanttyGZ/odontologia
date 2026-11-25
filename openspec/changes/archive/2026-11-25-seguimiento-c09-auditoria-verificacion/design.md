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

Al redactarse el seguimiento, la aprobación explícita de D-07 en el diseño archivado de C-09 cubría solo el UUID opaco del enlace en el resultado interno de validación; la aprobación previa de C-04 no cubría el nuevo campo de email expuesto a C-09. La revisión pendiente en ese momento se resolvió con la aprobación posterior de Fernando, registrada con la pregunta y respuesta exactas en la verificación de correcciones C-09.

**Addendum de estado (2026-11-25):** Santiago informó que Fernando aprobó explícitamente el uso interno del email confirmado de recuperación para avisos futuros, después de recibir la pregunta que delimitó ese alcance. La pregunta y la respuesta transmitida quedaron registradas en el [informe de verificación C-09](../2026-11-25-correcciones-c09-autogestion-turnos-api/verification.md). La sincronización posterior con `upstream/main` conserva los cierres archivados de C-10, C-12 y C-13.

### PB-06

Santiago informó el 2026-11-25 que Fernando confirmó que PB-06 puede quedar para C-14. C-09 mantiene el rechazo `409 MANAGEMENT_WINDOW_CLOSED`. El canal operativo concreto todavía debe acordarse antes de implementar C-14; no se agregan excepciones ni operaciones manuales.

### Estados del roadmap

Al momento del seguimiento en `ramaSanty`, C-10 figuraba `[ ]` sin change archivado y C-12 permanecía `[x]` completado/archivado. Tras sincronizar `upstream/main`, C-10 y C-13 también constan completados y archivados en `CHANGES.md`; C-09 conserva sus referencias de implementación y la aprobación CRITICAL posterior ya está registrada.

### Evidencia de disputa por horario

AG-05 ya define el resultado y la conservación del original. La prueba unitaria existente simula dependencias y no demuestra persistencia PostgreSQL. La prueba de integración de C-09 usa PostgreSQL real para sus escenarios cubiertos, pero reemplaza `CredencialesGestion` de C-04 por un doble; esto no valida internamente C-04. No se afirmará cobertura PostgreSQL de la disputa por horario sin ejecutar una prueba que lo compruebe.

### UI

Decision: no (no UI).

## Risks / Trade-offs

- [Riesgo] No poder ejecutar PostgreSQL en el entorno Windows → registrar el comando y el error observado y mantener el escenario AG-05 pendiente de evidencia real.
- [Riesgo] Confundir la autorización de C-04 con la ampliación del resultado interno de validación → citar aprobaciones por alcance y mantener pendiente la revisión CRITICAL adicional.
- [Riesgo] PB-06 quede sin canal al iniciar C-14 → documentar el acuerdo operativo como prerrequisito de C-14 y conservar el rechazo existente de C-09.
