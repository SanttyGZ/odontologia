# Design

## Context

En C-10 (`consulta-agenda-api`), `DentistAgendaController` delegó el parsing de `fecha` y `turnoId` en los conversores por defecto de Spring/JDK (`@DateTimeFormat(iso = ISO.DATE) LocalDate` y `@PathVariable UUID`). Esto provoca aceptaciones silenciosas y códigos de estado incorrectos frente a entradas mal formadas.

## Goals / Non-Goals

**Goals:**
- Rechazar con `400 {"codigo":"VALIDATION_ERROR"}` antes de invocar a `DentistAgendaService` cualquier `fecha` que no cumpla `YYYY-MM-DD` de calendario estricto o cualquier `turnoId` que no cumpla UUID hexadecimal `8-4-4-4-12`.
- Preservar `fecha` omitida (`null`) con el default actual del día en `consultorio.zonaHoraria` vía `Clock`.
- Preservar `200 OK` para turnos existentes y `404 {"codigo":"TURNO_NOT_FOUND"}` para UUID `8-4-4-4-12` válidos inexistentes (mayúsculas o minúsculas).

**Non-Goals:**
- Modificar `DentistAgendaService`, `AgendaQueries`, `DentistSecurityConfiguration` ni migraciones Flyway.

## Decisions

### D-01 — Validación estricta de `fecha` en `DentistAgendaController`
- Recibir `@RequestParam(required = false) String fecha`.
- Si `fecha == null`, pasar `null` a `service.agenda(vista, null)`.
- Si `fecha != null`, validar con `Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")` y parsear con `DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)`. Si no coincide o lanza `DateTimeParseException`, lanzar `AvailabilityException.validation()`.

### D-02 — Validación estricta de `turnoId` en `DentistAgendaController`
- Recibir `@PathVariable String turnoId`.
- Validar con `Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")`.
- Si no coincide, lanzar `AvailabilityException.validation()` sin llamar al servicio; si coincide, convertir con `UUID.fromString(turnoId)` y delegar a `service.turnoDetalle(...)`.

## Risks / Trade-offs

- Cambio acotado exclusivamente al binding de entrada en `DentistAgendaController` y sus pruebas unitarias; cero impacto en contratos válidos existentes.

## UI Mockups

Decision: no (no UI)
