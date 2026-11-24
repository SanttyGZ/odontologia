# Design

## Context

Ver [proposal.md](proposal.md) y [la spec de availability-configuration](specs/availability-configuration/spec.md). C-02 ya conserva motivos, franjas, bloqueos y turnos, expone consultas de agenda y define `agenda-write-serialization`; `AgendaWriteCoordinator` inicia la transacción raíz, toma el bloqueo de agenda, usa `READ COMMITTED` y limita las esperas. C-03 protege `/api/odontologo/**` con la sesión interna y CSRF existentes. C-06 agrega operaciones sobre ese modelo, sin modificar autenticación ni la estructura SQL.

## Goals / Non-Goals

**Goals:**

- Definir contratos estables para los consumidores posteriores C-07 y C-12.
- Validar la configuración y sus efectos sobre turnos después de obtener la coordinación de agenda.
- Rechazar una escritura incompatible sin persistir cambios parciales.
- Verificar invariantes estructurales y carreras con PostgreSQL real.

**Non-Goals:**

- Calcular ni retener horarios disponibles; la consulta pública pertenece a C-07.
- Crear, cancelar o reprogramar turnos, enviar avisos o cambiar credenciales.
- Cambiar autenticación, CSRF, configuración de instalación o esquema de base.
- Incorporar borrado de motivos, franjas nocturnas, clínicas múltiples o interfaz web.

## Decisions

### D-01 — Rutas y recursos de configuración

Se conservan las familias de rutas orientativas de `CHANGES.md` y se completan con recursos identificables:

| Recurso | Operaciones | Representación |
|---|---|---|
| Motivo | `GET, POST /api/odontologo/motivos`; `PUT /api/odontologo/motivos/{motivoId}` | `id`, `nombre`, `duracionMinutos`; el alta/modificación recibe nombre y duración. |
| Horario | `GET, PUT /api/odontologo/horario` | Objeto `franjas` con filas `id`, `diaSemana`, `horaInicio`, `horaFin`, `tipo`; `PUT` reemplaza el conjunto completo. |
| Bloqueo | `GET, POST /api/odontologo/bloqueos`; `DELETE /api/odontologo/bloqueos/{bloqueoId}` | `id`, `inicio`, `fin`, `descripcion`; instantes ISO 8601 y descripción opcional. |

Las colecciones representan la única agenda local. No se introduce paginación ni un endpoint de borrado de motivos, que el alcance excluye. Los errores JSON usan el campo `codigo`, coherente con C-03. Se proponen `400 VALIDATION_ERROR`, `404 MOTIVO_NOT_FOUND` o `BLOQUEO_NOT_FOUND`, `409 CONFIGURATION_CONFLICT` o `AGENDA_BUSY`, y `500 TECHNICAL_FAILURE`; los rechazos `401/403` siguen siendo responsabilidad del filtro existente. Estos contratos se deben entregar a C-07 y C-12 antes de que implementen sus consumidores.

La alternativa de mantener `PUT` sobre la colección de motivos requeriría actualizaciones masivas ambiguas; el recurso por UUID elegido permite cambiar un motivo por solicitud y conserva la familia aprobada.

### D-02 — Validar y reemplazar el horario como una unidad

La validación se realiza en el servicio de dominio y se aplica al conjunto completo antes de persistirlo. Se admiten franjas de atención contiguas, pero no duplicadas ni superpuestas dentro del mismo día. Cada pausa debe quedar contenida en exactamente una franja de atención; pausas duplicadas o superpuestas se rechazan. La superposición de una pausa con su franja contenedora es la relación intencional del modelo, no un conflicto.

Las horas siguen siendo locales y se interpretan usando la zona ya persistida del consultorio (`America/Argentina/Mendoza`), nunca la zona predeterminada de la JVM. El reemplazo completo se ejecuta dentro de una sola escritura coordinada para que una fila inválida no deje un horario parcial.

### D-03 — Proteger reservas con la política PB-05 elegida

