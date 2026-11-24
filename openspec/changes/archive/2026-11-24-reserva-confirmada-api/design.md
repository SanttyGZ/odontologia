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
- `AvailabilityRules.isSlotAvailable(...)` verifica en el `Instant` exacto (`truncatedTo(ChronoUnit.MICROS)`, rechazando solo sub-microsegundos): `inicio >= now + 24h`, `fechaLocal(inicio) <= fechaLocal(now) + 30 días`, contenido en una única franja `ATENCION` e igual a `franja.horaInicio() + k * 15 minutos` (conservando los segundos y microsegundos de `franja.horaInicio()` y rechazando cualquier desplazamiento fuera de la grilla), sin intersección con `PAUSA`, `BloqueoAgenda` ni `Turno` `CONFIRMADO`.
- Si `inicio` es nulo o tiene precisión sub-microsegundo → `400 VALIDATION_ERROR`; si falta un dato obligatorio o es inválido → `400 VALIDATION_ERROR`; si el motivo no existe → `404 MOTIVO_NOT_FOUND`; si el horario queda fuera de ventana, fuera de la grilla exacta de la franja, en pausa, bloqueado o ocupado → `409 SLOT_UNAVAILABLE`.
- Respuesta `201 Created`: `ReservaResponse(turnoId, motivoId, inicio, fin, duracionReservadaMinutos, estado, notificacionEstado)` donde `estado = "CONFIRMADO"` y `notificacionEstado = "PENDIENTE"`.

## UI Mockups

Decision: no (no UI)
