# Comprobaciones de C-02 core-models

El usuario aprobó el diseño HIGH y eligió Grupo13 y Carlos Garcia para la demostración en UTC−3. Se usó `America/Argentina/Mendoza` como identificador IANA para la demo. La excepción autorizada para C-02 cubre el contrato incompleto de evidencia de sdd-orchestrator. Se conservan la ejecución contra PostgreSQL real y la revisión independiente.

## Implementación

- V1 crea ocho tablas, claves, índices y la exclusión GiST para turnos confirmados. Flyway valida el historial y conserva migraciones aplicadas.
- El inicializador completa consultorio, odontólogo y agenda. Si difieren los nombres o la zona explícitos, conserva los registros y falla el arranque.
- Los adaptadores JDBC conservan UUID, horas semanales locales e instantes con precisión de microsegundos. Los cambios futuros de disponibilidad comparten la transacción de `AgendaWriteCoordinator`.

## Comandos reproducibles

Con el JDK documentado en README y PostgreSQL Docker en marcha, desde la raíz:

```bash
python3 scripts/with-local-db.py --integration ./backend/gradlew -p backend test integrationTest bootJar
openspec validate --all --strict --no-interactive
```

Las pruebas utilizan bases efímeras `odontologia_c02_*` en PostgreSQL 17.12. El helper activa ese permiso de forma explícita. Cada base creada por una prueba se elimina al terminar.

| Requisitos | Comprobación |
|---|---|
| CM-01 a CM-04, CM-08 | Instalación vacía con ocho tablas e índices. Rechazo de duplicados de configuración, referencias inválidas, atributos incoherentes y borrados de registros referenciados. Admite pacientes con datos coincidentes. Un cambio de duración del motivo conserva la duración del turno. |
| CM-05 | Dos conexiones con solapamiento igual o parcial, espera observada en PostgreSQL y rechazo por exclusión después del primer commit. La prueba también admite turnos contiguos y la reutilización del intervalo de un cancelado. |
| CM-06, LR-06 | Validación temprana de zona, nombres y conexión. Reloj UTC sustituible, precisión de microsegundos, horas locales e instantes comparados con zonas JVM distintas. El arranque falla ante diferencia con la zona persistida. |
| CM-07, LR-03 | Arranque inicial, reintentos, dos inicializadores, fallo de configuración, rollback parcial e historial incoherente. Una segunda ejecución de Flyway aplica cero migraciones. Un esquema anterior a `public` en la URL JDBC no desvía la inicialización. |
| AW-01 a AW-05 | Espera por fila observada y lecturas posteriores a commit y rollback. Plazo de bloqueo, agenda ausente, rechazo de transacción previa y conflicto de reprogramación conservando el original. Revierte una segunda escritura junto con la primera. |

Se reiniciaron PostgreSQL Docker y la aplicación con una base efímera poblada con fixtures de las ocho tablas. El dump normalizado de esquema, filas e historial coincidió antes y después. Con el contenedor detenido, el helper falló indicando los comandos de arranque. El arranque con contraseña ficticia incorrecta también falló. El contenedor quedó operativo. La prueba de reinicio cubre persistencia local. La restauración de respaldos y las reglas funcionales de reserva, cambios de disponibilidad y autogestión corresponden a otros alcances.

En `odontologia_test` se aplicó V1 y se inicializó la configuración aprobada. Dos arranques conservaron los mismos UUID y una sola versión aplicada. La base contiene el consultorio, el profesional y la agenda, con las cinco tablas de disponibilidad, pacientes y turnos vacías.

## Revisión independiente

Un revisor independiente ejecutó `test integrationTest bootJar` desde una copia limpia del backend con el mismo wrapper y dependencias. La suite final obtuvo **22 pruebas unitarias y 18 de integración**, con cero fallos, errores o pruebas omitidas. La validación estricta del change también pasó.

La revisión detectó que una URL JDBC con otro esquema en el `search_path` podía desviar las consultas. La prueba de regresión falló antes del ajuste y pasó después de fijar `public` en las conexiones de Hikari, alineado con Flyway. El revisor corroboró la corrección y no dejó hallazgos abiertos.

Las primeras ejecuciones de las pruebas de esquema, coordinación, mapeos y configuración detectaron la ausencia de la migración, de los componentes y de la validación de nombres. Sus escenarios positivos y negativos pasaron con la implementación. Los resultados son reproducciones ordinarias bajo la excepción autorizada.
