# Proposal

## Why

El backend ya expone la consulta interna de agenda y el detalle de turno (`C-10`, `GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}`) y el frontend ya cuenta con el acceso autenticado del odontólogo y las pestañas de configuración (`C-12`), además de la ruta pública de reserva (`C-13`). Sin embargo, falta la vista web interna para que el odontólogo consulte su agenda diaria y semanal junto con los cinco datos básicos del paciente asociados a cada turno, completando la interfaz de `US-006` y el recorrido `FL-01`.

## What Changes

- Extender `frontend/src/types/odontologia.ts`, `frontend/src/utils/errors.ts`, `frontend/src/utils/timezone.ts` y `frontend/src/api/apiClient.ts` con tipos estrictos y guardas en runtime (`TS-01`, cero `any`) para `GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}`, códigos de error `TURNO_NOT_FOUND` y `ACCESS_DENIED`, y utilidades puras de calendario `YYYY-MM-DD` sin depender de la zona horaria ni del reloj del navegador.
- Implementar `frontend/src/components/AgendaSection.tsx` e integrarlo como pestaña «Agenda» en el área autenticada de `frontend/src/App.tsx`, reutilizando la gestión de sesión de `C-12`.
- Soportar vista diaria (`DIA`) y semanal (`SEMANA`), navegación por fecha (`YYYY-MM-DD`, «Anterior», «Hoy» omitiendo `fecha` para usar el `Clock` del backend en `consultorio.zonaHoraria`, y «Siguiente») y botón «Actualizar» para reflejar reservas, cancelaciones y reprogramaciones persistidas sin WebSocket ni polling automático.
- Soportar apertura y cierre de detalle de turno (`GET /api/odontologo/turnos/{turnoId}`) mostrando motivo, intervalo formateado en `zonaHoraria`, duración reservada, estado (`CONFIRMADO` o `CANCELADO`, sin inventar un estado `REPROGRAMADO`, `RN-RP-02`), `revision`, `creadoEn`, `canceladoEn` (cuando aplique) y los cinco datos básicos del paciente (`nombre`, `apellido`, `email`, `telefono`, `dni`).
- Diferenciar explícitamente los estados de carga, agenda vacía (`turnos: []`), error de consulta (sin presentar turnos de un período anterior como actuales, `FL-01`) y sesión expirada (`401 SESSION_REQUIRED` → vuelta al ingreso).
- **Fuera de alcance**: edición, fusión o borrado de pacientes (`RN-PA-03`), ficha clínica u odontograma, cobros, alta/cancelación/reprogramación manual por el odontólogo (`PB-06`, `RN-EX-03`), WebSocket o tiempo real, cambios en backend/seguridad/esquema PostgreSQL, nuevas librerías y sistema de diseño o mockups (política visual `off`).

## Capabilities

### New Capabilities

- `dentist-agenda-web`: Interfaz web interna de solo lectura para consultar la agenda diaria y semanal del odontólogo y el detalle de cada turno con los cinco datos básicos del paciente.

### Modified Capabilities

- None.

## Impact

- Código afectado en `apply`: únicamente `frontend/src/` (`types/odontologia.ts`, `utils/errors.ts`, `utils/timezone.ts`, `api/apiClient.ts`, `components/AgendaSection.tsx`, `App.tsx`, `styles.css` y sus suites `.test.ts`/`.test.tsx`).
- APIs consumidas: `GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}` (`C-10`), bajo la sesión existente de `C-03`/`C-12`.
- Gobernanza: **BAJO** (`CHANGES.md` §C-15 — interfaz de consulta sobre mecanismos de acceso existentes, sin tocar autenticación ni autorización).
