# Proposal

## Why

En `AgendaSection.tsx`, cuando el odontólogo borra o ingresa una fecha inválida mientras hay una consulta de agenda en curso (`fetchAgenda` o `handleRefresh`), `handleDateInputChange` limpia `agenda` y el detalle y muestra el mensaje de validación, pero no incrementa `latestAgendaRequestIdRef.current` ni apaga `loadingAgenda`. Al resolverse la promesa pendiente, los turnos del período anterior reaparecen junto al error de validación, violando `AW-04` y `FL-01`.

## What Changes

- En `frontend/src/components/AgendaSection.tsx` (`handleDateInputChange`), al recibir un valor que no cumple `isValidIsoCalendarDate(value)`, incrementar `latestAgendaRequestIdRef.current += 1` y establecer `setLoadingAgenda(false)` además de `setAgenda(null)`, `clearDetalleState()` y `setAgendaError(...)` (y asegurar en `handleRefresh` el corte temprano cuando `!isLatestAgenda && !isLatestDetalle`), dejando la pantalla sin carga colgada y lista para elegir otra fecha válida o accionar «Reintentar» / «Hoy».
- Agregar en `frontend/src/components/AgendaSection.test.tsx` una prueba de regresión (RED → GREEN) con respuesta demorada: iniciar consulta con detalle abierto / solicitud en curso, borrar la fecha antes de que termine, resolver la solicitud pendiente y comprobar que la lista sigue vacía, el detalle permanece cerrado, persiste el mensaje de fecha inválida y luego es posible seleccionar otra fecha o reintentar.
- **Fuera de alcance**: cambios en backend, seguridad, estilos u otros componentes fuera del alcance de `C-15`.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `dentist-agenda-web`: precisar en `AW-04` la invalidación de consultas de agenda en curso al borrar o invalidar la fecha.

## Impact

- `frontend/src/components/AgendaSection.tsx` y `frontend/src/components/AgendaSection.test.tsx`.
- Gobernanza **BAJO** (`LOW`).
