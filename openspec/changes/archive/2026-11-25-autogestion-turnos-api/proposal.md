# Proposal

## Why

El paciente necesita consultar y gestionar su turno desde el enlace privado sin crear una cuenta ni depender de una intervención manual dentro del plazo permitido. La API debe mantener la reserva y las intenciones de aviso coherentes cuando una operación compite con otra o falla.

## What Changes

- Definir una consulta privada y limitada al turno autorizado, sin revelar datos de contacto ni modificar el turno al abrir o previsualizar el enlace.
- Definir las escrituras expresas de cancelación y reprogramación, con plazo, disponibilidad y revisión vigentes; una reprogramación fallida conserva el turno original.
- Coordinar los cambios confirmados con las intenciones de aviso existentes y evitar que una solicitud repetida por una credencial revocada revele datos o genere otro aviso (PB-08, decisión 3B).
- Implementar y verificar C-09 usando las capacidades existentes de disponibilidad, escritura por agenda y avisos. Extender de forma interna y acotada el resultado de validación de C-04 para incluir el UUID opaco de la credencial vigente que necesita C-05; no cambia el secreto ni el contrato público de credenciales.

## Capabilities

### New Capabilities

- `appointment-self-management`: consulta limitada, cancelación y reprogramación de un turno mediante su enlace privado.

### Modified Capabilities

Ninguna en los requisitos públicos. La implementación interna de C-04 amplía `TurnoAutorizado` con la referencia opaca de la credencial vigente para asociar los avisos de reprogramación de C-05; no cambian las políticas de credenciales ni de avisos.

## Impact

- API backend: `GET /api/gestion/turno`, `POST /api/gestion/turno/cancelacion` y `POST /api/gestion/turno/reprogramacion`.
- Dominio de turnos, coordinación de escritura por agenda, validación de disponibilidad y preparación de avisos C-05.
- Criterios US-007/US-008/US-009, FL-05/FL-06/FL-07, CT-01 y PB-01/PB-06/PB-07/PB-08/PB-10/PB-11.
- Sin cambios de esquema ni al secreto, autenticación, emisión, reglas de validación, transporte, revocación, criptografía o recuperación de C-04. La referencia opaca solo se entrega al consumidor interno de la validación.
