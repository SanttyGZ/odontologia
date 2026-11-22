# Funcionalidades

**Estado del documento**: aprobado por el usuario el 2026-11-18.
**Confirmación**: «Aprobar el documento 06 y continuar con el 07».
**Fecha**: 2026-11-18.
**Base**: [01 — Visión y aceptación](01_vision_y_objetivos.md), [02 — Descripción](02_descripcion_general.md), [03 — Actores](03_actores_y_roles.md), [04 — Datos](04_modelo_de_datos.md) y [05 — Reglas](05_reglas_de_negocio.md), aprobados.

El usuario aprobó el desglose en historias y criterios de este documento para un único consultorio. Ninguna funcionalidad está implementada o probada; las casillas quedan sin marcar. Los criterios que requieren una respuesta a las preguntas `PB-NN` del documento 05 no se consideran cerrados hasta resolverla. La aprobación no decide sus respuestas.

## Convenciones y prioridad

- `US-NNN` identifica una historia; `CA-US-NNN-NN` identifica un criterio único. Las referencias `RN-…` y `PB-…` remiten al documento 05.
- Todas las historias de este documento pertenecen al MVP acordado. Su numeración organiza la lectura y no fija el orden de implementación, calendario ni cambios OpenSpec.
- La aprobación de una historia con dependencias abiertas acepta su intención y alcance, no completa automáticamente los criterios condicionados.
- No se agregan cuentas de pacientes, verificación previa de email, recepción, clínica ni cobros.

## Épica 1: Configuración de disponibilidad

### US-001 — Configurar motivos de consulta

**Como** odontólogo, **quiero** configurar los motivos de consulta y su duración **para** ofrecer horarios que alcancen para la prestación prevista.

**Criterios de aceptación**:

- [ ] **CA-US-001-01**: El odontólogo puede consultar, crear y modificar motivos con nombre y duración positiva en minutos.
- [ ] **CA-US-001-02**: Un motivo sin nombre o con duración no positiva se rechaza sin guardar una configuración inválida.
- [ ] **CA-US-001-03**: La disponibilidad se calcula según la duración del motivo elegido; no se impone una duración clínica fija.
- [ ] **CA-US-001-04**: Cambiar la duración configurada no reescribe silenciosamente intervalos de turnos existentes; el manejo de conflictos se verifica una vez definida su política.

**Reglas relacionadas**: RN-AG-01, RN-AG-03, RN-AG-06, RN-AU-03, RN-EX-02.
**Dependencias abiertas**: PB-03 y PB-05; el procedimiento de cambio con reservas y la desactivación de motivos no quedan definidos por esta historia.

### US-002 — Establecer horario semanal y pausas

**Como** odontólogo, **quiero** configurar mi horario habitual y sus pausas **para** ofrecer turnos dentro de mis períodos de atención.

**Criterios de aceptación**:

- [ ] **CA-US-002-01**: Se pueden consultar y modificar franjas semanales de atención y pausas para la agenda única.
- [ ] **CA-US-002-02**: En un día configurado, los horarios ofrecidos excluyen las pausas y respetan la duración completa del turno.
- [ ] **CA-US-002-03**: La configuración se interpreta en la zona del consultorio que se acuerde; no depende de la zona de la computadora del desarrollador.
- [ ] **CA-US-002-04**: Una modificación no cancela ni desplaza silenciosamente turnos existentes. Franjas cruzando medianoche, superpuestas y cambios con reservas se comprueban contra la política que se acuerde.

**Reglas relacionadas**: RN-CO-01, RN-AG-02, RN-AG-03, RN-AG-08, RN-AU-03, RN-EX-02.
**Dependencias abiertas**: PB-02, PB-03 y PB-05.

### US-003 — Bloquear vacaciones o excepciones

**Como** odontólogo, **quiero** agregar y retirar bloqueos de fechas concretas **para** evitar nuevas reservas durante mis ausencias o excepciones.

**Criterios de aceptación**:

