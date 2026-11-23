# Design

## Context

Ver [motivación y alcance](proposal.md). C-01 está archivado y aporta Spring JDBC, Flyway instalado pero deshabilitado, PostgreSQL 17.12 local y pruebas de conexión reales. No existen tablas del dominio. `PostgresConnectivityTest` exige actualmente ausencia de Flyway; `with-local-db.py` y `.env.example` usan Etc/UTC solo como fixture de C-01. Estos contratos deben evolucionar juntos.

La KB 04 aprobó el modelo lógico. El usuario aprobó el diseño HIGH de C-02 el 2026-11-22. Los escenarios quedan pendientes de implementación y comprobación.

## Goals / Non-Goals

**Goals:** definir esquema físico revisable, migración inicial segura, inicialización idempotente y un único límite transaccional reutilizable por C-06/C-08/C-09 y componible con C-04/C-05.

**Non-Goals:** introducir servicios CRUD por entidad, endpoints funcionales o seguridad; completar algoritmos de disponibilidad, políticas de cambios con reservas, comunicaciones o aceptación integral de CT-01/CT-04.

## Decisions

Las decisiones técnicas siguientes corresponden al diseño aprobado de C-02. La política de pacientes repetidos queda para el flujo de reserva en C-08.

### D-01 — Acceso y evolución del esquema (AT-02 / PT-01)

Continuar con Spring JDBC y consultas explícitas; gestionar transacciones con Spring. Evita incorporar JPA y permite expresar los bloqueos sin otro modelo de entidades. Organizar tipos y adaptadores por función bajo `dev.odontologia` (`consultorio`, `agenda`, `pacientes`); compartir solo reloj y límite transaccional. Usar tipos concretos y constructor injection.

Activar Flyway y crear `V1__core_models.sql` en `backend/src/main/resources/db/migration/`. Incluir las ocho tablas e índices en la migración; no incluir valores del consultorio ni ejemplos. Mantener `spring.sql.init.mode=never`, validación de historial, limpieza deshabilitada y sin baseline/repair automático. No modificar una migración aplicada; las siguientes evolucionan hacia adelante. No añadir librerías salvo necesidad revisada.

Flyway y el pool JDBC usan `public` como esquema de la aplicación. La prueba de arranque con otro esquema al principio del `search_path` comprueba que el inicializador opera sobre las tablas migradas.

### D-02 — Esquema físico e índices (CM-01 a CM-04, CM-08)

Nombres SQL en `snake_case`, esquema `public`, PK UUID no nula en cada tabla, UUID generado por la aplicación. Mantener UUID aprobados aunque una clave secuencial resulte más compacta; no añadir una extensión de generación. Todos los campos son NOT NULL salvo los indicados opcionales. Texto como `text`, enteros como `integer`, estados como `text` con CHECK; sin máximos de longitud ni normalización automática no acordados. Comprobar texto obligatorio no vacío con `btrim`, conservando su valor original. Ningún FK usa borrado en cascada.

| Tabla | Columnas además de id | Restricciones e índices propuestos |
|---|---|---|
| `consultorio` | `nombre_visible text`, `zona_horaria text`, `unico boolean` | `unico` siempre true, UNIQUE y NOT NULL para máximo una fila; nombres no vacíos. Zona validada en Java con catálogo IANA y comparada al arrancar. |
| `odontologo` | `consultorio_id uuid`, `nombre_visible text` | FK consultorio, UNIQUE consultorio_id; nombre no vacío. |
| `agenda` | `odontologo_id uuid` | FK odontologo, UNIQUE odontologo_id; sin duplicar consultorio_id. |
| `motivo_consulta` | `agenda_id uuid`, `nombre text`, `duracion_minutos integer` | FK agenda; nombre no vacío, duración > 0; índice agenda_id y UNIQUE (id, agenda_id) para FK compuesta de turno. |
| `franja_semanal` | `agenda_id uuid`, `dia_semana integer`, `hora_inicio time`, `hora_fin time`, `tipo text` | FK agenda; día 1–7, inicio < fin, tipo ATENCION/PAUSA; índice (agenda_id, dia_semana, hora_inicio). |
| `bloqueo_agenda` | `agenda_id uuid`, `inicio timestamptz`, `fin timestamptz`, `descripcion text` opcional | FK agenda; inicio/fin finitos e inicio < fin; índice (agenda_id, inicio). |
| `paciente` | `consultorio_id uuid`, `nombre text`, `apellido text`, `email text`, `telefono text`, `dni text` | FK consultorio; cinco campos no vacíos; índice consultorio_id. Sin UNIQUE o búsquedas por datos naturales. |
| `turno` | `agenda_id uuid`, `paciente_id uuid`, `motivo_id uuid`, `inicio timestamptz`, `fin timestamptz`, `duracion_reservada_minutos integer`, `estado text`, `revision integer`, `creado_en timestamptz`, `cancelado_en timestamptz` opcional | FK agenda, FK paciente, FK (motivo_id, agenda_id) a motivo (id, agenda_id); índices (agenda_id, inicio), paciente_id y (motivo_id, agenda_id), más exclusión D-04. |

