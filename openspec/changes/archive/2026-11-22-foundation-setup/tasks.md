# Tasks

Responsable principal: Fernando. Implementación y comprobaciones completadas para el alcance de C-01, con las excepciones explícitas D-06 y D-08 del usuario. Referencias: [diseño](design.md), [resultados](verification.md), [RB-01 a RB-04](specs/reproducible-builds/spec.md) y [LR-01 a LR-06](specs/local-runtime/spec.md).

## 1. Revisión y condiciones de ejecución

- [x] 1.1 Obtener y registrar aprobación HIGH explícita de D-01 a D-05 del diseño, incluyendo versiones, Kotlin DSL, JDBC/Flyway, puertos, variables, distribuciones portables y comprobaciones; verificar que la confirmación identifica esta revisión antes de escribir configuración ejecutable. Aprobado por el usuario el 2026-11-22 mediante «si continuemos»; ver D-06.
- [x] 1.2 Comprobar los módulos y controles exigidos para `apply`. Módulos obligatorios disponibles; contrato de evidencia incompleto omitido por excepción explícita del usuario el 2026-11-22. Continuar con comprobaciones normales y revisiones independientes disponibles, sin inventar recibos ni declarar cumplimiento del contrato omitido; ver D-06.

  Control previo del 2026-11-22: módulos `artifacts` y `bounded-command-output` disponibles; contrato completo de evidencia no disponible. La detención inicial se levantó con la excepción posterior del usuario. Ver D-06 y [verification.md](verification.md) para builds, pruebas y revisión independiente ordinaria, sin afirmar cumplimiento del contrato omitido.

- [x] 1.3 Confirmar una instancia PostgreSQL 17.12 de prueba habilitada y su conexión local, sin pedir secretos en el chat ni modificar servicios existentes; verificar versión del servidor y permiso de uso mediante el canal local acordado. Docker autorizado por el usuario; servidor 17.12 verificado en loopback:5433. Ver D-07 y [verification.md](verification.md).

## 2. Herramientas y backend reproducible

- [x] 2.1 Seleccionar Temurin 21.0.13.1+2 y Node 24.22.3; obtener distribuciones portables bajo `.tools/` solo si hace falta, comprobar sus checksums oficiales y documentar la selección local; verificar versiones efectivas y ausencia de cambios en herramientas globales o perfiles personales (RB-01).
- [x] 2.2 Crear el backend Java en `dev.odontologia` con Spring Boot 4.2.0, Gradle Kotlin DSL, BOM nativo y Wrapper 9.9.2 con `distributionSha256Sum`; verificar la distribución y que `./gradlew bootJar` produce un JAR sin PostgreSQL en ejecución (RB-01/RB-03).
- [x] 2.3 Añadir Web MVC, Flyway/JDBC, soporte PostgreSQL y pruebas administradas por el BOM; separar `test` de `integrationTest`, habilitar bloqueos de las configuraciones resolubles y generar el bloqueo inicial; verificar que la resolución posterior conserva las versiones y no modifica el bloqueo (RB-02/RB-03).
- [x] 2.4 Documentar requisitos, directorio de trabajo y comandos del backend, incluido el rechazo de Java incompatible; verificar que los comandos documentados coinciden con los ejecutados y con sus salidas observables (RB-01/RB-04).

## 3. Frontend reproducible

- [x] 3.1 Cargar la skill React local antes de escribir componentes y crear la entrada mínima de compilación con React/React DOM 19.3.1, TypeScript 7.0.2, Vite 8.3.8 y plugin React 6.3.0; activar modo estricto sin `any` manual y sin pantalla funcional; verificar tipos y build estático (RB-01/RB-03).
- [x] 3.2 Fijar versiones directas auxiliares compatibles, registrar npm 12.2.3 y Node requerido, generar `package-lock.json` y documentar instalación mediante la versión explícita de npm; verificar que `ci` desde instalación limpia conserva el bloqueo y rechaza un manifiesto inconsistente en una copia temporal (RB-01/RB-02).
- [x] 3.3 Añadir comandos `typecheck`, `build` y `dev`; documentar directorios y resultados esperados; verificar build sin base disponible y que un error de tipos controlado en una copia temporal hace fallar el comando antes de producir un resultado presentado como exitoso (RB-03/RB-04).

## 4. Contrato de configuración local