- [ ] **CA-US-003-01**: Se pueden consultar, agregar y retirar bloqueos de la agenda con inicio anterior al fin.
- [ ] **CA-US-003-02**: Un turno que invadiría un bloqueo no se ofrece como disponible ni se confirma como nueva reserva.
- [ ] **CA-US-003-03**: Retirar un bloqueo vuelve a permitir el cálculo de disponibilidad, sujeto a horarios, pausas y turnos vigentes.
- [ ] **CA-US-003-04**: Agregar o retirar un bloqueo no cancela ni elimina turnos por sí solo; los conflictos con reservas existentes siguen la política que se acuerde.

**Reglas relacionadas**: RN-AG-02, RN-AG-03, RN-AG-04, RN-AG-07, RN-AU-03, RN-EX-02.
**Dependencias abiertas**: PB-02 y PB-05; la concurrencia con reservas se concretará en arquitectura.

## Épica 2: Reserva web del paciente

### US-004 — Consultar horarios disponibles

**Como** paciente, **quiero** elegir un motivo y consultar horarios disponibles **para** encontrar un turno sin coordinarlo por mensajes.

**Criterios de aceptación**:

- [ ] **CA-US-004-01**: Se pueden consultar motivos y disponibilidad desde la página de reserva local, sin cuenta ni verificación previa del email.
- [ ] **CA-US-004-02**: Los horarios ofrecidos permiten completar la duración requerida y no invaden pausas, bloqueos ni turnos confirmados.
- [ ] **CA-US-004-03**: Las opciones respetan la ventana de 24 horas a 30 días, con los límites y el paso de horarios que se acuerden.
- [ ] **CA-US-004-04**: La consulta no reserva el horario y no expone nombres ni datos de otros pacientes o detalles de citas ocupadas.

**Reglas relacionadas**: RN-AG-02, RN-AG-03, RN-AG-04, RN-AG-08, RN-RE-01, RN-RE-02, RN-AU-01.
**Dependencias abiertas**: PB-01, PB-02 y PB-03.

### US-005 — Reservar con confirmación automática

**Como** paciente, **quiero** ingresar mis datos y reservar un horario disponible **para** obtener un turno confirmado sin aprobación manual.

**Criterios de aceptación**:

- [ ] **CA-US-005-01**: La reserva solicita nombre, apellido, email, teléfono y DNI; si falta cualquiera de los cinco, no se confirma un turno con datos incompletos.
- [ ] **CA-US-005-02**: Con datos válidos y horario permitido y libre, el turno queda `CONFIRMADO` automáticamente y aparece en la agenda. No se exige cuenta, verificación previa ni segunda confirmación del paciente.
- [ ] **CA-US-005-03**: Se vuelve a comprobar la disponibilidad al registrar la operación. Con dos solicitudes válidas concurrentes para intervalos superpuestos, no se crean dos turnos confirmados superpuestos y el intento que pierde el horario informa el conflicto.
- [ ] **CA-US-005-04**: La reserva guarda el intervalo y la duración acordados. La coincidencia de DNI o email con un registro previo no permite mostrar, fusionar ni sobrescribir datos anteriores sin la regla correspondiente.

**Reglas relacionadas**: RN-CO-03, RN-AG-05, RN-AG-06, RN-PA-01, RN-PA-02, RN-PA-03, RN-RE-01, RN-RE-02, RN-RE-03, RN-RE-04, RN-EX-01.
**Dependencias abiertas**: PB-01, PB-02, PB-04 y PB-08 para repetición de solicitudes; PB-10 para la respuesta ante falla de correo. La confirmación por email se especifica en US-010.

## Épica 3: Consulta de la agenda interna

### US-006 — Consultar agenda y datos básicos de turnos

**Como** odontólogo, **quiero** consultar mi agenda diaria y semanal y el detalle básico de cada turno **para** organizar la atención y contactar al paciente cuando sea necesario.

**Criterios de aceptación**:

- [ ] **CA-US-006-01**: Con acceso interno válido se puede consultar la agenda diaria y semanal del único consultorio en la zona acordada.
- [ ] **CA-US-006-02**: Se pueden consultar motivo, intervalo, estado y los cinco datos básicos del paciente asociados al turno, sin incorporar ficha clínica ni cobros.
- [ ] **CA-US-006-03**: Las reservas, cancelaciones y reprogramaciones confirmadas se reflejan en la información consultada; un turno cancelado deja de ocupar el intervalo, sin que se presuponga su borrado.
- [ ] **CA-US-006-04**: Consultar directamente los endpoints internos sin acceso del odontólogo no permite obtener agenda ni datos de pacientes.

