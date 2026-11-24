# Design

## Context

`C-07` habilita la consulta pública de motivos y disponibilidad (`US-004`, `FL-03`) sobre el modelo de `C-02` y la configuración de `C-06`. El usuario aprobó explícitamente los bordes `PB-01` el 2026-11-24: límite inferior inclusivo (`inicio >= ahora + 24h`) y límite superior calendario inclusivo (`fechaLocal(inicio) <= fechaLocal(ahora) + 30 días` en la zona del consultorio).

## Goals / Non-Goals

**Goals:**
- Exponer `GET /api/public/motivos` y `GET /api/public/disponibilidad` con respuestas mínimas sin datos de pacientes.
- Reutilizar `AgendaQueries`, `ConsultorioQueries`, `Clock`, `AvailabilityErrorAdvice` y las reglas de intervalo ya probadas en `AvailabilityRules`.
- Garantizar determinismo mediante `Clock` inyectado y pruebas reales contra PostgreSQL 17.12.

**Non-Goals:**
- Implementar `POST /api/public/turnos` (`C-08`), credenciales (`C-04`), notificaciones (`C-05`/`C-11`) ni interfaz web (`C-13`).
- Modificar `DentistSecurityConfiguration` (`/api/public/**` ya queda fuera del `securityMatcher("/api/odontologo/**")`).

## Decisions

### D-01 — Contrato HTTP público
`GET /api/public/motivos` reutiliza `MotivoResponse(id, nombre, duracionMinutos)`.
`GET /api/public/disponibilidad` recibe `motivoId` (`UUID` obligatorio) y `desde`/`hasta` (`LocalDate` opcionales, ISO `YYYY-MM-DD`) y responde `DisponibilidadResponse(motivoId, duracionMinutos, zonaHoraria, List<HorarioDisponibleResponse(inicio, fin)>)`.

### D-02 — Ventana temporal PB-01 y cálculo por día local
Con `now = clock.instant()` y `zone = config.consultorio().zonaHoraria()`:
- `minStart = now.plus(24, ChronoUnit.HOURS)` (inclusivo).
- `maxDate = ZonedDateTime.ofInstant(now, zone).toLocalDate().plusDays(30)` (inclusivo).
- El rango de fechas locales a evaluar es `[max(desde, minStart.atZone(zone).toLocalDate()), min(hasta, maxDate)]`. Si `desde > hasta` → `400 VALIDATION_ERROR`. Si el rango efectivo queda vacío → `horarios: []`.
- Para cada fecha local y cada franja `ATENCION` de ese `DayOfWeek`, se generan candidatos desde `horaInicio` en pasos de 15 minutos mientras `candidateEnd <= franja.horaFin()`. Se filtran por `inicio >= minStart`, no intersección con `PAUSA` del día, ni con `BloqueoAgenda` ni `Turno` `CONFIRMADO` en el rango `[effectiveMinInstant, effectiveMaxInstant)`.

### D-03 — Sin bloqueo de escritura en lectura
La consulta pública no adquiere `AgendaWriteCoordinator` ni persiste registros (`RN-RE-02`).

## Risks / Trade-offs

- [Un turno reservado entre la consulta y `POST /api/public/turnos` puede invalidar una opción mostrada] → esperado por `RN-RE-02`; `C-08` revalida bajo bloqueo de agenda.

## UI Mockups

Decision: no (no UI)