Turno exige instantes finitos, inicio < fin, duración > 0 y fin = inicio + duración en minutos transcurridos; revisión >= 0 (inicial 0, sin historial implícito). Estado limitado a CONFIRMADO/CANCELADO; cancelado_en nulo para confirmado y obligatorio para cancelado. Los cambios de revisión se implementarán con sus casos de uso, sin trigger que invente transiciones ni política de repetición.

La FK compuesta impide asignar un motivo de otra agenda. El contexto del paciente se obtiene por consultorio → odontólogo → agenda: las FKs y singleton hacen único el contexto, sin duplicar consultorio_id en agenda. No implementar transferencia de propietarios; las operaciones de persistencia de C-02 no ofrecen ese cambio. No crear índices adicionales ya cubiertos por PK/UNIQUE o prefijos útiles. Los índices temporales son iniciales, sin prometer rendimiento para volúmenes no informados.

### D-03 — Instantes, horas y reloj (CM-06, LR-06)

Usar `Instant` para eventos concretos, `ZoneId` para zona de consultorio y horas semanales locales sin zona. Proveer un bean `Clock.systemUTC()` por constructor; en pruebas sustituir por `Clock.fixed`. UTC del reloj no fija la zona comercial. Normalizar precisión de instantes a microsegundos para un round-trip PostgreSQL definido; probar igualdad del instante y duración.

Representar las franjas con `LocalTime` y SQL `time without time zone`, precisión de microsegundos y valores menores que 24:00. El extremo SQL 24:00 excede el rango de `LocalTime`. C-06 resolverá con PB-02 las franjas nocturnas, los cierres a las 24:00 y las políticas de superposición y contención de pausas.

Exigir y validar la configuración explícita antes de migrar o inicializar. El usuario indicó Argentina UTC−3. Para la demostración local se seleccionó `America/Argentina/Mendoza` como identificador IANA. La ciudad concreta queda sin especificar.

| Variable | Valor de demostración |
|---|---|
| `APP_TIME_ZONE` | `America/Argentina/Mendoza` |
| `APP_CONSULTORIO_NOMBRE` | `Grupo13` |
| `APP_ODONTOLOGO_NOMBRE` | `Carlos Garcia` |

El arranque que persiste datos requiere que el operador aporte estos valores. Las pruebas aisladas aportan sus propios valores ficticios. PB-01 y PB-03 quedan para el cálculo de ventanas y pasos de disponibilidad en los cambios que los utilizan.

### D-04 — Defensa de ocupación en PostgreSQL (CM-05)

Usar `btree_gist` y exclusión GiST combinando igualdad de agenda UUID con solapamiento de `tstzrange(inicio, fin, '[)')`, aplicada solo a estado CONFIRMADO, inmediata y no diferida. Las cancelaciones liberan el intervalo. Un cambio de intervalo que falla conserva el original. Si la extensión o los permisos faltan, la migración debe informar el problema y detenerse.

Alternativas consideradas: unicidad por inicio no cubre solapamientos parciales; solo bloqueo exige que todo escritor coopere. La exclusión añade protección de turnos incluso ante SQL que omita el protocolo, pero **no** protege turnos frente a bloqueos o cambios de horario. Por eso se complementa con D-05.

### D-05 — Protocolo transaccional común (AT-05 / PT-02, AW-01 a AW-05)

Un componente de coordinación abre la transacción raíz READ COMMITTED y recibe agenda y trabajo de base a ejecutar. Rechaza entrada con una transacción ya abierta para evitar escrituras anteriores al bloqueo. Los adaptadores usan la conexión vinculada por Spring y participan en esa misma transacción; no abren REQUIRES_NEW ni confirman por su cuenta.

1. Establecer límites locales de 2 s para `lock_timeout`, 4 s para `statement_timeout` y 8 s para el timeout transaccional Spring. Medir en pruebas estos límites de ejecución de la entrega local.
2. Adquirir la fila de agenda por UUID con `SELECT ... FOR UPDATE`. Si no existe, abortar antes de ejecutar el trabajo.
3. En consultas **posteriores** al bloqueo releer motivo, horario, pausas, bloqueos, ocupación y turno/revisión que condicionen la operación. Una lectura previa de disponibilidad no se reutiliza como decisión. Obtener también el tiempo actual mediante Clock cuando el futuro caso de uso evalúe sus reglas.
4. Validar la política del caso de uso; escribir sus cambios. C-02 aporta mecanismo y fixtures de prueba; C-06/C-08/C-09 aportan reglas y endpoints. Orden común: agenda primero; filas adicionales, si se bloquean, en orden fijo motivo → franja → bloqueo → paciente → turno y UUID ascendente dentro de cada tabla. No adquirir primero un turno y luego la agenda.
5. Confirmar al terminar o revertir todo ante rechazo/error. Liberar coordinación al finalizar. Credenciales/intenciones futuras se insertan dentro de este trabajo, tras revisión de C-04/C-05. SMTP y cualquier espera externa quedan fuera.

