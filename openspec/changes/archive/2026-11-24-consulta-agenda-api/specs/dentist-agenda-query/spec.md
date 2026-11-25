# Specification

## ADDED Requirements

### Requirement: AQ-01 Consulta interna de agenda diaria y semanal en la zona del consultorio
La API interna SHALL exponer `GET /api/odontologo/agenda` con parámetros opcionales `vista` (`DIA` por defecto o `SEMANA` lunes a domingo ISO) y `fecha` (`YYYY-MM-DD`, por defecto hoy en `consultorio.zonaHoraria` vía `Clock`). La respuesta `200 OK` SHALL devolver `vista`, `desde`, `hasta`, `zonaHoraria` y `turnos` (`CONFIRMADO` y `CANCELADO`) ordenados por `inicio ASC, id ASC` que intersecten `[desde 00:00, hasta+1d 00:00)` en `consultorio.zonaHoraria`.

#### Scenario: Consulta diaria y semanal interpretada en la zona horaria del consultorio
- **WHEN** el odontólogo autenticado consulta `GET /api/odontologo/agenda` con `vista=DIA` o `vista=SEMANA` y una `fecha` (o la omite para usar `Clock` en `consultorio.zonaHoraria`)
- **THEN** la API interpreta los límites de medianoche local en `consultorio.zonaHoraria`, incluye los turnos cuya duración intersecta `[periodoInicio, periodoFin)` y excluye turnos contiguos donde `fin == periodoInicio` o `inicio == periodoFin`.

#### Scenario: Inclusión de turnos confirmados y cancelados sin borrado físico
- **WHEN** en el período consultado existen turnos en estado `CONFIRMADO` y `CANCELADO`
- **THEN** la API devuelve `200 OK` incluyendo ambos estados ordenados por `inicio ASC, id ASC` con `id`, `motivoId`, `motivoNombre`, `inicio`, `fin`, `duracionReservadaMinutos`, `estado`, `pacienteNombre` y `pacienteApellido`.

#### Scenario: Distinción entre agenda vacía y fallo de consulta
- **WHEN** no existen turnos en el período consultado
- **THEN** la API responde `200 OK` con `turnos: []`; si falta la configuración del consultorio o falla la persistencia, responde `500 TECHNICAL_FAILURE`.

#### Scenario: Rechazo de parámetros de filtro inválidos
- **WHEN** `vista` es distinta de `DIA` o `SEMANA`, o `fecha` tiene formato inválido
- **THEN** la API responde `400 VALIDATION_ERROR`.

### Requirement: AQ-02 Consulta de detalle de turno y cinco datos básicos del paciente
La API interna SHALL exponer `GET /api/odontologo/turnos/{turnoId}` para turnos de la agenda única, devolviendo `200 OK` con `id`, `motivoId`, `motivoNombre`, `inicio`, `fin`, `duracionReservadaMinutos`, `estado`, `revision`, `creadoEn`, `canceladoEn` y `paciente` (`nombre`, `apellido`, `email`, `telefono`, `dni`). SHALL NOT exponer credenciales de autogestión, notificaciones, identificadores internos (`agendaId`, `consultorioId`), historia clínica ni cobros.

#### Scenario: Consulta exitosa de detalle con los cinco datos básicos del paciente
- **WHEN** el odontólogo autenticado consulta `GET /api/odontologo/turnos/{turnoId}` para un turno existente de la agenda única (en estado `CONFIRMADO` o `CANCELADO`)
- **THEN** la API responde `200 OK` con los datos del turno, su motivo, `revision`, `creadoEn`, `canceladoEn` y `paciente` (`nombre`, `apellido`, `email`, `telefono`, `dni`), sin exponer credenciales, notificaciones ni campos clínicos.

#### Scenario: Turno inexistente o identificador con formato inválido
- **WHEN** `{turnoId}` es un UUID válido inexistente en la agenda única
- **THEN** la API responde `404 TURNO_NOT_FOUND`; cuando `{turnoId}` no tiene formato UUID válido, responde `400 VALIDATION_ERROR`.

### Requirement: AQ-03 Reutilización estricta del acceso interno del odontólogo
Los endpoints `GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}` SHALL quedar protegidos por la configuración existente de `C-03` (`/api/odontologo/**` con rol `ODONTOLOGO`), rechazando llamadas directas sin sesión válida con `401 SESSION_REQUIRED` sin divulgar datos de agenda ni de pacientes, y SHALL NOT modificar `DentistSecurityConfiguration` ni agregar mecanismos de acceso paralelos.

#### Scenario: Rechazo de llamadas directas sin sesión autenticada
- **WHEN** un cliente sin sesión autenticada solicita `GET /api/odontologo/agenda` o `GET /api/odontologo/turnos/{turnoId}`
- **THEN** la API responde `401` con `{"codigo":"SESSION_REQUIRED"}` sin devolver información de la agenda ni de pacientes.
