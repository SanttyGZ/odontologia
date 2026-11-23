# appointment-notification-intents Specification

## Purpose
Persistir intenciones de comunicación coherentes con la revisión del turno y recuperarlas para el emisor local.

## Requirements

### Requirement: NO-01 Datos de la intención
El sistema SHALL conservar tipo, turno, revisión, destinatario previsto, programación, estado e intentos de cada intención por separado del turno. Fuentes RN-NO-01/02/05 y FL-08.

#### Scenario: CA-05-01A Reserva nominal
- **WHEN** se confirma una reserva válida con más de 24 horas de anticipación, revisión R y destinatario previsto E
- **THEN** se persisten una intención CONFIRMACION prevista para procesamiento inmediato y una RECORDATORIO programada 24 horas antes del inicio, ambas para el mismo turno, revisión R y destinatario E

#### Scenario: CA-05-01B Cancelación nominal
- **WHEN** se cancela válidamente un turno y se confirma su estado CANCELADO con revisión resultante R y destinatario previsto E
- **THEN** se persiste una intención CAMBIO por la cancelación, asociada al mismo turno, revisión R y destinatario E, y el recordatorio anterior deja de ser elegible para nuevos envíos

#### Scenario: CA-05-01C Reprogramación nominal
- **WHEN** se reprograma válidamente un turno a un inicio con más de 24 horas de anticipación, nueva revisión R y destinatario previsto E
- **THEN** se persiste una intención CAMBIO por la reprogramación y queda un recordatorio elegible programado 24 horas antes del nuevo inicio, ambos asociados al mismo turno, revisión R y destinatario E

#### Scenario: CA-05-02 Destinatario persistido
- **WHEN** se edita el email del paciente después de preparar un aviso
- **THEN** la edición por sí sola conserva el destinatario del aviso persistido

### Requirement: NO-02 Confirmación conjunta
El cambio de turno, la credencial emitida en esa operación cuando corresponda y sus intenciones durables SHALL compartir confirmación o rollback según AW-03/05 y el alcance de C-04/C-05. Las llamadas SMTP quedan fuera de la transacción.

#### Scenario: CA-05-03 Rollback conjunto
- **WHEN** dentro de una reserva coordinada se escriben el turno, la credencial y al menos una intención que referencia la credencial, y se provoca un fallo antes de confirmar
- **THEN** una lectura posterior de la base comprueba que no quedaron el turno nuevo, la credencial nueva ni ninguna intención de esa operación, que no quedaron referencias huérfanas y que los registros previos siguen intactos

### Requirement: NO-03 Unicidad del evento
El sistema SHALL impedir la creación repetida de una misma intención por evento de origen. Fuentes RN-NO-06, PB-08 y PB-11.

#### Scenario: CA-05-04 Duplicado concurrente
- **WHEN** dos escrituras representan el mismo evento y tipo según la identidad acordada
- **THEN** solo persiste una intención para esa identidad

### Requirement: NO-04 Revisión vigente
El sistema SHALL evitar que un recordatorio obsoleto pueda iniciar un envío después de que la cancelación o reprogramación lo haya dejado inelegible. Fuentes RN-NO-04, PB-11 y AT-06.

#### Scenario: CA-05-05 Cambio antes del envío
- **WHEN** se confirma una cancelación o reprogramación y el emisor todavía no ha seleccionado ni iniciado el procesamiento del recordatorio anterior
- **THEN** el emisor no puede iniciar un envío del recordatorio de la revisión anterior

### Requirement: NO-05 Resultado de comunicación independiente
El resultado de un intento de comunicación SHALL mantenerse separado del estado del turno. La aceptación SMTP SHALL registrarse como aceptación del servidor. Fuentes RN-NO-02/03 y FL-08.

#### Scenario: CA-05-06 Fallo del emisor
- **WHEN** falla el procesamiento de un aviso de un turno ya confirmado
- **THEN** el turno mantiene su estado y el resultado del intento queda en el dominio de comunicaciones

#### Scenario: CA-05-07 Aceptación SMTP
- **WHEN** el servidor SMTP acepta un mensaje
- **THEN** el registro indica aceptación SMTP y la evidencia de recepción queda sujeta a una comprobación de buzón

### Requirement: NO-06 Recuperación y despacho combinado
Las intenciones SHALL conservar la identidad del evento, revisión y destinatario. Un despacho combinado SHALL registrar un resultado conjunto para sus dos intenciones. La recuperación asistida SHALL coordinar la entrega con la nueva referencia de C-04.

#### Scenario: CA-05-08 Reserva exactamente a 24 horas
- **WHEN** se admite la reserva en el borde de 24 horas
- **THEN** existen una CONFIRMACION y un RECORDATORIO elegibles de inmediato, con revisión, destinatario e identidad de despacho comunes, y un solo intento simulado actualiza ambos resultados

#### Scenario: CA-05-09 Aviso de recuperación
- **WHEN** C-04 confirma una recuperación asistida con destinatario confirmado
- **THEN** se registra una CONFIRMACION de recuperación y el recordatorio vigente sin autorización ni historial mantiene identidad, revisión y programación con la nueva referencia y el destinatario confirmado, sin crear otro recordatorio

#### Scenario: CA-05-13 Recuperación después de autorizar un recordatorio
- **WHEN** se recupera el acceso después de autorizar un intento del recordatorio o mientras su entrega es incierta
- **THEN** se conservan los datos e historial del intento, no se reasigna ni reintenta el aviso ligado al enlace revocado y la confirmación de recuperación se registra por separado

#### Scenario: CA-05-14 Rollback de recuperación
- **WHEN** la transacción de recuperación falla tras emitir la credencial y reasignar el recordatorio pendiente
- **THEN** se conservan la credencial anterior, el destinatario, la referencia, la identidad y la programación del recordatorio, sin nuevas credenciales ni avisos huérfanos

### Requirement: NO-07 Autorización y resultado del intento
La autorización durable de un intento SHALL comprobar la elegibilidad vigente bajo coordinación. El cambio posterior SHALL impedir nuevos intentos de la revisión anterior. Un resultado incierto SHALL quedar diferenciado de un rechazo conocido y fuera del reintento automático.

#### Scenario: CA-05-10 Intento anterior ya autorizado
- **WHEN** se autoriza el intento anterior y después se confirma una cancelación o reprogramación
- **THEN** el intento autorizado puede terminar incluso después del aviso de cambio, se registra su propio resultado, no se reintenta la revisión anterior y existe una intención CAMBIO de la revisión vigente

#### Scenario: CA-05-11 Referencia revocada antes de componer
- **WHEN** el intento autorizado todavía necesita componer el enlace y C-04 ya revocó su referencia
- **THEN** se registra el fallo de composición y ese intento no envía el mensaje

#### Scenario: CA-05-12 Reintentos y vencimiento
- **WHEN** con reloj controlado se registra un fallo transitorio conocido, un resultado incierto o un aviso vencido
- **THEN** los fallos transitorios vigentes admiten como máximo tres intentos con esperas de 1 y 5 minutos, y ni el resultado incierto ni el aviso vencido habilitan reintento automático
