# Tasks

## 1. Destinatario confirmado y avisos de C-09

- [x] 1.1 Extender solo el resultado interno autorizado de C-04 con el destinatario de recuperación más reciente y usarlo para avisos nuevos de C-09, con fallback al email del paciente; comprobar el comportamiento con pruebas unitarias de servicio y de credenciales.
- [x] 1.2 Agregar integración PostgreSQL de cancelación y reprogramación después de recuperación: ningún aviso nuevo usa el email anterior, paciente e intentos de envío existentes permanecen intactos, y los avisos de reprogramación referencian la credencial vigente.
- [x] 1.3 Permitir un puerto PostgreSQL dedicado en el helper de integración, conservar `5433` por defecto y comprobar las pruebas con un contenedor efímero sin volumen.

## 2. Rechazo del intervalo idéntico

- [x] 2.1 Comparar inicio y fin calculados con la duración vigente; responder `409 SAME_INTERVAL` y comprobar en pruebas unitarias el rechazo y el caso de inicio igual con fin distinto.
- [x] 2.2 Agregar integración PostgreSQL que verifique que `SAME_INTERVAL` conserva revisión, credencial e intenciones de aviso.

## 3. Decisiones y documentación

- [x] 3.1 Mantener `409 MANAGEMENT_WINDOW_CLOSED` y marcar PB-06 pendiente para C-14 hasta acordar el contacto operativo; comprobar texto y respuestas existentes.
- [x] 3.2 Corregir los enlaces de la KB al diseño archivado, quitar la línea vacía extra de la spec archivada y actualizar CHANGES para C-09 y los pendientes de C-14, comprobando que los estados de C-10 y C-12 se conservan.
- [x] 3.3 Registrar el contrato protegido candidato de disponibilidad para C-14 con aprobación CRITICAL pendiente, sin añadir código, cambiar la consulta actual ni ampliar el spec de C-09.

## 4. Verificación y cierre

- [x] 4.1 Ejecutar unitarias, integración contra PostgreSQL real, `bootJar`, validación estricta de OpenSpec y `git diff --check`; guardar comandos, resultados y decisiones pendientes en `verification.md`.

## Workflow follow-up

- Archivar el change con OpenSpec después de verificar las tareas y volver a consultar su estado archivado.
