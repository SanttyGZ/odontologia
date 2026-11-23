# Design

## Context

C-01 corresponde a `foundation-setup`; su responsable es Fernando. Ver [proposal.md](proposal.md) para la motivación y el alcance. El CLI confirmó un proyecto local con esquema `spec-driven`, sin otros changes ni especificaciones consolidadas al comenzar esta ronda.

La inspección previa encontró Java 17.0.12 Oracle activo, Node.js 20.19.2, npm 9.2.0, cliente PostgreSQL 17.11 y OpenSpec 2.0.0. `pg_isready` no obtuvo respuesta en el socket local predeterminado; esto no demuestra que no exista otra instancia. No se encontró Temurin 21 entre los JDK locales inspeccionados. Son observaciones del entorno, no resultados de pruebas de la aplicación.

La KB 01–07 está aprobada; las elecciones PT y AT de KB 08–10 siguen pendientes salvo decisiones posteriores explícitas. Este diseño presenta AT-01 y el contrato inicial de AT-02/AT-08/AT-10. **Revisión HIGH D-01 a D-05: aprobada por el usuario el 2026-11-22 mediante «si continuemos». Implementación y comprobaciones de C-01 completadas: builds, 18 pruebas unitarias, conexión real a PostgreSQL 17.12, revisión independiente y reproducción final propia autorizada en D-08. Las excepciones del usuario y límites de aceptación quedan en verification.md.**

## Goals / Non-Goals

**Goals:**

- Separar la compilación, que no necesita base disponible, de las comprobaciones de arranque/conectividad.
- Proporcionar a los cinco integrantes un contrato común de herramientas y configuración, con archivos de bloqueo y sin instalaciones globales obligatorias.
- Preparar el límite de persistencia sin adelantarse al esquema de C-02 ni ejecutar migraciones sobre bases existentes.

**Non-Goals:**

- Resolver autenticación, credenciales de autogestión, correo, concurrencia de reservas o reglas temporales del dominio.
- Implementar pantallas funcionales, endpoints de negocio o datos de ejemplo. La entrada React solo permitirá comprobar la compilación, sin diseñar una pantalla del producto.
- Garantizar el empaquetado integrado, los flujos completos o la aceptación de C-16; elegir una zona del consultorio por inferencia; instalar servicios del sistema.

## Decisions

### D-01 — Matriz de versiones propuesta (AT-01)

Matriz aprobada por el usuario el 2026-11-22 como parte de D-01 a D-05, obtenida de metadatos oficiales en esa fecha. No se utilizan versiones preliminares. Las bibliotecas Java administradas por el BOM de Spring Boot se fijarán por ese BOM y el bloqueo de Gradle, sin agregar versiones arbitrarias.

| Herramienta | Versión propuesta | Motivo / alternativa considerada |
|---|---|---|
| Eclipse Temurin JDK | `21.0.13.1+2` | Mantener Java 21 aprobado; no sustituirlo por los JDK 17/25/26 presentes. |
| Spring Boot | `4.2.0` | Versión estable publicada por Initializr; `4.1.3` es alternativa si aparece una incompatibilidad reproducible. No bajar de versión silenciosamente. |
| Gradle Wrapper | `9.9.2` | Dentro de la familia 9.x soportada por Boot; distribución con SHA-256 publicado. |
| Node.js | `24.22.3` LTS | Línea con soporte; Node 20 instalado ya figura EOL en el calendario oficial. |
| npm | `12.2.3` | Compatible con la línea Node elegida; ejecución local con versión explícita, sin `npm install -g`. |
| React / React DOM | `19.3.1` / `19.3.1` | Pareja alineada según sus metadatos de dependencias. |
| TypeScript | `7.0.2` | Versión estable del registro oficial; configuración estricta. |
| Vite / plugin React | `8.3.8` / `6.3.0` | El plugin declara compatibilidad con Vite 8; ambos admiten Node 24. |
| PostgreSQL | `17.12` | Rama soportada hasta 2029; continuar con la familia del cliente existente sin imponer el recién publicado PostgreSQL 19. |

