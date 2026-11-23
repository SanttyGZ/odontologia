# Decisiones y supuestos

**Estado del documento**: redactado mediante avance en bloque autorizado; registro consolidado de decisiones aprobadas, propuestas y supuestos.
**Fecha**: 2026-11-18.
**Base**: [Discovery](../discovery/discovery.md), [stack y entrega acordados](../discovery/propuesta-tecnica-kb.md) y documentos [01](01_vision_y_objetivos.md), [02](02_descripcion_general.md), [03](03_actores_y_roles.md), [04](04_modelo_de_datos.md), [05](05_reglas_de_negocio.md), [06](06_funcionalidades.md) y [07](07_flujos_principales.md), aprobados.

Los registros DD consolidan decisiones ya tomadas. Los PT son recomendaciones nuevas del documento 08 y los SU son hipótesis por validar. La autorización de completar la KB sin pausas no aprueba automáticamente PT ni convierte SU en hechos.

## Decisiones documentadas

### DD-01 — Producto comercial y problema inicial

**Decisión**: construir un SaaS para vender a consultorios, empezando por el odontólogo independiente en Argentina y el trabajo de reservar, confirmar y reprogramar.
**Contexto**: el Discovery comparó alternativas de mercado y luego se delimitó el producto con el usuario.
**Alternativas consideradas**: el estudio exploró sistemas integrales; el alcance aprobado priorizó coordinación de turnos. No se atribuye al usuario una comparación comercial medida.
**Justificación**: foco explícito del usuario.
**Trade-offs aceptados**: no cubrir desde el inicio toda la gestión odontológica; demanda y disposición a pagar siguen sin validar.
**Estado y fuente**: aprobado en Discovery; [01](01_vision_y_objetivos.md).

### DD-02 — Primera entrega local para un consultorio

**Decisión**: una aplicación web local con un consultorio, un odontólogo y una agenda; emails reales a destinatarios de prueba.
**Contexto**: convertir el objetivo comercial en una entrega acotada.
**Alternativas consideradas**: aplicación compartida entre consultorios, instalaciones independientes o primera versión de un solo consultorio; se eligió la última. Se eligió entrega local frente a piloto alojado o producto público.
**Justificación**: elección explícita del usuario.
**Trade-offs aceptados**: los enlaces `localhost` se prueban en la misma computadora; no se ofrece servicio público ni disponibilidad continua.
**Estado y fuente**: aprobado en la fase KB; [propuesta técnica](../discovery/propuesta-tecnica-kb.md) y [01](01_vision_y_objetivos.md).

### DD-03 — Actores y reserva sin cuenta

**Decisión**: odontólogo como único usuario interno; paciente sin cuenta ni verificación previa de email. La reserva válida queda confirmada automáticamente y la autogestión requiere el enlace privado de su turno.
**Contexto**: reducir coordinación manual y fricción en la reserva.
**Alternativas consideradas**: se ofrecieron opciones de confirmación y acceso durante el Discovery; se eligió confirmación automática sin cuenta ni verificación previa.
**Justificación**: decisiones expresas del usuario.
**Trade-offs aceptados**: correo incorrecto, enlace reenviado o perdido requieren recuperación por diseñar; el enlace no prueba identidad.
**Estado y fuente**: aprobado; [03](03_actores_y_roles.md), RN-RE-03 y RN-AU-01 a RN-AU-05 de [05](05_reglas_de_negocio.md).

### DD-04 — Disponibilidad y ventanas temporales

**Decisión**: duración por motivo, horario semanal, pausas y bloqueos. Reserva de 24 horas a 30 días; cancelación y reprogramación autónomas hasta 24 horas antes. Cinco datos obligatorios: nombre, apellido, email, teléfono y DNI.
**Contexto**: definir cuándo y cómo se obtiene un turno.
**Alternativas consideradas**: en el Discovery se eligieron las opciones configurables de duración y horario. No hay una alternativa ya seleccionada para los bordes exactos.
**Justificación**: reglas elegidas por el usuario.
**Trade-offs aceptados**: no admitir coordinación autónoma de último momento; zona, inclusión de extremos y granularidad siguen pendientes.
**Estado y fuente**: aprobado; [Discovery](../discovery/discovery.md), [05](05_reglas_de_negocio.md), PB-01 a PB-04.

