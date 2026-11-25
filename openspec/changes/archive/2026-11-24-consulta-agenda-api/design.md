# Design

## Context

`C-10` implementa la consulta interna de agenda y detalle de turnos (`US-006`, `FL-01`, gobernanza `BAJO`) sobre el esquema de `C-02`, la seguridad de sesión de `C-03` y las reservas de `C-08`, dejando listo el contrato HTTP que consumirá `C-15` (`agenda-odontologo-web`).

## Goals / Non-Goals

**Goals:**
- Exponer `GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}`.
- Calcular ventanas diarias y semanales en `consultorio.zonaHoraria` con `Clock` inyectado.
- Resolver lecturas con un único `JOIN` SQL sin N+1 sobre índices existentes.
- Distinguir respuesta `200` vacía (`turnos: []`) de errores `400`/`404`/`500`.
- Preservar intacto `dev.odontologia.acceso` (`DentistSecurityConfiguration`).

**Non-Goals:**
- Mutaciones de turnos o pacientes, alta manual o excepciones del odontólogo (`PB-06`).
- Ficha clínica o cobros.
- Cambios en `DentistSecurityConfiguration` ni nuevos mecanismos de autenticación.
- Nuevas migraciones Flyway o interfaz React (`C-15`).

## Decisions

### D-01 — Contrato HTTP de agenda y detalle
- `GET /api/odontologo/agenda?vista=DIA|SEMANA&fecha=YYYY-MM-DD`: responde `AgendaResponse(vista, desde, hasta, zonaHoraria, List<AgendaTurnoResumenResponse(id, motivoId, motivoNombre, inicio, fin, duracionReservadaMinutos, estado, pacienteNombre, pacienteApellido)>)`. Incluir nombre y apellido en el resumen permite identificar cada cita en la grilla/lista de `C-15`, reservando los cinco datos de contacto/identificación completos al endpoint de detalle.
- `GET /api/odontologo/turnos/{turnoId}`: responde `TurnoDetalleResponse(id, motivoId, motivoNombre, inicio, fin, duracionReservadaMinutos, estado, revision, creadoEn, canceladoEn, PacienteBasicoResponse(nombre, apellido, email, telefono, dni))`.

### D-02 — Ventana diaria y semanal en zona horaria del consultorio
- `vista` admite `DIA` (valor por defecto si se omite) y `SEMANA`; cualquier otro valor produce `400 VALIDATION_ERROR`.
- `fecha` usa `LocalDate.ofInstant(clock.instant(), zone)` cuando se omite (nunca `LocalDate.now()` de la JVM).
- `DIA`: `desde = fecha`, `hasta = fecha`.
- `SEMANA`: semana ISO lunes a domingo (`desde = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))`, `hasta = desde.plusDays(6)`), consistente con `FranjaSemanal` (`1=Lunes .. 7=Domingo`).
- Rango de instantes semiabierto: `[desde.atStartOfDay(zone).toInstant(), hasta.plusDays(1).atStartOfDay(zone).toInstant())`.

### D-03 — Consultas SQL JOIN sin N+1 sobre índices existentes
- En `AgendaQueries`, consultar `turno t JOIN motivo_consulta m ON m.id = t.motivo_id AND m.agenda_id = t.agenda_id JOIN paciente p ON p.id = t.paciente_id AND p.consultorio_id = ?` filtrando `t.agenda_id = ? AND t.inicio < ? AND t.fin > ? ORDER BY t.inicio, t.id` (aprovechando `turno_agenda_inicio_idx` y claves primarias de `motivo_consulta` y `paciente`), y para detalle `WHERE t.agenda_id = ? AND t.id = ?`.

### D-04 — Manejo de errores y reutilización de seguridad C-03
- Añadir `public static AvailabilityException turnoNotFound()` (`404 TURNO_NOT_FOUND`) y visibilidad `public` en fábricas existentes necesarias (`validation()`, `technicalFailure()`), registrando `DentistAgendaController.class` en `@RestControllerAdvice(assignableTypes = ...)` de `AvailabilityErrorAdvice`.
- `DentistSecurityConfiguration` ya protege `/api/odontologo/**` con `hasRole("ODONTOLOGO")` y devuelve `401 {"codigo":"SESSION_REQUIRED"}` sin sesión.

## Risks / Trade-offs

- [La verificación sobre cancelaciones y reprogramaciones originadas por el flujo real de autogestión depende de `C-09`] → se comprueba extremo a extremo en `C-16`; en `C-10` los estados `CANCELADO` y revisiones reprogramadas se verifican con fixtures en PostgreSQL real.

## UI Mockups

Decision: no (no UI)
