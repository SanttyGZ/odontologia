# Tasks

## 1. Conciliar decisiones y roadmap

- [x] 1.1 Registrar en la KB la confirmación reportada de Fernando para diferir PB-06 a C-14, conservar `409 MANAGEMENT_WINDOW_CLOSED` y dejar pendiente el canal operativo concreto; verificar los textos y enlaces con lectura dirigida.
- [x] 1.2 Registrar en el informe archivado el alcance limitado de aprobación CRITICAL y quitar los espacios finales; agregar los resultados actuales de Windows, los dobles de C-04 y las limitaciones, verificando el archivo con `git diff --check`.
- [x] 1.3 Contrastar la especificación AG-05 con las pruebas presentes y dejar explícito qué evidencia existe para destinatario recuperado, intervalo idéntico y disputa por horario.

## 2. Verificar C-09 en Windows

- [x] 2.1 Ejecutar `test --tests "*GestionTurno*"` y `bootJar` con el wrapper de Windows y Java 21; registrar resultado, errores recuperados y recibos.
- [x] 2.2 Ejecutar `integrationTest --tests "*GestionTurnoIntegrationTest"` con la configuración PostgreSQL del README; registrar el resultado o el bloqueo observado sin declararlo aprobado.
- [x] 2.3 Validar `appointment-self-management` en modo estricto y revisar la suficiencia de las pruebas para destinatario recuperado, intervalo idéntico y conservación del original ante disputa por horario.

## 3. Validar el seguimiento

- [x] 3.1 Validar el change con OpenSpec estricto y archivar el seguimiento luego de registrar resultados y pendientes.

## Workflow follow-up

- Actualizar `CHANGES.md` después del archivo: reflejar el estado de C-09, conservar el estado local `[ ]` de C-10 y el cierre `[x]` de C-12, y registrar el estado de PB-06.
- Ejecutar `git diff --check` después de actualizar el índice.
