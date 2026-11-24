# Design

## Context

See `proposal.md` and the `availability-configuration` delta. The archived C-06 design requires integral motive durations and requires each confirmed turn to fit within one same-day attention range. The current DTO binds the duration directly to `Integer`, which lets Jackson coerce fractional JSON numbers, while `coversConfirmedTurn` joins adjacent ranges. The configuration-first concurrency test currently writes the schedule directly through SQL.

## Goals / Non-Goals

**Goals:**

- Preserve the public C-06 routes and error envelope while rejecting fractional durations before service calls.
- Apply the archived single-range rule without rejecting valid contiguous schedule configuration.
- Exercise the real C-06 schedule service and coordinator in the configuration-first PostgreSQL race.
- Preserve the verified C-04/C-05 integration from `upstream/main` and leave C-07 pending.

**Non-Goals:**

- Implement C-07 or change its public contract.
- Change the database schema, authentication, CSRF, credentials, or notification behavior.
- Reopen PB-02, PB-03, or PB-05; these decisions remain approved as recorded below.

## Decisions

### D-01 — Reject non-integral JSON duration values at the HTTP boundary

Bind `duracionMinutos` as `BigDecimal` instead of `Integer`, then convert with exact integer semantics before invoking the C-06 service. A fractional value or an integer outside the supported Java range produces `VALIDATION_ERROR`; the service is not called and no persistence can occur. Keep the service and persisted model integer-based. This is narrower than global Jackson coercion settings, which could change unrelated APIs.

### D-02 — Require one attention range to contain the whole confirmed turn

After converting both instants into the consultorio zone and confirming they fall on the same local day, accept a turn only when one `ATENCION` range contains its complete half-open interval and it does not intersect a pause. Do not accumulate coverage across adjacent ranges. Continue allowing adjacent ranges in schedule validation; this keeps schedule editing and confirmed-turn compatibility as separate rules.

### D-03 — Hold the agenda lock while the real configuration service writes

In the PostgreSQL concurrency test, hold the agenda row lock from a blocker connection, start the actual `AvailabilityConfigurationService.replaceHorario` call, observe that it is waiting, and then queue the reservation fixture through `AgendaWriteCoordinator`. Observe both waiters before releasing the blocker. The fixture must read the schedule only after acquiring the coordinator lock, reject the now-incompatible interval, and insert no turn. Keep the existing reservation-first race unchanged. Avoid direct SQL for the configuration-first schedule update.

### D-04 — Preserve approved PB decisions and record their provenance

No new business rule is introduced. Santiago, identificado como el usuario responsable de estas decisiones en la conversación de C-06, aprobó las alternativas seleccionadas: PB-02 (franjas del mismo día, sin duplicados ni solapes; pausas contenidas), PB-03 (inicios cada 15 minutos anclados al comienzo de cada franja; duraciones de 15–240 en múltiplos de 15), PB-05a (rechazar cambios incompatibles con turnos confirmados) y PB-05b (usar la duración configurada vigente al reprogramar). La conversación de decisión y su registro de sesión de C-06 son la evidencia de aprobación; `knowledge-base/10_preguntas_abiertas.md` ya las identifica como decisiones del usuario, y el diseño archivado D-02/D-03/D-05 junto con su spec fija el contrato resultante. Aclarar en la KB el nombre del aprobador y las referencias, sin cambiar el contenido de las reglas.

### D-05 — Mantener C-06 y actualizar desde la rama base correcta

`upstream/main` incorpora cierres verificados de C-04/C-05 pero conserva una versión anterior del estado y las decisiones C-06. Al resolver `CHANGES.md`, conservar los cierres de C-04/C-05, reflejar el C-06 archivado más este seguimiento correctivo y mantener C-07 pendiente. No publicar la rama ni fusionar la PR.

## Risks / Trade-offs

- [Un DTO decimal añade conversión explícita] → centralizar la conversión exacta en el límite HTTP y conservar el servicio tipado como entero.
- [Un turno que antes cruzaba franjas contiguas será rechazado] → esto restablece el diseño C-06; la configuración de ambas franjas seguirá aceptándose.
- [Las pruebas concurrentes pueden ser frágiles si solo se sincronizan por tiempo] → usar latch y observar el bloqueo real en PostgreSQL antes de liberar las transacciones.

## Migration Plan

No se requiere migración. Los registros existentes no cambian. Los clientes que envíen fracciones reciben `400 VALIDATION_ERROR`; las solicitudes enteras aprobadas conservan su contrato.

## Open Questions

Ninguna. La evidencia disponible identifica al aprobador de PB-02/PB-03/PB-05; no se requiere pedir otra confirmación para estas reglas.

## UI Mockups

Decision: no (no UI)
