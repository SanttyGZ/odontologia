# Spec Delta

## Purpose

Permitir que una persona consulte y gestione únicamente el turno autorizado por su enlace privado, sin crear una cuenta. Las escrituras conservan la reserva y sus avisos coherentes ante errores o solicitudes concurrentes.

## ADDED Requirements

### Requirement: AG-01 Consulta privada y limitada del turno
La API SHALL exponer `GET /api/gestion/turno` para consultar solo el turno autorizado por C-04, recibiendo el secreto en `X-Management-Token` y devolviendo fecha, hora, estado y acciones disponibles, sin datos de contacto ni información de otros turnos. Las respuestas SHALL incluir `Cache-Control: no-store`; la consulta SHALL ser de solo lectura.

#### Scenario: Consulta válida sin efectos secundarios
- **WHEN** se consulta el turno enviando una credencial vigente en `X-Management-Token`
- **THEN** la API responde `200 OK` con fecha, hora, estado, acciones disponibles y un `ETag` opaco de la revisión actual, sin revelar datos de contacto, no almacena la respuesta en caché y no modifica turno, credencial o avisos

#### Scenario: Enlace no disponible
- **WHEN** la credencial está ausente, es inválida, venció, fue revocada o no corresponde a un turno disponible
- **THEN** la API responde `404 LINK_NOT_AVAILABLE` sin revelar el motivo específico, datos del turno ni acciones

#### Scenario: Aperturas repetidas o previsualizadas
- **WHEN** un navegador o escáner solicita varias veces la consulta del enlace
- **THEN** todas las respuestas autorizadas reflejan el turno actual y ninguna solicitud cambia su revisión, estado, credencial o avisos

### Requirement: AG-02 Cancelación expresa y dentro del plazo
La API SHALL exponer `POST /api/gestion/turno/cancelacion`; solo SHALL cancelar un turno confirmado con credencial vigente en `X-Management-Token`, `If-Match` correspondiente a la revisión actual y al menos 24 horas hasta su inicio, incluyendo el límite exacto, evaluado bajo coordinación de agenda. Las respuestas SHALL incluir `Cache-Control: no-store`.

#### Scenario: Cancelación confirmada
- **WHEN** se solicita expresamente cancelar un turno confirmado con credencial vigente y revisión actual exactamente a 24 horas o antes del inicio
- **THEN** la API responde `200 OK` con estado `CANCELADO` y notificación `PENDIENTE`, libera el intervalo, revoca la credencial y confirma la intención `CAMBIO` mientras deja inelegible el recordatorio anterior

#### Scenario: Cancelación fuera de plazo o sobre turno no confirmado
- **WHEN** el turno comienza en menos de 24 horas o no está confirmado
- **THEN** si el plazo de autogestión está cerrado la API responde `409 MANAGEMENT_WINDOW_CLOSED`; en ambos casos rechaza la operación sin cambiar el turno, la credencial ni sus avisos

#### Scenario: Repetición después de cancelar
- **WHEN** se vuelve a solicitar la cancelación usando el enlace revocado por una cancelación confirmada
- **THEN** la API responde `404 LINK_NOT_AVAILABLE` de forma genérica y no crea otra intención de aviso

### Requirement: AG-03 Reprogramación atómica con disponibilidad vigente
La API SHALL exponer `POST /api/gestion/turno/reprogramacion` y aceptar el nuevo inicio explícito. SHALL conservar el mismo turno confirmado, aplicar la duración actualmente configurada para su motivo y exigir credencial vigente en `X-Management-Token`, revisión actual y las reglas de ventana y disponibilidad al confirmar la operación. Las respuestas SHALL incluir `Cache-Control: no-store`.

#### Scenario: Reprogramación válida
- **WHEN** se solicita un nuevo inicio disponible con una credencial vigente, revisión actual y al menos 24 horas hasta el inicio actual del turno
- **THEN** la API responde `200 OK` con el mismo identificador de turno, nuevo intervalo, estado `CONFIRMADO`, notificación `PENDIENTE` y nueva revisión opaca; conserva la credencial vigente y confirma la intención `CAMBIO` y el recordatorio elegible para la nueva revisión

