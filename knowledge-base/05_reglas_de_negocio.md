# Reglas de negocio

**Estado del documento**: aprobado por el usuario el 2026-11-18.
**Confirmación**: «Aprobar el documento 05 y continuar con el 06».
**Fecha**: 2026-11-18.
**Base**: [Discovery aprobado](../discovery/discovery.md) y documentos aprobados [01 — Visión](01_vision_y_objetivos.md), [02 — Descripción](02_descripcion_general.md), [03 — Actores](03_actores_y_roles.md) y [04 — Datos](04_modelo_de_datos.md).

El usuario aprobó la codificación de los acuerdos de este documento para citarlos desde historias, flujos y comprobaciones. Las reglas no acreditan implementación. Las doce preguntas abiertas mantienen identificadores distintos de las reglas y continúan pendientes; la aprobación del documento no decide sus respuestas.

## Convenciones y alcance

- Cada regla usa `RN-{DOMINIO}-{NN}`. Conservar su identificador al citarla en los documentos siguientes; la codificación de este documento ya está aprobada.
- La columna **Origen** remite al Discovery o a los documentos 01–04 enlazados arriba. Un origen indica trazabilidad, no prueba de ejecución.
- Los límites de ocupación de intervalos están definidos; los bordes exactos de las ventanas de reserva y autogestión siguen pendientes. No son el mismo concepto.
- El plazo de una semana, el stack, el entorno local y la preferencia por tecnologías gratuitas son restricciones de proyecto. No se convierten en reglas para el paciente.

## Dominio: Consultorio y agenda (RN-CO)

| Código | Regla | Origen |
|---|---|---|
| RN-CO-01 | La primera entrega opera un único consultorio, un odontólogo y una agenda. La inicialización debe establecer esas relaciones antes de ofrecer reservas. | 01 y 04. |
| RN-CO-02 | El odontólogo es el único usuario interno y puede consultar su agenda diaria y semanal y los datos básicos asociados a los turnos. No se incorpora acceso de recepción. | Discovery, 01 y 03. |
| RN-CO-03 | Motivo, paciente y agenda de un turno deben pertenecer al contexto del consultorio configurado y referenciar registros válidos. | 04. |

## Dominio: Motivos y disponibilidad (RN-AG)

| Código | Regla | Origen |
|---|---|---|
| RN-AG-01 | El odontólogo configura motivos de consulta con nombre no vacío y duración positiva en minutos. No se imponen duraciones clínicas predeterminadas. | Discovery, 01 y 04. |
| RN-AG-02 | La disponibilidad parte del horario semanal habitual y descuenta pausas, bloqueos concretos y ocupación de turnos confirmados. | Discovery, 01 y 04. |
| RN-AG-03 | Un horario ofrecido debe permitir completar la duración requerida dentro de la disponibilidad, sin invadir una pausa, bloqueo o turno ocupado. No basta con que esté libre el instante de inicio. | 01 y 04; explicitación de disponibilidad según duración. |
| RN-AG-04 | Todo intervalo de turno o bloqueo debe tener inicio anterior al fin. La ocupación usa inicio incluido y fin excluido: dos turnos pueden ser contiguos sin superponerse. | 04. |
| RN-AG-05 | No pueden coexistir turnos confirmados con intervalos superpuestos en la misma agenda, incluso ante solicitudes simultáneas. La solución concreta de concurrencia queda para arquitectura. | Discovery, CA-VIS-03 del 01 y 04. |
| RN-AG-06 | Al reservar se conserva la duración y el intervalo acordados. Cambiar después la duración configurada de un motivo no reescribe silenciosamente las reservas existentes. | 04. |
| RN-AG-07 | Crear o retirar un bloqueo modifica la disponibilidad, pero no cancela ni elimina turnos por sí solo. La política de conflictos con reservas existentes sigue pendiente. | 03 y 04. |
| RN-AG-08 | El horario semanal se interpreta en la zona horaria acordada del consultorio, que se conserva separada de los instantes de turnos y avisos. No se infiere de la computadora del desarrollador. | 04; valor de zona pendiente. |

## Dominio: Pacientes y datos básicos (RN-PA)

