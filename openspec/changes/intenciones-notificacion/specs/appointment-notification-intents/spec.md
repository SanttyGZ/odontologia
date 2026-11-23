# Spec Delta

## Purpose

Persistir intenciones de comunicación coherentes con la revisión del turno y recuperarlas para el emisor local.

## ADDED Requirements

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
