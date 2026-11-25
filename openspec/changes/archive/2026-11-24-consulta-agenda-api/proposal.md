# Proposal

## Why

El odontólogo necesita consultar su agenda diaria y semanal y acceder al detalle básico de cada turno junto con los cinco datos obligatorios del paciente (`US-006`, `FL-01`) para organizar la atención y contactar al paciente cuando corresponda. Con `C-03` (acceso interno del odontólogo) y `C-08` (reservas confirmadas) ya implementados y archivados, están completas las dependencias de `C-10`.

## What Changes

- Exponer `GET /api/odontologo/agenda` para consultar los turnos persistidos (`CONFIRMADO` y `CANCELADO`) de la agenda única del consultorio filtrando por vista diaria (`DIA`) o semanal (`SEMANA`, lunes a domingo ISO) interpretada en la zona horaria persistida del consultorio (`Consultorio.zonaHoraria`), usando `Clock` inyectado cuando se omita `fecha`.
- Exponer `GET /api/odontologo/turnos/{turnoId}` para obtener el detalle de un turno perteneciente a la agenda única junto con su motivo y los cinco datos básicos del paciente (`nombre`, `apellido`, `email`, `telefono`, `dni`), sin exponer credenciales de autogestión, estados internos de notificaciones, historia clínica ni cobros, y sin habilitar edición de pacientes ni alta manual.
- Diferenciar una agenda vacía (`200 OK` con `turnos: []`) de parámetros inválidos (`400 VALIDATION_ERROR`), turno inexistente o ajeno a la agenda (`404 TURNO_NOT_FOUND`) y fallo técnico o de persistencia (`500 TECHNICAL_FAILURE`), reutilizando sin modificaciones la cadena de seguridad `C-03` sobre `/api/odontologo/**` (`401 SESSION_REQUIRED` sin sesión autenticada).

## Capabilities

### New Capabilities
- `dentist-agenda-query`: consulta interna de agenda diaria/semanal y detalle de turno con los cinco datos básicos del paciente para el odontólogo autenticado.

### Modified Capabilities
- None.

## Impact

- Nuevas clases de lectura en `dev.odontologia.agenda` (`DentistAgendaController`, `DentistAgendaService`, `DentistAgendaDtos`), proyecciones SQL `JOIN` sin N+1 en `AgendaQueries`, fábrica `turnoNotFound()` en `AvailabilityException` y registro de `DentistAgendaController` en `AvailabilityErrorAdvice`.
- Sin cambios en migraciones Flyway (reutiliza tablas e índices `turno_agenda_inicio_idx`, `motivo_consulta`, `paciente` de `V1__core_models.sql`) ni en `DentistSecurityConfiguration` o `dev.odontologia.acceso` (gobernanza `BAJO`).
