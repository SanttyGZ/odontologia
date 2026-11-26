# dentist-agenda-web Specification

## Purpose

Precisar el comportamiento de cierre de detalle obsoleto, visibilidad de revisión `0` y contención horizontal de textos largos en `dentist-agenda-web`.

## MODIFIED Requirements

### Requirement: AW-02 Consulta de detalle de turno y cinco datos básicos del paciente

Al seleccionar un turno, SHALL consultar `GET /api/odontologo/turnos/{turnoId}` y mostrar motivo, intervalo en `zonaHoraria`, duración, estado (`CONFIRMADO` o `CANCELADO`), `Revisión` siempre visible (incluido `0`), `creadoEn`, `canceladoEn` y los cinco datos básicos (`nombre`, `apellido`, `email`, `telefono`, `dni`). SHALL NOT mostrar estado `REPROGRAMADO` ni campos clínicos (`CA-US-006-02`). Ante `404 TURNO_NOT_FOUND`, SHALL informarlo sin romper la agenda.

#### Scenario: Apertura exitosa de detalle con los cinco datos básicos del paciente

- **WHEN** el odontólogo selecciona un turno de la agenda (incluido un turno recién reservado con `revision: 0`) y `GET /api/odontologo/turnos/{turnoId}` responde `200 OK`
- **THEN** la interfaz muestra el panel de detalle con `motivoNombre`, intervalo en `zonaHoraria`, `duracionReservadaMinutos`, `estado`, `Revisión` siempre visible (incluido `0`), `creadoEn` y los cinco campos de `paciente` (`nombre`, `apellido`, `email`, `telefono`, `dni`), y permite cerrarlo

#### Scenario: Turno reprogramado o cancelado reflejado fielmente según su estado y metadatos

- **WHEN** corresponde a un turno reprogramado (`estado: CONFIRMADO`, `revision > 0`, `canceladoEn: null`) o cancelado (`estado: CANCELADO`, `canceladoEn` presente)
- **THEN** muestra «Confirmado» con la revisión actualizada sin estado `REPROGRAMADO` en el primer caso, y «Cancelado» con `canceladoEn` formateado en `zonaHoraria` en el segundo caso

#### Scenario: Turno inexistente al consultar detalle

- **WHEN** `GET /api/odontologo/turnos/{turnoId}` responde `404 TURNO_NOT_FOUND`
- **THEN** informa que el turno no se encontró y mantiene operativa la lista de agenda

### Requirement: AW-04 Distinción de agenda vacía, carga, fallos de consulta y expiración de sesión

La interfaz SHALL distinguir carga (`aria-busy`), agenda vacía (`turnos: []`), errores de consulta y `401 SESSION_REQUIRED`. Al cambiar fecha o vista (`DIA`/`SEMANA`), SHALL conservar el detalle solo si el turno pertenece al nuevo resultado, cerrándolo si queda fuera, si está vacío o si falla la consulta, e invalidando respuestas demoradas tras cambiar de período o cerrar manualmente.

#### Scenario: Período sin turnos muestra estado vacío y no error

- **WHEN** `GET /api/odontologo/agenda` responde `200 OK` con `turnos: []`
- **THEN** la interfaz muestra un mensaje informativo de agenda vacía para el período seleccionado sin alertas de error y cierra cualquier detalle abierto

#### Scenario: Cambio de período mantiene o cierra el detalle según pertenencia

- **WHEN** el odontólogo tiene un detalle abierto y cambia fecha o alterna entre `DIA` y `SEMANA`
- **THEN** el detalle solo permanece visible si el turno seleccionado pertenece al nuevo resultado de `GET /api/odontologo/agenda`, cerrándose en caso contrario

#### Scenario: Respuesta demorada de detalle tras cambiar de período o cerrar manualmente no reabre el panel

- **WHEN** una consulta `GET /api/odontologo/turnos/{turnoId}` en vuelo resuelve después de cambiar de período o de accionar «Cerrar detalle»
- **THEN** la respuesta demorada se descarta y no reabre el panel de detalle ni pisa el estado vigente

#### Scenario: Fallo de consulta informa imposibilidad con reintento sin presentar datos previos no recuperados como actuales

- **WHEN** el odontólogo cambia de período o reintenta una consulta de agenda y el backend responde con error (`400`, `403`, `500`, `NETWORK_ERROR`, `INVALID_RESPONSE` o fecha inválida)
- **THEN** la interfaz limpia los turnos y el detalle previos para no mostrarlos como actuales, informa el error de consulta y ofrece un botón «Reintentar»

#### Scenario: Respuestas de consultas fuera de orden se descartan

- **WHEN** una primera solicitud de agenda o detalle tarda más que una segunda solicitud posterior y resuelve después
- **THEN** la interfaz ignora la respuesta obsoleta de la primera solicitud y conserva exclusivamente los datos de la última solicitud emitida

#### Scenario: Respuesta 401 SESSION_REQUIRED redirige al formulario de ingreso

- **WHEN** `GET /api/odontologo/agenda` o `GET /api/odontologo/turnos/{turnoId}` responde `401 SESSION_REQUIRED`
- **THEN** la interfaz ejecuta el manejador de sesión expirada de `C-12`, limpia el estado autenticado y vuelve a mostrar el formulario de ingreso

### Requirement: AW-05 Interfaz de solo lectura, accesibilidad por teclado y diseño adaptable

La vista de agenda SHALL ser de solo lectura sin edición de pacientes, clínica, cobros ni operaciones manuales (`RN-PA-03`, `RN-EX-03`, `PB-06`). Nombres, apellidos y motivos largos sin espacios SHALL partirse en varias líneas sin ocultar texto ni controles y sin desbordamiento horizontal en `390×844` y `1440×900`, manteniendo operación completa por teclado.

#### Scenario: Ausencia de controles de edición de pacientes u operaciones manuales pendientes

- **WHEN** el odontólogo navega por la lista de agenda y abre el detalle de un turno
- **THEN** no existen controles ni formularios para modificar pacientes, cargar historia clínica o cobros, ni crear, cancelar o reprogramar turnos manualmente

#### Scenario: Operación por teclado y legibilidad en pantallas pequeña y grande

- **WHEN** el usuario opera los controles de vista, fecha, actualización y selección/cierre de detalle con teclado en resoluciones `390×844` y `1440×900`, incluyendo apellidos de 100 caracteres sin espacios y motivos largos con detalle abierto o cerrado
- **THEN** todos los botones y entradas son alcanzables con foco visible y etiquetas accesibles, los textos largos se parten en varias líneas sin ocultar texto ni controles y no aparece desplazamiento horizontal (`scrollWidth <= innerWidth`)