Las versiones auxiliares, como tipos de React y Node, se fijarán exactamente al resolver la instalación inicial y quedarán en el bloqueo. Si la resolución o el build revela incompatibilidad, se presenta una revisión de esta matriz y se detiene la parte afectada; no se registra una sustitución como aprobada por defecto.

### D-02 — Organización y construcción

Backend independiente en `backend/`, paquete Java propuesto `dev.odontologia`, Java toolchain 21, Gradle Kotlin DSL y BOM nativo de Gradle. Kotlin DSL aporta comprobación del script; Groovy DSL también sería válido, pero se elige un único formato común. El lenguaje de la aplicación sigue siendo Java.

Frontend independiente en `frontend/`, plantilla React/TypeScript de Vite reducida a lo necesario para compilar, sin marca, biblioteca visual, calendario ni enrutador añadidos. Se activa TypeScript estricto y se conserva la prohibición de `any` manual.

```text
backend/
  build.gradle.kts
  settings.gradle.kts
  gradlew / gradlew.bat
  gradle/wrapper/
  gradle.lockfile
  src/main/java/dev/odontologia/
  src/main/resources/application.properties
  src/test/java/dev/odontologia/
frontend/
  package.json
  package-lock.json
  vite.config.ts
  tsconfig*.json
  index.html
  src/
```

Los módulos funcionales se materializan cuando incorporen código en los changes posteriores; no se crean clases vacías de agenda, pacientes, acceso o notificaciones. Los documentos OpenSpec permanecen separados del código fuente.

Comandos implementados; ver [README](../../../../README.md) para preparación y
[comprobaciones](verification.md) para ejecuciones y pendientes:

| Directorio | Comando previsto | Resultado |
|---|---|---|
| `backend/` | `./gradlew bootJar` | JAR de Spring Boot sin necesitar PostgreSQL. |
| `backend/` | `./gradlew test` | Comprobaciones de configuración que no necesitan base. |
| `backend/` | `./gradlew integrationTest` | Arranque y conexión real de solo lectura; exige entorno de prueba. |
| `backend/` | `./gradlew bootRun` | Backend local con la configuración requerida. |
| `frontend/` | `npx --yes npm@12.2.3 ci` | Instalar respetando `package-lock.json`. |
| `frontend/` | `npx --yes npm@12.2.3 run typecheck` | Comprobar tipos estrictos. |
| `frontend/` | `npx --yes npm@12.2.3 run build` | Tipos y build estático en `dist/`. |
| `frontend/` | `npx --yes npm@12.2.3 run dev` | Vite local con proxy. |

El Wrapper y `distributionSha256Sum` se versionan; las configuraciones resolubles del backend se bloquean para cubrir build y pruebas. Cambiar bloqueos requiere una actualización deliberada. Las herramientas faltantes podrán obtenerse como distribuciones portables en `.tools/` ignorado, comprobando sus checksums oficiales, sin editar perfiles ni reemplazar el Java/Node global. PostgreSQL requiere una instancia de prueba habilitada: C-01 no instala ni modifica un servicio del sistema.

### D-03 — Spring JDBC y preparación de Flyway (AT-02)

Se propone Spring JDBC con SQL explícito, no JPA, por el control de lecturas y transacciones que necesitarán las reglas de agenda. Se acepta más mapeo manual; la ventaja de JPA sería reducirlo, a cambio de introducir el ciclo de persistencia de un ORM.

Flyway permitirá migraciones SQL versionadas en C-02. En C-01 se prepara su dependencia, incluyendo soporte PostgreSQL, pero se configura **deshabilitado**: no debe crear siquiera su tabla de historial. También se deshabilita la inicialización por `schema.sql`/`data.sql`. La comprobación ejecuta solo `SELECT 1`; no hay migraciones vacías, tablas auxiliares, datos ficticios ni seeds.

