# Specification

## MODIFIED Requirements

### Requirement: AQ-01 Consulta interna de agenda diaria y semanal en la zona del consultorio
La API interna SHALL exponer `GET /api/odontologo/agenda` con `vista` opcional (`DIA` por defecto o `SEMANA` lunes a domingo ISO) y `fecha` opcional. Si `fecha` se omite, SHALL usar hoy en `consultorio.zonaHoraria` vía `Clock`; si se envía, SHALL cumplir exactamente `YYYY-MM-DD` (4 dígitos de año, sin offset ni espacios) y ser una fecha de calendario válida. La respuesta `200 OK` SHALL devolver `vista`, `desde`, `hasta`, `zonaHoraria` y `turnos` (`CONFIRMADO` y `CANCELADO`) ordenados por `inicio ASC, id ASC`.

#### Scenario: Consulta diaria y semanal interpretada en la zona horaria del consultorio
- **WHEN** el odontólogo autenticado consulta `GET /api/odontologo/agenda` con `vista=DIA` o `vista=SEMANA` y una `fecha` `YYYY-MM-DD` válida como `2026-11-26` (o la omite para usar `Clock` en `consultorio.zonaHoraria`)
- **THEN** la API interpreta los límites de medianoche local en `consultorio.zonaHoraria`, incluye los turnos cuya duración intersecta `[periodoInicio, periodoFin)` y excluye turnos contiguos donde `fin == periodoInicio` o `inicio == periodoFin`.

#### Scenario: Inclusión de turnos confirmados y cancelados sin borrado físico
- **WHEN** en el período consultado existen turnos en estado `CONFIRMADO` y `CANCELADO`
- **THEN** la API devuelve `200 OK` incluyendo ambos estados ordenados por `inicio ASC, id ASC` con `id`, `motivoId`, `motivoNombre`, `inicio`, `fin`, `duracionReservadaMinutos`, `estado`, `pacienteNombre` y `pacienteApellido`.

#### Scenario: Distinción entre agenda vacía y fallo de consulta
- **WHEN** no existen turnos en el período consultado
- **THEN** la API responde `200 OK` con `turnos: []`; si falta la configuración del consultorio o falla la persistencia, responde `500 TECHNICAL_FAILURE`.

#### Scenario: Rechazo de parámetros de filtro inválidos
- **WHEN** `vista` es distinta de `DIA` o `SEMANA`, o `fecha` presente es vacía, tiene espacios, offset (`2026-11-26Z`, `2026-11-26-03:00`), fecha de calendario inexistente (`2026-02-30`) o año extendido (`+10000-01-01`, `+999999999-12-31`)
- **THEN** la API responde `400` con `{"codigo":"VALIDATION_ERROR"}` antes de invocar al servicio.

### Requirement: AQ-02 Consulta de detalle de turno y cinco datos básicos del paciente
La API interna SHALL exponer `GET /api/odontologo/turnos/{turnoId}` exigiendo que `{turnoId}` tenga formato UUID hexadecimal estricto `8-4-4-4-12` (aceptando mayúsculas y minúsculas), devolviendo `200 OK` con `id`, `motivoId`, `motivoNombre`, `inicio`, `fin`, `duracionReservadaMinutos`, `estado`, `revision`, `creadoEn`, `canceladoEn` y `paciente` (`nombre`, `apellido`, `email`, `telefono`, `dni`). SHALL NOT exponer credenciales, notificaciones, `agendaId`, `consultorioId`, historia clínica ni cobros.

#### Scenario: Consulta exitosa de detalle con los cinco datos básicos del paciente
- **WHEN** el odontólogo autenticado consulta `GET /api/odontologo/turnos/{turnoId}` con un UUID `8-4-4-4-12` (en mayúsculas o minúsculas) de un turno existente (`CONFIRMADO` o `CANCELADO`)
- **THEN** la API responde `200 OK` con los datos del turno, su motivo, `revision`, `creadoEn`, `canceladoEn` y `paciente` (`nombre`, `apellido`, `email`, `telefono`, `dni`), sin exponer credenciales, notificaciones ni campos clínicos.

#### Scenario: Turno inexistente o identificador con formato inválido
- **WHEN** `{turnoId}` es un UUID `8-4-4-4-12` válido inexistente en la agenda única
- **THEN** la API responde `404` con `{"codigo":"TURNO_NOT_FOUND"}`; cuando `{turnoId}` no cumple el formato hexadecimal `8-4-4-4-12` (como `1-1-1-1-1` o `no-es-uuid`), responde `400` con `{"codigo":"VALIDATION_ERROR"}` antes de invocar al servicio.
