# Design

## Context

Ver `proposal.md` para la motivación y `specs/appointment-self-management/spec.md` para el contrato observable. C-04 persiste la dirección confirmada en `recuperacion_enlace` con una secuencia `orden`; la validación de credenciales ya entrega a C-09 un resultado interno y opaco. C-05 guarda destinatario, referencia de credencial e historial de intentos por separado del paciente.

## Goals / Non-Goals

**Goals:**

- Elegir para avisos nuevos el destinatario de la recuperación más reciente, sin editar `paciente.email` ni reescribir intentos existentes.
- No producir una nueva revisión ni avisos cuando inicio y fin calculados coincidan con el turno actual.
- Proponer una consulta de disponibilidad para C-14 limitada al motivo del turno autorizado, pendiente de aprobación CRITICAL.

**Non-Goals:**

- Cambiar la autenticación, emisión, revocación, recuperación o vigencia de C-04.
- Cambiar el contrato actual de `GET /api/gestion/turno` o su respuesta visible.
- Implementar la ruta candidata de C-14 antes de su aprobación CRITICAL, decidir el contacto operativo de PB-06, ampliar la autogestión ni enviar SMTP.

## Decisions

### D-01 — Pasar el destinatario recuperado solo por el resultado interno autorizado

Extender el resultado interno de validación de C-04 con el destinatario de la recuperación más reciente, consultado por `recuperacion_enlace.orden DESC`. C-09 usará ese valor para avisos nuevos y recurrirá al email vigente del paciente solo si no existe recuperación. No habrá escritura sobre el paciente ni actualización retroactiva de intenciones o intentos. Esto conserva la responsabilidad de C-04 por sus datos y evita leer su tabla privada desde C-09. La credencial, el token y la respuesta HTTP no incorporan ni exponen el email.

**Aprobación CRITICAL:** el usuario autorizó esta ampliación acotada el 2026-11-25 al pedir terminar C-09 después de identificarle expresamente el alcance pendiente. La autorización cubre que el servicio interno reciba el destinatario confirmado para preparar avisos futuros; no amplía las respuestas HTTP, el mecanismo de credenciales ni la política de recuperación.

**Alternativa descartada:** consultar `recuperacion_enlace` directamente desde el servicio de C-09, porque acopla la autogestión al esquema privado de C-04. Tampoco se reutilizarán los destinatarios de avisos previos, porque podrían ser anteriores a una recuperación.

### D-02 — Rechazar el mismo intervalo después de calcular la duración vigente

Dentro de la coordinación de escritura, calcular `nuevoFin` desde el motivo y duración configurados actualmente. Si `nuevoInicio == turno.inicio` y `nuevoFin == turno.fin`, responder `409 SAME_INTERVAL` antes de actualizar el turno o preparar avisos. Si solo coincide el inicio pero la duración vigente produce otro fin, no aplicar este rechazo: siguen rigiendo las reglas actuales de disponibilidad.

**Alternativa descartada:** comparar solo el inicio o usar `SLOT_UNAVAILABLE`, porque el primero ignora cambios de duración y el segundo oculta el caso de intervalo idéntico.

### D-03 — Conservar el enlace vigente en avisos de reprogramación

Mantener la referencia de credencial obtenida por C-04 durante la autorización de la operación y pasarla a C-05 para las intenciones `CAMBIO` y `RECORDATORIO` nuevas. No cambiar la política de credenciales ni las referencias de avisos históricos.

### D-04 — Contrato candidato de disponibilidad C-14, sujeto a aprobación CRITICAL

La interfaz necesita horarios del motivo asociado al turno del enlace. El contrato candidato es:

```http
GET /api/gestion/turno/disponibilidad?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
X-Management-Token: <credencial vigente>
```

`desde` y `hasta` serían opcionales y seguirían los valores por defecto, límites inclusivos y formato de C-07 (`GET /api/public/disponibilidad`). La API obtendría internamente `motivoId` del único turno autorizado y delegaría el cálculo a la capacidad de C-07. Respondería `200 OK` con `Cache-Control: no-store` y `{ "zonaHoraria": "...", "horarios": [{"inicio":"...", "fin":"..."}] }`; un rango válido sin opciones devolvería `horarios: []`. No aceptaría ni devolvería `motivoId`, ni revelaría datos de turno distintos de disponibilidad. Un enlace inválido o vencido produciría el `404 LINK_NOT_AVAILABLE` genérico de C-09.

La ruta amplía la lectura asociada a la credencial de C-04 aunque la limite al motivo de ese turno. Por eso esta propuesta no autoriza su implementación: requiere aprobación CRITICAL explícita antes de modificar código o publicar una delta de spec para C-14. Mientras tanto C-14 no tiene una fuente autorizada para cargar horarios; PB-06 también sigue pendiente hasta acordar el contacto operativo con el odontólogo.

**Alternativa descartada:** agregar `motivoId` al cuerpo visible de `GET /api/gestion/turno` o llamar C-07 con un `motivoId` elegido por el cliente; la primera amplía los datos visibles y la segunda permitiría consultar motivos ajenos al turno autorizado.

### D-05 — Aislar la verificación PostgreSQL del volumen local

Permitir que `TestDatabase` reciba `ODONTOLOGIA_TEST_DATABASE_PORT`, con `5433` como valor predeterminado, para ejecutar la integración en un contenedor PostgreSQL 17.12 efímero, con un puerto dedicado y sin volumen. Así las pruebas pueden crear y eliminar sus bases propias sin detener ni cambiar credenciales del servicio persistente local.

**Alternativa descartada:** reutilizar la base persistente del proyecto cuando su contraseña local no está disponible o detenerla para reclamar el puerto `5433`, porque ambas opciones alteran o interrumpen el estado del usuario.

## Risks / Trade-offs

- [Riesgo] Una recuperación más reciente cambia el destinatario de avisos futuros → seleccionar por la secuencia persistida de C-04 y no modificar datos del paciente ni intentos previos.
- [Riesgo] El nuevo código de conflicto puede requerir manejo específico de C-14 → documentar `409 SAME_INTERVAL`; el turno y los avisos permanecen intactos.
- [Riesgo] C-14 no puede consultar disponibilidad hasta aprobar la ampliación protegida → dejar contrato y decisión visibles, sin simular que ya existe un endpoint.

## Migration Plan

No hay migración de base de datos. Se reutilizan la secuencia y los registros existentes de C-04/C-05. C-14 conserva su implementación pendiente hasta obtener la aprobación indicada en D-04.

## UI Mockups

Decision: no (no UI).
