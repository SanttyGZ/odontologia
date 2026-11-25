# Design

## Context

`C-13` construye la pantalla pública de reserva de pacientes (`US-004`, `US-005`, `US-010`, `FL-03`, `FL-04`) sobre la base React 19.3.1 + TypeScript 7.0.2 estricto + Vite 8.3.8 creada en `C-12`, consumiendo `GET /api/public/motivos` y `GET /api/public/disponibilidad` (`C-07`) y `POST /api/public/turnos` (`C-08`). La política visual es `off` (`AT-11`).

## Goals / Non-Goals

**Goals:**
- Habilitar `/reservar` sin sesión ni token CSRF interno, manteniendo intacto el comportamiento existente de `/` para los tests y flujos de `C-12`, con navegación accesible entre ambas vistas.
- Preservar el string ISO exacto `horario.inicio` devuelto por `GET /api/public/disponibilidad` al invocar `POST /api/public/turnos` (compatible con minutos exactos, segundos y microsegundos).
- Cumplir `TS-01` (cero `any`, validación en runtime con type guards sobre `unknown`), navegación completa por teclado y diseño adaptable móvil/escritorio.

**Non-Goals:**
- Incorporar `react-router-dom` u otras dependencias externas.
- Implementar autogestión por enlace privado (`C-14`) o agenda del odontólogo (`C-15`).
- Modificar endpoints o seguridad del backend.

## Decisions

### D-01 — Enrutamiento liviano por `pathname` sin dependencias nuevas
`App` determina la vista pública cuando `pathname` comienza con `/reservar` (soportando prop opcional `initialPath` para pruebas y sincronización con `window.history.pushState` + `popstate`). En `/reservar`, `App` renderiza `PublicBookingPage` y **no** ejecuta `checkSession()`. Desde la pantalla de ingreso en `/` se ofrece un enlace/acción hacia `/reservar`, y desde `/reservar` uno hacia `/` para acceso del odontólogo.

### D-02 — Extensión pública de `apiClient` sin CSRF interno
Las rutas `/api/public/*` están fuera del `SecurityFilterChain` de `/api/odontologo/**`. Se agregan en `ApiClient`:
- `getPublicMotivos()` → `GET /api/public/motivos`
- `getPublicDisponibilidad(params)` → `GET /api/public/disponibilidad?motivoId=...&desde=...&hasta=...`
- `createPublicTurno(input)` → `POST /api/public/turnos` con `Content-Type: application/json` directo, sin solicitar `/api/odontologo/csrf`.

### D-03 — Concurrencia de consultas, doble clic (`PB-08`) y separación turno/correo (`PB-10`)
- Un contador incremental de solicitud en `PublicBookingPage` descarta respuestas tardías al cambiar rápido de motivo o rango de fechas.
- Mientras `submitting` es `true`, el botón de reserva y selectores de horario permanecen deshabilitados para impedir doble envío accidental.
- Los errores `NETWORK_ERROR` e `INVALID_RESPONSE` en `POST /api/public/turnos` activan un aviso específico de **resultado incierto**, distinto de los rechazos confirmados por el servidor (`400`, `404`, `409 SLOT_UNAVAILABLE`, `409 AGENDA_BUSY`, `500`).
- La vista de éxito separa visual y semánticamente **Reserva confirmada** (`CONFIRMADO`) de **Correo de confirmación** (`PENDIENTE` — envío asíncrono independiente de la confirmación del turno).

## Risks / Trade-offs

- [Formato de horas con segundos en `zonaHoraria`] → extender `timezone.ts` con un formateador que muestre segundos cuando el instante los contenga, sin alterar `horario.inicio` crudo enviado al backend.

## UI Mockups

Decision: no (project policy)