La propuesta usa Spring Web MVC, el starter Flyway que ya incorpora JDBC, driver PostgreSQL y módulo PostgreSQL de Flyway, más soporte de pruebas Spring/JUnit administrado por Boot. No se duplica un starter JDBC ya transitivo ni se añade H2, JPA, Spring Security, Actuator o SMTP por conveniencia. C-02 revisará la activación de migraciones y su esquema; el protocolo concurrente AT-05 sigue sin decidirse.

### D-04 — Contrato local y configuración (AT-08)

Estos son los valores y comportamientos concretos sometidos a revisión HIGH:

| Elemento | Propuesta |
|---|---|
| Backend | `SERVER_ADDRESS=127.0.0.1`, `SERVER_PORT=8080`; fallar si el puerto está ocupado. |
| Vite | `127.0.0.1:5173`, `strictPort=true`; proxy `/api` a `http://127.0.0.1:8080`, sin reescritura de ruta. |
| URL pública de desarrollo | `APP_BASE_URL=http://127.0.0.1:5173`; navegación a través de Vite. |
| URL prevista de demostración | `http://127.0.0.1:8080`; se validará al integrar el frontend en C-16, sin probarlo como cumplido en C-01. |
| PostgreSQL de prueba | Con Compose autorizado posteriormente: `jdbc:postgresql://127.0.0.1:5433/odontologia_test`. Se conserva configuración explícita para otra instancia habilitada. Ver D-07. |
| Identidad de base | `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` obligatorias, aportadas localmente; sin valores reales en el repositorio. |
| Zona | `APP_TIME_ZONE` obligatoria y validada como IANA al arrancar; sin valor por defecto ni elección desde la máquina. Para pruebas aisladas se usa `Etc/UTC`, identificado como fixture y sin persistirlo. PB-02 se resolverá en C-02. |
| Tiempo de conexión | Conexión JDBC y espera del pool limitadas a 5 segundos; lectura de socket también limitada a 5 segundos. La comprobación no admite espera indefinida. |

La URL base se valida y se configura según el modo de ejecución; no genera enlaces en C-01. Las propiedades `APP_…` se mapearán expresamente y se comprobarán al iniciar; no son propiedades de autoconfiguración de Spring Boot. Credenciales, URL y zona ausentes deben provocar un error de configuración identificable sin imprimir sus valores sensibles.

La plantilla de entorno contendrá marcadores y comentarios, sin credenciales reales. La documentación explicará cómo exportar variables en la sesión de trabajo; un `.env` no se considerará cargado por Spring automáticamente. `.gitignore` excluirá configuración local sensible, `.tools/`, cachés y artefactos generados. No se ejecutarán cargas de variables desde archivos arbitrarios ni se añadirá una librería dotenv.

Para comprobar el proxy sin publicar un endpoint, se solicitará una ruta API inexistente y se verificará que conserva el 404 del backend; un 404 controlado es un resultado esperado de esa comprobación, no una API de negocio implementada. Con el backend detenido se comprobará el fallo de conexión, sin fallback HTML exitoso.

### D-05 — Comprobaciones y evidencia prevista (AT-10)

Las especificaciones son [reproducible-builds](specs/reproducible-builds/spec.md), RB-01 a RB-04, y [local-runtime](specs/local-runtime/spec.md), LR-01 a LR-06. La implementación deberá asociar cada requisito a sus comprobaciones; no se propone un porcentaje de cobertura inexistente.

| Grupo | Comprobación prevista |
|---|---|
| RB-01/RB-02 | Versiones efectivas, checksums de herramientas y Wrapper, resolución bloqueada e instalación limpia sin modificar bloqueos; rechazo de herramientas incompatibles y bloqueo frontend inconsistente. |
| RB-03/RB-04 | Build backend/frontend sin base disponible, tipos estrictos, fallo de tipos controlado en una copia temporal y reproducción de instrucciones por otro integrante. |
| LR-01/LR-04 | Arranque loopback y puertos exactos, fallo por puerto ocupado, proxy a ruta inexistente y fallo observable cuando el backend está detenido. |
| LR-02/LR-03 | Arranque con PostgreSQL de prueba y `SELECT 1`; rechazo de conexión inválida; comparación de esquema y datos antes/después de comprobaciones y reinicios sin migraciones. |
| LR-05/LR-06 | Rechazo por configuración ausente o zona inválida, plantilla sin secretos y fixture temporal explícito sin persistencia de configuración del consultorio. |

