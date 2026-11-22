# Arquitectura propuesta

**Estado del documento**: redactado mediante avance en bloque autorizado; propuestas técnicas pendientes de revisión.
**Fecha**: 2026-11-18.
**Base**: documentos aprobados [02 — Descripción](02_descripcion_general.md), [03 — Actores](03_actores_y_roles.md), [04 — Datos](04_modelo_de_datos.md), [05 — Reglas](05_reglas_de_negocio.md), [06 — Funcionalidades](06_funcionalidades.md) y [07 — Flujos](07_flujos_principales.md).

El usuario autorizó completar la redacción restante sin pausas por archivo. Esto no transforma las recomendaciones nuevas en decisiones aprobadas ni habilita implementación de dominios CRITICAL. Este documento es análisis y diseño: no crea archivos de configuración, tablas ni código de aplicación.

## Base acordada y límites

La primera entrega es una aplicación web local para un consultorio, un odontólogo y una agenda. El stack aprobado es Java 21 Temurin, Spring Boot con Gradle Wrapper, React con TypeScript, Vite y CSS, PostgreSQL local, Spring Mail mediante SMTP y tareas internas de recordatorios con estado persistido.

La demostración servirá la interfaz compilada desde Spring Boot. Durante el desarrollo, Vite podrá ejecutarse separado y reenviar la API al backend. No se necesitan alojamiento público, dominio, microservicios ni un servidor Node.js para ejecutar la demostración compilada. Node.js y npm sí son herramientas de compilación.

Las versiones exactas, excepto la línea Java 21, siguen pendientes. Antes de generar el proyecto se comprobarán compatibilidad, soporte y reproducibilidad; no se fijan versiones a partir de ramas `main`, snapshots o valores supuestos.

## Patrones aplicados

| Enfoque | Dónde se usa | Por qué y estado |
|---|---|---|
| Una aplicación organizada por funciones | Agenda, pacientes, comunicaciones y acceso dentro de Spring Boot. | Acordado en 02; limita procesos para la primera entrega. |
| Casos de uso con reglas explícitas | Reservar, cancelar, reprogramar y configurar disponibilidad. | Propuesta PT-01: centralizar validaciones y escrituras para evitar caminos que omitan reglas. |
| Adaptadores de persistencia y correo | Acceso a PostgreSQL y envío SMTP. | Propuesta PT-01: aislar detalles técnicos sin crear una jerarquía de abstracciones por cada entidad. |
| Reloj controlable | Ventanas temporales y recordatorios. | Enfoque de prueba acordado en 01; propuesta de diseño PT-01 para poder probar límites sin esperar un día. |
| Serialización de escrituras por agenda | Reservas, cambios de turno y cambios de configuración que afectan disponibilidad. | Propuesta PT-02, descrita abajo; mecanismo no aprobado todavía. |
| Intención de notificación persistida junto a cambios | Confirmación y avisos derivados de turnos. | Propuesta PT-03 de coordinación transaccional; desarrolla el estado persistido ya aprobado. |
| Acceso interno con sesión y enlace privado acotado | Odontólogo y autogestión del paciente. | Propuesta PT-04; son dos mecanismos distintos, sin cuenta de paciente. |

PT-01 a PT-06 identifican propuestas técnicas y se registran en [decisiones y supuestos](09_decisiones_y_supuestos.md). Las preguntas de negocio PB del documento 05 permanecen abiertas.

## Estructura de directorios

Estructura propuesta, todavía no creada. El paquete Java y el formato del archivo de build de Gradle se fijarán al preparar el proyecto.

```text
odontologia/
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/<paquete>/
│       │   │   ├── agenda/
│       │   │   ├── pacientes/
│       │   │   ├── notificaciones/
│       │   │   ├── acceso/
│       │   │   └── shared/
│       │   └── resources/
│       │       ├── db/
│       │       └── static/
│       └── test/
├── frontend/
│   └── src/
│       ├── features/
│       │   ├── reserva/
│       │   ├── agenda/
│       │   └── autogestion/
│       └── shared/
├── discovery/
└── knowledge-base/
```

