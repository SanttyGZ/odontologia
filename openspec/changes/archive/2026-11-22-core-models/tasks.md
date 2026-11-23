# Tasks

El diseño HIGH está aprobado. Las decisiones y comprobaciones previstas para los tres deltas de `specs/` están en [design.md](design.md).

## 1. Condiciones previas de implementación

- [x] 1.1 Registrar Grupo13 y Carlos Garcia con la zona IANA elegida para la demostración en UTC−3. Comprobar el identificador y el desplazamiento mediante el catálogo de zonas local. D-03 conserva los valores y D-07 delimita PB-04 para C-08.
- [x] 1.2 Registrar la aprobación HIGH explícita del diseño de C-02. La sección de revisión del diseño conserva la fecha y la respuesta del usuario.
- [x] 1.3 Comprobar los controles de ejecución/evidencia exigidos por la skill al iniciar apply y disponer del contrato completo o instrucción explícita aplicable; verificar que no se reutilicen automáticamente las excepciones de C-01 ni se inventen recibos.

## 2. Esquema versionado e integridad

- [x] 2.1 Preparar soporte de pruebas PostgreSQL con bases efímeras dedicadas y límites de limpieza; comprobar creación/aislamiento y que la base local existente y el volumen permanezcan intactos, documentando el comando de integración.
- [x] 2.2 Implementar V1 con consultorio, odontólogo y agenda, sus UUID/FKs/unicidad; verificar CM-01 con instalación vacía, duplicados y referencias inexistentes en PostgreSQL real.
- [x] 2.3 Añadir motivos, franjas y bloqueos con CHECK e índices de D-02; verificar CM-02 con datos válidos, nulos, intervalos invertidos, día/tipo inválidos y catálogo de índices/FKs.
- [x] 2.4 Añadir paciente y turno con referencias compuestas, duración, revisión y cancelación coherentes; verificar CM-03/CM-04/CM-08 mediante inserciones inválidas, duplicados naturales permitidos y borrado restringido sin cascada.
- [x] 2.5 Añadir btree_gist y exclusión inmediata filtrada por CONFIRMADO; verificar CM-05 con transacciones sincronizadas sin coordinador, solapamientos iguales/parciales rechazados, contiguos permitidos y cancelados sin ocupación.
- [x] 2.6 Activar Flyway con las protecciones de D-01 y adaptar PostgresConnectivityTest; verificar LR-03 en esquema vacío, historial vigente, checksum incompatible y esquema no reconocido, sin clean/repair/baseline automáticos; documentar evolución de migraciones en README.

## 3. Tipos de dominio y tiempo

- [x] 3.1 Implementar los tipos mínimos de las ocho entidades y mapeos JDBC por función, sin CRUD genérico ni endpoints; comprobar round-trip de atributos obligatorios/opcionales y preservación de duración reservada al editar el motivo (CM-02/CM-04).
- [x] 3.2 Incorporar Clock inyectable, instantes y precisión temporal revisada; verificar CM-06 con reloj fijo, otra zona JVM, instantes equivalentes y horas locales sin conversión silenciosa de 24:00; documentar las convenciones del modelo.
- [x] 3.3 Validar nombres iniciales y zona explícitos antes de inicialización, ajustar helper y plantilla sin default persistible de Etc/UTC; verificar LR-06 con valores ausentes/inválidos y conservar los comandos de inicio Docker cuando la base esté detenida.

## 4. Coordinación de escrituras

- [x] 4.1 Implementar el límite transaccional raíz READ COMMITTED, bloqueo de agenda, participación de adaptadores y rechazo de entrada transaccional previa según D-05; verificar AW-01/AW-04 con dos conexiones y agenda inexistente.
- [x] 4.2 Aplicar relecturas posteriores al bloqueo y orden de recursos; verificar AW-02 con barreras/latches, commits y rollbacks concurrentes de ocupación y configuración, acreditando la espera real sin usar solo sleeps.
- [x] 4.3 Configurar límites locales y clasificación de errores; verificar AW-04 con bloqueo retenido, SQLSTATE y tiempo finito, sin reintentos de negocio, éxito falso ni mensajes con datos sensibles.
- [x] 4.4 Verificar AW-03/AW-05 con varias escrituras en la misma conexión transaccional, fallo inyectado, rechazo de destino ocupado y conservación del turno original; documentar el contrato para C-04/C-05/C-06/C-08/C-09 y revisar que no incluya llamadas externas.

## 5. Inicialización y conservación

- [x] 5.1 Implementar el inicializador transaccional de D-06 con valores aprobados, sin carga de ejemplos; verificar CM-07/CM-08 con dos inicializadores simultáneos y exactamente tres filas de configuración relacionadas.
- [x] 5.2 Completar únicamente configuración faltante sin sobrescribir filas existentes; verificar ejecución repetida con UUID/datos intactos y rollback total ante fallo intermedio o nombres/zona incompatibles (CM-07/LR-03/LR-06).
- [x] 5.3 Documentar configuración necesaria y procedimiento de arranque en README y plantilla; comprobar que los comandos documentados usan valores explícitos y no crean credenciales, avisos ni datos clínicos/de demostración automáticos.

## 6. Integración y revisión

- [x] 6.1 Ejecutar las comprobaciones integradas del backend y los casos positivos/negativos de conexión con configuración revisada; registrar resultados por requisito y escenario, sin reutilización de caché ni dar por aceptadas APIs o historias futuras.
- [x] 6.2 Reproducir reinicio de aplicación y PostgreSQL con fixtures ficticios en entorno aislado; comprobar igualdad de UUID, datos, intervalos, estados e historial de migraciones antes/después, dejando explícito que no prueba restauración de respaldos.
- [x] 6.3 Realizar revisión independiente del estado implementado con los controles y evidencia aplicables; resolver hallazgos, completar el registro de aceptación y ejecutar validación estricta OpenSpec antes de proponer el archivo del change.

## Workflow follow-up

- Archivar `core-models` con el CLI cuando las tareas y aceptación estén completadas; consolidar los deltas sin mover directorios manualmente.
- Después del archivo, actualizar CHANGES y estados documentales pertinentes; sincronizar AGENTS/CLAUDE si cambian sus instrucciones. Informar que se desbloquean C-03 y C-04 únicamente tras el cierre efectivo de C-02.
