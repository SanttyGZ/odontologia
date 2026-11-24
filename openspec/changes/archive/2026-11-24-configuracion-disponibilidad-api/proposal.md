# Proposal

## Why

El odontólogo necesita administrar motivos, horario semanal, pausas y bloqueos desde una API interna para que las reservas futuras usen una configuración consistente. C-02 ya provee el modelo persistente y la coordinación transaccional, y C-03 protege la familia de rutas internas; C-06 completa la capacidad de configuración que necesitan C-07 y C-12.

## What Changes

- Exponer lectura, alta y modificación de motivos sin borrado, validando nombre no vacío y duración entre 15 y 240 minutos en múltiplos de 15.
- Exponer consulta y reemplazo del horario semanal, con franjas del mismo día, sin duplicados ni solapes; cada pausa debe quedar contenida en una franja de atención.
- Exponer lectura, alta y retiro de bloqueos concretos; rechazar un cambio de horario, pausa o bloqueo que vuelva incompatible un turno confirmado.
- Serializar las escrituras de configuración con las escrituras de la misma agenda, releer su estado después de adquirir la coordinación y revertir la operación completa ante un rechazo.
- Mantener intactos el intervalo y la duración reservada de turnos existentes cuando cambia la duración configurada de un motivo. C-09 usará la duración configurada vigente al reprogramar.
- Ofrecer inicios en pasos de 15 minutos desde el comienzo de cada franja de atención para el contrato de disponibilidad que implementará C-07.

## Capabilities

### New Capabilities

- `availability-configuration`: administración interna de motivos, franjas semanales y bloqueos con validación de reglas, protección de turnos y escrituras coordinadas.

### Modified Capabilities

No se modifican requisitos existentes. C-06 consume `agenda-write-serialization` y `dentist-authentication` sin cambiar sus contratos.

## Impact

- API interna: `GET, POST /api/odontologo/motivos`, `PUT /api/odontologo/motivos/{motivoId}`, `GET, PUT /api/odontologo/horario` y `GET, POST /api/odontologo/bloqueos`, `DELETE /api/odontologo/bloqueos/{bloqueoId}`.
- Persistencia existente de motivos, franjas, bloqueos y turnos confirmados; no se requiere migración para las reglas elegidas.
- Consumidores posteriores: C-07 consulta motivos y configuración para calcular disponibilidad; C-12 opera las rutas internas desde la interfaz.
- Verificación con fixtures ficticios en PostgreSQL real y operaciones sincronizadas para las carreras entre reserva y cambio de configuración. La cobertura de interfaz y recorrido completo queda para C-16.
- PB-01 permanece fuera de C-06 y se resuelve en C-07/C-08/C-09.
