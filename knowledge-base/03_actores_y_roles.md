# Actores y roles

**Estado del documento**: aprobado por el usuario el 2026-11-18.
**Confirmación**: «Aprobar el documento 03 y continuar con el 04».
**Fecha**: 2026-11-18.
**Base**: [Visión y objetivos aprobados](01_vision_y_objetivos.md), [descripción general aprobada](02_descripcion_general.md) y [Discovery aprobado](../discovery/discovery.md).

Los actores, la matriz de permisos y las condiciones de acceso de este documento fueron aprobados por el usuario. Describen comportamiento esperado; no acreditan controles implementados. El mecanismo técnico de acceso del odontólogo y de los enlaces se diseñará en arquitectura. Las decisiones identificadas como pendientes siguen abiertas.

## Actores del sistema

| Actor | Descripción | Cómo interactúa |
|---|---|---|
| Odontólogo | Único usuario interno del único consultorio de esta entrega. | Configura motivos y duraciones, horario semanal, pausas y bloqueos; consulta agenda y datos básicos asociados a turnos. |
| Paciente | Persona que solicita y gestiona un turno. No tiene cuenta ni verifica previamente su email. | Consulta motivos y horarios disponibles, ingresa nombre, apellido, email, teléfono y DNI, reserva y recibe por email enlaces de gestión. |

Para describir los permisos del paciente se distinguen dos situaciones: acceso público para reservar y acceso con la credencial del enlace privado para gestionar un turno concreto. Son dos condiciones de acceso del mismo actor, no dos roles con cuentas.

El sistema confirma automáticamente una reserva válida y ejecuta los avisos. El proceso de recordatorios y el servidor SMTP son componentes técnicos, no usuarios de negocio. No se añade un rol de recepción, administrador de cadenas ni administrador comercial a esta versión.

## RBAC — Matriz de permisos

La matriz combina un único rol interno con permisos públicos y permisos limitados a un recurso por enlace privado. No se propone un registro o inicio de sesión de pacientes.

Leyenda: **C** crear; **R** consultar; **U** modificar; **D** retirar un bloqueo; **—** sin permiso por esta vía; **pendiente** requiere una decisión adicional. Los permisos de escritura no autorizan modificar campos ajenos a la operación indicada.

| Recurso u operación | Paciente: acceso público | Paciente: enlace privado | Odontólogo: acceso interno | Condiciones |
|---|---|---|---|---|
| Motivos ofrecidos y horarios disponibles | R | R, por la misma vía pública | R | La respuesta pública muestra opciones de reserva, no ocupantes ni datos de pacientes. |
| Solicitar una reserva | C | C, por la misma vía pública | Pendiente para alta manual | Cinco datos obligatorios, horario válido y libre, confirmación automática. Tener un enlace no permite saltar las reglas de reserva. |
| Consultar el turno para autogestión | — | R, solo turno autorizado por el enlace | R, turnos de su agenda | La información expuesta al paciente se limitará a la necesaria para identificar y gestionar ese turno; campos exactos pendientes. |
| Cancelar un turno por autogestión | — | U, solo turno autorizado por el enlace | Excepciones manuales pendientes | Debe cumplir el plazo de hasta 24 horas antes. Repetición de la solicitud y límites exactos por definir. |
| Reprogramar un turno por autogestión | — | U, solo turno autorizado por el enlace | Excepciones manuales pendientes | Debe cumplir el plazo; el nuevo horario debe estar libre y ser válido. No habilita editar otros datos del turno. |
| Consultar la agenda diaria o semanal completa | — | — | R | Acceso exclusivo del odontólogo; no hay una lista pública de citas ni pacientes. |
| Consultar los datos básicos asociados a turnos | — | Solo información mínima de su turno por definir | R | No habilita un directorio público ni un historial clínico. |
| Crear o modificar motivos y duraciones | — | — | C, R, U | Retirar motivos y efectos de cambios sobre turnos existentes pendientes de reglas específicas. |
| Configurar horario semanal y pausas | — | — | R, U | Efectos sobre turnos ya reservados pendientes de definición. |
| Gestionar bloqueos de vacaciones o excepciones | — | — | C, R, D | Retirar un bloqueo no elimina turnos. Crear bloqueos que intersectan turnos existentes requiere una política pendiente. |
| Corregir datos básicos, identificar o fusionar duplicados | — | — | Pendiente | El formulario de reserva recoge datos; una reserva no autoriza sobrescribir registros anteriores por coincidir el DNI o el email. |
| Eliminar pacientes, turnos o registros de notificación | — | — | Pendiente | Cancelar un turno es una operación de agenda; no implica borrado físico ni define una política de conservación. |

Los permisos describen el acceso funcional de esta primera versión. La presencia de un único usuario interno no permite inferir que sus pantallas o endpoints puedan quedar públicos.

## Rutas públicas

“Público” significa sin sesión de paciente; el acceso de red sigue limitado al entorno local acordado. Se conserva el mapa orientativo del documento 02, sin fijar un contrato HTTP definitivo.

| Método y ruta orientativos | Acceso esperado | Información o efecto permitido |
|---|---|---|
| `GET /api/public/motivos` | Sin cuenta y sin enlace privado. | Motivos ofrecidos para reservar. |
| `GET /api/public/disponibilidad` | Sin cuenta y sin enlace privado. | Horarios disponibles según motivo y período permitido. |
| `POST /api/public/turnos` | Sin cuenta y sin verificación previa del email. | Solicitar una reserva con los cinco datos obligatorios, sujeta a validación y disponibilidad. |

