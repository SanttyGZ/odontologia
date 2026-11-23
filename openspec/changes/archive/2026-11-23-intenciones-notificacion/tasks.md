# Tasks

## 1. Revisión del contrato

- [x] 1.1 El diseño v1.1 aprobado registra PB-08 a PB-11 y AT-06, el contrato con C-04 y la frontera con C-11. Se incorpora el ajuste explícito de recuperación y el tratamiento de intentos históricos.
- [x] 1.2 Ignacio aprobó expresamente el diseño v1.1 y autorizó la migración y persistencia HIGH de C-05 conforme al contrato CRITICAL de C-04. La autorización excluye C-11 y C-14.

## 2. Trabajo futuro sujeto a aprobación

- [x] 2.1 Materializar preparación y almacenamiento de intenciones según la política aprobada. Verificar el contrato de los casos nominales y CA-05-03 con PostgreSQL, incluido el rollback de turno, credencial e intenciones sin referencias huérfanas.
- [x] 2.2 Materializar unicidad de evento, recuperación y control de elegibilidad. Verificar conflicto de origen, carrera entre recuperación y autorización, conservación del intento y rollback de recuperación.
- [x] 2.3 Verificar despacho combinado, composición fallida, vencimiento, resultado incierto e intervalos de reintento con emisor simulado, reloj controlado y reinicio. Mantener CA-05-07, los objetivos de envío y la recepción real para C-11. Veredicto favorable del revisor independiente en el registro de C-04.

## Workflow follow-up

- C-08/C-09 consumen la preparación de intenciones. C-11 prueba envío real y recepción en buzón. C-16 comprueba el recorrido integral.