| Código | Regla | Origen |
|---|---|---|
| RN-PA-01 | Para reservar son obligatorios nombre, apellido, email, teléfono y DNI. Los formatos, longitudes y normalización se precisarán antes de implementar su validación. | Discovery, 01 y 04. |
| RN-PA-02 | La coincidencia de DNI, teléfono o email no autoriza consultar datos anteriores, sobrescribirlos ni fusionar registros de pacientes. | 03 y 04. |
| RN-PA-03 | El registro de paciente conserva datos básicos; no incorpora historia clínica, odontograma ni cobros en esta versión. La identificación, reutilización y corrección de registros requieren reglas todavía pendientes. | 01 y 04. |

## Dominio: Reserva y confirmación (RN-RE)

| Código | Regla | Origen |
|---|---|---|
| RN-RE-01 | La ventana de reserva va desde 24 horas de anticipación hasta 30 días hacia adelante. Inclusión exacta de extremos e interpretación del límite de 30 días pendientes: ver PB-01. | Discovery y 01. |
| RN-RE-02 | Consultar disponibilidad no reserva el horario. Al registrar la reserva se deben comprobar nuevamente motivo, datos obligatorios, disponibilidad, ventana temporal y ausencia de superposición. | 01, 02 y 04. |
| RN-RE-03 | Una reserva válida que obtiene un horario libre queda confirmada automáticamente. No espera aprobación manual del odontólogo, creación de cuenta ni verificación previa del email del paciente. | Discovery y 01. |
| RN-RE-04 | El turno confirmado ocupa su intervalo; el cancelado no lo ocupa. Los estados funcionales de esta versión son `CONFIRMADO` y `CANCELADO`. | 04. |

## Dominio: Acceso y autorización (RN-AU)

Estas reglas describen requisitos funcionales de acceso. No seleccionan el mecanismo de autenticación ni autorizan escribir código del dominio CRITICAL.

| Código | Regla | Origen |
|---|---|---|
| RN-AU-01 | El paciente puede consultar motivos y disponibilidad y solicitar una reserva sin cuenta ni verificación previa de email. Esa vía pública no permite consultar la agenda completa ni datos de pacientes. | Discovery, 02 y 03. |
| RN-AU-02 | La autogestión requiere una credencial privada válida y se limita al turno autorizado por ella. Conocer nombre, DNI, teléfono, email o identificador de turno no concede ese permiso. | 03 y 04. |
| RN-AU-03 | La consulta y configuración internas requieren comprobar en el backend el acceso del odontólogo; ocultar pantallas no sustituye la autorización. | 03. |
| RN-AU-04 | Una credencial válida no omite las reglas de plazo y disponibilidad. Cambiar identificadores no permite gestionar otro turno. | 03. |
| RN-AU-05 | Abrir o previsualizar un enlace no cancela ni reprograma por sí solo. Los cambios se solicitan mediante la operación de escritura correspondiente. | 02 y 03. |

## Dominio: Cancelación (RN-CA)

| Código | Regla | Origen |
|---|---|---|
| RN-CA-01 | El paciente puede cancelar por autogestión hasta 24 horas antes del turno vigente. El instante exacto del límite sigue pendiente en PB-01. | Discovery y 01. |
| RN-CA-02 | Una cancelación válida cambia el estado a `CANCELADO`, libera la ocupación y genera el aviso de cambio correspondiente. No borra físicamente al paciente ni define conservación de datos. | CA-VIS-04 del 01 y 04. |
| RN-CA-03 | Fuera del plazo permitido no se admite cancelación autónoma y se prevé contacto directo con el odontólogo. Sus excepciones manuales y procedimiento siguen pendientes. | Discovery, 01 y 03. |

## Dominio: Reprogramación (RN-RP)

| Código | Regla | Origen |
|---|---|---|
| RN-RP-01 | El paciente puede reprogramar por autogestión hasta 24 horas antes del turno vigente; el nuevo horario debe cumplir la ventana de reserva y estar disponible para la duración aplicable. Los bordes exactos y la duración si cambió el motivo siguen pendientes. | Discovery, 01 y 04. |
| RN-RP-02 | Una reprogramación válida conserva el identificador del turno, cambia su intervalo y actualiza su revisión. El turno vigente permanece `CONFIRMADO`; no se introduce un estado `REPROGRAMADO`. | 04. |
| RN-RP-03 | La obtención del nuevo horario y el cambio del anterior deben tratarse como una operación indivisible. Si falla la reprogramación, se conserva la reserva original sin liberar su horario. | 03 y 04. |
| RN-RP-04 | La reprogramación genera un aviso de cambio y requiere que los recordatorios correspondan a la nueva revisión. Una revisión numérica no sustituye el diseño de concurrencia ni constituye auditoría completa. | 04. |
| RN-RP-05 | Fuera del plazo de autogestión se prevé contacto directo con el odontólogo; no se infieren permisos manuales adicionales por su rol. | 01 y 03. |

