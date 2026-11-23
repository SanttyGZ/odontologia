# Design

## Context

El [borrador de C-04](../credenciales-gestion-turno/design.md#contrato-propuesto-entre-c-04-y-c-05) propone la referencia y la composición interna de enlaces. C-02 aporta AW-01 a AW-05. La KB 04, 05 y 07 fija el estado durable, su separación del turno y los resultados nominales. PT-03 y PB-08 a PB-11 mantienen pendiente la política de coordinación con el emisor.

## Goals / Non-Goals

**Goals:** especificar la preparación de intenciones durables y el límite entre cambios del turno, elegibilidad y entrega.

**Non-Goals:** envío, selección de proveedor, intervalos de sondeo, configuración de reintentos y diseño visual. C-11 y C-16 verifican SMTP y recepción.

Decision: no (no UI)

## Decisions

### Contrato propuesto

Esta sección describe la contraparte del [contrato de credenciales de C-04](../credenciales-gestion-turno/design.md#contrato-propuesto-entre-c-04-y-c-05). Los casos nominales de la especificación verifican qué se persiste. La programación concreta de los avisos CAMBIO y la tolerancia de procesamiento inmediato siguen sujetas a PB-10.

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

## Risks / Trade-offs

- Cambio después de autorizar un inicio → acordar tratamiento del envío en curso y alcance del aviso actualizado.
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
