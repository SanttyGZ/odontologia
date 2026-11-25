# Proposal

## Why

Los pacientes necesitan consultar motivos y horarios disponibles y confirmar una reserva desde el navegador local sin crear cuenta ni verificar su email previamente (`US-004`, `US-005` y resultado en interfaz de `US-010`, flujos `FL-03` y `FL-04`), consumiendo los endpoints públicos ya verificados en `C-07` y `C-08`.

## What Changes

- Incorporar la ruta y vista pública de reserva (`/reservar`, accesible directamente por URL y mediante navegación desde la pantalla inicial sin exigir sesión ni token CSRF del odontólogo).
- Extender `odontologia.ts`, `errors.ts` y `apiClient.ts` con tipos estrictos y guardas en runtime (`TS-01`) para `GET /api/public/motivos`, `GET /api/public/disponibilidad` y `POST /api/public/turnos`, incluyendo `SLOT_UNAVAILABLE`.
- Implementar el recorrido React de selección de motivo, filtro opcional de fechas (`desde`/`hasta`), grilla de horarios disponibles en la `zonaHoraria` devuelta por la API, formulario con los cinco datos obligatorios (`nombre`, `apellido`, `email`, `telefono`, `dni`) y pantalla de confirmación.
- Diferenciar explícitamente indisponibilidad vacía, error de consulta, validación (`400`), conflicto concurrente (`409 SLOT_UNAVAILABLE`), agenda ocupada (`409 AGENDA_BUSY`), resultado incierto de red/respuesta y estado separado entre reserva (`CONFIRMADO`) y notificación por correo (`PENDIENTE`, `PB-10`), bloqueando envíos duplicados en vuelo (`PB-08`).

## Capabilities

### New Capabilities
- `patient-booking-web`: interfaz web pública de consulta de motivos/disponibilidad y reserva confirmada de turnos para pacientes sin cuenta.

### Modified Capabilities
- None.

## Impact

- Código y pruebas en `frontend/src/` (`types/odontologia.ts`, `utils/errors.ts`, `utils/timezone.ts`, `api/apiClient.ts`, `components/PublicBookingPage.tsx`, `App.tsx`, `styles.css` y suites Vitest/RTL).
- Sin cambios en backend, esquema PostgreSQL, políticas de seguridad ni dependencias de producción.
