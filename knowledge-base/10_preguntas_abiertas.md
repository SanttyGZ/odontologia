# Preguntas abiertas

**Estado del documento**: redactado mediante avance en bloque autorizado; pendientes consolidados, sin respuestas presumidas.
**Fecha**: 2026-11-18.
**Base**: [Discovery](../discovery/discovery.md) y documentos [01](01_vision_y_objetivos.md) a [07](07_flujos_principales.md) aprobados; [08](08_arquitectura_propuesta.md) y [09](09_decisiones_y_supuestos.md) redactados en esta entrega.

Este es un registro de trabajo, no una solicitud de respuestas inmediatas. El usuario autorizó completar los documentos sin pausas. Los pendientes se mantienen para resolverlos antes del flujo o fase que bloquean; completar la KB no los cierra.

## Inconsistencias detectadas

Las tres diferencias siguientes son aclaraciones históricas resueltas, no contradicciones activas entre requisitos vigentes.

### IN-01 — Estudio amplio frente a MVP acotado

**Documento A dice**: el [estudio de mercado](../discovery/informe-mercado.md) explora suites integrales y un MVP inicial más amplio.
**Documento B dice**: el [Discovery aprobado](../discovery/discovery.md) y [01](01_vision_y_objetivos.md) limitan la primera entrega a agenda y datos básicos.
**Impacto**: incorporar clínica, cobros o varias agendas sin autorización ampliaría la entrega.
**Resolución**: resuelta por precedencia de decisiones posteriores del usuario. El estudio queda como antecedente, sin ampliar el MVP.

### IN-02 — Formulación inicial del presupuesto

**Antecedente A dice**: el usuario mencionó inicialmente «presupuesto minimo: 0ARS».
**Documento B dice**: el [Discovery aprobado](../discovery/discovery.md) recoge la aclaración «no existe presupuesto minimo» y prioridad a tecnologías gratuitas cuando sea posible.
**Impacto**: interpretar un máximo absoluto de ARS 0 o contratar servicios por inferencia falsearía la restricción.
**Resolución**: resuelta; no hay mínimo definido ni máximo absoluto acordado. Tampoco hay autorización para contratar servicios pagos.

### IN-03 — Objetivo SaaS frente a demostración local

**Documento A dice**: objetivo comercial de vender un SaaS a consultorios.
**Documento B dice**: [primera entrega acordada](../discovery/propuesta-tecnica-kb.md) para un consultorio, local y con destinatarios de prueba.
**Impacto**: confundir objetivo con entrega agregaría multiempresa, alojamiento o lanzamiento comercial.
**Resolución**: resuelta; la primera versión es una etapa local del producto comercial futuro. Ni `localhost` habilita una reserva pública por internet ni el objetivo SaaS prueba que ya existe operación multiempresa.

## Preguntas abiertas de negocio — priorizadas

Los códigos PB conservan exactamente los identificadores del [documento 05](05_reglas_de_negocio.md). El estado se indica por fila. PB-02, PB-03 y PB-05 se resolvieron para C-06; las demás siguen abiertas. “Alta” significa antes de implementar o aceptar el comportamiento afectado; no exige resolverlas durante la redacción.

