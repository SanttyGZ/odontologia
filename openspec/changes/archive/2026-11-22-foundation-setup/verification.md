# Comprobaciones de C-01

Fecha: 2026-11-22. Entorno: Linux x64. Estado: implementación y comprobaciones
completadas para C-01 con las excepciones explícitas D-06 y D-08 del usuario.

Archivo realizado por `openspec archive foundation-setup --yes --json` el
2026-11-22: exit 0, 21/21 tareas completas y 10 requisitos consolidados en las
especificaciones principales. El CLI creó
`openspec/changes/archive/2026-11-22-foundation-setup/`; se comprobaron propósitos
y requisitos de las dos especificaciones consolidadas, sin diferencias respecto
de sus deltas. No se omitió la sincronización ni la validación del archivo.

## Condiciones

El usuario aprobó D-01 a D-05 y autorizó omitir para este change el contrato
incompleto de evidencia SDD. Este informe describe ejecuciones observadas;
no constituye un recibo de ese contrato ni afirma cumplirlo. Se conservaron
las pruebas normales y una revisión por un agente distinto del constructor.

No se modificaron Java/Node globales, perfiles personales, el servicio PostgreSQL
ni bases existentes. Las herramientas portables y cachés están en `.tools/`,
ignorado por Git. Los servidores de aplicación de prueba se detuvieron al terminar
cada recorrido. Posteriormente el usuario autorizó PostgreSQL en Docker y pidió
comandos de inicio: esa base propia queda funcionando con volumen persistente.

## Herramientas y bloqueos

Versiones efectivas: Temurin `21.0.13.1+2`, Node `24.22.3`, npm `12.2.3`,
Gradle `9.9.2`. Dependencias del frontend y BOM Boot `4.2.0` resueltos con los
manifiestos aprobados. Versiones auxiliares: tipos React/DOM `19.3.0` y Node
`24.19.2`; fijadas exactamente en `package.json` y `package-lock.json`.

Se compararon los SHA-256 con las fuentes oficiales:

| Archivo | SHA-256 |
|---|---|
| Temurin Linux x64 | `bb8a93f9ae6e7a693b2b721e85dbeff26ae1a77447985a8d7afcdd9c7cf6222e` |
| Node Linux x64 | `4f2b175a6d53a5c617a80dba2676238a0688cef28c6da4eba35c7d844e66fcfb` |
| Distribución Gradle | `971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b` |
| Gradle Wrapper JAR | `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8` |