READ COMMITTED permite que cada consulta posterior vea los commits anteriores a ella. La garantía del proyecto requiere que **todos** los escritores de disponibilidad adquieran primero la misma agenda; ningún snapshot previo al bloqueo basta. Las lecturas públicas no reservan nada. Advisory locks y SERIALIZABLE se descartan inicialmente por añadir convenciones o reintentos que esta agenda única no necesita.

Clasificar sin datos sensibles: agenda ausente; espera agotada (55P03); superposición (23P01); integridad de entrada (23503/23514/23502); deadlock/serialización (40P01/40001); otro error técnico. Revertir antes de devolver el error. No convertir todos los errores en horario ocupado ni reintentar automáticamente una operación de negocio. Una pérdida de conexión al confirmar tiene resultado incierto y requiere reconciliación en el caso de uso futuro, sin declarar éxito ni repetir a ciegas. El mapeo HTTP corresponde a sus APIs posteriores.

### D-06 — Inicialización conservadora (CM-07/CM-08, LR-03)

Ejecutar después de Flyway y antes de completar el arranque, en una transacción READ COMMITTED. Insertar consultorio si falta con conflicto sobre singleton sin actualizar valores; después releerlo con bloqueo. Comparar nombres y zona explícitos con lo persistido: discrepancia detiene el arranque y revierte, no aplica una edición silenciosa. Completar profesional y agenda faltantes con sus relaciones únicas y UUID generados; conservar los existentes y comprobar sus referencias. La coordinación sobre consultorio serializa inicializadores concurrentes.

La primera instalación crea exactamente esas tres filas. No borrar o recrear registros al reiniciar. No introducir disponibilidad clínica predeterminada, ejemplos automáticos, contraseñas ni tokens. Las pruebas generan fixtures ficticios únicamente en su base aislada. Un cambio deliberado de nombre o zona necesita una operación revisada distinta del inicializador.

### D-07 — Pacientes y límite de PB-04

La persistencia admite registros distintos con datos repetidos. El esquema de C-02 evita la unicidad por DNI, email o teléfono, conforme a CM-03. La decisión de crear un registro nuevo por reserva o reutilizar uno existente queda para C-08 junto con los formatos, límites de entrada y reglas de corrección de datos.

### D-08 — Pruebas y alcance de la evidencia

Tras la aprobación, usar bases de integración efímeras con nombre dedicado de C-02 dentro del PostgreSQL Docker del proyecto, separadas de `odontologia_test`. Su creación y eliminación estarán limitadas a las bases generadas por la prueba; nunca borrar el volumen ni limpiar la instalación existente. Probar con PostgreSQL real, sin H2. Mantener integración sin caché/up-to-date y los comandos de arranque Docker del README.

| Requisitos | Comprobaciones previstas |
|---|---|
| CM-01/02/03/04/08 | Migración desde vacío, catálogo de restricciones e índices, inserciones inválidas por atributo, FKs, duplicados de paciente, borrado restringido y ausencia de ejemplos. |
| CM-05 | Dos conexiones sincronizadas insertan solapamientos iguales/parciales **sin** coordinador para probar la exclusión; contiguos y cancelados permitidos. |
| CM-06 / LR-06 | Clock fijo, round-trip de instantes y precisión, JVM con otra zona, zona ausente/inválida/distinta y helper sin fixture implícito. |
| CM-07 / LR-03 | Dos inicializadores simultáneos; ejecución repetida, fallo inyectado, configuración diferente, reinicio de aplicación y PostgreSQL preservando filas e historial. |
| AW-01/02 | Dos conexiones/hilos con latches o barreras y comprobación de espera real; B relee cambios de A después de confirmar y no observa lo revertido. Probar cambios de configuración además de turnos, sin inventar PB-05. |
| AW-03/04/05 | Error después de varias escrituras, reprogramación fixture en conflicto con original intacto, lock timeout, agenda ausente, clasificación y composición en una transacción. Revisar ausencia de SMTP/llamadas externas. |