JUnit/Spring cubre arranque y configuración del backend; scripts/comandos documentados comprueban builds y proxy. No se agrega un runner frontend para probar una interfaz que aún no existe: Vitest y recorridos Playwright se revisarán cuando haya comportamiento frontend. La propuesta inicial no exigía Docker/Testcontainers. El usuario autorizó después el camino Docker de D-07; no se agrega Testcontainers y los builds siguen sin necesitar Docker. La alternativa de una instancia PostgreSQL propia habilitada se conserva.

El flujo SDD instalado exige un contrato completo de evidencia y corroboración independiente para aceptar ejecuciones. La ronda inicial de `propose` solo produjo planificación. En la reanudación, el usuario autorizó la excepción descrita en D-06: las comprobaciones normales, su alcance y pendientes quedan en [verification.md](verification.md), sin afirmar cumplimiento del contrato omitido.

### D-06 — Aprobación y forma de trabajo

**Excepción posterior, autorizada por el usuario el 2026-11-22:** mediante «te puedo pedir que lo esquives y sigamos como puedas?» se omite para C-01 el contrato incompleto de evidencia SDD que bloqueaba el control 1.2. Se reanuda la implementación aprobada y se ejecutan comprobaciones normales, con comandos, resultados y límites documentados. No se inventan recibos ni se declara cumplimiento del contrato omitido. La excepción no modifica la skill global ni se extiende a otros changes. Se mantienen los requisitos funcionales, la aprobación HIGH, la prohibición de cambios CRITICAL y el uso de bases habilitadas. Las revisiones independientes disponibles se conservan sin exigir el formato ausente.

**Aprobación recibida:** el usuario aprobó D-01 a D-05 el 2026-11-22 mediante «si continuemos». Incluye versiones, estructura, Kotlin DSL, JDBC/Flyway inicialmente deshabilitado, contrato local y comprobaciones propuestas. No se requiere repetir esta aprobación para el mismo alcance. Esta autorización no sustituye las condiciones de evidencia de la skill ni habilita cambios CRITICAL, servicios globales o uso de una base existente sin habilitación.

La aprobación HIGH debe identificar esta revisión de D-01 a D-05 antes de escribir sus archivos ejecutables. El usuario puede revisar o cambiar JDBC, Flyway, versiones y contrato local; si cambia una decisión, se ajustan los artefactos afectados antes de implementar. Esta propuesta no aprueba PT completas, no cierra pendientes por inferencia y no autoriza código CRITICAL.

**Control previo de implementación, 2026-11-22:** se consultaron las instrucciones `apply` del CLI y la plantilla oficial `openspec-apply-change` embebida en OpenSpec 2.0.0 (`dist/core/templates/workflows/apply-change.js`), sin instalar nuevas skills. Los módulos obligatorios `artifacts` y `bounded-command-output` están disponibles. La skill SDD instalada declara expresamente incompleto el contrato de evidencia: faltan el módulo literal, el mecanismo de identificación del árbol probado y los acuerdos completos de captura. No se encontró un módulo que resuelva ese contrato en las rutas locales de skills inspeccionadas. No se inventa un sustituto. La tarea 1.2 permanece pendiente y se detiene `apply` en ese control previo; no se ejecutaron builds ni pruebas, no se crearon backend/frontend ni se conectó a PostgreSQL. La aprobación HIGH queda vigente. Para reanudar la acción afectada debe estar disponible el contrato completo y la corroboración independiente que exige la skill, o una protección equivalente verificada conforme a sus reglas. No se modifican las instrucciones globales desde este change.

