# Tasks

## 1. Referencia interna de credencial C-04

- [x] 1.1 Agregar una prueba para que `CredencialesGestion.validar` devuelva el UUID opaco de la credencial vigente separado del UUID del turno; confirmar primero el fallo RED porque `TurnoAutorizado` aún no expone esa referencia.
- [x] 1.2 Extender `TurnoAutorizado` y el mapeo de `validar` para incluir solo el UUID de credencial requerido por C-05, sin incluir el secreto; ejecutar la prueba de 1.1 en GREEN y toda la suite unitaria del backend.

## 2. Contratos y consulta de autogestión

- [x] 2.1 Agregar pruebas unitarias de las reglas de autogestión para límite inclusivo de 24 horas, ventana local de 30 días del nuevo inicio, duración vigente del motivo, estado del turno y respuestas ante credencial o revisión no disponible; verificar bordes con reloj controlado.
- [x] 2.2 Agregar pruebas de contrato HTTP para consulta, cancelación y reprogramación: `X-Management-Token`, datos permitidos, `ETag` opaco, `If-Match` requerido, `Cache-Control: no-store`, errores `404/409/412/428`, cuerpo genérico y ausencia de cambios ante rechazos; comprobar cada estado y cuerpo en el controlador.
- [x] 2.3 Implementar `GET /api/gestion/turno` reutilizando C-04; devuelve fecha, hora, estado, acciones y `ETag` opaco sin datos de contacto.

## 3. Escrituras coordinadas

- [x] 3.1 Implementar cancelación expresa con credencial vigente, plazo inclusivo y `If-Match`; integrar revocación y preparación C-05 en la escritura coordinada.
- [x] 3.2 Implementar reprogramación con duración vigente, reglas actuales de disponibilidad y `If-Match`; pasar a C-05 la referencia de credencial devuelta por C-04 y conservar la identidad del turno.

## 4. Integración, documentación y verificación

- [x] 4.1 Agregar pruebas de integración con PostgreSQL real para rollback conjunto de turno/credencial/avisos y solicitudes sincronizadas que compitan por el mismo destino o revisión; comprobar que como máximo una escritura obsoleta se confirma y no se crean avisos duplicados.
- [x] 4.2 Actualizar `knowledge-base/10_preguntas_abiertas.md` para reflejar el alcance resuelto de PB-01, PB-06, PB-07 y PB-08 en C-09 y mantener PB-10/PB-11 pendientes para C-11.
- [x] 4.3 Ejecutar `bootJar`, `openspec validate autogestion-turnos-api --strict --no-interactive` y `git diff --check`; registrar la compilación y la evidencia obtenida con las suites unitarias e integración completas.
- [x] 4.4 Verificar los contratos HTTP y reglas con pruebas unitarias, de controlador e integración PostgreSQL; ejecutar las suites afectadas y reconciliar evidencia de aceptación antes de archivar.

## Workflow follow-up

- Archivar el change con OpenSpec después de la verificación y de completar las revisiones requeridas.
- Marcar C-09 como completado en `CHANGES.md` únicamente después del archivo y de comprobar el estado final.