Después de obtener la coordinación, el servicio consulta los turnos `CONFIRMADO` actuales. Rechaza el reemplazo semanal si un turno deja de quedar enteramente dentro de atención o intersecta una pausa; rechaza la creación de un bloqueo si intersecta un turno. Las intersecciones de intervalos usan el límite semiabierto `[inicio, fin)`, por lo que los turnos contiguos no se consideran superpuestos. El error de negocio se traduce a `409 CONFIGURATION_CONFLICT` y la transacción revierte todo.

Retirar un bloqueo solo amplía disponibilidad y no modifica turnos. Cambiar el nombre o duración configurada de un motivo no modifica los intervalos ni duraciones reservadas; la duración vigente se usa para reservas nuevas y reprogramaciones posteriores, conforme a PB-05b. No se agrega una política de desactivación o eliminación de motivos.

### D-04 — Reutilizar la coordinación de C-02

Todas las escrituras de motivos, horario y bloqueos llaman `AgendaWriteCoordinator.write` como transacción raíz. No se inicia una transacción envolvente. Las lecturas que condicionan el cambio se vuelven a realizar dentro de `work`, después del bloqueo. Se propagan los rechazos de negocio para el rollback completo; una espera agotada se devuelve como `409 AGENDA_BUSY`, sin reintento automático.

No se crea un lock alternativo ni se usa la consulta anterior al bloqueo para decidir. Para probar la carrera con reserva antes de C-08, una operación sincronizada de integración que use el mismo coordinador representa la escritura de reserva y consulta los mismos datos persistidos. C-08 debe conservar la validación recíproca si la reserva adquiere primero la coordinación.

### D-05 — Límites de duración e inicio de candidatos

La duración se valida como múltiplo de 15 entre 15 y 240 minutos, inclusive. C-07 ofrecerá candidatos separados por 15 minutos, anclados al comienzo de cada franja de atención. Por ejemplo, una franja que empieza a las 09:10 produce inicios 09:10, 09:25, 09:40, y así sucesivamente. Se elige el anclaje relativo al horario configurado sobre alinear todos los inicios a los cuartos de hora del reloj, porque permite conservar el inicio definido por el odontólogo. Cada candidato debe caber completo dentro de atención y no cruzar pausas, bloqueos ni turnos confirmados.

### D-06 — Sin migración

La migración V1 ya contiene las entidades y restricciones estructurales necesarias. La política elegida prohíbe cruces de medianoche, y la validación de duplicados, solapes y conflictos con turnos se hace en la escritura coordinada. C-06 no agrega una migración. Si más adelante se decide permitir franjas nocturnas o reforzar estas reglas con restricciones de base, deberá proponerse una revisión HIGH de esquema antes de modificarlo.

## Risks / Trade-offs

- [La validación de solapes vive en la aplicación y no en una restricción V1] → centralizar todas las escrituras por agenda y probar el protocolo contra PostgreSQL real; no ofrecer una vía de escritura directa.
- [La zona puede cambiar el día local de un instante cerca de medianoche] → convertir turnos a la zona persistida del consultorio al revisar compatibilidad; rechazar si no caben íntegramente en una sola franja del mismo día.
- [C-07 y C-12 pueden implementar consumidores antes de acordar payloads] → entregar este contrato y revisar sus propuestas antes de que bifurquen sus integraciones.
- [C-08 aún no existe para la prueba end-to-end de carrera] → probar C-06 con dos escritores sincronizados en PostgreSQL usando el coordinador común y repetir la integración real cuando C-08 esté disponible.

## Migration Plan

No hay migración de base ni cambio de instalación. En cada escritura, el servicio valida y persiste en una transacción coordinada; cualquier validación o persistencia fallida revierte el conjunto completo. Para volver atrás una futura implementación se revierten los cambios de aplicación; no hay DDL que revertir.

## Open Questions

- PB-01 — inclusión y evaluación de los límites de 24 horas y 30 días: permanece para C-07/C-08/C-09 y no cambia el contrato de configuración de C-06.

## UI Mockups

Decision: no (no UI)
