# Proposal

## Why

En la revisión de `C-15` (`agenda-odontologo-web`) se detectaron cuatro puntos a corregir y verificar antes del cierre definitivo:
1. El detalle de turno permanece abierto o puede reabrirse por respuestas demoradas al cambiar de período o cerrar manualmente.
2. Nombres, apellidos o motivos largos sin espacios desbordan horizontalmente la lista de agenda.
3. El campo «Revisión» se oculta cuando vale `0` en turnos recién reservados.
4. `CHANGES.md` requiere resolver el conflicto con `upstream/main` preservando el cierre de `C-09` y actualizando el registro real de verificación de `C-15`.

## What Changes

- Sincronizar el panel de detalle con el período consultado en `AgendaSection.tsx`: cerrar el detalle si el turno queda fuera del nuevo período tras cambiar fecha o alternar `Día`/`Semana`, si el período está vacío o si falla la consulta; invalidar respuestas demoradas tras cambio de período o cierre manual; conservar el comportamiento de «Actualizar» cuando el turno sigue dentro del período.
- Corregir el desbordamiento horizontal en la lista de agenda (`styles.css`) para que nombres, apellidos (ej. 100 caracteres sin espacios) y motivos largos se partan en varias líneas sin ocultar texto ni controles, verificándolo en navegador Chromium real a `390×844` y `1440×900` con detalle abierto y cerrado y navegación por teclado.
- Mostrar siempre el campo «Revisión» en el detalle del turno, incluido el valor `0` (turno recién reservado), manteniendo el estado «Confirmado» con la revisión actualizada en turnos reprogramados.
- Agregar pruebas de regresión en `AgendaSection.test.tsx` para detalle obsoleto, respuesta demorada y revisión `0`.
- Integrar `upstream/main` en `sebastian/c15`, resolver el conflicto de `CHANGES.md` preservando el cierre de `C-09` y demás actualizaciones vigentes, y actualizar el estado y verificación de `C-15` según lo realmente ejecutado.
- **Fuera de alcance**: cambios en backend, seguridad o esquema PostgreSQL; edición de pacientes o acciones manuales pendientes (`PB-06`); cambios en `C-12` o `C-13`.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- dentist-agenda-web: precisar el cierre de detalle obsoleto e invalidación de respuestas demoradas al cambiar de período o cerrar manualmente, la visibilidad permanente de `revision` (incluido `0`) y el corte de textos largos sin desbordamiento horizontal en `390×844` y `1440×900`.

## Impact

- Archivos afectados en `apply`: `frontend/src/components/AgendaSection.tsx`, `frontend/src/components/AgendaSection.test.tsx`, `frontend/src/styles.css` y `CHANGES.md` (preservando `C-09`).
- Gobernanza: **BAJO** (reutiliza el acceso existente sin tocar autenticación ni autorización).
