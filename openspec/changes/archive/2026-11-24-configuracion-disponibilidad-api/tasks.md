# Tasks

## 1. Motivos y contrato HTTP

- [x] 1.1 Implementar lectura, alta y modificación de motivos bajo las rutas de la spec; verificar respuestas HTTP, nombre no vacío, límites inclusivos de duración, múltiplos de 15, motivo inexistente y conservación de la duración reservada en pruebas unitarias y de controlador.
- [x] 1.2 Definir el mapeo común de errores de validación, recurso inexistente, conflicto de configuración, agenda ocupada y fallo técnico; verificar que las respuestas mantengan el campo JSON `codigo` y no incluyan detalles de filas.

## 2. Horario semanal y bloqueos

- [x] 2.1 Implementar consulta y reemplazo atómico del horario semanal; verificar días, horas del mismo día, atención contigua, rechazo de duplicados/solapes, pausas contenidas en exactamente una atención y rollback completo ante cualquier fila inválida.
- [x] 2.2 Implementar consulta, alta y retiro de bloqueos; verificar intervalos finitos positivos, respuestas de recurso inexistente, que el retiro borre solo el bloqueo solicitado y que los turnos no se modifiquen.
- [x] 2.3 Agregar pruebas de integración con PostgreSQL real para rechazar cambios semanales y bloqueos que afecten turnos confirmados, incluyendo intersecciones parciales; comprobar que configuración y turnos permanezcan intactos tras cada rechazo.

## 3. Coordinación y concurrencia de agenda

- [x] 3.1 Integrar todas las escrituras de C-06 como transacciones raíz de `AgendaWriteCoordinator`; verificar con pruebas que la validación relea estado después del bloqueo, revierta ante rechazo y traduzca agotamiento de espera a `409 AGENDA_BUSY` sin reintento automático.
- [x] 3.2 Crear pruebas sincronizadas sobre PostgreSQL para las carreras reserva/configuración en ambos órdenes; comprobar que una reserva confirmada primero hace rechazar la configuración incompatible, y que una configuración confirmada primero se aplica a la validación de la reserva posterior, sin sleeps como única sincronización.
- [x] 3.3 Verificar en pruebas HTTP que las nuevas rutas conservan la autorización y CSRF de C-03; una solicitud no autorizada no devuelve ni modifica configuración y no se requieren cambios en `DentistSecurityConfiguration`.

## 4. Integración del change

- [x] 4.1 Ejecutar desde la raíz `python3 scripts/with-local-db.py --integration ./backend/gradlew -p backend test integrationTest bootJar` y `openspec validate configuracion-disponibilidad-api --strict --no-interactive`; verificar que suites, build y validación estricta terminen sin fallos y registrar cualquier excepción con evidencia.

## Workflow follow-up

- Revisar los contratos HTTP con los responsables de C-07 y C-12 antes de que implementen consumidores; mantener este spec como referencia para rutas, payloads y errores.
- Completar verificación del change y archivar con OpenSpec CLI cuando las tareas autorizadas estén implementadas y verificadas.
- Actualizar `CHANGES.md` solo después de verificar y archivar C-06.