## Dominio: Comunicaciones (RN-NO)

| Código | Regla | Origen |
|---|---|---|
| RN-NO-01 | El canal inicial es email: confirmación inmediata de reserva, aviso de cambios y un recordatorio 24 horas antes del turno. No se requiere una segunda confirmación del paciente. | Discovery y 01. |
| RN-NO-02 | Se persiste el estado de cada notificación separado del estado del turno, con turno, revisión, tipo, destinatario, momento previsto e intentos. | 02 y 04. |
| RN-NO-03 | `ACEPTADA_SMTP` expresa aceptación de la solicitud de envío; no acredita recepción ni lectura en el buzón. La recepción de cada tipo de correo se comprobará en pruebas con destinatarios de prueba. | CA-VIS-06 del 01, 02 y 04. |
| RN-NO-04 | Cancelar o reprogramar exige evitar recordatorios obsoletos. La revisión del turno ayuda a reconocerlos; la política de anulación, reemplazo y sincronización sigue pendiente. | 04. |
| RN-NO-05 | El destinatario guardado de un aviso no cambia silenciosamente por una edición posterior del paciente. Una corrección requiere definir qué hacer con avisos preparados y enlaces anteriores. | 04. |
| RN-NO-06 | Crear repetidamente el mismo aviso debe prevenirse según su evento de origen. La clave y reintentos se diseñarán en los flujos; persistir estados no garantiza entrega SMTP exactamente una vez. | 04. |

“Confirmación inmediata” establece el comportamiento esperado, pero el plazo técnico de envío todavía no tiene una tolerancia acordada. La reserva exactamente a 24 horas coincide con el momento del recordatorio; no se decide aquí enviar dos correos, combinarlos u omitir uno. El comportamiento frente a SMTP no disponible también sigue pendiente, sin inventar un nuevo estado de turno o una política de reversión.

## Dominio: Excepciones globales (RN-EX)

| Código | Regla | Origen |
|---|---|---|
| RN-EX-01 | Las condiciones se aplican conjuntamente. Una solicitud autorizada con datos completos sigue siendo inválida si el horario está ocupado o fuera de la disponibilidad o del plazo permitido. | 01, 03 y 04. |
| RN-EX-02 | Cambios de horarios, motivos, bloqueos o datos no deben modificar o eliminar silenciosamente reservas o destinatarios existentes. El procedimiento para resolver conflictos y avisar se acordará expresamente. | 03 y 04. |
| RN-EX-03 | Las operaciones manuales, eliminación, conservación y recuperación identificadas como pendientes no se habilitan por inferencia de los roles, claves o estados del modelo. | 03 y 04. |

## Transiciones de turno definidas

| Situación inicial | Operación y condición | Resultado esperado | Reglas |
|---|---|---|---|
| Sin turno registrado | Reserva válida con horario libre. | Nuevo turno `CONFIRMADO`; ocupa el intervalo. | RN-RE-02, RN-RE-03, RN-RE-04. |
| `CONFIRMADO` | Cancelación autónoma autorizada y dentro de plazo. | Mismo turno `CANCELADO`; deja de ocupar el intervalo; aviso de cambio. | RN-AU-02, RN-CA-01, RN-CA-02. |
| `CONFIRMADO` | Reprogramación autónoma válida. | Mismo turno `CONFIRMADO`, nuevo intervalo y revisión; aviso de cambio. | RN-RP-01, RN-RP-02, RN-RP-04. |
| `CONFIRMADO` | Reprogramación rechazada o fallida. | Reserva original conservada. | RN-RP-03. |

Reactivar un turno cancelado, repetir una cancelación o resolver dos cambios simultáneos sobre el mismo turno no tiene todavía política detallada. No se deriva una transición adicional del solo hecho de tener un enlace válido. Un fallo de email no constituye por sí solo una transición de turno definida.

## Decisiones de negocio pendientes