- [x] 4.1 Configurar el backend en loopback:8080 y mapear/validar URL base, zona IANA y variables de base sin valores sensibles predeterminados; añadir comprobaciones de configuración válida, ausente e inválida; verificar errores identificables sin exponer valores ni inferir zona desde la computadora (LR-01/LR-05/LR-06).
- [x] 4.2 Preparar Flyway deshabilitado en C-01, desactivar inicialización SQL automática y limitar espera de pool, conexión y lectura según D-04; verificar configuración efectiva y ausencia de scripts, migraciones o seeds ejecutables de negocio (LR-02/LR-03).
- [x] 4.3 Crear una plantilla de entorno sin secretos y reglas `.gitignore` para configuración sensible, herramientas locales y generados; documentar exportación de variables sin asumir carga automática de `.env`; verificar que la plantilla se versiona, los valores locales quedan excluidos y el diff no contiene credenciales (LR-05).
- [x] 4.4 Configurar Vite en loopback:5173 con puerto estricto y proxy `/api` sin reescritura; documentar el modo de desarrollo y su URL base; verificar que los archivos reflejan D-04 y que un puerto ocupado detiene cada servidor sin sustitución automática (LR-01/LR-04).

## 5. Conectividad real y conservación de datos

- [x] 5.1 Implementar la comprobación de integración de arranque y `SELECT 1` sobre PostgreSQL habilitado, fuera del comando de build sin base; verificar conexión real y fallo finito con base inaccesible o credenciales inválidas, sin fallback en memoria ni secretos en resultados (LR-02/LR-05).
- [x] 5.2 Comprobar arranques y conexiones repetidas sobre la base de prueba mediante consultas de inspección de solo lectura antes y después; verificar conservación de su esquema/datos y ausencia de tabla de historial de Flyway o configuración persistida del consultorio (LR-03/LR-06). Base recién inicializada sin tablas de usuario; alcance y límites en [verification.md](verification.md).
- [x] 5.3 Documentar cómo habilitar la comprobación con valores locales, identificar `Etc/UTC` solo como fixture y distinguir los resultados fallidos; reproducir las instrucciones sin compartir secretos ni instalar/modificar un servicio global (RB-04/LR-02/LR-05/LR-06). Reproducción propia autorizada por el usuario en reemplazo de otro integrante, D-08; integración desde copia limpia exit 0.

## 6. Integración y revisión de C-01

- [x] 6.1 Ejecutar el backend y Vite con la configuración revisada y solicitar una ruta `/api` inexistente; verificar que el proxy conserva el 404 del backend y que al detenerlo aparece un fallo observable, sin HTML exitoso ni endpoint de negocio añadido (LR-01/LR-04).
- [x] 6.2 Repetir instalación y builds documentados desde una copia limpia del estado a revisar, sin PostgreSQL disponible durante la compilación; verificar artefactos, versiones y bloqueos sin diferencias (RB-01 a RB-04). Reproducción propia autorizada por el usuario en reemplazo de otro integrante, D-08; ambos builds y 18 pruebas pasaron, bloqueos intactos.
- [x] 6.3 Consolidar resultados por RB-01 a RB-04 y LR-01 a LR-06 con comprobaciones normales y revisión independiente, bajo la excepción del usuario al contrato incompleto de evidencia (D-06); verificar cobertura de escenarios y dejar explícitamente pendiente cualquier resultado no corroborado o no ejecutado, sin declarar aceptación de C-16. Ver [verification.md](verification.md).
- [x] 6.4 Revisar el diff final con un revisor distinto del constructor; verificar que contiene solo el alcance autorizado de C-01, que no cambió datos ajenos, autenticación, reglas de negocio ni estados de aceptación del roadmap, y que la documentación de ejecución coincide con el resultado integrado. Revisión independiente por `review_c01`; ver [verification.md](verification.md).

Reproducción final completada por el agente con autorización del usuario (D-08). Se conservaron las revisiones independientes anteriores y se registraron resultados en [verification.md](verification.md), sin atribuir participación a otra persona ni cumplimiento del contrato de evidencia omitido.

## Workflow follow-up

- Archivar `foundation-setup` con el CLI únicamente después de completar implementación, comprobaciones, revisión y aprobación requeridas; la existencia de los artefactos de planificación no acredita esos resultados.
- Sincronizar las especificaciones durante el archivo según las instrucciones del CLI. Después de verificar y archivar, actualizar C-01 en `CHANGES.md` y los registros de decisiones pertinentes; conservar pendientes los changes posteriores.
