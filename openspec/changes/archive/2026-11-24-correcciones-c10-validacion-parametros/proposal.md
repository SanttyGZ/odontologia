# Proposal

## Why

La revisión de C-10 detectó que el binding por defecto de Spring/JDK en `DentistAgendaController` acepta entradas inválidas en `fecha` (`?fecha=`, `?fecha=%20`, offsets `Z`/`-03:00`, fechas inexistentes como `2026-02-30` resueltas silenciosamente, y años extendidos `+10000-01-01` / `+999999999-12-31` que provocan `500`) y en `turnoId` (UUIDs abreviados como `1-1-1-1-1` que `UUID.fromString` normaliza y derivan en `404` en lugar de `400`).

## What Changes

- En `GET /api/odontologo/agenda`, recibir `fecha` como `String` opcional: si se omite (`null`), mantener el default del día actual en `consultorio.zonaHoraria`; si se envía, exigir exactamente `^\d{4}-\d{2}-\d{2}$` y calendario válido (`ResolverStyle.STRICT`), devolviendo `400 {"codigo":"VALIDATION_ERROR"}` antes de invocar al servicio en cualquier otro caso.
- En `GET /api/odontologo/turnos/{turnoId}`, recibir `turnoId` como `String` y exigir formato hexadecimal estricto `8-4-4-4-12` (mayúsculas o minúsculas) antes de invocar al servicio, devolviendo `400 {"codigo":"VALIDATION_ERROR"}` para valores como `1-1-1-1-1`, preservando `200` para turnos existentes y `404 {"codigo":"TURNO_NOT_FOUND"}` para UUID válidos inexistentes.
- Agregar pruebas de regresión en `DentistAgendaControllerTest` verificando estado HTTP, JSON exacto de error y `verifyNoInteractions(service)` para todos los casos inválidos.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `dentist-agenda-query`: precisión de validación estricta de `fecha` (`YYYY-MM-DD` calendario estricto) y `turnoId` (UUID hexadecimal `8-4-4-4-12`) en `AQ-01` y `AQ-02`.

## Impact

- Solo `backend/src/main/java/dev/odontologia/agenda/DentistAgendaController.java` y `backend/src/test/java/dev/odontologia/agenda/DentistAgendaControllerTest.java`.
- Sin cambios en `DentistAgendaService`, consultas SQL, seguridad `C-03` ni migraciones Flyway (gobernanza `BAJO`).
