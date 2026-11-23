# Proposal

## Why

Las comunicaciones deben sobrevivir al reinicio y mantener coherencia con el turno ante repeticiones, cambios concurrentes y resultados de envío inciertos.

## What Changes

- Definir el contrato durable para confirmación, cambio y recordatorio, con revisión, destinatario y evento de origen.
- Documentar atomicidad con el turno y reglas de coordinación pendientes para duplicados, obsolescencia, reinicio y resultado SMTP incierto.
- Trazar casos de aceptación, incluido el límite exacto de 24 horas, con resultados pendientes sujetos a PB-08 a PB-11 y AT-06.

## Capabilities

### New Capabilities

- `appointment-notification-intents`: persistencia y preparación coherente de comunicaciones derivadas de turnos.

### Modified Capabilities

## Impact

Depende del contrato de material privado de C-04. C-08/C-09 originan avisos y C-11 gestiona envío real. C-16 verifica la aceptación integral. El alcance es documental. La persistencia de C-05 requiere revisión HIGH y el material privado queda bajo el contrato CRITICAL de C-04.
