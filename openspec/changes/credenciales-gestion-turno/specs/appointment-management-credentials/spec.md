# Spec Delta

## Purpose

Autorizar la autogestión limitada a un turno y permitir componer enlaces privados para avisos persistidos.

## ADDED Requirements

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
- **THEN** la consulta entrega únicamente los datos permitidos del turno A, usando la lista de campos que se apruebe en PB-07, y no expone el turno B ni modifica el estado de ningún turno

### Requirement: EN-03 Composición interna durable
La composición autorizada SHALL poder recuperar el material de un enlace vigente referenciado por un aviso elegible tras reiniciar. El material privado SHALL quedar fuera de logs y respuestas públicas. Fuentes CHANGES C-04, CT-03/04 y AT-04.

#### Scenario: CA-04-05 Reinicio con aviso elegible
- **WHEN** se prepara un aviso con referencia vigente, se reinicia el proceso y se solicita la composición interna
- **THEN** se obtiene un enlace válido para el mismo turno y el secreto queda fuera de logs y respuestas públicas

#### Scenario: CA-04-06 Referencia inutilizable
- **WHEN** se pide componer el enlace con una referencia ajena, revocada o con material no recuperable
- **THEN** la composición solicitada falla sin divulgar material privado ni devolver un enlace para esa referencia
