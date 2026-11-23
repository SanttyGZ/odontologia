# Tasks

## 1. Revisión del contrato

- [ ] 1.1 Revisar el contrato aprobado de C-04 y resolver PB-08 a PB-11 y AT-06. Verificar que CA-05-P1 a CA-05-P6 tengan resultados aprobados y que la frontera con C-11 quede acordada.
- [ ] 1.2 Revisar y aprobar la persistencia y configuración HIGH antes de escribir. Verificar que se mantenga el contrato CRITICAL de C-04 y obtener autorización explícita si el alcance toca material privado.

## 2. Trabajo futuro sujeto a aprobación

- [ ] 2.1 Materializar preparación y almacenamiento de intenciones según la política aprobada. Verificar CA-05-01A, CA-05-01B, CA-05-01C, CA-05-02 y CA-05-03 con PostgreSQL, incluido el rollback de turno, credencial e intenciones sin referencias huérfanas.
- [ ] 2.2 Materializar unicidad de evento y control de elegibilidad. Verificar CA-05-04 y CA-05-05 con concurrencia sincronizada y las dos secuencias de CA-05-P4.
- [ ] 2.3 Verificar el contrato de recuperación y observación de resultados con emisor simulado y reinicio, según CA-05-06 y CA-05-P2 a CA-05-P6. Mantener trazabilidad de CA-05-07 para el emisor real.

## Workflow follow-up

- C-08/C-09 consumen la preparación de intenciones. C-11 prueba envío real y recepción en buzón. C-16 comprueba el recorrido integral.