Registro de reanudación: `{action: apply, step: 3, signal: aprobación explícita D-01 a D-05 y lectura del contexto, direct}` para registrar la autorización; `{action: apply, step: 1, signal: contrato completo de evidencia no disponible en el control previo 1.2, stopped}` para la implementación. No se solicitó revisión independiente ni se declaró verificación de ejecución.

**Resultado posterior a la excepción:** implementación directa por depender del diseño compartido y requerir lectura completa; verificación independiente por `review_c01`, sin escrituras. Registros: `{action: apply, step: 3, signal: decisiones aprobadas y excepción explícita del usuario, direct}` y `{action: verify, step: 1, signal: revisión independiente requerida, delegated}`. Ambos builds y 18 pruebas unitarias pasaron; se reprodujeron desde copias limpias y se corrigió el hallazgo de caché de integración. Los resultados parciales y pendientes reales están en [verification.md](verification.md). La revisión independiente ordinaria se realizó bajo la excepción, sin afirmar cumplimiento del contrato de evidencia SDD omitido. No se archiva ni se marca C-01 en el roadmap hasta completar sus comprobaciones restantes.

Registro de la ronda: `{action: propose, step: 3, signal: decisiones de la conversación y lectura íntegra del resultado, direct}`. No se delega porque el constructor necesita revisar los documentos completos y mantener las decisiones compartidas; no hay asignaciones de archivos en conflicto. La revisión independiente de ejecuciones, cuando corresponda, deberá hacerse con otro agente o integrante.

### D-07 — PostgreSQL Docker autorizado y comandos de recuperación

El 2026-11-22 el usuario indicó «levantemos docker y deja aclarado que si no se encuentra la base de datos corriendo deja los comandos para iniciarlo». Autoriza crear una instancia propia, separada del cluster y contenedores existentes, y documentar su inicio cuando esté detenida. Se deja funcionando para uso local, con volumen persistente; no es una instancia descartada al terminar la prueba.

- `compose.yaml`: proyecto `odontologia-local`, servicio `db`, imagen oficial `postgres:17.12` fijada al digest descargado, puerto `127.0.0.1:5433` y volumen propio en `/var/lib/postgresql/data`. No se modifica el cluster local que utiliza 5432.
- `scripts/init-local-env.py`: genera una contraseña aleatoria en `.env`, ignorado por Git, con permisos 0600 y creación exclusiva; nunca sobrescribe un archivo existente ni imprime el secreto.
- `scripts/with-local-db.py`: consulta la configuración resuelta de Compose en memoria, sin mostrarla ni ejecutar `.env` como código; comprueba disponibilidad y exporta conexión solo al comando hijo. Cuando la base está detenida muestra `python3 scripts/init-local-env.py` y `docker compose up -d --wait --wait-timeout 60 db`. Si Docker no está disponible, indica cómo iniciar el daemon o Docker Desktop.
- `.env.example` y README documentan requisitos y comandos, incluido `docker compose stop db` y el reinicio sin borrar el volumen. Docker es el camino local autorizado; no se requiere para compilar ni para usar otra instancia configurada explícitamente.

La inicialización del contenedor crea únicamente la base y catálogos PostgreSQL de prueba. La aplicación sigue sin crear tablas, ejecutar migraciones, poblar datos ni persistir la zona del consultorio. Las comprobaciones usan la base recién inicializada sin tablas de usuario; su conservación no demuestra todavía persistencia de datos del dominio.

### D-08 — Reproducción propia autorizada para el cierre

El 2026-11-22 el usuario indicó «salteemos la reproduccion por alguien mas, hacela vos y esta todo bien sigamos». Para C-01, las tareas 5.3 y 6.2 se satisfacen mediante reproducción del agente desde copias limpias, sin requerir una persona adicional. No se atribuye esta ejecución a otro integrante. Se conservan las revisiones independientes ya realizadas por `review_c01`; esta excepción no cambia reglas globales ni se extiende a otros changes. Si las comprobaciones pasan, se completa el seguimiento y se archiva con el CLI, sincronizando las especificaciones y actualizando el roadmap después del archivo.