No usar sleeps como única sincronización ni pruebas secuenciales para afirmar concurrencia. Un reinicio verifica persistencia, no restauración. Conservar asociación requisito → escenario → ejecución; no aceptar C-08/C-09 ni historias completas con fixtures de C-02.

El usuario autorizó aplicar a C-02 la misma excepción al contrato incompleto de evidencia que en C-01 mediante «si sigamos con lo misma excepcion». El alcance conserva pruebas con PostgreSQL real, resultados por requisito y revisión independiente. Los recibos y la identificación formal del árbol del contrato siguen fuera de la evidencia de esta ronda.

## Risks / Trade-offs

- [Una escritura por agenda] → transacciones breves, límites finitos y medición; suficiente como propuesta para agenda única, sin prometer escalabilidad.
- [La exclusión no cubre bloqueos/horario] → mismo protocolo para todos los escritores, relectura posterior y pruebas de integración en sus changes.
- [Permisos/extensión no disponibles] → detectar al migrar y detener; revisar alternativa antes de reducir garantías.
- [Índices/FK compuesta adicionales] → coste de escritura a cambio de integridad; evitar índices redundantes y medir cuando existan consultas reales.
- [Zona/nombres diferentes al reiniciar] → fallo explícito conservando datos; edición deliberada fuera del inicializador.
- [Datos ficticios confundidos con instalación] → bases aisladas y carga solo en pruebas; nunca activar semillas por omisión.

## Migration Plan

1. Usar el diseño HIGH aprobado y los valores de demostración registrados en D-03. Aplicar el alcance de persistencia descrito en D-07.
2. Preparar y probar migración e inicializador en una base efímera del servicio Docker autorizado para C-02. No actuar sobre bases ajenas al proyecto.
3. Actualizar configuración, helper, plantilla, prueba C-01 y README de forma coherente. Mantener comprobación de conexión y de indisponibilidad sin secretos.
4. Aplicar a la base local del proyecto solo con la configuración revisada y comprobar historial, inicialización y reinicios. Un esquema desconocido no se adopta automáticamente.
5. Ante fallo no borrar ni ejecutar clean/repair automático. Las transacciones fallidas revierten sus cambios; ante una migración aplicada corregir con otra versión revisada. No prometer rollback destructivo ni restauración de respaldo no probada.
6. Realizar revisión independiente con controles aplicables antes de declarar verificado; archivar con CLI y actualizar índices solo después de completar aceptación. C-03/C-04 siguen bloqueados hasta ese cierre.

## Revisión requerida

| Punto | Estado al 2026-11-22 |
|---|---|
| Revisión HIGH del diseño de C-02 | Aprobación explícita del usuario el 2026-11-22. |
| Configuración de demostración | El usuario indicó «apruebo diseño, ciudad argentina con UTC-3 consultorio "Grupo13" y nombre Carlos Garcia». D-03 registra el identificador IANA elegido para la demo. |
| Registro nuevo por reserva (PB-04) | La política del flujo de reserva sigue pendiente para C-08. |
| Contrato de evidencia SDD | Excepción autorizada para C-02. Se mantienen pruebas reales y revisión independiente. |

## UI Mockups

Decision: no (no UI), 2026-11-22. No hay cambios visuales en C-02.

## Fuentes y coordinación de la ronda

KB [04](../../../../knowledge-base/04_modelo_de_datos.md), [05](../../../../knowledge-base/05_reglas_de_negocio.md), [08](../../../../knowledge-base/08_arquitectura_propuesta.md), [10](../../../../knowledge-base/10_preguntas_abiertas.md) y [CHANGES C-02](../../../../CHANGES.md) delimitan los acuerdos y pendientes. Las referencias consultadas mediante Context7 documentan [bloqueos](https://www.postgresql.org/docs/17/explicit-locking.html), [consistencia con bloqueos](https://www.postgresql.org/docs/17/applevel-consistency.html), [rangos y exclusión](https://www.postgresql.org/docs/17/rangetypes.html), [btree_gist y UUID](https://www.postgresql.org/docs/17/btree-gist.html) y [timeouts](https://www.postgresql.org/docs/17/runtime-config-client.html).

No hay skill `openspec-propose` separada instalada; se usa el workflow oficial `propose` incluido en el CLI OpenSpec 2.0.0 y las instrucciones por artefacto, con su límite de planificación. Módulo obligatorio `artifacts` disponible; skill PostgreSQL y referencias de claves/tipos/bloqueos leídas.

Decisiones de coordinación: `{action: propose, step: 3, signal: depende de decisiones de esta conversación y se lee el resultado completo, direct}`, `{action: apply, step: 3, signal: escritura coordinada del esquema y pruebas, direct}`, `{action: verify, step: 1, signal: revisión independiente, delegated}` y `{action: archive, step: 3, signal: cierre por CLI tras la verificación, direct}`.
