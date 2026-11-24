# Design

## Context

`C-08` implementa `POST /api/public/turnos` (`US-005`, `FL-04`) reutilizando `AgendaWriteCoordinator` (`C-02`), `AvailabilityRules` (`C-06`/`C-07`), `CredencialesGestion` (`C-04`) y `PreparacionAvisos` (`C-05`), bajo las decisiones explícitas `PB-04`, `PB-08` y `PB-10` aprobadas por el usuario el 2026-11-24.

## Goals / Non-Goals

**Goals:**
- Confirmar reservas en una única transacción coordinada (`Paciente` + `Turno` + `CredencialGestion` + `Origen/IntencionNotificacion`), con rollback total ante fallo.
- Probar concurrencia real con hilos sincronizados contra PostgreSQL 17.12 (`CT-01`), incluyendo intervalos iguales, parcialmente solapados y carrera contra cambios de horario/bloqueo.
- No exponer secretos de credencial ni datos previos de pacientes en la respuesta HTTP (`CT-03`).

**Non-Goals:**
- Envío SMTP (`C-11`), autogestión (`C-09`), agenda interna (`C-10`) o pantallas React (`C-13`).

## Decisions

### D-01 — Validación y normalización de paciente (`PB-04`)
- `nombre`, `apellido`: obligatorios no vacíos tras `trim()`, longitud 1..100.
- `email`: obligatorio tras `trim()`, minúsculas para almacenamiento/comparación, formato básico `local@dominio.tld`, longitud <= 254.
- `telefono`: obligatorio tras `trim()`, longitud 6..30, solo dígitos y separadores telefónicos habituales (`+`, `-`, espacio, paréntesis) con al menos 6 dígitos.
- `dni`: obligatorio tras `trim()`, longitud 6..20, alfanumérico sin espacios internos.
- Búsqueda exacta en `PacienteQueries.findExact(...)` por `(consultorio_id, nombre, apellido, email, telefono, dni)`; si no hay coincidencia exacta, `PacienteQueries.insert(...)` crea un registro nuevo.

### D-02 — Regla de intervalo exacto y respuesta (`PB-08`, `PB-10`)
- `AvailabilityRules.isSlotAvailable(...)` verifica en el `Instant` exacto (`truncatedTo(ChronoUnit.MICROS)`): `inicio` con segundos/nanos en cero, `inicio >= now + 24h`, `fechaLocal(inicio) <= fechaLocal(now) + 30 días`, contenido en una única franja `ATENCION` con paso de 15 minutos desde `franja.horaInicio()`, sin intersección con `PAUSA`, `BloqueoAgenda` ni `Turno` `CONFIRMADO`.
- Si falla la validación de formato/rango de `inicio` (p. ej. segundos != 0 o fuera de paso de 15 min) → `400 VALIDATION_ERROR` o `409 SLOT_UNAVAILABLE` si el horario no está disponible; específicamente: datos faltantes/inválidos → `400 VALIDATION_ERROR`; motivo inexistente → `404 MOTIVO_NOT_FOUND`; horario fuera de ventana, fuera de franja, en pausa, bloqueado o ocupado → `409 SLOT_UNAVAILABLE`.
- Respuesta `201 Created`: `ReservaResponse(turnoId, motivoId, inicio, fin, duracionReservadaMinutos, estado, notificacionEstado)` donde `estado = "CONFIRMADO"` y `notificacionEstado = "PENDIENTE"`.

## UI Mockups

Decision: no (no UI)
