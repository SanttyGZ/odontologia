# Specification

## ADDED Requirements

### Requirement: PBW-01 Acceso público a consulta de motivos y disponibilidad
La interfaz web SHALL ofrecer la vista pública de reserva en `/reservar` (y enlace de navegación desde la pantalla de acceso) sin requerir cuenta, sesión ni token CSRF del odontólogo. Al cargar y al cambiar motivo o período (`desde`/`hasta`), SHALL consultar `GET /api/public/motivos` y `GET /api/public/disponibilidad` descartando respuestas obsoletas.

#### Scenario: Consulta válida y período sin huecos
- **WHEN** el paciente abre `/reservar`, elige un motivo y consulta un período válido
- **THEN** la interfaz muestra los horarios libres formateados en la `zonaHoraria` devuelta por el backend, o informa explícitamente la ausencia de horarios disponibles cuando `horarios` está vacío.

#### Scenario: Fallo al consultar motivos o disponibilidad
- **WHEN** `GET /api/public/motivos` o `GET /api/public/disponibilidad` falla por red o error `500`
- **THEN** la interfaz informa el fallo con opción de reintento y no presenta horarios falsos ni trata el error como agenda vacía.

### Requirement: PBW-02 Solicitud de reserva con cinco datos obligatorios y envío exacto
Una vez elegido un horario disponible, la interfaz SHALL solicitar `nombre`, `apellido`, `email`, `telefono` y `dni` obligatorios, validar que ninguno esté vacío antes de enviar y transmitir en `POST /api/public/turnos` el instante `inicio` exacto devuelto por `GET /api/public/disponibilidad` sin truncar segundos ni microsegundos. Mientras la solicitud está en curso, SHALL deshabilitar el envío repetido (`PB-08`).

#### Scenario: Validación de campos obligatorios y bloqueo de doble envío
- **WHEN** falta alguno de los cinco datos o el paciente intenta accionar nuevamente el envío mientras `POST /api/public/turnos` sigue pendiente
- **THEN** la interfaz impide el envío incompleto o duplicado y conserva los datos ingresados.

### Requirement: PBW-03 Tratamiento de conflictos, errores y resultados inciertos
Ante `400 VALIDATION_ERROR`, `404 MOTIVO_NOT_FOUND`, `409 SLOT_UNAVAILABLE`, `409 AGENDA_BUSY` o `500 TECHNICAL_FAILURE`, la interfaz SHALL mostrar el estado real sin presentar el turno como confirmado y conservando los datos del formulario. Ante `409 SLOT_UNAVAILABLE`, SHALL limpiar la selección vencida y actualizar la disponibilidad. Ante fallo de red o respuesta inválida en `POST /api/public/turnos`, SHALL advertir que el resultado es incierto sin afirmar confirmación ni descarte seguro.

#### Scenario: Conflicto concurrente por horario ocupado
- **WHEN** `POST /api/public/turnos` responde `409 SLOT_UNAVAILABLE`
- **THEN** la interfaz informa que el horario ya no está disponible, conserva los cinco datos del paciente y recarga los horarios libres del motivo.

#### Scenario: Resultado incierto ante corte de conexión
- **WHEN** `POST /api/public/turnos` falla con `NETWORK_ERROR` o `INVALID_RESPONSE`
- **THEN** la interfaz advierte que no pudo verificarse si la reserva se registró e indica revisar el correo o volver a consultar disponibilidad antes de reintentar.

### Requirement: PBW-04 Confirmación separada del estado del correo (PB-10)
Cuando `POST /api/public/turnos` responde `201 Created`, la interfaz SHALL presentar por separado el estado confirmado del turno (`CONFIRMADO`, motivo, intervalo en la zona del consultorio y duración reservada) y el estado de la notificación por correo (`notificacionEstado: "PENDIENTE"`), aclarando que la reserva no requiere verificación por email y que la respuesta HTTP no acredita recepción en el buzón.

#### Scenario: Reserva confirmada con correo pendiente
- **WHEN** `POST /api/public/turnos` devuelve `201 Created` con `estado: "CONFIRMADO"` y `notificacionEstado: "PENDIENTE"`
- **THEN** la pantalla muestra ambos estados en bloques diferenciados y permite iniciar una nueva consulta si el paciente lo desea.
