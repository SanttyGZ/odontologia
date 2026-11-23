# appointment-management-credentials Specification

## Purpose
Autorizar la autogestión limitada a un turno y permitir componer enlaces privados para avisos persistidos.

## Requirements

### Requirement: EN-01 Autorización acotada
La autogestión SHALL exigir una credencial privada vigente para el turno solicitado. Fuentes RN-AU-01/02/04 y CT-02/03.

#### Scenario: CA-04-01 Credencial ausente o ilegítima
- **WHEN** se solicita acceso sin credencial, con una inválida, vencida o invalidada, o con una de otro turno
- **THEN** se deniega el acceso sin exponer datos protegidos ni efectuar cambios

#### Scenario: CA-04-02 Datos básicos
- **WHEN** se presenta DNI, email o UUID del turno como única prueba
- **THEN** la autogestión se deniega y la reserva pública sigue disponible sin cuenta ni verificación previa de correo

### Requirement: EN-02 Apertura y escrituras
Abrir un enlace SHALL permitir solo consulta autorizada. Las escrituras SHALL requerir acción expresa y validar alcance, plazo y disponibilidad aplicable. Fuentes RN-AU-04/05 y FL-05 a FL-07.

#### Scenario: CA-04-03 Previsualización
- **WHEN** un navegador o escáner abre repetidamente un enlace vigente
- **THEN** el turno, su revisión y sus avisos quedan intactos

#### Scenario: CA-04-04 Escritura rechazada
- **WHEN** se intenta cancelar fuera de plazo o reprogramar a un intervalo no disponible
- **THEN** la operación se rechaza y conserva el turno original

#### Scenario: CA-04-07 Consulta válida acotada
- **WHEN** se abre una credencial vigente del turno A y existe también un turno B
- **THEN** la consulta entrega fecha, hora, estado y acciones disponibles del turno A, sin mostrar datos de contacto, exponer el turno B ni modificar ningún turno

#### Scenario: CA-04-08 Vigencia y cambios del turno
- **WHEN** se reprograma un turno confirmado
- **THEN** el enlace existente consulta el nuevo horario y sus acciones vigentes hasta el nuevo fin previsto, excluido ese instante

#### Scenario: CA-04-09 Cancelación confirmada
- **WHEN** se abre un enlace anterior después de confirmar la cancelación
- **THEN** la credencial queda revocada y se informa enlace no disponible sin revelar datos ni permitir acciones

#### Scenario: CA-04-10 Borde de escritura
- **WHEN** una reserva o escritura de autogestión satisface sus demás condiciones y se evalúa exactamente a 24 horas bajo coordinación de agenda
- **THEN** se admite en ese borde, independientemente del vencimiento de consulta, y la reserva incluye el extremo de 30 días calendario a la misma hora local

### Requirement: EN-03 Composición interna durable
La composición autorizada SHALL poder recuperar el material de un enlace vigente referenciado por un aviso elegible tras reiniciar. El material privado SHALL quedar fuera de logs y respuestas públicas. Fuentes CHANGES C-04, CT-03/04 y AT-04.

#### Scenario: CA-04-05 Reinicio con aviso elegible
- **WHEN** se prepara un aviso con referencia vigente, se reinicia el proceso y se solicita la composición interna
- **THEN** se obtiene un enlace válido para el mismo turno y el secreto queda fuera de logs y respuestas públicas

#### Scenario: CA-04-06 Referencia inutilizable
- **WHEN** se pide componer el enlace con una referencia ajena, revocada o con material no recuperable
- **THEN** la composición solicitada falla sin divulgar material privado ni devolver un enlace para esa referencia

#### Scenario: CA-04-11 Clave de composición ausente
- **WHEN** falta la versión de clave necesaria para reconstruir una credencial vigente
- **THEN** falla la composición y se registra la causa sin secretos, mientras el enlace ya emitido conserva su validación hasta vencimiento o revocación

### Requirement: EN-04 Recuperación asistida
La operación interna de recuperación SHALL exigir un odontólogo autenticado y verificación confirmada. Para un turno confirmado y vigente, SHALL revocar las credenciales anteriores, emitir una nueva y coordinar con C-05 la entrega al destinatario confirmado en la misma operación.

#### Scenario: CA-04-12 Reemisión verificada
- **WHEN** el odontólogo confirma una comprobación presencial de documento o una devolución de llamada a un número previamente verificado y solicita recuperar acceso a un turno vigente
- **THEN** el enlace anterior deja de autorizar, se confirman juntos la emisión, la intención de recuperación y la actualización del recordatorio elegible para reasignación según C-05, y la respuesta devuelve una referencia operativa sin incluir el enlace ni copiar el documento

#### Scenario: CA-04-13 Recuperación rechazada
- **WHEN** falta autenticación o verificación suficiente, o el turno está cancelado o vencido
- **THEN** se deniega la reemisión y no se modifica el turno, las credenciales ni sus intenciones
