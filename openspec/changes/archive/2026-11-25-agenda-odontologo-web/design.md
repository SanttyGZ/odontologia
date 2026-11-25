# Design

## Context

`C-10` implementó los endpoints internos de solo lectura en el backend:

- `GET /api/odontologo/agenda` (parámetros opcionales `vista`: `DIA` | `SEMANA`, por defecto `DIA`; `fecha`: `YYYY-MM-DD`, por defecto resuelta con el `Clock` del backend en `consultorio.zonaHoraria`). Devuelve `vista`, `desde`, `hasta`, `zonaHoraria` y `turnos[]` ordenados por `inicio ASC, id ASC`, donde cada ítem incluye `id`, `motivoId`, `motivoNombre`, `inicio`, `fin`, `duracionReservadaMinutos`, `estado` (`CONFIRMADO` | `CANCELADO`), `pacienteNombre` y `pacienteApellido`.
- `GET /api/odontologo/turnos/{turnoId}`. Devuelve `id`, `motivoId`, `motivoNombre`, `inicio`, `fin`, `duracionReservadaMinutos`, `estado` (`CONFIRMADO` | `CANCELADO`), `revision`, `creadoEn`, `canceladoEn` (`string | null`) y `paciente` (`nombre`, `apellido`, `email`, `telefono`, `dni`).
- Errores estándar del backend: `400 VALIDATION_ERROR`, `401 SESSION_REQUIRED`, `403 ACCESS_DENIED`, `404 TURNO_NOT_FOUND`, `500 TECHNICAL_FAILURE`.

En el frontend (`frontend/src/`), `C-12` implementó la consola autenticada del odontólogo en `App.tsx` con las pestañas de configuración (`motivos`, `horario`, `bloqueos`), `apiClient.ts` y la gestión de sesión (`SESSION_REQUIRED`), mientras que `C-13` agregó la ruta pública `/reservar`. `C-15` agrega la vista de consulta de agenda dentro del área autenticada existente sin modificar el backend ni la seguridad.

## Goals / Non-Goals

**Goals:**

- Integrar la pestaña «Agenda» en `App.tsx` reutilizando la sesión y el manejo de `401 SESSION_REQUIRED` de `C-12`.
- Consumir `GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}` con tipos estrictos y validación runtime (`TS-01`, sin `any`).
- Manejar fechas calendario `YYYY-MM-DD` e intervalos en `consultorio.zonaHoraria` sin depender de la zona horaria ni del reloj del navegador.
- Mantener la vista estrictamente de solo lectura, accesible por teclado y adaptable a `390×844` y `1440×900`.

**Non-Goals:**

- No modificar código del backend, seguridad, CSRF ni esquema PostgreSQL.
- No agregar edición/fusión/borrado de pacientes, ficha clínica, cobros ni alta/cancelación/reprogramación manual de turnos.
- No introducir WebSocket, polling automático, librerías externas ni sistema de diseño/mockups (política visual `off`).

## Decisions

### D-01 — Integración como pestaña «Agenda» en App.tsx preservando compatibilidad con C-12 y C-13

Extender el tipo `SectionTab` a `'agenda' | 'motivos' | 'horario' | 'bloqueos'` y renderizar el botón de pestaña `tab-agenda` junto a las pestañas existentes. Mantener el estado inicial de `activeTab` en `'motivos'` (permitiendo opcionalmente `initialTab?: SectionTab` como prop de `App` para pruebas puntuales) para no alterar las expectativas de las pruebas existentes de `C-12` en `App.test.tsx`. Deshabilitar `tab-agenda` cuando `sectionBusy || loggingOut` sea `true`, consistente con el resto de pestañas.

### D-02 — Manejo de fechas calendario YYYY-MM-DD y zona horaria sin inferir del navegador