Los identificadores `PB-NN` registran preguntas para resolver; **no son reglas aprobadas**. No se reemplazan sus respuestas por valores de ejemplo o configuración del desarrollador.

| Código | Decisión necesaria | Impacto |
|---|---|---|
| PB-01 | Inclusión exacta de 24 horas y 30 días, significado de 30 días y momento de evaluación de los límites. | Aceptación de reserva, cancelación y reprogramación en los bordes. |
| PB-02 | Zona horaria del consultorio, franjas que cruzan medianoche y tratamiento de horarios repetidos o superpuestos. | Construcción de disponibilidad y comparación de instantes. |
| PB-03 | Paso de los horarios ofrecidos y límites o múltiplos admitidos de duración. | Generación de opciones de reserva; no se asumen intervalos de 5, 15 o 30 minutos. |
| PB-04 | Identificación y reutilización de pacientes, formatos y longitudes de campos, duplicados y corrección de datos. | Alta y actualización de pacientes sin sobrescritura no autorizada. |
| PB-05 | Cambios de horario, pausas, motivos o bloqueos cuando existen reservas, incluida la duración aplicable al reprogramar después de un cambio de motivo. | Protección de reservas y resolución de conflictos. |
| PB-06 | Alta manual de turnos y cancelación o reprogramación por el odontólogo, incluidas excepciones de plazo. | Alcance preciso de la gestión interna y sus avisos. |
| PB-07 | Vigencia e invalidación de enlaces, datos visibles, recuperación por pérdida o error de email y efecto de reprogramar o cancelar. | Autogestión limitada y continuidad del acceso. |
| PB-08 | Repetición de solicitudes, cancelación ya realizada, reactivación y cambios simultáneos del mismo turno. | Resultados previsibles y preservación del turno. |
| PB-09 | Confirmación y recordatorio cuando la reserva se hace exactamente a 24 horas. | Evitar comunicaciones duplicadas o inconsistentes. |
| PB-10 | Resultado ante falla de correo, tolerancia de envío inmediato, reintentos, pendientes al reiniciar y recordatorios vencidos. | Flujos de error, experiencia del paciente y pruebas reales de email. |
| PB-11 | Evento único de aviso, sustitución o anulación al cambiar el turno y política ante respuesta SMTP incierta. | Evitar avisos obsoletos y controlar duplicados sin prometer entrega única. |
| PB-12 | Conservación, corrección, eliminación y recuperación de datos. | Operación posterior de los datos personales. |

Las decisiones de proveedor SMTP, credenciales internas y mecanismos de concurrencia se desarrollarán también en arquitectura. La falta de una respuesta no autoriza implementar arbitrariamente un borde del flujo. La redacción de historias puede avanzar, pero el criterio que dependa de una pregunta pendiente deberá declararlo.

## Trazabilidad con la aceptación del documento 01

| Criterio aprobado | Reglas principales |
|---|---|
| CA-VIS-01 — Configuración y disponibilidad. | RN-CO-01, RN-CO-02, RN-AG-01, RN-AG-02, RN-AG-03, RN-AG-04, RN-AG-07. |
| CA-VIS-02 — Reserva válida y confirmada. | RN-PA-01, RN-RE-01, RN-RE-02, RN-RE-03, RN-RE-04. |
| CA-VIS-03 — Prevención de solapamientos concurrentes. | RN-AG-05, RN-RE-02, RN-RP-03. |
| CA-VIS-04 — Cancelación y reprogramación con aviso. | RN-AU-02, RN-CA-01, RN-CA-02, RN-RP-01, RN-RP-02, RN-RP-03, RN-RP-04, RN-NO-01. |
| CA-VIS-05 — Autogestión fuera de plazo impedida. | RN-AU-04, RN-CA-03, RN-RP-05. |
| CA-VIS-06 — Emails y enlaces comprobados localmente. | RN-AU-02, RN-AU-05, RN-NO-01, RN-NO-02, RN-NO-03. |
| CA-VIS-07 — Persistencia tras reinicio. | Datos y estados persistidos según el documento 04; RN-RE-04 y RN-NO-02. No equivale a definir recuperación de avisos vencidos. |

El [índice](README.md) registra los estados de revisión. Estas reglas son insumo de las historias del documento 06 y los flujos del 07; no hay pruebas de aplicación ejecutadas en esta fase.
