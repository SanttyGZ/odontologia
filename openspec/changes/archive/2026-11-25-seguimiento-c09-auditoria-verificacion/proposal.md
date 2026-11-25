# Proposal

## Why

El change archivado de correcciones de C-09 dejó resultados y pendientes relevantes, pero una revisión posterior debe distinguir el alcance de la aprobación CRITICAL, la decisión de PB-06 y la evidencia realmente disponible en Windows. La especificación vigente ya exige conservar el turno original cuando se pierde una disputa por el horario; falta dejar claro qué comprobación local puede acreditarse.

## What Changes

- Registrar que la aprobación CRITICAL documentada para `CredencialesGestion.validar` cubre únicamente el UUID opaco; la exposición del email confirmado de recuperación queda pendiente de revisión humana, sin modificar código de credenciales.
- Registrar que Fernando confirmó, según lo transmitido por Santiago el 2026-11-25, que PB-06 puede quedar para C-14. El canal operativo continúa pendiente de acuerdo antes de C-14 y C-09 conserva `409 MANAGEMENT_WINDOW_CLOSED`.
- Alinear el estado de C-09 en `CHANGES.md` con los límites de aprobación y evidencia. Conservar C-10 como pendiente en esta rama, según la instrucción de Santiago, y conservar el cierre documentado de C-12.
- Corregir los espacios finales del `verification.md` archivado y registrar la verificación Windows actual, incluidas las limitaciones de PostgreSQL y el uso de dobles de C-04.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna. `appointment-self-management` ya requiere conservar el turno original ante una disputa por el horario (AG-05). Este seguimiento actualiza documentación y evidencia; no cambia el comportamiento requerido.

## Impact

Solo documentación y evidencias de OpenSpec/roadmap: `CHANGES.md`, `knowledge-base/10_preguntas_abiertas.md` y el informe archivado de verificación de C-09. No se modifican rutas, credenciales, datos visibles ni controles de seguridad.