Al montar `AgendaSection` y al accionar «Hoy», omitir el query param `fecha` en `GET /api/odontologo/agenda` para que el backend determine «hoy» usando su `Clock` en `consultorio.zonaHoraria` (`AQ-01`). Una vez recibida la respuesta, usar `selectedFecha ?? agenda.desde` como fecha ancla activa para el selector de fecha y los desplazamientos de período. Incorporar helpers puros en `frontend/src/utils/timezone.ts` (`isValidIsoCalendarDate`, `shiftIsoCalendarDate`, `formatIsoCalendarDate`) operando exclusivamente con aritmética `Date.UTC` y formateo directo sobre `YYYY-MM-DD` para evitar corrimientos de un día en zonas horarias negativas (por ejemplo `UTC-3`). Formatear todos los instantes (`inicio`, `fin`, `creadoEn`, `canceladoEn`) con `Intl.DateTimeFormat` fijando `timeZone: agenda.zonaHoraria`.

### D-03 — Guardas de tipo estrictas (TS-01) y códigos de error completos

Incorporar `TURNO_NOT_FOUND` y `ACCESS_DENIED` en `ApiErrorCode`, `KNOWN_ERROR_CODES` (`odontologia.ts`) y `ERROR_MESSAGES` (`errors.ts`). Implementar guardas de runtime sobre `unknown` (cero `any`) para validar `AgendaResponse`, `AgendaTurnoResumen`, `TurnoDetalleResponse` (incluyendo `canceladoEn: string | null`) y `PacienteBasico` (cinco campos requeridos como `string`). Añadir `getAgenda(params?)` y `getTurnoDetalle(turnoId)` en `ApiClient` reutilizando `getJson` (peticiones `GET` de solo lectura que no requieren token CSRF).

### D-04 — Concurrencia de lecturas, limpieza ante error (FL-01) y recarga paralela

Usar contadores monotónicos en refs (`latestAgendaRequestIdRef`, `latestDetalleRequestIdRef`) y bandera de montaje para descartar respuestas fuera de orden o posteriores al desmontaje. Al cambiar de vista/fecha o fallar una consulta de agenda, limpiar el listado previo en lugar de mantener turnos de otro período como actuales (`FL-01`) y ofrecer un botón «Reintentar». Al accionar «Actualizar» cuando existe un detalle abierto, disparar en paralelo (`Promise.all`, regla `async-parallel` de `vercel-react-best-practices`) la consulta de agenda y la del detalle abierto; si tras actualizar el turno ya no pertenece a `agenda.turnos` o el detalle devuelve `404 TURNO_NOT_FOUND`, cerrar el panel de detalle obsoleto.

### D-05 — Representación fiel de estados y restricción de solo lectura

Mostrar badges diferenciados para `CONFIRMADO` y `CANCELADO` tanto en la lista como en el detalle, aclarando visualmente en turnos `CANCELADO` que liberaron el intervalo pero conservan su registro. Exponer `revision` y `canceladoEn` (cuando no sea `null`) de forma informativa sin crear ni sugerir un estado `REPROGRAMADO` (`RN-RP-02`). No renderizar ningún control de creación, edición, cancelación o reprogramación.

## Risks / Trade-offs

- **Riesgo de desfase de zona horaria en navegación de fechas:** Si se usara `new Date(yyyyMmDd)` local del navegador, los usuarios en `America/Argentina/Buenos_Aires` verían desplazamientos de 1 día. Se mitiga usando aritmética pura `Date.UTC` en `utils/timezone.ts` y pruebas unitarias específicas.
- **Riesgo de romper pruebas existentes de `C-12` al cambiar la pestaña inicial:** Se mitiga conservando `'motivos'` como `activeTab` por defecto en `App.tsx` y permitiendo `initialTab` opcional para pruebas.
- **Riesgo de mostrar datos obsoletos tras error de red:** Se mitiga limpiando el estado `agenda` ante error de consulta para evitar presentar turnos de un período anterior como si correspondieran al período fallido (`FL-01`).

## Migration Plan

No aplica migración de base de datos ni cambios de despliegue en backend. El cambio es puramente aditivo en `frontend/src/` y se valida con la suite `vitest`, `tsc` y `vite build`.

## Open Questions

Ninguna pregunta abierta bloqueante para C-15.

## UI Mockups

Decision: no (project policy)