Dentro de cada función se propone separar las reglas de negocio, los casos de uso y los adaptadores cuando esa separación aporte claridad. `shared/` queda limitado a tipos o utilidades realmente comunes. La organización de frontend no constituye diseño de pantallas ni selección de librerías visuales.

## Persistencia y transacciones

Como opción inicial de PT-01 se propone Spring JDBC, con consultas explícitas y transacciones gestionadas por Spring. Permite expresar el control temporal y de concurrencia sin introducir un ORM. La alternativa es Spring Data JPA si facilita el trabajo del equipo; no hay una selección aprobada de ambas opciones.

Las migraciones deberán ser versionadas y reproducibles. Herramienta, esquema físico, índices definitivos y reglas de evolución quedan en AT-02 del documento 10. No se propone recrear la base ni cargar ejemplos sobre datos existentes al reiniciar.

Cada caso de uso de escritura deberá coordinar los registros que modifica. La propuesta PT-03 guarda el cambio de turno y la intención de aviso en la misma transacción de PostgreSQL, para evitar que se confirme uno sin registrar la otra. Credenciales y contenido necesario para construir los emails requieren un diseño compatible con ese límite. SMTP queda fuera de la transacción de base de datos.

### Control de concurrencia propuesto — PT-02

Para una única agenda se propone serializar sus escrituras con un bloqueo transaccional sobre la fila de agenda:

1. Iniciar la transacción y obtener el bloqueo antes de consultar las condiciones que se usarán para decidir la escritura.
2. Una vez obtenido, volver a leer configuración, ocupación y turno vigente con datos actuales. Como opción se propone aislamiento `READ COMMITTED`, con la consulta de validación posterior a la adquisición del bloqueo.
3. Validar referencias, permisos, tiempo y disponibilidad; aplicar reserva, cancelación, reprogramación o cambio de configuración según las políticas acordadas.
4. Guardar también las intenciones de aviso necesarias y confirmar. Si se rechaza o falla el cambio, no se confirma una modificación parcial.
5. Liberar el bloqueo al terminar la transacción. Definir tiempos máximos de espera y respuestas de conflicto antes de implementar.

Todas las rutas que alteren disponibilidad deberán seguir el mismo protocolo y orden de bloqueo. Bloquear solo la fila de un turno no impide crear otro intervalo que se superponga. Consultar los intervalos antes de esperar el bloqueo tampoco garantiza que sigan vigentes después.

La propuesta simplifica la corrección a cambio de permitir una sola escritura de agenda por vez. No se plantea como arquitectura de alta concurrencia. Una restricción de exclusión sobre rangos en PostgreSQL es una defensa adicional posible frente a turnos superpuestos; su DDL, filtro por estado y eventual extensión requerida deben revisarse, y no resuelve por sí sola conflictos con bloques o configuración.

