# Spec Delta

## MODIFIED Requirements

### Requirement: AC-02 Duración de motivos y reservas existentes

La duración de un motivo SHALL recibirse como un valor JSON entero entre 15 y 240 minutos, inclusive, y múltiplo de 15. Cambiar esa duración SHALL aplicar a nuevas reservas y la duración vigente SHALL regir al reprogramar; la modificación SHALL conservar el intervalo y la duración reservada de cada turno existente.

#### Scenario: Límites admitidos
- **WHEN** se crea o modifica un motivo con una duración entera de 15, 30, 45, hasta 240 minutos en pasos de 15
- **THEN** la API acepta la duración y la conserva como configuración vigente del motivo.

#### Scenario: Duración fraccionaria al crear
- **WHEN** `POST /api/odontologo/motivos` recibe una duración JSON fraccionaria, como `30.5`
- **THEN** la API responde `400 VALIDATION_ERROR` y no crea el motivo.

#### Scenario: Duración fraccionaria al modificar
- **WHEN** `PUT /api/odontologo/motivos/{motivoId}` recibe una duración JSON fraccionaria, como `30.5`
- **THEN** la API responde `400 VALIDATION_ERROR` y conserva sin cambios el motivo existente.

#### Scenario: Duración enviada como texto
- **WHEN** `POST /api/odontologo/motivos` o `PUT /api/odontologo/motivos/{motivoId}` recibe `duracionMinutos` como texto JSON, como `"45"`
- **THEN** la API responde `400 VALIDATION_ERROR` y no crea ni modifica el motivo.

#### Scenario: Duración fuera de límites o no múltiplo
- **WHEN** se envía una duración menor que 15, mayor que 240 o que no sea múltiplo de 15
- **THEN** la API responde `400 VALIDATION_ERROR` y no guarda el cambio.

#### Scenario: Cambio de duración con turnos existentes
- **WHEN** se modifica la duración configurada de un motivo que ya tiene turnos confirmados
- **THEN** esos turnos conservan su intervalo y duración reservada, y la nueva duración queda vigente para reservas y reprogramaciones posteriores.

### Requirement: AC-03 Consulta y reemplazo del horario semanal

La API interna SHALL permitir consultar y reemplazar en una sola operación el horario semanal mediante `GET` y `PUT /api/odontologo/horario`. Cada franja SHALL incluir `id`, `diaSemana` (lunes 1 a domingo 7), `horaInicio`, `horaFin` y `tipo` (`ATENCION` o `PAUSA`); sus horas locales se interpretarán en la zona configurada del consultorio. Las franjas `ATENCION` contiguas SHALL ser válidas.

#### Scenario: Reemplazo válido
- **WHEN** se envía un horario con días válidos e intervalos del mismo día con inicio anterior al fin, sin atención duplicada ni superpuesta y con cada pausa contenida en exactamente una franja de atención
- **THEN** la API reemplaza el horario completo y devuelve la configuración guardada.

#### Scenario: Atención contigua
- **WHEN** el horario incluye franjas `ATENCION` del mismo día que terminan y comienzan a la misma hora, como 09:00–10:00 y 10:00–11:00
- **THEN** la API acepta y conserva ambas franjas.

#### Scenario: Pausa no contenida o franjas semanales ambiguas
- **WHEN** el horario contiene una pausa fuera de atención o no contenida íntegramente en una sola franja, una pausa duplicada o superpuesta, una franja de atención duplicada o superpuesta, un día inválido o un intervalo que cruza medianoche
- **THEN** la API responde `400 VALIDATION_ERROR` y conserva íntegro el horario anterior.

#### Scenario: Reemplazo atómico
- **WHEN** cualquier franja del horario completo no supera la validación
- **THEN** no se persiste ninguna parte del reemplazo.

### Requirement: AC-05 Protección de turnos ante cambios de disponibilidad

Una modificación del horario semanal o un bloqueo nuevo SHALL rechazarse con `409 CONFIGURATION_CONFLICT` si deja un turno `CONFIRMADO` sin caber por completo dentro de una única franja `ATENCION` del mismo día, dentro de una pausa o intersectado por el bloqueo, sin cancelar ni desplazar el turno ni persistir parcialmente la configuración. La contigüidad entre franjas `ATENCION` SHALL seguir siendo válida para configurar horarios, pero no SHALL combinar varias franjas para cubrir un turno.

#### Scenario: Cambio semanal incompatible con un turno confirmado
- **WHEN** el reemplazo del horario excluye parte o todo el intervalo de un turno confirmado, o lo cubre con una pausa
- **THEN** la API responde `409 CONFIGURATION_CONFLICT` y conserva sin cambios el horario y el turno.

#### Scenario: Turno que cruza franjas contiguas
- **WHEN** un turno confirmado va de 09:30 a 10:30 y el nuevo horario contiene franjas `ATENCION` contiguas de 09:00–10:00 y 10:00–11:00
- **THEN** la API responde `409 CONFIGURATION_CONFLICT` y conserva el horario previo y el turno.

#### Scenario: Nuevo bloqueo sobre un turno confirmado
- **WHEN** el intervalo de un bloqueo nuevo se superpone, incluso parcialmente, con un turno confirmado
- **THEN** la API responde `409 CONFIGURATION_CONFLICT` y no persiste el bloqueo ni altera el turno.
