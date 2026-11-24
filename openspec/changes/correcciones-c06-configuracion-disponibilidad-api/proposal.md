# Proposal

## Why

C-06 está archivado, pero la implementación permite que Jackson trunque duraciones decimales antes de validarlas y considera cubierto un turno que cruza dos franjas contiguas. Además, la prueba de concurrencia con configuración primero evita el servicio real de C-06, por lo que no demuestra la ruta de escritura coordinada solicitada.

## What Changes

- Rechazar valores JSON decimales de duración en `POST` y `PUT /api/odontologo/motivos`, devolver `400 VALIDATION_ERROR` y conservar el estado; mantener aceptadas las duraciones enteras aprobadas.
- Exigir que cada turno confirmado quede completamente dentro de una única franja `ATENCION`; seguir permitiendo configurar franjas contiguas y rechazar con `409 CONFIGURATION_CONFLICT` los reemplazos incompatibles sin alterar horario ni turno.
- Probar el orden de concurrencia configuración-primero mediante el servicio real de C-06 y `AgendaWriteCoordinator`, con sincronización explícita y PostgreSQL real.
- Registrar la evidencia de aprobación de PB-02, PB-03 y PB-05 sin reabrir esas decisiones ni implementar C-07.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `availability-configuration`: precisar la aceptación de duraciones como enteros JSON y la regla de cobertura de turnos confirmados por una sola franja de atención.

## Impact

Se afectan los DTO y la validación de C-06, sus pruebas HTTP y de integración PostgreSQL, el test de concurrencia, el delta OpenSpec y el registro de decisiones PB. No se cambia el esquema, autenticación, CSRF, credenciales ni el alcance de C-07.
