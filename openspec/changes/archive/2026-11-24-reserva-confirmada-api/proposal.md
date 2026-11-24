# Proposal

## Why

Los pacientes necesitan confirmar una reserva web sin crear cuenta ni verificar su email previamente (`US-005`, `FL-04`), garantizando mediante coordinación de agenda en PostgreSQL que no existan turnos confirmados superpuestos (`CT-01`) y originando en la misma transacción la credencial de autogestión (`C-04`) y las intenciones durables de notificación (`C-05`).

## What Changes

- Exponer `POST /api/public/turnos` para recibir `motivoId`, `inicio` y los cinco datos obligatorios del paciente (`nombre`, `apellido`, `email`, `telefono`, `dni`).
- Aplicar las decisiones aprobadas por el usuario el 2026-11-24 para `C-08`:
  - `PB-04`: validar los cinco datos obligatorios; reutilizar `Paciente` únicamente cuando coinciden exactamente los cinco campos en el consultorio y crear uno nuevo en cualquier otro caso, sin sobrescribir ni exponer datos previos.
  - `PB-08`: sin idempotencia por cliente; cada solicitud compite bajo `AgendaWriteCoordinator` y cualquier solapamiento o repetición recibe `409 SLOT_UNAVAILABLE`.
  - `PB-10`: devolver `201 Created` con el turno `CONFIRMADO` y `notificacionEstado: "PENDIENTE"` separado, sin depender de SMTP.
- Revalidar motivo, ventana `PB-01`, regla de franja `ATENCION`/pausa (`PB-03`), bloqueos y ocupación dentro de `AgendaWriteCoordinator.write(...)` antes de insertar el turno, emitir su credencial y preparar las intenciones de confirmación y recordatorio.

## Capabilities

### New Capabilities
- `public-booking`: reserva pública confirmada atómicamente con prevención de solapamientos, política de paciente `PB-04` y preparación durable de credencial e intenciones de aviso.

### Modified Capabilities
- None.

## Impact

- Código backend en `dev.odontologia.disponibilidad` / `dev.odontologia.pacientes` (`PublicBookingController`, `PublicBookingService`, `PublicBookingDtos`, `PacienteQueries`, `AvailabilityRules`, `AvailabilityErrorAdvice`).
- Consume `AgendaWriteCoordinator` (`C-02`), `CredencialesGestion` (`C-04`) y `PreparacionAvisos` (`C-05`) sin modificar seguridad, credenciales ni esquema PostgreSQL.
