# Proposal

## Why

C-01 dejó el entorno local reproducible y la conexión a PostgreSQL, pero todavía no existe un modelo persistente de agenda. C-02 establece las relaciones e invariantes comunes que necesitan C-03 y C-04 y las escrituras posteriores de disponibilidad y turnos.

## What Changes

- Materializar las ocho entidades aprobadas en KB 04: Consultorio, Odontologo, Agenda, MotivoConsulta, FranjaSemanal, BloqueoAgenda, Paciente y Turno, con UUID, claves foráneas e índices revisados.
- Incorporar migraciones versionadas con Flyway y acceso explícito mediante Spring JDBC, aprovechando las dependencias existentes de C-01.
- Inicializar un solo consultorio, profesional y agenda sin recrear ni sobrescribir datos al reiniciar. La configuración de demostración usa Grupo13, Carlos Garcia y la zona IANA documentada en el diseño. Los motivos, horarios, pacientes y turnos de prueba se crean solo mediante fixtures aislados.
- Conservar duración e intervalo reservados, estados CONFIRMADO/CANCELADO, revisión y coherencia de cancelación. Proponer una defensa en PostgreSQL contra turnos confirmados superpuestos.
- Establecer un protocolo transaccional común de escritura por agenda, con lecturas posteriores al bloqueo, espera finita y rollback completo; los flujos funcionales lo consumirán en sus changes.
- Incorporar reloj inyectable y separar instantes, horas semanales locales y zona IANA persistida. No decidir los bordes de 24 horas/30 días ni el paso de los horarios.
- Mantener los datos de paciente sin unicidad por DNI/email/teléfono ni fusión automática. La propuesta de registro nuevo por reserva queda pendiente de decisión PB-04.
- Evolucionar el contrato de arranque de C-01: permitir únicamente migraciones e inicialización revisadas y preservar registros existentes. Actualizar la comprobación que hoy exige ausencia de Flyway.

## Capabilities

### New Capabilities

- `core-data-model`: integridad del modelo de consultorio, disponibilidad, pacientes y turnos, tratamiento temporal e inicialización persistente.
- `agenda-write-serialization`: coordinación transaccional compartida de las escrituras que afectan disponibilidad y conservación del estado ante conflictos.

### Modified Capabilities

- `local-runtime`: LR-03 permite la evolución versionada e inicialización aprobadas desde C-02; LR-06 exige coherencia entre zona configurada y persistida y evita persistir por omisión el fixture Etc/UTC de C-01.

## Impact

Se prevén cambios en `backend/src/main/resources/`, paquetes Java bajo `dev.odontologia`, pruebas unitarias/integración, `scripts/with-local-db.py`, `.env.example` y README. El esquema utilizará PostgreSQL 17.12 real; la defensa de exclusión propuesta requiere revisar la extensión `btree_gist`. No se propone añadir ORM ni infraestructura externa.

Fuera de alcance: APIs de reserva/configuración/autogestión, cálculo completo de disponibilidad, credenciales y autenticación (C-03/C-04), notificaciones/SMTP (C-05/C-11), interfaz, datos reales, borrados o restauraciones. Estas capacidades no quedan aceptadas por disponer de tablas o fixtures.

El usuario aprobó el diseño HIGH el 2026-11-22. La sección de revisión del diseño registra la configuración inicial y el estado del contrato de evidencia para C-02.