Fuentes: [Adoptium](https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=linux),
[Node](https://nodejs.org/dist/v24.22.3/SHASUMS256.txt),
[Gradle](https://services.gradle.org/versions/current).

## Resultados por requisito

| Requisito | Resultado observado | Límite o pendiente |
|---|---|---|
| RB-01 | Versiones efectivas y checksums correctos; Java 17 rechazado con error explícito; Node 20/npm 11 rechazados con `EBADENGINE`. | Otros sistemas operativos no ejecutados. |
| RB-02 | Wrapper generado oficialmente; bloqueo de configuraciones Gradle; `npm ci` limpio y builds desde copias temporales conservaron sus bloqueos. Un manifiesto frontend inconsistente fue rechazado. | Revisión independiente descrita abajo. |
| RB-03 | Backend produce JAR, frontend produce `dist/`, sin PostgreSQL disponible. 18 pruebas unitarias pasaron; clases de integración compilan por separado. Error TypeScript controlado hace fallar `typecheck` y `build`. | No acredita funciones de negocio. |
| RB-04 | README con directorios, herramientas, comandos, variables y resultados. Reproducción final limpia realizada por el agente y revisión independiente previa por otro agente. | El usuario reemplazó la reproducción humana adicional por reproducción propia, D-08. |
| LR-01 | JAR arranca en loopback:8080 y Vite en loopback:5173; otra instancia falla ante cada puerto ocupado. | No se verificó empaquetado integrado de C-16. |
| LR-02 | Conexión positiva, repetida y tras reinicio a PostgreSQL 17.12; `SELECT 1` en solo lectura. Base inaccesible y contraseña inválida terminan con fallo; sin fallback en memoria. | Alcance de conectividad, sin operaciones de negocio. |
| LR-03 | Flyway deshabilitado, inicialización SQL `never`; dumps de esquema y datos iguales antes y después de conexiones y reinicio del contenedor, sin tablas de usuario ni historial Flyway. | Base recién inicializada; no acredita conservación de pacientes o turnos todavía inexistentes. |
| LR-04 | `/api/c01-probe` conserva estado 404 y ruta del backend; al detenerlo el proxy responde 502. | Sin endpoints de negocio. |
| LR-05 | Arranque real rechaza cada una de las cinco variables obligatorias ausentes. Contraseña inválida contra PostgreSQL accesible provoca fallo de autenticación. `.env` generado sin mostrar secretos, ignorado e inalterado al repetir inicialización. | Las credenciales son de desarrollo local. |
| LR-06 | `Etc/UTC` se usa solo como fixture explícito; `Mars/Base` y offsets no IANA son rechazados. La base sigue sin tablas ni configuración persistida del consultorio después de reiniciar. | Zona de consultorio PB-02 pendiente para C-02. |

## Comandos y escenarios ejecutados

- Backend: `./gradlew test bootJar integrationTestClasses --no-daemon`, exit 0;
  XML JUnit: 18 tests, 0 failures, 0 errors, 0 skipped.
- Backend desde copia temporal sin `build/` ni `.gradle/`:
  `./gradlew test bootJar --no-daemon`, exit 0; bloqueo sin cambios.
- Frontend desde copia temporal sin `node_modules/` ni `dist/`:
  `npx --yes npm@12.2.3 ci --no-audit --no-fund` y `run build`, exit 0;
  bloqueo sin cambios.
- En esa copia: asignar un string a una variable `number` produjo TS2322 y
  exit 1 tanto en `run typecheck` como en `run build`.
- Cambiar React en el manifiesto sin actualizar el bloqueo produjo `EUSAGE`
  y exit 1 en `ci`; no se regeneró el bloqueo automáticamente.
- `./gradlew help` con Java 17 terminó con exit 1 indicando Java 21 requerido.
  Instalación con Node 20/npm 11 terminó con exit 1, `EBADENGINE`.
- Arranques reales del JAR: faltantes individuales de URL base, zona, URL JDBC,
  usuario y contraseña; zona inválida; todos terminaron con exit 1 identificable.
- JAR y Vite simultáneos: comparación del 404 directo y por proxy; segunda
  instancia en puertos ocupados; backend detenido con respuesta 502 del proxy.
- `./gradlew bootRun --no-daemon` también arrancó en loopback:8080 y permitió
  comprobar la ruta por proxy. El script temporal comprobó desconexión al terminar
  Gradle, antes de concluir el proceso Java hijo, y falló esa aserción. Se comprobó
  ausencia de listeners tras la limpieza. El caso de desconexión aprobado
  corresponde al JAR directo; esa ejecución parcial del script no se cuenta como exitosa.
- `./gradlew integrationTest --no-daemon`, con fixture JDBC en un puerto
  inaccesible: exit 1 esperado, `PSQLException` por conexión rechazada. Es una
  prueba negativa, no un test positivo de PostgreSQL. No se usa como aprobación.

Los logs locales de esta ronda están en `/tmp/odontologia-c01-logs/`, con rutas
personales y contraseña de fixture omitidas; son temporales y no se versionan.
La persistencia de este informe no garantiza que esos archivos sigan disponibles.

Validación documental final: `openspec validate foundation-setup --type change
--strict --json --no-interactive`, exit 0, `valid: true`, sin issues. Esta
validación comprueba los artefactos OpenSpec, no sustituye las pruebas pendientes.

## Revisión independiente

El agente `review_c01`, distinto del constructor, revisó código, configuración y
documentación y reprodujo desde una copia temporal limpia:

- `./gradlew test bootJar integrationTestClasses`: exit 0, 18 pruebas sin fallos,
  errores ni omisiones; JAR generado.
- npm 12.2.3 `ci` y `build`: exit 0; archivos estáticos generados.
- Bloqueos de ambos componentes sin cambios.
- Integración con base inaccesible: exit 1 esperado, 15,62 segundos.

Su hallazgo fue impedir reutilización de `integrationTest`: el estado de una base
externa cambia sin cambiar el código. Se corrigió con
`outputs.upToDateWhen { false }` y exclusión de build cache. Revisó la corrección,
ejecutó el build actualizado y confirmó las guardas con `--info`. No quedaron
hallazgos materiales abiertos. Logs y `summary.json` en
`/tmp/odontologia-c01-review/`, temporales y separados de los del constructor.

## PostgreSQL Docker habilitado

Autorización posterior del usuario el 2026-11-22, registrada en D-07. Docker local
ya estaba funcionando; no se modificó su configuración ni se iniciaron los
contenedores antiguos. Compose creó `odontologia-local-db-1`, red y volumen propios.
Imagen oficial `postgres:17.12` fijada por digest
`sha256:223efeb196329b55b3b9a13db144956411dbc18e727e59f681c3e0aede6475cf`.
`SHOW server_version` devolvió `17.12 (Debian 17.12-1.pgdg13+1)`.

Comprobaciones ejecutadas por el constructor:

- `docker compose config --quiet`: exit 0, sin imprimir la configuración resuelta.
- `docker compose up -d --wait --wait-timeout 60 db`: exit 0, servicio healthy en
  `127.0.0.1:5433`; volumen `odontologia-local_postgres-data`.
- `python3 scripts/with-local-db.py ./backend/gradlew -p backend integrationTest
  --no-daemon`: positivo inicial y repetido, exit 0; 1 test sin fallos.
- La misma integración con una contraseña incorrecta, manteniendo servidor y URL
  válidos, terminó con exit 1 en 15,28 segundos. El servidor registró
  `password authentication failed`; el secreto real se omitió de los logs.
- `docker compose stop db`, seguido del helper: este terminó con exit 1 en
  0,33 segundos e indicó los comandos para iniciar la base; no intentó crearla
  automáticamente.
- El comando de inicio documentado volvió a dejar el contenedor healthy. La
  integración posterior al reinicio terminó con exit 0 en 10,59 segundos.
- Repetir `python3 scripts/init-local-env.py` conservó exactamente el `.env` existente.
- Dumps `pg_dump --schema-only` y `--data-only` antes y después de la secuencia,
  sin propietarios ni privilegios, coincidieron. Se eliminaron únicamente las
  líneas de tokens aleatorios `\\restrict`/`\\unrestrict` antes de comparar.
  Se consultó el catálogo de relaciones: cero tablas o relaciones de usuario,
  incluido ningún `flyway_schema_history`. La prueba no creó datos del dominio.

Logs de esta ampliación: `/tmp/odontologia-c01-docker-logs/`, temporales. Se deja
PostgreSQL corriendo por pedido del usuario; README documenta cómo detenerlo y
volver a iniciarlo sin borrar datos. Los scripts no muestran ni versionan `.env`.

Corroboración independiente de la ampliación por `review_c01`:

- Integración real desde copia limpia del backend mediante el helper: exit 0 en
  11,4 segundos; 1 prueba, sin fallos, errores ni omisiones.
- Consulta propia de solo lectura: PostgreSQL 17.12, cero tablas de usuario y
  ausencia de `public.flyway_schema_history`.
- Pruebas aisladas de los scripts: permisos 0600 y ausencia de sobrescritura;
  Docker/configuración/base detenidos producen error con comandos de inicio;
  secreto de fixture ausente en mensajes; código de salida del hijo propagado.
- Sin hallazgos materiales abiertos ni escrituras del revisor en el proyecto.
  Resúmenes en `/tmp/odontologia-c01-review/docker-*.json`.

La corroboración del revisor no se atribuye las comparaciones de dumps ni los
reinicios: esos casos fueron ejecutados por el constructor como se detalla arriba.

## Reproducción final propia autorizada

El usuario indicó «salteemos la reproduccion por alguien mas, hacela vos y esta
todo bien sigamos». D-08 registra la excepción para C-01; no se atribuye esta
reproducción a otro integrante.

Se copiaron backend, frontend, scripts y archivos de configuración a un directorio
temporal sin `build/`, `.gradle/`, `node_modules/` ni `dist/`. Se reutilizaron las
distribuciones y cachés de herramientas fijadas. La contraseña local de Compose
se pasó en memoria al entorno, sin copiar `.env` ni mostrar el valor.

- PostgreSQL se detuvo durante las compilaciones y se volvió a iniciar después,
  con el mismo volumen y comandos documentados; ambos comandos exit 0.
- Backend limpio: `./gradlew test bootJar --no-daemon`, exit 0; 18 pruebas, sin
  fallos, errores ni omisiones; JAR generado.
- Frontend limpio: npm 12.2.3 `ci`, `run typecheck` y `run build`, todos exit 0;
  archivos estáticos generados.
- Desde la copia limpia, el helper documentado ejecutó `integrationTest` contra
  PostgreSQL real, exit 0; 1 prueba sin fallos, errores ni omisiones.
- Ambos bloqueos de dependencias conservaron exactamente su contenido.

Logs y resumen: `/tmp/odontologia-c01-final-logs/`, temporales. Esta reproducción
propia cumple la excepción D-08; las revisiones independientes anteriores se
mantienen como ejecuciones separadas. PostgreSQL queda funcionando. No quedan
pendientes técnicos de C-01. Archivo y actualización del índice completados
conforme al workflow, sin atribuir recepción de correo, persistencia del dominio
o aceptación del producto completo.

No se comprobaron recepción de correo, concurrencia de reservas, pantallas,
integración de C-16 ni restauración de respaldos.