**Reglas relacionadas**: RN-CO-01, RN-CO-02, RN-PA-03, RN-RE-04, RN-AU-03, RN-CA-02, RN-RP-02.
**Dependencias abiertas**: PB-02; mecanismo de ingreso interno pendiente del documento 08. La historia no habilita alta manual, corrección de pacientes ni excepciones: PB-04 y PB-06.

## Épica 4: Autogestión del turno por enlace

### US-007 — Consultar el turno autorizado por el enlace

**Como** paciente, **quiero** abrir el enlace privado recibido por email **para** reconocer mi turno y acceder a las acciones permitidas.

**Criterios de aceptación**:

- [ ] **CA-US-007-01**: Una credencial privada válida permite consultar únicamente la información acordada del turno autorizado, sin crear una cuenta de paciente.
- [ ] **CA-US-007-02**: Sin credencial, con una no válida o intentando acceder a otro turno cambiando su identificador, no se muestran ni modifican datos ajenos al permiso.
- [ ] **CA-US-007-03**: Abrir o previsualizar el enlace no cancela ni reprograma el turno.
- [ ] **CA-US-007-04**: Nombre, DNI, email, teléfono o identificador de turno no sustituyen la credencial. Vencimiento, datos visibles y recuperación se comprueban contra la política que se acuerde.

**Reglas relacionadas**: RN-AU-02, RN-AU-04, RN-AU-05, RN-EX-01.
**Dependencias abiertas**: PB-07 y mecanismo técnico de credencial pendiente del documento 08.

### US-008 — Cancelar dentro del plazo

**Como** paciente, **quiero** cancelar mi turno desde el enlace privado **para** liberar el horario sin coordinar la cancelación manualmente.

**Criterios de aceptación**:

- [ ] **CA-US-008-01**: Una solicitud con credencial válida y dentro del plazo permitido cambia el turno a `CANCELADO` y libera su ocupación.
- [ ] **CA-US-008-02**: La operación no elimina físicamente al paciente ni el turno y genera el aviso de cambio definido en US-011.
- [ ] **CA-US-008-03**: Fuera del plazo de hasta 24 horas antes se impide la cancelación autónoma y se indica el contacto directo con el odontólogo, sin habilitar una excepción automática.
- [ ] **CA-US-008-04**: La apertura del enlace no ejecuta la cancelación; esta requiere una solicitud expresa de escritura autorizada.

**Reglas relacionadas**: RN-AU-02, RN-AU-04, RN-AU-05, RN-CA-01, RN-CA-02, RN-CA-03, RN-NO-01, RN-NO-04.
**Dependencias abiertas**: PB-01, PB-06, PB-07, PB-08, PB-10 y PB-11 para bordes, contacto operativo, repetición y comunicación de cambios.

### US-009 — Reprogramar sin perder la reserva original si falla

**Como** paciente, **quiero** elegir un nuevo horario desde mi enlace privado **para** mover el turno dentro del plazo permitido sin llamadas.

**Criterios de aceptación**:

- [ ] **CA-US-009-01**: Con credencial válida y dentro del plazo se puede solicitar un nuevo horario que respete disponibilidad, duración aplicable y ventana de reserva.
- [ ] **CA-US-009-02**: Si el cambio es válido, se conserva el identificador del turno, se actualizan intervalo y revisión, permanece `CONFIRMADO` y se genera un aviso de cambio.
- [ ] **CA-US-009-03**: Si el nuevo horario queda ocupado, incumple las reglas o falla el cambio, se conserva la reserva original. La operación no crea superposición ni libera por anticipado el horario anterior.
- [ ] **CA-US-009-04**: Fuera del plazo permitido se impide la reprogramación autónoma y se prevé contacto con el odontólogo. Abrir o previsualizar el enlace no mueve el turno.

