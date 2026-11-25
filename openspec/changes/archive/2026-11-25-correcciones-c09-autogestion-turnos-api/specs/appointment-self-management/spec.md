# Spec Delta

## ADDED Requirements

### Requirement: AG-07 Destinatario y enlace vigente de avisos de autogestión
Las nuevas intenciones de una cancelación o reprogramación válida SHALL usar el destinatario confirmado en la recuperación asistida más reciente, si existe; en otro caso SHALL usar el email vigente del paciente. La reprogramación SHALL asociar sus nuevas intenciones al enlace vigente. Estas operaciones SHALL NOT modificar el email del paciente ni los intentos de envío existentes.

#### Scenario: Cancelación después de recuperar acceso con email corregido
- **WHEN** se cancela válidamente el turno después de una recuperación asistida que confirmó un nuevo destinatario
- **THEN** la intención `CAMBIO` nueva usa el destinatario confirmado y el sistema conserva el email del paciente y los intentos de envío anteriores

#### Scenario: Reprogramación después de recuperar acceso con email corregido
- **WHEN** se reprograma válidamente el turno después de una recuperación asistida que confirmó un nuevo destinatario
- **THEN** las intenciones `CAMBIO` y `RECORDATORIO` nuevas usan ese destinatario, hacen referencia al enlace vigente y el sistema conserva el email del paciente y los intentos de envío anteriores

#### Scenario: Sin recuperación asistida previa
- **WHEN** se cancela o reprograma válidamente un turno que no tiene una recuperación asistida
- **THEN** las nuevas intenciones usan el email vigente del paciente

### Requirement: AG-08 Rechazo de reprogramación al mismo intervalo
La API SHALL rechazar una reprogramación cuando, después de aplicar la duración vigente del motivo, tanto el inicio como el fin coincidan con los del turno actual. El rechazo SHALL responder `409 SAME_INTERVAL` y SHALL NOT modificar la revisión, la credencial ni las intenciones de aviso.

#### Scenario: Inicio y fin calculados coinciden con el turno actual
- **WHEN** el nuevo inicio y el fin calculado con la duración vigente son iguales al intervalo actual
- **THEN** la API responde `409 SAME_INTERVAL` y conserva la revisión, la credencial y todas las intenciones de aviso