### DD-05 — Agenda y datos básicos antes que clínica y cobros

**Decisión**: primera versión con agenda y datos básicos; clínica y cobros se posponen. Email es el canal inicial; WhatsApp, recepción y múltiples profesionales no se incorporan.
**Contexto**: limitar el MVP al problema prioritario.
**Alternativas consideradas**: las recomendaciones iniciales del estudio incluían más funciones; el usuario definió un alcance menor.
**Justificación**: selección explícita del MVP.
**Trade-offs aceptados**: menor amplitud funcional frente a suites integrales; el mecanismo de suscripción del SaaS se define después y no se confunde con cobros a pacientes.
**Estado y fuente**: aprobado; [01](01_vision_y_objetivos.md) y [06](06_funcionalidades.md).

### DD-06 — Semana objetivo y preferencia por gratuidad

**Decisión**: una semana para la primera versión funcional, con tecnologías y servicios gratuitos cuando sea posible. No existe presupuesto mínimo definido ni un máximo absoluto de ARS 0.
**Contexto**: el usuario aclaró posteriormente la formulación inicial del presupuesto.
**Alternativas consideradas**: no se acordó una comparación de planes pagos ni se autorizó contratación.
**Justificación**: «no existe presupuesto minimo, todo con tegnologias gratuitas en lo posible».
**Trade-offs aceptados**: el correo puede depender de cuotas o cuentas; las fechas exactas no están fijadas y no se garantiza una operación comercial gratuita.
**Estado y fuente**: aprobado; [Discovery](../discovery/discovery.md) y [01](01_vision_y_objetivos.md).

### DD-07 — Stack y empaquetado

**Decisión**: Java 21 Temurin, Spring Boot con Gradle Wrapper, React con TypeScript y Vite, CSS, PostgreSQL local y SMTP mediante Spring Mail. Frontend compilado servido por Spring Boot; Vite separado durante desarrollo; recordatorios internos con estado persistido.
**Contexto**: Spring Boot, Gradle y TypeScript eran obligatorios; el usuario pidió una combinación sencilla y gratuita para revisar.
**Alternativas consideradas**: TypeScript sin React y persistencia embebida figuraron como alternativas. Se aprobó la combinación propuesta, sin fijar las versiones restantes ni ORM.
**Justificación**: «Aprobar stack y estructura; comenzar la redacción» y posterior aprobación de 02.
**Trade-offs aceptados**: dos herramientas de build y un proceso PostgreSQL local; SMTP y versiones exactas pendientes.
**Estado y fuente**: aprobado; [propuesta técnica](../discovery/propuesta-tecnica-kb.md) y [02](02_descripcion_general.md).

### DD-08 — Modelo lógico y estados separados

**Decisión**: diez entidades lógicas, UUID, duración reservada guardada, reprogramación del mismo turno con revisión, estados de turno `CONFIRMADO` y `CANCELADO`, ocupación con inicio incluido y fin excluido, notificaciones con revisión y destinatario persistidos.
**Contexto**: conservar reservas y distinguir cambios de turno de resultados del correo.
**Alternativas consideradas**: no se eligieron unicidad natural de paciente, formato de credencial ni esquema físico. Son pendientes, no alternativas descartadas.
**Justificación**: aprobación explícita de 04.
**Trade-offs aceptados**: la revisión no sustituye auditoría; aceptación SMTP no demuestra entrega; no hay fusión o corrección automática de pacientes definida.
**Estado y fuente**: aprobado; [04](04_modelo_de_datos.md).

### DD-09 — Trazabilidad de reglas, historias y flujos

