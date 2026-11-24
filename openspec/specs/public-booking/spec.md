# public-booking Specification

## Purpose

TBD - created by archiving change reserva-confirmada-api. Update Purpose after archive.

## Requirements

### Requirement: PB-RES-01 Reserva pública confirmada sin cuenta ni verificación previa
La API pública SHALL exponer `POST /api/public/turnos` sin requerir autenticación ni verificación previa de email, aceptando `motivoId` (UUID), `inicio` (Instant ISO-8601) y los cinco datos obligatorios del paciente (`nombre`, `apellido`, `email`, `telefono`, `dni`).

#### Scenario: Reserva válida confirmada automáticamente
- **WHEN** se envía una solicitud válida para un motivo existente y un inicio disponible dentro de la ventana `PB-01`
- **THEN** la API responde `201 Created` con `turnoId`, `motivoId`, `inicio`, `fin`, `duracionReservadaMinutos`, `estado: "CONFIRMADO"` y `notificacionEstado: "PENDIENTE"`, sin incluir datos de otros pacientes ni material de credencial.

#### Scenario: Datos obligatorios faltantes o inválidos
- **WHEN** falta alguno de los cinco datos del paciente, `nombre`/`apellido`/`dni`/`telefono`/`email` no supera la validación, o `motivoId`/`inicio` son nulos o inválidos
- **THEN** la API responde `400 VALIDATION_ERROR` y no persiste paciente, turno, credencial ni notificación.

### Requirement: PB-RES-02 Política de paciente sin sobrescritura ni divulgación (PB-04)
Al confirmar una reserva, el sistema SHALL reutilizar un registro `Paciente` del consultorio únicamente si coinciden exactamente los cinco datos normalizados (`nombre`, `apellido`, `email`, `telefono`, `dni`); en cualquier otro caso SHALL crear un nuevo registro `Paciente` sin sobrescribir, fusionar ni exponer datos previos.

#### Scenario: Coincidencia parcial de DNI o email
- **WHEN** se reserva con un DNI o email ya existente pero difiere alguno de los cinco campos
- **THEN** se crea un nuevo `Paciente` sin modificar ni devolver los datos del registro anterior.

#### Scenario: Coincidencia exacta de los cinco campos
- **WHEN** se reserva con los cinco campos idénticos a un `Paciente` existente del consultorio
- **THEN** se reutiliza ese `Paciente` para el nuevo turno sin duplicar el registro.

### Requirement: PB-RES-03 Revalidación bajo bloqueo y concurrencia sin solapamientos (PB-08)
Dentro de `AgendaWriteCoordinator.write(...)`, la reserva SHALL revalidar motivo, ventana `PB-01`, anclaje cada 15 minutos en una única franja `ATENCION` sin pausas, y ausencia de bloqueos y turnos `CONFIRMADO` superpuestos antes de persistir turno, credencial (`CredencialesGestion.emitir`) e intenciones (`PreparacionAvisos.reserva`). Ante solapamiento o repetición, a lo sumo una reserva SHALL confirmarse y la perdedora SHALL recibir `409 SLOT_UNAVAILABLE` sin efectos parciales.

#### Scenario: Reservas simultáneas iguales o parcialmente solapadas
- **WHEN** dos solicitudes concurrentes compiten por el mismo intervalo o intervalos parcialmente superpuestos
- **THEN** exactamente una obtiene `201 Created` y la otra recibe `409 SLOT_UNAVAILABLE` con rollback completo de su transacción.

#### Scenario: Carrera con cambio de configuración o bloqueo
- **WHEN** una modificación de horario o bloqueo se confirma antes que la reserva pendiente
- **THEN** la reserva relee la configuración vigente bajo bloqueo, rechaza el intervalo con `409 SLOT_UNAVAILABLE` y no persiste turno, credencial ni notificación.
