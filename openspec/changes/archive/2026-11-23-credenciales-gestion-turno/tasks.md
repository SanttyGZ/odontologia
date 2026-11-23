# Tasks

## 1. Revisión del contrato

- [x] 1.1 El diseño v1.1 aprobado registra PB-01, PB-07 y AT-04 y la frontera con C-05. Se incorpora el ajuste autorizado para preservar el recordatorio pendiente sin intentos durante la recuperación.
- [x] 1.2 Ignacio aprobó expresamente el diseño v1.1 y autorizó el código y la persistencia sensible CRITICAL de C-04, incluida la recuperación asistida. La autorización excluye C-11 y C-14.

## 2. Trabajo futuro sujeto a aprobación

- [x] 2.1 Materializar emisión, validación e invalidación y la operación interna de recuperación asistida del diseño v1.1. Verificación de primitivas, sesión, CSRF, alcance, revocación y rechazo en la corrida independiente registrada. Las rutas públicas, la presentación y los bordes de las operaciones consumidoras se integran en C-08/C-09/C-14.
- [x] 2.2 Materializar composición interna y persistencia según AT-04 aprobado. Verificar CA-04-05, CA-04-06 y CA-04-11 con reinicio, pérdida de clave, validación del enlace y revisión de exposición del secreto.
- [x] 2.3 Verificar el límite compartido con un consumidor de prueba. CA-05-03 comprueba el rollback coordinado de turno, credencial e intenciones sin referencias huérfanas. Veredicto independiente favorable registrado en revision-independiente.md.

## Workflow follow-up

- Integrar las operaciones de turno en C-08/C-09, composición de correo en C-11 y navegador en C-14 según el roadmap.