**Decisión**: 40 reglas RN, doce preguntas PB separadas, doce historias US con 48 criterios, seis criterios transversales y nueve flujos FL. Las siete comprobaciones CA-VIS de visión orientan la entrega.
**Contexto**: documentar resultados observables sin declarar funcionalidades implementadas.
**Alternativas consideradas**: no se realizó una evaluación de metodologías alternativas; se siguió el flujo de fundación solicitado.
**Justificación**: aprobaciones sucesivas de 01 y 05–07.
**Trade-offs aceptados**: los criterios que dependen de PB no están cerrados para implementación; ninguna casilla se marca por existir documentación.
**Estado y fuente**: aprobado; [05](05_reglas_de_negocio.md), [06](06_funcionalidades.md), [07](07_flujos_principales.md).

### DD-10 — Completar la redacción restante en bloque

**Decisión**: aprobar 07 y redactar 08–10 sin nuevas pausas por archivo, con comunicación mínima.
**Contexto**: el usuario pidió «si apruebo y trabaja en modo silencioso, avance con todos los siguientes documentos».
**Alternativas consideradas**: sustituye la revisión individual que se venía aplicando al resto de la KB.
**Justificación**: instrucción explícita del usuario, con prioridad sobre el workflow de la skill.
**Trade-offs aceptados**: los documentos nuevos se entregan juntos; las propuestas no vistas no se registran como aprobadas. No se autoriza implementación de seguridad ni se omiten las decisiones de diseño visual de fases posteriores.
**Estado y fuente**: aprobado en esta conversación; registro de estado en [README](README.md).

## Propuestas técnicas pendientes de revisión

**Actualización aprobada en C-01 (2026-11-22):** el usuario aprobó D-01 a D-05
del [diseño archivado](../openspec/changes/archive/2026-11-22-foundation-setup/design.md),
incluyendo versiones exactas, Gradle Kotlin DSL, Spring JDBC, Flyway inicialmente
deshabilitado, puertos y variables locales. Después autorizó PostgreSQL Docker
aislado con inicio documentado y las excepciones de evidencia/reproducción de
D-06 a D-08. La reproducción final y el archivo están documentados en
[verification.md](../openspec/changes/archive/2026-11-22-foundation-setup/verification.md).

Esto resuelve la elección inicial JDBC/Flyway de PT-01, el build separado y sus
bloqueos de PT-05, y la base de pruebas de configuración/conectividad de PT-06.
No aprueba esas PT completas: esquema y protocolo concurrente quedan para C-02;
empaquetado integrado, pruebas del dominio y correo siguen en sus changes.

**Actualización C-02 (2026-11-22).** El usuario aprobó el [diseño de C-02](../openspec/changes/archive/2026-11-22-core-models/design.md). El change concretó la persistencia base y el reloj de PT-01 con Spring JDBC y Flyway. Para PT-02 implementó el bloqueo transaccional por agenda, relectura con READ COMMITTED y exclusión de intervalos confirmados. Los flujos de C-06, C-08 y C-09 consumen este contrato.

**Actualización C-03 (2026-11-22).** El usuario aprobó el acceso interno de PT-04 y autorizó por separado el código CRITICAL. C-03 implementó el aprovisionamiento y la recuperación local, sesión en memoria, cierre, expiración y CSRF. La credencial de autogestión sigue en C-04. [Diseño archivado](../openspec/changes/archive/2026-11-22-acceso-interno-odontologo/design.md)

| ID | Recomendación del documento 08 | Alternativa y costo | Qué falta comprobar |
|---|---|---|---|
| PT-01 | Casos de uso por función, adaptadores acotados, reloj controlable y Spring JDBC como primera opción. | JPA si facilita implementación; JDBC requiere consultas y mapeo explícitos. | Compatibilidad, experiencia y elección de acceso a datos y migraciones. |
| PT-02 | Bloqueo transaccional por agenda y lectura actualizada posterior al bloqueo para serializar escrituras. | Exclusión temporal en base como defensa adicional; aislamiento serializable requiere otro tratamiento de conflictos. | Protocolo común, orden, tiempos de espera y pruebas de todas las rutas. |
| PT-03 | Guardar cambio de turno e intención de aviso en una transacción; emisor local desde pendientes persistidos. | Eventos solo en memoria no cubren reinicios; componentes externos aumentan operación. | Clave de evento, recuperación, entrega incierta y coordinación con cambios durante SMTP. |
| PT-04 | Sesión interna mediante Spring Security y credencial opaca acotada para paciente. | Proveedor externo o JWT agregan decisiones que no se requieren todavía. | C-03 resolvió el acceso interno. C-04 conserva el diseño y la aprobación CRITICAL de la credencial de enlaces. |
| PT-05 | Compilación reproducible y empaquetado de frontend estático con el backend. | Compilar manualmente sin integración aumenta pasos operativos. | Versiones, comandos, archivos de bloqueo y verificación desde un checkout limpio. |
| PT-06 | Pruebas temporales con reloj controlado, PostgreSQL real y concurrencia sincronizada. | Pruebas solo secuenciales o base en memoria no acreditan estos riesgos. | Herramientas, escenarios, evidencia y validación de correo. |