| Código | Prioridad | Pregunta | Bloquea | Decisor |
|---|---|---|---|---|
| PB-01 | Resuelta en C-07 | ¿Son inclusivos 24 horas y 30 días? ¿30 días calendario o duración equivalente, y cuándo se evalúa el límite? | Bordes de reserva y autogestión; US-004, US-005, US-008 y US-009. | Usuario (2026-11-24): límite inferior inclusivo (`inicio >= ahora + 24h`) y límite superior calendario inclusivo hasta `fecha(ahora) + 30 días` en zona del consultorio (`America/Argentina/Mendoza`), evaluado con el `Clock` del backend al consultar y al confirmar bajo bloqueo. |
| PB-02 | Resuelta en C-02/C-06 | ¿Qué zona usa el consultorio? ¿Se admiten franjas nocturnas, repetidas o superpuestas? | Zona fijada en C-02; C-06 usa franjas del mismo día, sin duplicados ni solapes. No bloquea C-06. | Santiago (usuario); confirmó explícitamente estas reglas el 2026-11-24. Ver [registro de aprobación C-06](#registro-de-aprobación-de-decisiones-c-06-2026-11-24), diseño archivado D-02 y spec AC-03. |
| PB-03 | Resuelta en C-06 | ¿Con qué paso se ofrecen inicios y qué límites o múltiplos tienen las duraciones? | C-06 fija inicios cada 15 minutos, anclados al comienzo de cada franja, y duraciones múltiplos de 15 entre 15 y 240 minutos; C-07 consume esa regla. | Santiago (usuario); confirmó explícitamente estas reglas el 2026-11-24. Ver [registro de aprobación C-06](#registro-de-aprobación-de-decisiones-c-06-2026-11-24), diseño archivado D-05 y spec AC-02/AC-07. |
| PB-04 | Resuelta en C-08 | ¿Cómo crear o reutilizar pacientes, validar campos y corregir errores sin sobrescribir por una coincidencia de DNI o email? | Reserva, persistencia de paciente y destinatarios. | Usuario (2026-11-24): reutilizar `Paciente` únicamente si coinciden exactamente los 5 campos obligatorios (`nombre`, `apellido`, `email`, `telefono`, `dni`); en cualquier otro caso crear un nuevo registro, sin sobrescribir ni fusionar por coincidencia parcial. |
| PB-05 | Resuelta en C-06 | ¿Qué pasa al cambiar horario, motivo o bloqueos con turnos existentes? ¿Qué duración se usa al reprogramar tras cambiar el motivo? | C-06 rechaza cambios que afecten turnos confirmados; al reprogramar se usa la duración actualmente configurada. | Santiago (usuario); confirmó explícitamente PB-05a/b el 2026-11-24. Ver [registro de aprobación C-06](#registro-de-aprobación-de-decisiones-c-06-2026-11-24), diseño archivado D-03 y spec AC-02/AC-05. |
| PB-06 | Media | ¿Se incorpora alta manual? ¿Qué cancelaciones o reprogramaciones puede hacer el odontólogo fuera del plazo y cómo se lo contacta? | Alcance de operaciones manuales y contacto operativo. | Usuario; validar con odontólogo cuando sea posible. |
| PB-07 | Alta | ¿Cuándo vence o se invalida un enlace, qué datos muestra y cómo recuperar acceso tras pérdida o email incorrecto? | Autogestión y ciclo de credenciales. | Usuario sobre experiencia; agente sobre mecanismo. |
| PB-08 | Alta | ¿Qué respuesta corresponde a solicitudes repetidas, cancelación ya hecha, reactivación y cambios simultáneos? | Resultados consistentes y tratamiento de repetición. | Usuario sobre política; agente sobre integridad. |
| PB-09 | Alta | ¿Cómo combinar confirmación y recordatorio al reservar exactamente a 24 horas? | Criterio de recordatorios en el límite. | Usuario. |
| PB-10 | Alta | ¿Qué se comunica si falla el email? ¿Qué tolerancia, reintentos y tratamiento de avisos vencidos o reinicios se acepta? | Flujos de error, comunicaciones y aceptación de correo. | Usuario; agente propone opciones verificables. |
| PB-11 | Alta | ¿Cómo identificar un aviso único y coordinar sustitución, anulación, envío en curso y resultado SMTP incierto? | Avisos obsoletos, duplicados y concurrencia de comunicaciones. | Agente diseña; usuario revisa la semántica. |
| PB-12 | Alta antes de datos reales | ¿Qué conservación, corrección, eliminación y recuperación se requieren? | Operación con información real y procedimientos de recuperación. | Usuario y responsable operativo; revisión especializada cuando corresponda. |

PB-06 no amplía el MVP automáticamente. Si la aceptación de la primera entrega requiere una operación manual o contacto concreto, la decisión pasa a prioridad alta para ese caso. No se reabre el requisito de reserva sin cuenta o sin verificación previa como respuesta implícita a PB-07.

## Preguntas técnicas abiertas

AT identifica pendientes técnicos y no reemplaza las preguntas PB. Las propuestas PT del [documento 08](08_arquitectura_propuesta.md) son puntos de partida para revisarlos, no elecciones ya aprobadas.

**Actualización C-01 (2026-11-22):** AT-01 quedó resuelto con versiones fijadas
y comprobadas; ver [README](../README.md). AT-02 quedó parcialmente resuelto
con Spring JDBC y Flyway deshabilitado: esquema, migraciones y concurrencia
requieren revisión en C-02. AT-08 tiene puertos, escucha, URL y configuración
local aprobados; la zona persistida PB-02 sigue pendiente. AT-10 tiene JUnit,
builds y pruebas reales de conexión a PostgreSQL comprobados; reglas temporales,
concurrencia de negocio y correo permanecen pendientes para sus changes.
Fuente: aprobación D-01 a D-05 y decisiones posteriores en el
[diseño archivado](../openspec/changes/archive/2026-11-22-foundation-setup/design.md).
La tabla conserva las preguntas originales para distinguir el cierre parcial.

**Actualización C-02 (2026-11-22).** AT-02 se resolvió para el esquema inicial con Spring JDBC y Flyway. AT-05 se concretó con el coordinador de escritura y la exclusión en PostgreSQL. AT-08 y la parte de zona de PB-02 se concretaron para la demo con `America/Argentina/Mendoza` (UTC−3), Grupo13 y Carlos Garcia. Franjas nocturnas y solapamientos quedaron para C-06. El esquema admite pacientes con datos repetidos y la política de creación o reutilización de PB-04 se decide en C-08. Ver el [diseño archivado](../openspec/changes/archive/2026-11-22-core-models/design.md).

**Actualización C-06.** PB-02, PB-03 y PB-05 quedaron resueltas; sus reglas están en el [change archivado](../openspec/changes/archive/2026-11-24-configuracion-disponibilidad-api/) y la aprobación directa de Santiago consta en el registro que sigue. PB-01 permanece abierta para definir ventana temporal y límites antes de C-07/C-08/C-09.

### Registro de aprobación de decisiones C-06 (2026-11-24)

**Aprobador:** Santiago, responsable del proyecto.

**Confirmación registrada:** ante la pregunta que enumeró las alternativas y reglas PB-02, PB-03, PB-05a y PB-05b, Santiago respondió: «Sí, confirmo todas».

La confirmación comprende: franjas semanales del mismo día, sin duplicados ni solapes, y pausas contenidas en una franja de atención; inicios cada 15 minutos anclados al comienzo de cada franja y duraciones de 15 a 240 minutos en múltiplos de 15; rechazar cambios que afecten turnos confirmados; y usar la duración actualmente configurada al reprogramar. El contenido aprobado se detalla en el [diseño archivado D-02/D-03/D-05](../openspec/changes/archive/2026-11-24-configuracion-disponibilidad-api/design.md) y en [AC-02/AC-03/AC-05/AC-07](../openspec/changes/archive/2026-11-24-configuracion-disponibilidad-api/specs/availability-configuration/spec.md).

| Código | Prioridad | Pregunta | Bloquea | Decisor |
|---|---|---|---|---|
| AT-01 | Alta | ¿Qué versiones compatibles se fijan para Spring Boot, Gradle, React, TypeScript, Vite, Node.js, npm y PostgreSQL? ¿Qué actualización Java 21 y DSL de Gradle? | Preparación reproducible del proyecto. | Agente verifica y propone; usuario revisa cambios relevantes. |
| AT-02 | Alta | ¿Spring JDBC o JPA? ¿Qué migraciones, esquema, índices y evolución física materializan el modelo? | Persistencia y primer build con base. | Agente propone; respetar gobernanza de datos y configuración. |
| AT-03 | Resuelta en C-03 | Spring Security con sesión en memoria de 30 minutos de inactividad, reinicio que invalida sesiones, credencial recuperable desde la computadora local y protección CSRF. | Contrato de acceso interno para C-06, C-10 y C-12. | Usuario aprobó el diseño y el código CRITICAL. [Diseño](../openspec/changes/archive/2026-11-22-acceso-interno-odontologo/design.md) |
| AT-04 | Alta | ¿Cómo generar, transportar, persistir y recuperar material de enlaces de forma compatible con emails pendientes y reinicios? | Autogestión y composición durable del correo. | Agente propone; usuario revisa PB-07 y código CRITICAL. |
| AT-05 | Alta | ¿Se adopta bloqueo por agenda? ¿Qué orden, aislamiento, tiempos de espera y defensa en base se usarán en todas las escrituras? | Correctitud concurrente de FL-02, FL-04, FL-06 y FL-07. | Agente diseña y prueba; usuario revisa decisiones. |
| AT-06 | Alta | ¿Cómo coordinar intención persistida, emisor, cambios concurrentes, reinicios, avisos vencidos e intentos inciertos? | Implementación de PB-10 y PB-11; FL-08 y FL-09. | Agente propone; usuario revisa comportamiento. |
| AT-07 | Alta | ¿Qué cuenta o servicio SMTP, remitente y destinatarios de prueba se habilitan? ¿Qué cuotas y condiciones gratuitas se verifican? | Emails reales y CA-VIS-06. | Usuario habilita acceso por canal local seguro; agente verifica documentación, sin pedir secretos en la conversación. |
| AT-08 | Alta | ¿Qué URL, puertos, escucha local y mecanismo de configuración se usarán? ¿Cómo se mantiene consistente la zona persistida? | Enlaces y ejecución local reproducible. | Agente propone; usuario revisa entorno. |
| AT-09 | Alta antes de datos reales | ¿Qué medidas y procedimientos de acceso, almacenamiento, registros, respaldo y recuperación se deben implementar y verificar? | Paso de demostración ficticia a piloto real. | Usuario y responsable operativo; agente analiza, sin declarar cumplimiento legal. |
| AT-10 | Media antes de verificar | ¿Qué herramientas y evidencias se usarán para reglas temporales, concurrencia real, emails y comparación visual? | Verificación de las historias. | Agente propone; usuario revisa criterios ya acordados. |
| AT-11 | Antes de trabajo visual | ¿Qué política de sistema de diseño y mockups se adoptará, con qué nombre y referencias provisionales? | Fase de diseño y aplicación visual. | Usuario conforme a la política del proyecto; no se elige un modo por omisión. |

Las variables de 08 son un contrato propuesto. No hay configuración de datos, credenciales o respaldo aplicada por redactar esta tabla. No se usa el modo silencioso como aprobación de código, nuevas contrataciones o un sistema visual no presentado.

## Preguntas de producto y operación posterior

| Código | Prioridad | Pregunta | Bloquea | Decisor |
|---|---|---|---|---|
| AP-01 | Media | ¿Qué nombre comercial, identidad y referencias se usarán? | Marca y diseño; puede usarse un identificador provisional explícito. | Usuario. |
| AP-02 | Antes de comercializar | ¿Qué tarifa y mecanismo de suscripción tendrá el SaaS? | Oferta y cobro comercial; no son cobros a pacientes. | Usuario, tras validación. |
| AP-03 | Alta para planificar | ¿Cuándo empieza y termina la semana y qué disponibilidad de trabajo existe? | Calendario real; no reabre los criterios CA-VIS ya aprobados. | Usuario. |
| AP-04 | Alta para validar valor | ¿Cómo obtener un odontólogo para entrevista o piloto y medir trabajo de coordinación y uso autónomo? | Evidencia de problema, beneficio y disposición a pagar. | Usuario; agente prepara guion y medición. |
| AP-05 | Posterior al MVP | ¿En qué orden se priorizan clínica, cobros, WhatsApp, lista de espera u otras extensiones? | Roadmap posterior; no bloquea documentar el alcance actual. | Usuario con evidencia del piloto. |
| AP-06 | Antes de servicio público | ¿Cómo evolucionar a alojamiento, varios consultorios, soporte y operación comercial sostenible? | Lanzamiento SaaS; no es requisito de la demostración local. | Usuario; agente propone arquitectura y costos verificables. |

## Orden recomendado de resolución

1. Para preparar el proyecto: AT-01, AT-02 y AT-08; AP-03 para poner fechas reales.
2. Antes de C-07/C-08/C-09: resolver PB-01 para ventana temporal y límites; PB-02, PB-03 y PB-05 ya quedaron resueltas en C-06. Definir PB-08 para las operaciones que se implementen.
3. C-03 resolvió AT-03 para el acceso interno. Antes de autogestión, resolver PB-07 y AT-04 con revisión del diseño y autorización de código CRITICAL.
4. Antes de completar correo: PB-09 a PB-11, AT-06 y AT-07; no basta con sustituir emails reales por una simulación para dar por cumplida la entrega acordada.
5. Antes de trabajo visual: AT-11. Antes de datos reales: PB-12 y AT-09. La entrevista de AP-04 debe buscarse en paralelo cuando exista acceso a un profesional.

Este orden es una recomendación para decidir, no un roadmap de cambios OpenSpec ni un calendario prometido. No requiere pausar la entrega documental ya autorizada.

## Estado de cierre documental

La KB contiene los diez archivos canónicos y el índice. Los documentos 01–07 tienen aprobación registrada; 08–10 fueron redactados en bloque por autorización del usuario y conservan propuestas y pendientes explícitos. La aprobación global de nuevas propuestas no se presume.

El [README](README.md) es el punto de entrada y referencia de estados. No se han ejecutado pruebas de aplicación, enviado correos ni implementado controles por completar la documentación.