## Risks / Trade-offs

- [Java 21 y Node acordado no están activos] → Usar distribuciones portables con selección explícita; no reemplazar instalaciones globales.
- [Base local detenida] → Docker fue habilitado y probado. Consultar `docker compose ps db` y ejecutar los comandos de inicio documentados; no borrar volúmenes ni modificar bases ajenas para resolver un fallo.
- [Versiones estables recién publicadas o resolución incompatible] → Ejecutar los builds tras la revisión; detener y proponer ajuste si hay incompatibilidad. La consulta de metadatos no prueba compatibilidad de la aplicación.
- [Flyway inicializa metadatos por defecto] → Mantenerlo deshabilitado en C-01 y comprobar ausencia de cambios; su activación exige el diseño de C-02.
- [Versiones o URL divergen entre integrantes] → Wrapper, bloqueos y una tabla de entorno; puertos estrictos y configuración validada.
- [Una comprobación superficial se confunde con aceptación del producto] → Registrar alcance y requisito; reservar concurrencia de turnos, correo real, frontend integrado y persistencia del dominio para sus changes.
- [El contrato completo de evidencia del coordinador no está disponible] → No afirmar verificación; resolver ese requisito antes de aceptar ejecuciones según la skill.

## Migration Plan

1. Revisar D-01 a D-05 y registrar aprobación HIGH explícita antes de `apply`; mantener las tareas de implementación sin marcar hasta entonces.
2. Incorporar estructura, manifiestos, bloqueos, configuración y documentación únicamente dentro del repositorio; obtener herramientas portables si son necesarias y están autorizadas por esa revisión.
3. Compilar sin base; después, con la instancia de prueba habilitada, ejecutar las comprobaciones no destructivas y de configuración.
4. Obtener la revisión/evidencia exigidas por el flujo. Archivar mediante CLI solo al cumplir aceptación y aprobación; recién entonces actualizar C-01 en el roadmap.
5. Ante fallo, revertir únicamente los archivos de C-01 después de revisar el diff; no borrar datos ni revertir otros cambios del equipo. Al no ejecutar DDL/DML en C-01, no se necesita rollback de esquema.

## UI Mockups

- Decision: no (no UI).
- Fecha: 2026-11-22.
- Alcance: infraestructura de compilación y ejecución; no se implementan pantallas del producto. La política visual general `off` se conserva.

## Sources

Consultadas el 2026-11-22; respaldan compatibilidad y opciones técnicas, no constituyen aprobación del proyecto:

- [Spring Initializr: metadatos de versiones estables](https://start.spring.io/metadata/client).
- [Spring Boot: requisitos de Java y Gradle](https://docs.spring.io/spring-boot/system-requirements.html).
- [Gradle: versión y checksum publicados](https://services.gradle.org/versions/current).
- [Adoptium: distribución Temurin 21](https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse).
- [Node.js: calendario de soporte](https://nodejs.org/en/about/previous-releases) y [checksums de Node 24](https://nodejs.org/dist/latest-v24.x/SHASUMS256.txt).
- [npm: React](https://registry.npmjs.org/react/19.3.1), [React DOM](https://registry.npmjs.org/react-dom/19.3.1), [TypeScript](https://registry.npmjs.org/typescript/7.0.2), [Vite](https://registry.npmjs.org/vite/8.3.8), [plugin React](https://registry.npmjs.org/@vitejs/plugin-react/6.3.0) y [npm](https://registry.npmjs.org/npm/12.2.3).
- [Vite: requisitos del entorno](https://vite.dev/guide/).
- [PostgreSQL: versiones soportadas](https://www.postgresql.org/support/versioning/).
- [Spring Boot: inicialización y Flyway](https://docs.spring.io/spring-boot/how-to/data-initialization.html).
- [Spring Boot: gestión de dependencias con Gradle](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html).