La página de reserva y los archivos necesarios para cargarla deberán ser accesibles en el navegador local. Sus rutas de interfaz todavía no están fijadas. Cargar los archivos de la interfaz no otorga permiso para consultar agenda ni datos internos.

### Autogestión sin cuenta, con enlace privado

Estas rutas no requieren una cuenta de paciente, pero sí la credencial válida del enlace para un turno concreto. No son accesos públicos irrestrictos.

| Método y ruta orientativos | Alcance permitido |
|---|---|
| `GET /api/gestion/turno` | Consultar la información necesaria del turno autorizado. |
| `POST /api/gestion/turno/cancelacion` | Solicitar su cancelación dentro del plazo permitido. |
| `POST /api/gestion/turno/reprogramacion` | Solicitar su reprogramación dentro del plazo permitido. |

El enlace autoriza una gestión limitada; no prueba por sí mismo la identidad de la persona que lo abre. Si el email es incorrecto o el enlace se reenvía, un tercero podría recibirlo. Se conserva la decisión de no exigir cuenta ni verificación previa del correo; las medidas sobre enlaces, su vencimiento y recuperación se diseñarán sin cambiar esa regla de manera implícita.

Nombre, teléfono, DNI, email o identificador de turno no constituyen por sí solos una credencial de autogestión. La forma de transportar y validar la credencial privada sigue pendiente. No se define un formato de token ni se presupone JWT.

### Rutas internas del odontólogo

El acceso interno comprende las familias orientativas `/api/odontologo/agenda`, `/api/odontologo/turnos/{id}`, `/api/odontologo/motivos`, `/api/odontologo/horario` y `/api/odontologo/bloqueos`, según el documento 02. Todas requieren comprobar el acceso del odontólogo en el backend.

El mecanismo de ingreso, alta inicial y recuperación está pendiente. No se presume registro abierto de profesionales, múltiples cuentas ni un proveedor externo de identidad.

## Condiciones de autorización

1. Comprobar en el backend que cada operación tiene el permiso requerido; ocultar opciones en la interfaz no sustituye esa comprobación.
2. Limitar la credencial privada al turno y operaciones autorizados. Cambiar un identificador no debe permitir acceder a otro turno.
3. Comprobar tanto la autorización como las reglas de negocio: una credencial válida no permite cancelar fuera de plazo ni reprogramar sobre un horario ocupado.
4. Abrir o previsualizar un enlace permite consultar; por sí solo no cancela ni reprograma. Los cambios requieren la operación de escritura correspondiente.
5. Evitar que la consulta pública de motivos o disponibilidad exponga nombre, apellido, email, teléfono, DNI o detalles de turnos ocupados.
6. Mantener las operaciones manuales y de corrección identificadas como pendientes. El título de odontólogo no define automáticamente excepciones, sobrescritura de pacientes ni borrado de datos.

## Comprobaciones acordadas para la implementación posterior

| Caso | Resultado esperado |
|---|---|
| Consultar motivos y disponibilidad sin cuenta. | Se obtienen opciones de reserva sin datos de pacientes. |
| Intentar acceder a agenda o datos internos sin acceso del odontólogo. | La consulta se rechaza, incluso si se llama directamente a la API. |
| Acceder a autogestión sin credencial o con una no válida. | No se muestran ni modifican datos del turno. |
| Intentar acceder a otro turno cambiando su identificador. | La credencial de un turno no autoriza acceso a otro. |
| Abrir o previsualizar un enlace de gestión. | No se cancela ni reprograma el turno por la apertura. |
| Solicitar reprogramación con credencial válida hacia un horario ocupado. | Se rechaza el nuevo horario y no se pierde la reserva original; el detalle transaccional se diseñará en los flujos. |

Son comprobaciones documentadas, aún no ejecutadas. La implementación de acceso y enlaces pertenece a dominios CRITICAL y requerirá la aprobación de código correspondiente según la gobernanza suministrada por el usuario.

## Decisiones pendientes

| Pendiente | Decisión necesaria | Documento previsto |
|---|---|---|
| Acceso interno | Mecanismo de ingreso del odontólogo, alta inicial, recuperación y cierre de acceso. | 07 y 08. |
| Credencial del enlace | Generación, transporte, vencimiento y efecto de cancelación o reprogramación sobre el enlace. | 04, 07 y 08. |
| Email incorrecto o enlace perdido | Procedimiento de recuperación y corrección, preservando la ausencia de verificación previa obligatoria. | 05 y 07. |
| Datos mostrados en autogestión | Campos mínimos necesarios para reconocer y gestionar el turno. | 04 y 07. |
| Operaciones manuales del odontólogo | Alta manual, cancelación, reprogramación y excepciones fuera del plazo; efectos y avisos. | 05 y 07. |
| Datos de pacientes | Identificación, duplicados, correcciones y conservación. | 04, 05 y 08. |

El documento es un análisis de requisitos y permisos del producto, basado en los acuerdos del proyecto. No declara adecuación normativa, controles verificados ni seguridad demostrada. El [índice de la KB](README.md) identifica los estados de redacción y aprobación de todos los documentos.