#### Scenario: Nuevo intervalo no disponible o fuera de ventana
- **WHEN** el nuevo inicio está ocupado, bloqueado, fuera de la disponibilidad configurada, fuera del mínimo inclusivo de 24 horas o fuera del máximo de 30 días calendario aplicable
- **THEN** la API responde `409 SLOT_UNAVAILABLE` y conserva sin cambios el intervalo original, su revisión, credencial e intenciones de aviso

#### Scenario: Duración actual del motivo
- **WHEN** se reprograma un turno cuyo motivo tiene una duración configurada distinta de la duración originalmente reservada
- **THEN** el nuevo intervalo usa la duración configurada vigente y el turno original permanece confirmado hasta confirmar conjuntamente el nuevo intervalo

#### Scenario: Plazo de autogestión cerrado
- **WHEN** el turno actual comienza en menos de 24 horas al evaluar la escritura bajo coordinación de agenda
- **THEN** la API responde `409 MANAGEMENT_WINDOW_CLOSED` y conserva el turno original, la credencial y sus intenciones

### Requirement: AG-04 Precondición de revisión para escrituras
Las operaciones de cancelación y reprogramación SHALL exigir el `ETag` opaco obtenido en la consulta vigente mediante `If-Match`. Una revisión obsoleta SHALL rechazarse sin sobrescribir cambios confirmados.

#### Scenario: Revisión obsoleta
- **WHEN** una escritura presenta un `If-Match` diferente de la revisión vigente del turno
- **THEN** la API responde `412 TURN_VERSION_MISMATCH` sin modificar turno, credencial ni avisos

#### Scenario: Precondición ausente
- **WHEN** una solicitud de cancelación o reprogramación no incluye `If-Match`
- **THEN** la API responde `428 PRECONDITION_REQUIRED` sin efectuar cambios

#### Scenario: Espera agotada por coordinación de agenda
- **WHEN** no se obtiene la coordinación de agenda dentro del límite vigente
- **THEN** la API responde `409 AGENDA_BUSY` sin efectuar cambios

### Requirement: AG-05 Errores sin efectos parciales
Ante una escritura rechazada o fallida, el sistema SHALL conservar el estado confirmado previamente y no dejar credenciales ni intenciones de aviso parcialmente aplicadas. Los errores públicos SHALL evitar datos personales y detalles internos.

#### Scenario: Reprogramación pierde una carrera por el horario
- **WHEN** otra escritura obtiene o bloquea primero el nuevo intervalo solicitado
- **THEN** la operación perdedora responde `409 SLOT_UNAVAILABLE` y conserva confirmado el intervalo original

#### Scenario: Fallo al confirmar turno o aviso
- **WHEN** falla la persistencia del turno, la revocación necesaria o la preparación de avisos antes de confirmar la operación
- **THEN** se revierten conjuntamente los cambios de la operación y no queda un turno, credencial o aviso parcialmente actualizado

#### Scenario: Error técnico
- **WHEN** ocurre un fallo técnico durante la lectura o escritura
- **THEN** la API responde con un error genérico sin excepción, secreto de credencial ni dato personal, y una escritura no deja efectos parciales

### Requirement: AG-06 Avisos de autogestión pendientes
Una cancelación o reprogramación confirmada SHALL preparar las intenciones de comunicación de C-05 dentro de la misma operación; SHALL NOT enviar correo SMTP desde esta API ni hacer depender la confirmación del turno del resultado de envío.

#### Scenario: Cambio confirmado sin envío síncrono
- **WHEN** se confirma una cancelación o reprogramación válida
- **THEN** la API informa la notificación como `PENDIENTE`, la intención `CAMBIO` corresponde al resultado y revisión confirmados, y cualquier recordatorio anterior queda inelegible según C-05 sin invocar SMTP