PT-05 y PT-06 desarrollan aspectos de entrega y prueba ya acordados; sus mecanismos concretos son propuestas. Las fuentes de capacidades técnicas están en [08](08_arquitectura_propuesta.md). No se inventan precios, cuotas o servicios gratuitos disponibles.

## Supuestos inferidos

### SU-01 — La coordinación autónoma reduce trabajo relevante

**Supuesto**: reservar, cancelar y reprogramar sin mensajes manuales aporta valor suficiente al odontólogo independiente.
**Origen**: objetivo definido por el usuario, todavía sin entrevista con odontólogo.
**Riesgo si es falso**: producto correcto técnicamente que no resuelve una prioridad ni justifica pago.
**Cómo validar**: entrevistar al menos un odontólogo disponible y medir coordinación antes y durante un piloto comparable; cantidades y metas por acordar.

### SU-02 — Una instancia local alcanza para la demostración

**Supuesto**: una instancia de backend y PostgreSQL puede servir los recorridos y la concurrencia de prueba de esta entrega.
**Origen**: alcance de un consultorio y propuesta PT-02, sin volumen numérico informado.
**Riesgo si es falso**: esperas o conflictos que impidan completar las pruebas.
**Cómo validar**: comprobar flujos y concurrencia sobre el entorno local antes de comprometer capacidad. Un odontólogo interno no significa un único paciente concurrente.

### SU-03 — Se puede habilitar correo de prueba sin contratar

**Supuesto**: una cuenta existente o servicio con condiciones gratuitas permite enviar a los destinatarios de prueba.
**Origen**: preferencia por gratuidad y entrega con emails reales; no hay proveedor seleccionado.
**Riesgo si es falso**: bloqueo de CA-VIS-06 o necesidad de revisar condiciones de la demostración.
**Cómo validar**: elegir una opción, verificar remitente y límites oficiales y comprobar recepción; no solicitar secretos en la conversación ni contratar sin autorización.

### SU-04 — La disponibilidad semanal representa el trabajo inicial

**Supuesto**: horario semanal, pausas y bloqueos bastan para representar la agenda del primer profesional que valide.
**Origen**: alcance aprobado sin observación de un consultorio real.
**Riesgo si es falso**: necesidad de excepciones o formas de atención no contempladas.
**Cómo validar**: contrastar un ejemplo de semana real durante la entrevista, sin ampliar el MVP antes de priorizar.

### SU-05 — El acceso por email resulta utilizable en el segmento

**Supuesto**: el paciente puede recibir y usar los enlaces con un nivel aceptable de asistencia.
**Origen**: canal elegido, sin pruebas de usabilidad con pacientes.
**Riesgo si es falso**: recuperación de enlaces y coordinación manual reducen el beneficio esperado.
**Cómo validar**: observar reserva y autogestión en pruebas; distinguir la limitación de `localhost` de los problemas de experiencia y de entrega de email.

## Convención para cambios posteriores

Una decisión nueva debe indicar si modifica un DD o resuelve un PB, AT o AP del [documento 10](10_preguntas_abiertas.md). Mantener la trazabilidad y actualizar los documentos afectados. No convertir recomendaciones del estudio de mercado, ejemplos de datos o mecanismos propuestos en hechos aprobados sin respaldo.
