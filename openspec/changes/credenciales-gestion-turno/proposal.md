# Proposal

## Why

La reserva sin cuenta requiere un medio de autogestión privado por turno. C-05 necesita un contrato para componer enlaces válidos a partir de avisos persistidos tras reiniciar.

## What Changes

- Documentar emisión, validación, invalidación y composición autorizada de enlaces, incluida la recuperación del material privado.
- Trazar escenarios para acceso limitado, previsualización, vigencia, recuperación y reinicio.
- Elevar PB-07 y AT-04 a revisión, junto con el límite compartido con C-05 para PB-10/PB-11.

## Capabilities

### New Capabilities

- `appointment-management-credentials`: autorización de autogestión por turno y composición interna de enlaces para avisos.

### Modified Capabilities

## Impact

C-04 depende de C-02 y su coordinación de escrituras. C-05 consume este contrato. C-08 y C-09 integran las operaciones de turno, C-11 compone los correos y C-14 integra el navegador. El diseño está pendiente de revisión y el código CRITICAL requiere autorización explícita separada. Esta entrega es documental.