**Reglas relacionadas**: RN-AG-03, RN-AG-05, RN-RE-01, RN-AU-02, RN-AU-04, RN-AU-05, RN-RP-01, RN-RP-02, RN-RP-03, RN-RP-04, RN-RP-05, RN-EX-01.
**Dependencias abiertas**: PB-01, PB-02, PB-03, PB-05, PB-06, PB-07 y PB-08; PB-10 y PB-11 para comunicación del resultado.

## Épica 5: Comunicaciones por email

### US-010 — Recibir confirmación y enlaces por email

**Como** paciente, **quiero** recibir la confirmación y los enlaces de gestión **para** conservar la información de mi turno y poder gestionarlo.

**Criterios de aceptación**:

- [ ] **CA-US-010-01**: Una reserva confirmada origina una notificación de confirmación inmediata con el destinatario y la revisión del turno persistidos.
- [ ] **CA-US-010-02**: En una prueba con SMTP disponible se comprueba recepción del correo en el buzón de prueba y apertura de sus enlaces desde la computadora que ejecuta la aplicación.
- [ ] **CA-US-010-03**: No se exige pulsar un enlace para confirmar nuevamente la reserva ni verificar previamente el email. Los enlaces habilitan solo la gestión autorizada.
- [ ] **CA-US-010-04**: La aceptación SMTP se registra separada del estado del turno y no se presenta como prueba de recepción o lectura.

**Reglas relacionadas**: RN-RE-03, RN-AU-02, RN-NO-01, RN-NO-02, RN-NO-03, RN-NO-05, RN-NO-06.
**Dependencias abiertas**: Proveedor y remitente; PB-07, PB-09, PB-10 y PB-11. No se fija la tolerancia de envío inmediato ni el resultado ante SMTP no disponible.

### US-011 — Recibir aviso de cancelación o reprogramación

**Como** paciente, **quiero** recibir el aviso de un cambio de mi turno **para** disponer de la información vigente de la cita.

**Criterios de aceptación**:

- [ ] **CA-US-011-01**: Una cancelación o reprogramación válida origina un aviso que corresponde al evento y a los datos vigentes del turno.
- [ ] **CA-US-011-02**: En pruebas con destinatarios habilitados se comprueba recepción del aviso de cancelación y del de reprogramación como casos distintos.
- [ ] **CA-US-011-03**: Se conservan tipo, turno, revisión, destinatario previsto y estado de la notificación, sin cambiar silenciosamente el destinatario por una edición posterior del paciente.
- [ ] **CA-US-011-04**: Los avisos y recordatorios derivados del cambio se coordinan según la política que se acuerde para no enviar información obsoleta ni crear repetidamente el mismo aviso.

**Reglas relacionadas**: RN-CA-02, RN-RP-04, RN-NO-01, RN-NO-02, RN-NO-03, RN-NO-04, RN-NO-05, RN-NO-06.
**Dependencias abiertas**: Proveedor y remitente; PB-04, PB-07, PB-08, PB-10 y PB-11. No se agregan avisos de operaciones manuales todavía no definidas.

### US-012 — Recibir recordatorio 24 horas antes

**Como** paciente, **quiero** recibir un recordatorio 24 horas antes **para** tener presente el turno reservado.

**Criterios de aceptación**:

- [ ] **CA-US-012-01**: Un turno confirmado tiene previsto un recordatorio 24 horas antes, asociado a su revisión y destinatario. La coincidencia con una reserva hecha exactamente a 24 horas se verifica una vez acordada su política.
- [ ] **CA-US-012-02**: Se comprueba la recepción del recordatorio mediante el mecanismo controlado de tiempo acordado para pruebas, manteniendo la regla real de 24 horas.
- [ ] **CA-US-012-03**: Un turno cancelado o reprogramado no debe recibir un recordatorio obsoleto. La anulación, reemplazo y sincronización se comprueban según la política que se acuerde.
- [ ] **CA-US-012-04**: Las notificaciones conservan su estado e intentos. Recuperación tras reinicios, avisos vencidos, tolerancia temporal y reintentos se verifican después de definir el comportamiento esperado.

**Reglas relacionadas**: RN-RE-04, RN-NO-01, RN-NO-02, RN-NO-03, RN-NO-04, RN-NO-06.
**Dependencias abiertas**: Proveedor y remitente; PB-01, PB-02, PB-09, PB-10 y PB-11. La demostración requiere backend ejecutándose y conectividad para el envío real.

