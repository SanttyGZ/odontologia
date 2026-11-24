# Proposal

## Why

Los pacientes necesitan consultar los motivos de consulta y los horarios realmente disponibles antes de solicitar una reserva (`US-004` / `FL-03`), sin crear cuenta ni exponer datos de la agenda o de otros pacientes. Con `C-06` y la resolución de `PB-01` ya definidas, es posible habilitar los endpoints públicos de solo lectura.

## What Changes

- Resolver `PB-01` en la KB con los bordes aprobados por el usuario: límite inferior inclusivo a 24 horas (`inicio >= ahora + 24h`) y límite superior calendario inclusivo hasta `fecha(ahora) + 30 días` en la zona del consultorio.
- Exponer `GET /api/public/motivos` para listar motivos de consulta sin datos internos de agenda.
- Exponer `GET /api/public/disponibilidad` para calcular inicios disponibles cada 15 minutos anclados al comienzo de cada franja `ATENCION`, validando duración completa dentro de una única franja, pausas, bloqueos, turnos `CONFIRMADO`, zona horaria del consultorio y ventana temporal `PB-01`.
- Mantener la consulta libre de mutaciones, reservas implícitas y datos de pacientes, reutilizando `AvailabilityErrorAdvice` y el `Clock` inyectado para pruebas determinísticas.

## Capabilities

### New Capabilities
- `public-availability`: consulta pública de motivos y cálculo de horarios disponibles según configuración, ocupación, zona y ventana temporal aprobada.

### Modified Capabilities
- None.

## Impact

- Código backend en `dev.odontologia.disponibilidad` (`PublicAvailabilityController`, `PublicAvailabilityService`, `PublicAvailabilityDtos`, extensión de `AvailabilityRules` y `AvailabilityErrorAdvice`).
- Sin cambios en esquema PostgreSQL, seguridad (`/api/public/**` ya está fuera del filtro `/api/odontologo/**`), credenciales ni notificaciones.