La documentación oficial respalda los bloqueos de fila y las diferencias entre aislamientos; el protocolo concreto anterior es una propuesta del proyecto, pendiente de probar. [PostgreSQL: bloqueos](https://www.postgresql.org/docs/current/explicit-locking.html), [aislamiento](https://www.postgresql.org/docs/current/transaction-iso.html), [rangos y exclusión](https://www.postgresql.org/docs/current/rangetypes.html).

## Comunicaciones y recordatorios — PT-03

Se propone una tarea interna y un emisor único para esta ejecución local. El trabajo parte de intenciones persistidas, no exclusivamente de eventos en memoria. El emisor obtiene el aviso y comprueba turno, revisión, destinatario y vigencia antes de solicitar SMTP. Se conserva el modelo de estados de notificación del documento 04.

Una clave de evento impedirá crear dos veces la misma intención de aviso; su composición y el tratamiento de solicitudes repetidas siguen en PB-08 y PB-11. Frecuencia de ejecución, tiempos de espera, reintentos, avisos vencidos y recuperación tras reinicio requieren valores y políticas acordados. Los ejemplos de fuentes no se adoptan como valores de producto.

Un cambio de turno puede coincidir con un envío en curso: releer la revisión antes de enviar no elimina esa carrera. PT-03 exige definir el punto de coordinación entre modificación, selección y envío, con una semántica visible para el usuario y pruebas de concurrencia. No se mantiene un bloqueo de agenda durante una llamada SMTP como decisión implícita.

Si SMTP acepta un mensaje y la aplicación se detiene antes de registrar el resultado, puede quedar una entrega incierta. No se promete exactamente una entrega ni se decide reintentar a ciegas. La política de resultado incierto debe resolverse en PB-10 y PB-11.

Spring Boot dispone de integración de correo; sus tiempos de espera deben configurarse expresamente para no depender de valores que permitan bloqueo indefinido. [Spring Boot: email](https://docs.spring.io/spring-boot/reference/io/email.html). El proveedor gratuito o cuenta concreta sigue pendiente: no se crean cuentas ni se contrata un servicio.

## Seguridad

### Autenticación del odontólogo — propuesta PT-04

Se propone Spring Security con una cuenta interna del odontólogo y sesión del servidor. Evita necesitar un proveedor de identidad externo o tokens JWT para esta entrega local. Alta inicial, forma de aportar la credencial, recuperación, cierre de acceso y expiración de sesión deben revisarse antes de implementar.

Si se adopta contraseña, su representación deberá usar un verificador de contraseña apropiado; no se guardará una contraseña legible en el perfil de odontólogo ni en el repositorio. La elección concreta no se presenta como resuelta por seleccionar Spring Security. La política de sesión tras reinicio también sigue pendiente.

La cookie de sesión se propone inaccesible a JavaScript, con política de envío entre sitios definida y atributo de transporte seguro al usar HTTPS. El entorno HTTP local necesita una configuración acotada al loopback; no se traslada esa configuración a un despliegue público. La protección CSRF debe integrarse con React para las escrituras autenticadas por cookie; no se propone desactivarla globalmente para hacer funcionar la interfaz.

[Spring Security: persistencia de autenticación](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html) y [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) documentan los mecanismos. Su mera disponibilidad no acredita que el proyecto ya los tenga configurados.

### Autogestión del paciente — propuesta PT-04

Se conserva la reserva sin cuenta ni verificación previa del email. Se propone una credencial opaca impredecible, limitada a un turno, sin usar sus datos básicos como secreto. Generación, transporte, representación persistida, vencimiento y renovación se concretarán al resolver PB-07 y AT-04.

Guardar solo un verificador de credencial no resuelve por sí solo cómo reconstruir un enlace para un email pendiente después de reiniciar. El diseño debe coordinar credencial, contenido del aviso y protección de su material sensible. No se propone guardar tokens o enlaces completos en logs ni exponerlos como atributos públicos.

Abrir el enlace mantiene semántica de consulta. Una acción de cambio valida nuevamente alcance, turno, plazo y disponibilidad. Si la credencial se reenvía o el email es ajeno, poseerla no prueba identidad: se documentará la recuperación sin agregar verificación previa obligatoria por omisión.

### Autorización, validación y secretos

- Autorizar cada recurso en backend conforme a 03 y RN-AU-01 a RN-AU-05; separar respuestas públicas de datos internos.
- Usar entradas y salidas explícitas, validación de campos y consultas parametrizadas. No aceptar campos de propietario, estado o revisión como cambios libres del paciente.
- Proponer límites de solicitud y protección de acceso, con umbrales pendientes para evitar inventar cuotas de uso. No se agrega CAPTCHA obligatorio como requisito.
- Mantener fuera del código credenciales SMTP, contraseña de base, verificadores internos y material privado de enlaces. Los mensajes de error y registros deben excluir esos valores y los datos personales innecesarios.
- No hay política de retención, cifrado en reposo, auditoría completa o restauración ya aprobada. Antes de datos reales se debe diseñar y verificar su operación; esta documentación no declara cumplimiento normativo.

Estos puntos son análisis de dominios CRITICAL. La autorización para redactar la KB no es autorización para escribir su código.

## Variables de entorno

Contrato propuesto de configuración, todavía sin archivos creados. `APP_…` son nombres del proyecto, no propiedades autoconfiguradas de Spring Boot: deberán mapearse expresamente. Los valores entre ángulos son marcadores, no credenciales ni valores aprobados.

| Variable propuesta | Descripción | Ejemplo ilustrativo | Sensible |
|---|---|---|---|
| `APP_BASE_URL` | URL de navegación y de los enlaces enviados. | `http://localhost:<puerto>` | No; debe revisarse antes de generar enlaces. |
| `SERVER_ADDRESS` | Interfaz local donde escucha el backend. | `127.0.0.1` como propuesta. | No. |
| `SERVER_PORT` | Puerto del backend. | `<puerto local por fijar>` | No. |
| `APP_TIME_ZONE` | Zona que debe coincidir con la configuración persistida del consultorio. | `<zona acordada>` | No. |
| `SPRING_DATASOURCE_URL` | Conexión a PostgreSQL local. | `jdbc:postgresql://localhost:<puerto>/<base>` | Potencialmente; no incrustar contraseña. |
| `SPRING_DATASOURCE_USERNAME` | Usuario de base. | `<usuario local>` | Restringir exposición. |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de base. | `<secreto local>` | Sí. |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` | Servidor SMTP elegido. | `<host>` / `<puerto SMTP>` | No por sí solos. |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | Acceso SMTP. | `<usuario>` / `<secreto>` | Sí. |
| `APP_MAIL_FROM` | Remitente habilitado por el proveedor. | `<remitente de prueba>` | Dato de contacto; no es secreto de autenticación. |
| `APP_REMINDER_POLL_INTERVAL` | Frecuencia del proceso de recordatorios. | `<pendiente PB-10>` | No. |

Temporización SMTP, material de acceso y configuración CSRF requieren también un contrato antes de implementar, sin fijar secretos ni desactivar validaciones. La zona persistida y la configuración del entorno no pueden divergir silenciosamente. El estado de la agenda seguirá en PostgreSQL; variables de entorno no reemplazan horarios o duraciones configurados por el odontólogo.

## Compilación, ejecución y verificación

La propuesta PT-05 integra la compilación frontend con el empaquetado del backend y conserva archivos de bloqueo de dependencias y Gradle Wrapper. Se comprobará el recorrido desde un checkout limpio, pero no se ofrecen comandos de una aplicación todavía inexistente. Mecanismo de migración y versiones exactas se resolverán antes de generar esa configuración.

PT-06 propone comprobaciones de reglas temporales con reloj controlado, integración sobre PostgreSQL real y concurrencia con solicitudes sincronizadas. No se sustituirá la prueba de concurrencia por dos llamadas secuenciales ni se usará una base en memoria como evidencia suficiente del comportamiento de PostgreSQL.

La validación de entrega seguirá los 48 criterios de las doce historias y CT-01 a CT-06, sin marcarlos cumplidos por existir documentación. Incluye recepción real de emails a buzones de prueba y persistencia tras reinicio. La verificación visual requiere la política de diseño de la fase siguiente; aquí no se crea ni aprueba un sistema visual.

## Fuentes y consulta

Consultadas el **2026-11-18** mediante Context7 y páginas oficiales; las fuentes explican capacidades, no aprueban nuestras propuestas.

| Fuente | URL | Uso |
|---|---|---|
| Spring Boot — SQL Databases | https://docs.spring.io/spring-boot/reference/data/sql.html | Integración JDBC y configuración de acceso relacional, consultada mediante Context7. |
| PostgreSQL — Explicit Locking | https://www.postgresql.org/docs/current/explicit-locking.html | Bloqueos transaccionales de fila. |
| PostgreSQL — Transaction Isolation | https://www.postgresql.org/docs/current/transaction-iso.html | Lecturas y condiciones de concurrencia por aislamiento. |
| PostgreSQL — Range Types | https://www.postgresql.org/docs/current/rangetypes.html | Exclusión de intervalos como defensa posible; evidencia también relevada durante Discovery. |
| Spring Security — Persisting Authentication | https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html | Persistencia de autenticación y sesión. |
| Spring Security — CSRF | https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html | Protección de escrituras y particularidades de integración SPA. |
| Spring Boot — Sending Email | https://docs.spring.io/spring-boot/reference/io/email.html | Adaptador SMTP y tiempos de espera. |

La propuesta sigue sujeta a los pendientes del [documento 10](10_preguntas_abiertas.md). Los antecedentes del stack ya aprobado se conservan en [la propuesta técnica aprobada](../discovery/propuesta-tecnica-kb.md).