## Criterios transversales aprobados

| Código | Criterio | Alcance de la comprobación |
|---|---|---|
| CT-01 | Coherencia ante concurrencia. | Reservas y reprogramaciones no crean turnos confirmados superpuestos. Una reprogramación fallida conserva el original. El mecanismo se diseña en arquitectura. |
| CT-02 | Acceso limitado en backend. | Las operaciones internas requieren acceso del odontólogo; el enlace solo autoriza su turno. No se prueban únicamente opciones ocultas en la interfaz. |
| CT-03 | Datos públicos limitados. | Las consultas públicas no devuelven agenda completa ni datos de pacientes; una coincidencia de DNI o email no concede acceso a registros previos. |
| CT-04 | Persistencia entre reinicios. | Después de reiniciar se conservan configuración, turno y estado de notificaciones guardados, según el modelo. Esto no demuestra restauración desde un respaldo ni decide el envío de avisos vencidos. |
| CT-05 | Entrega local comprobable. | La interfaz compilada se sirve desde Spring Boot y permite recorrer los flujos en el navegador local, con PostgreSQL local y emails reales de prueba. No se exige acceso desde otros dispositivos o despliegue público. |
| CT-06 | Evidencia de aceptación. | Se registran resultado y evidencia de los criterios aplicables. Una casilla marcada requiere comprobación; un criterio condicionado por PB no se declara cumplido sin resolverla y verificarlo. |

Estos criterios complementan las historias sin incorporar nuevas funciones clínicas o administrativas. Los controles de acceso y enlaces se analizan como requisitos; su código requiere la aprobación correspondiente de gobernanza.

## Cobertura de los criterios aprobados de visión

| Criterio del documento 01 | Historias y criterios transversales |
|---|---|
| CA-VIS-01 — Configuración y disponibilidad. | US-001, US-002, US-003, US-004. |
| CA-VIS-02 — Reserva válida y confirmada. | US-005, US-006, US-010. |
| CA-VIS-03 — Sin solapamientos concurrentes. | US-005, US-009, CT-01. |
| CA-VIS-04 — Cancelación y reprogramación con aviso. | US-007, US-008, US-009, US-011. |
| CA-VIS-05 — Autogestión fuera de plazo impedida. | US-008, US-009, CT-02. |
| CA-VIS-06 — Emails y enlaces comprobados localmente. | US-007, US-010, US-011, US-012, CT-05. |
| CA-VIS-07 — Persistencia tras reinicio. | CT-04, CT-06; configuración y turnos de US-001 a US-006, comunicaciones de US-010 a US-012. |

## Condiciones para considerar completa una historia

La historia debe tener su comportamiento detallado, reglas trazadas y dependencias necesarias resueltas antes de cerrar la implementación correspondiente. Su aceptación exige comprobar criterios aplicables y conservar evidencia. Las pruebas de email incluyen recepción en el buzón de prueba; registrar aceptación SMTP por sí solo no satisface ese criterio.

Las doce preguntas PB del documento 05 siguen abiertas. La trazabilidad indica qué historias dependen de ellas; las decisiones sobre acceso interno, proveedor y mecanismos técnicos se desarrollarán en los documentos 07 y 08. Aprobar este desglose no equivale a aceptar respuestas implícitas a esos pendientes ni a declarar un producto listo para vender.

## Alcance posterior

Clínica, cobros, facturación, WhatsApp, recepción, múltiples profesionales o consultorios, lista de espera, campañas, reportes avanzados y calendarios externos no se añaden a estas historias. Conforme a RN-EX-03, tampoco se incluyen por inferencia un registro comercial público, cobro de suscripciones, edición libre de pacientes o gestión manual de excepciones. El objetivo SaaS futuro y la validación con odontólogos permanecen en el proyecto, sin ampliar esta entrega local.

El [índice](README.md) registra el progreso de la KB. El [documento 07](07_flujos_principales.md) describe los flujos completos y sus errores sobre este alcance; todavía no se generó un roadmap ni cambios de implementación.
