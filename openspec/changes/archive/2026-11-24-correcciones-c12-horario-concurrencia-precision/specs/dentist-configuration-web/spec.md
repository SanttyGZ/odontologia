# Spec Delta

## MODIFIED Requirements

### Requirement: CW-04 Edición web del horario semanal y pausas
La interfaz autenticada SHALL consultar `GET /api/odontologo/horario` descartando respuestas de consultas anteriores o tras desmontar. Si la consulta vigente falla, SHALL mostrar error con reintento y bloquear edición y guardado. Con respuesta válida, SHALL permitir editar franjas (`ATENCION`/`PAUSA`, días `1` a `7`), mostrar `HH:mm:ss` en controles nativos indicando fracciones de segundo, validar intervalos con precisión completa y guardar en un único `PUT /api/odontologo/horario`.

#### Scenario: Fallo en la consulta inicial de horario
- **WHEN** `GET /api/odontologo/horario` falla con `500 TECHNICAL_FAILURE` o error de red
- **THEN** la interfaz muestra el error y un botón de reintento, y bloquea la edición y el guardado de forma que jamás habilite un `PUT` con una lista vacía.

#### Scenario: Respuestas de consultas de horario fuera de orden
- **WHEN** una consulta inicial queda en vuelo, otra falla, el usuario acciona «Reintentar» obteniendo `09:00` y después resuelve la consulta anterior con `08:00`
- **THEN** la interfaz descarta la respuesta tardía sin alterar pantalla ni estado y el `PUT` posterior conserva `09:00`.

#### Scenario: Consulta exitosa sin franjas configuradas
- **WHEN** `GET /api/odontologo/horario` responde `200` con `franjas: []`
- **THEN** la interfaz habilita el formulario para agregar franjas y crear un horario.

#### Scenario: Preservación de precisión en horas no editadas
- **WHEN** `GET /api/odontologo/horario` devuelve franjas con segundos como `09:00:30`–`12:00:45` y el usuario guarda sin modificarlas o editando otra franja
- **THEN** el cuerpo de `PUT /api/odontologo/horario` conserva exactamente `09:00:30` y `12:00:45` sin recortarlos.

#### Scenario: Horas con microsegundos en controles nativos
- **WHEN** `GET /api/odontologo/horario` devuelve una hora con microsegundos como `09:00:30.123456`
- **THEN** el control de hora muestra `09:00:30` con la fracción `.123456` indicada junto al campo, conserva `09:00:30.123456` en el `PUT` mientras no se edite ese campo y valida `inicio < fin` usando la precisión completa de microsegundos.

#### Scenario: Reemplazo válido del horario semanal
- **WHEN** el odontólogo guarda franjas válidas y `PUT /api/odontologo/horario` responde `200`
- **THEN** la interfaz actualiza el horario mostrado con las franjas devueltas por el backend y confirma el guardado.

#### Scenario: Conflicto con turnos confirmados o error de validación
- **WHEN** `PUT /api/odontologo/horario` responde `409 CONFIGURATION_CONFLICT` o `400 VALIDATION_ERROR`
- **THEN** la interfaz informa el conflicto o error de validación y conserva en pantalla la edición pendiente para que el odontólogo la corrija o descarte sin perder su trabajo.
