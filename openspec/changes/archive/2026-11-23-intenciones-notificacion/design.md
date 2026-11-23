# Design

## Context

El [diseño de C-04](../2026-11-23-credenciales-gestion-turno/design.md#contrato-propuesto-entre-c-04-y-c-05) documenta la referencia y la composición interna de enlaces. C-02 aporta AW-01 a AW-05. La KB 04, 05 y 07 fija el estado durable, su separación del turno y los resultados nominales. El diseño v1.1 registra la política aprobada de coordinación con el emisor.

## Goals / Non-Goals

**Goals:** especificar la preparación de intenciones durables y el límite entre cambios del turno, elegibilidad y entrega.

**Non-Goals:** envío, selección de proveedor, intervalos de sondeo, configuración de reintentos y diseño visual. C-11 y C-16 verifican SMTP y recepción.

Decision: no (no UI)

## Decisions

### Contrato propuesto

Esta sección describe la contraparte del [contrato de credenciales de C-04](../2026-11-23-credenciales-gestion-turno/design.md#contrato-propuesto-entre-c-04-y-c-05). Los casos nominales de la especificación verifican qué se persiste. El diseño v1.1 registra la programación y las tolerancias que consumirá C-11.

1. C-08/C-09 entregan el evento de origen, turno, revisión resultante y destinatario previsto dentro de la escritura coordinada. C-05 prepara los tipos aplicables: CONFIRMACION, CAMBIO y RECORDATORIO. Cada intención registra programación, estado e intentos. Cuando el mensaje incluye enlace registra la referencia interna de C-04.
2. El evento, el cambio de turno, cualquier credencial emitida en la operación y sus intenciones se confirman o revierten juntos. Cancelar o reprogramar actualiza la elegibilidad de recordatorios de la revisión anterior dentro de esa coordinación. SMTP queda fuera del bloqueo de agenda.
3. C-11 consume una intención elegible con su destinatario y revisión persistidos y usa C-04 para componer el enlace. El resultado registra los hechos conocidos del intento. La ausencia de material se tramita por la política de fallos. Una edición del paciente por sí sola conserva el destinatario que quedó guardado.

### Opciones para revisión

| Decisión | Alternativas y recomendación propuesta |
|---|---|
| PB-08 · repetición y concurrencia | Distinguir reintento de la misma operación, solicitud nueva y conflicto de revisión. Recomendar devolver el resultado previamente registrado cuando la identidad de operación y sus parámetros coinciden, y un conflicto identificable cuando difieren o la revisión esperada cambió. Comparar con responder siempre según el estado actual, que pierde el resultado original. La respuesta a cancelación repetida y cualquier reactivación requieren decisión de producto. |
| PB-09 · exactamente 24 h | Evaluar un solo mensaje que cumpla ambas finalidades versus dos intenciones y mensajes diferenciados. Recomendar evaluar primero el mensaje combinado para evitar dos correos simultáneos, conservando la capacidad de acreditar ambas finalidades. Confirmar cómo impacta en tipos persistidos y en las pruebas antes de adoptarlo. |
| PB-10 · fallos y reinicio | Proponer rangos y ventanas como parámetros sujetos a aprobación. Distinguir rechazo conocido, fallo transitorio, vencimiento y resultado incierto. Evaluar reintento acotado para fallos transitorios conocidos y revisión separada de intentos inciertos. Decidir tolerancia, máximo de intentos, expiración, comunicación al usuario y qué hacer con avisos sin enlace recuperable. |
| PB-11 / AT-06 · evento único y carrera | Recomendar identidad durable del evento más tipo, con turno y revisión como datos de alcance y consistencia. Evaluar clave basada solo en turno/revisión/tipo, que puede confluir con otros eventos si la semántica cambia. Proponer autorización durable de inicio con verificación de revisión bajo coordinación breve. Si el cambio gana, el aviso queda inelegible. Si la autorización gana, la política debe admitir o tratar el envío en curso. Una segunda lectura sin coordinación deja una carrera. |
| PB-11 / AT-06 · resultado incierto | Si hay interrupción tras una posible aceptación SMTP y antes de registrar el resultado, proponer un resultado operativo distinguible del rechazo conocido, sujeto a revisión. Evaluar retenerlo para revisión versus reintentar con riesgo explícito de duplicado. La unicidad de intención protege la base local. La entrega SMTP exactamente una vez queda fuera de esa garantía. |

## Diseño recomendado v1.1 · Pendiente de aprobación

La identidad es evento durable más tipo. Una repetición con iguales parámetros devuelve el resultado original. Los parámetros distintos para la misma identidad o una escritura nueva sobre revisión anterior producen conflicto. Cancelar un turno ya cancelado devuelve su estado sin otro aviso. La reactivación queda fuera del alcance. A 24 horas exactas se persisten CONFIRMACION y RECORDATORIO con la misma revisión, destinatario y referencia de despacho. C-11 compone un solo mensaje y registra su intento conjuntamente.

Confirmación y cambio quedan elegibles al confirmar el evento, con objetivo de primer intento en 2 minutos. El recordatorio queda elegible a inicio menos 24 horas, con objetivo de 5 minutos. El recordatorio vence 2 horas después de su hora prevista o antes si cambia el turno. La confirmación vence al inicio del turno y el aviso de cambio a las 24 horas del evento. El despacho combinado vence al cerrar su primera ventana. Se recuperan avisos vigentes tras reinicio. Fallos transitorios conocidos permiten hasta tres intentos, con esperas de 1 y 5 minutos después de los fallos respectivos. Rechazo permanente, material irrecuperable y resultado SMTP incierto quedan registrados para atención operativa. El resultado incierto no se reintenta automáticamente. C-11 ejecuta el calendario y prueba recepción.

### Autorización de envío y cambios posteriores

Si el cambio se confirma primero, el despacho anterior queda inelegible. Si se confirma primero la autorización durable de un intento, **el correo anterior puede salir y llegar después del aviso de cambio**. El intento conserva destinatario, revisión y contenido de su autorización. Si todavía falta material para componerlo y C-04 ya revocó la referencia, C-11 registra el fallo de composición. Un intento autorizado puede finalizar, pero la revisión anterior queda inelegible para nuevos intentos y reintentos. Un resultado SMTP incierto se conserva como tal.

La cancelación o reprogramación confirma su propia intención CAMBIO con la nueva revisión y su destinatario previsto. C-11 indica explícitamente si el turno fue cancelado o reprogramado. El aviso de cancelación identifica el horario cancelado y omite el enlace de gestión. Un aviso de reprogramación muestra el nuevo horario y, cuando corresponde, el enlace vigente. Al abrir un enlace conservado de un correo anterior, C-04 consulta el turno actual y C-14 muestra los datos y acciones de la revisión vigente. Tras cancelar, un enlace revocado muestra la respuesta genérica de enlace no disponible. El aviso de cambio comunica la cancelación.

CA-05-P4 debe cubrir ambas secuencias con barreras y un emisor simulado, incluida la eventual llegada en orden inverso. Comprobar el estado del intento anterior, la ausencia de reintentos obsoletos, la intención CAMBIO con su nueva revisión y el resultado de abrir el enlace en cada rama. C-05 verifica coordinación y persistencia. C-11 verifica el contenido y la recepción. C-14 verifica la presentación del estado vigente y del enlace revocado.

Para recuperación asistida, C-05 reutiliza el tipo CONFIRMACION con un evento durable explícito. El nuevo aviso usa el destinatario confirmado y la nueva referencia. La sección siguiente fija qué pendientes se reasignan y cuáles pierden elegibilidad. C-11 usa un contenido de recuperación que identifica el enlace nuevo sin presentar el turno como recién reservado.

### Recordatorio durante la recuperación

Actualizar únicamente el recordatorio vigente, pendiente y sin autorización previa ni historial de intentos. Mantener identidad, evento, revisión y hora prevista. Sustituir la referencia y, si corresponde, el destinatario confirmado dentro de la transacción que revoca y reemite. El aviso de recuperación se registra por separado. Si el recordatorio era parte de un despacho combinado todavía sin intentos, separar ese agrupamiento para que la confirmación anterior pueda quedar inelegible sin arrastrar el recordatorio conservado.

Si ya hubo una autorización, el intento conserva su fotografía de destinatario, referencia y revisión. Puede terminar según la regla de envío autorizado. Si todavía necesita componer un enlace ya revocado, se registra el fallo. Un resultado incierto conserva su estado. Ninguno de esos avisos se reasigna ni se reintenta con la credencial revocada. Un fallo previo conocido conserva sus intentos históricos y tampoco se reasigna automáticamente. La recuperación genera su propia confirmación y no duplica recordatorios.

La prueba de concurrencia debe cubrir dos órdenes con barreras. Si gana la recuperación, el primer intento elegible usa el destino confirmado y la referencia nueva. Si gana la autorización, el intento mantiene los datos anteriores, estables también al registrar su resultado. Inyectar un error antes de confirmar la recuperación debe conservar las credenciales e intenciones originales y no dejar una confirmación de recuperación huérfana.

## Risks / Trade-offs

- Cambio después de autorizar un inicio → aplicar la regla propuesta de intento en curso e informar el estado vigente mediante el aviso de cambio.
- Fallo después de posible aceptación → conservar evidencia del intento y aplicar la política de resultado incierto. Acreditar recepción en buzón de prueba.
- Destinatario corregido → acordar sustitución explícita de avisos y revocación o reemplazo de referencias con C-04.

## Casos pendientes de decisión

| Caso | Preparación | Resultado que se debe acordar |
|---|---|---|
| CA-05-P1 · PB-08 | Repetir con mismos y distintos parámetros, cancelar dos veces y sincronizar dos escrituras de una revisión. | Resultado de cada solicitud, intención única por evento y política expresa sobre reactivación. |
| CA-05-P2 · PB-01 / PB-09 | Con reloj controlado, intentar reservar a 24 horas menos un segundo, a 24 horas exactas y a 24 horas más un segundo. | PB-01 fija la admisión en el borde y el momento de evaluación. Si la reserva exacta es admitida, PB-09 fija tipos, cantidad de intenciones y mensajes y momento elegible. |
| CA-05-P3 · PB-10 | Reiniciar con aviso futuro, vencido, con fallo transitorio conocido o sin material recuperable. | Descarte, reintento, recuperación y mensaje conforme a tolerancias aprobadas. |
| CA-05-P4 · PB-11 / AT-06 | Sincronizar cambio y emisor con barreras a ambos lados del punto de autorización. | Elegibilidad si el cambio gana y tratamiento si el intento ya arrancó. |
| CA-05-P5 · PB-10/11 | Interrumpir después de posible aceptación y antes de registrar el resultado. | Resultado persistido, decisión de reintento y ausencia de afirmación de entrega sin buzón. |
| CA-05-P6 · PB-07/11 | Corregir el email después de preparar un aviso y antes del envío. | Sustitución explícita, destino de referencias anteriores y comportamiento del aviso en curso. |

## Aprobaciones

Revisar conjuntamente el contrato con C-04 antes de implementar C-05. Ignacio decide o valida la experiencia de PB-08 a PB-11 y revisa la semántica del emisor. La migración/configuración durable exige revisión HIGH. Cualquier modificación de material privado exige autorización explícita CRITICAL. C-08 y C-09 integran las escrituras, C-11 realiza el envío y C-16 verifica el recorrido integral.
