# Proposal

## Why

La autogestión prepara nuevas intenciones con el email del paciente aunque C-04 haya registrado un destinatario corregido mediante recuperación asistida. Además, una reprogramación al mismo intervalo vigente puede avanzar la revisión y crear avisos innecesarios. C-14 también necesita consultar opciones para el motivo autorizado, pero su contrato debe respetar el alcance CRITICAL de la credencial de C-04.

## What Changes

- Usar el destinatario más reciente confirmado por recuperación asistida para nuevos avisos de cancelación y reprogramación; conservar el email del paciente y los intentos ya registrados, y asociar los avisos de reprogramación a la credencial vigente.
- Rechazar con `409 SAME_INTERVAL` una reprogramación cuyo inicio y fin calculados con la duración vigente coincidan con el intervalo actual, sin cambiar revisión, credencial ni intenciones.
- Permitir que el helper de integración use un puerto temporal dedicado de PostgreSQL y conservar `5433` como valor predeterminado.
- Conservar `409 MANAGEMENT_WINDOW_CLOSED` y dejar PB-06 pendiente para C-14 hasta acordar el contacto operativo con el odontólogo.
- Documentar para C-14 un contrato protegido candidato y dejarlo sin implementar hasta recibir aprobación CRITICAL.
- Corregir referencias al diseño archivado de C-09, la fila de C-09 en el roadmap y el espacio final extra de su especificación archivada, preservando los cierres de C-10 y C-12.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `appointment-self-management`: precisar destinatarios vigentes de avisos tras recuperación asistida y el rechazo de reprogramaciones al mismo intervalo.

## Impact

Backend de autogestión y validación de credenciales de C-04; preparación de intenciones C-05 en PostgreSQL; pruebas unitarias y de integración; la especificación y referencias documentales de C-09; y el roadmap local. El contrato candidato de C-14 queda en diseño, sujeto a aprobación CRITICAL, sin ampliar la respuesta actual ni agregar una ruta en este change.
