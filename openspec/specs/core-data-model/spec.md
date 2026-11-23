# core-data-model Specification

## Purpose
Conservar el modelo persistente de un consultorio y su agenda con relaciones coherentes, intervalos temporales definidos e inicialización que respete los datos existentes.

## Requirements

### Requirement: CM-01 Configuración única relacionada

El sistema SHALL persistir como máximo un consultorio, un odontólogo asociado y una agenda asociada, identificados por UUID no nulos y relaciones obligatorias. La configuración necesaria SHALL estar completa antes de habilitar operaciones de agenda.

#### Scenario: Intento de duplicar la configuración
- **WHEN** se intenta crear otro consultorio, otro odontólogo para el consultorio o una segunda agenda para el odontólogo
- **THEN** la base rechaza el duplicado y conserva la configuración original.

#### Scenario: Referencia inexistente
- **WHEN** se intenta persistir una agenda o profesional con una referencia obligatoria inexistente
- **THEN** la base rechaza la operación sin dejar registros huérfanos.

### Requirement: CM-02 Configuración de disponibilidad íntegra

El sistema SHALL persistir motivos con nombre no vacío y duración entera positiva, franjas con día de lunes 1 a domingo 7, horas locales inicio anterior al fin y tipo ATENCION o PAUSA, y bloqueos con instantes inicio anterior al fin. Todos SHALL referenciar una agenda existente.

#### Scenario: Valores estructurales inválidos
- **WHEN** se intenta guardar un motivo sin nombre o con duración no positiva, una franja con día o tipo inválido o intervalo invertido, o un bloqueo sin intervalo positivo
- **THEN** la base rechaza la escritura y conserva el estado anterior.

#### Scenario: Disponibilidad sin efectos implícitos en turnos
- **WHEN** una prueba modifica la duración de un motivo o incorpora un bloqueo mediante el protocolo de escritura
- **THEN** los intervalos, estados y duraciones reservadas de los turnos existentes no cambian automáticamente.

### Requirement: CM-03 Datos básicos sin identidad natural implícita

El sistema SHALL guardar pacientes con UUID, consultorio válido y nombre, apellido, email, teléfono y DNI obligatorios y no vacíos. La persistencia SHALL admitir coincidencias de DNI, email o teléfono entre registros y no deducir de ellas fusión, sobrescritura ni autorización de consulta.

#### Scenario: Dos registros con datos coincidentes
- **WHEN** se guardan dos pacientes ficticios con UUID distintos y el mismo DNI o email
- **THEN** ambos pueden persistirse sin modificar el registro previo.

#### Scenario: Datos obligatorios ausentes
- **WHEN** se intenta persistir un paciente sin uno de sus cinco datos básicos o sin consultorio válido
- **THEN** la escritura se rechaza sin crear un registro incompleto.

### Requirement: CM-04 Turno coherente y referencias válidas

El sistema SHALL persistir turnos con agenda, paciente y motivo válidos del mismo contexto, inicio anterior al fin, duración positiva coherente con el intervalo, revisión no negativa y creación explícita. Los únicos estados SHALL ser CONFIRMADO y CANCELADO; cancelado_en SHALL existir únicamente para CANCELADO.

#### Scenario: Referencias o atributos incoherentes
- **WHEN** se intenta guardar un turno con referencias inválidas, motivo de otra agenda, duración distinta del intervalo, revisión negativa o cancelación incoherente con el estado
- **THEN** la base rechaza la operación completa.

#### Scenario: Cambio del motivo posterior a la reserva
- **WHEN** cambia la duración configurada de un motivo con un turno ya persistido
- **THEN** el turno conserva su duración reservada, inicio y fin.

### Requirement: CM-05 Ocupación de turnos sin superposición

La base SHALL impedir que coexistan turnos CONFIRMADO de la misma agenda cuyos intervalos se superpongan, incluso ante escrituras concurrentes. La ocupación SHALL usar [inicio, fin), permitir intervalos contiguos y excluir los turnos CANCELADO.

#### Scenario: Intervalos iguales o parcialmente superpuestos
- **WHEN** dos transacciones sincronizadas intentan confirmar intervalos iguales o parcialmente superpuestos en una agenda
- **THEN** como máximo una confirma y la otra revierte su escritura.

#### Scenario: Intervalos contiguos
- **WHEN** un turno confirmado comienza exactamente al finalizar otro
- **THEN** ambos pueden coexistir sin conflicto de superposición.

#### Scenario: Intervalo de un turno cancelado
- **WHEN** se confirma un turno en un intervalo ocupado únicamente por un turno CANCELADO
- **THEN** el cancelado no impide persistir el nuevo confirmado.

### Requirement: CM-06 Tiempo explícito y comprobable

El sistema SHALL conservar turnos y bloqueos como instantes, horarios semanales como horas locales y la zona IANA del consultorio por separado. El tiempo actual SHALL poder fijarse en pruebas sin depender del reloj o zona del equipo. Persistir estos datos no SHALL establecer por inferencia límites de reserva ni pasos de horarios.

#### Scenario: Independencia de la zona del equipo
- **WHEN** se guarda y recupera un turno con un desplazamiento horario y se cambia la zona predeterminada de la JVM
- **THEN** se conserva el mismo instante y la zona explícita del consultorio.

#### Scenario: Reloj de prueba fijo
- **WHEN** una prueba sustituye la fuente de tiempo por un instante fijo
- **THEN** el código que consulta el tiempo obtiene ese instante de manera repetible.

### Requirement: CM-07 Inicialización idempotente y sin sobrescritura

La inicialización SHALL completar consultorio, odontólogo y agenda en una transacción usando nombres y zona explícitos revisados. Repetirla o ejecutarla simultáneamente SHALL conservar identificadores y registros existentes. Una configuración incompatible SHALL producir un error sin sobrescribirla ni dejar inicialización parcial.

#### Scenario: Instalación vacía e inicializaciones simultáneas
- **WHEN** dos inicializaciones con la misma configuración operan sobre una base vacía
- **THEN** queda exactamente un consultorio, un profesional y una agenda relacionados con los mismos identificadores observables por ambos procesos.

#### Scenario: Reinicio con datos existentes
- **WHEN** se reinicia la aplicación y se repite la inicialización sobre una base con configuración y datos ficticios guardados
- **THEN** conserva identificadores, nombres, zona, pacientes, configuración de disponibilidad y turnos, sin duplicar ni reemplazar registros.

#### Scenario: Configuración incompatible o fallo parcial
- **WHEN** los valores iniciales difieren de la configuración persistida o falla la creación de una relación necesaria
- **THEN** el arranque falla de forma identificable y la inicialización no confirma cambios parciales.

### Requirement: CM-08 Ejemplos optativos y conservación de relaciones

El arranque SHALL evitar crear pacientes, turnos, motivos, horarios o bloqueos de ejemplo automáticamente. El sistema SHALL conservar registros referenciados sin borrado en cascada implícito y no crear credenciales ni notificaciones como efecto de inicializar el modelo básico.

#### Scenario: Arranque normal
- **WHEN** se inicializa una instalación vacía con configuración explícita
- **THEN** solo se crean consultorio, odontólogo y agenda; los ejemplos y datos de otros changes permanecen ausentes.

#### Scenario: Eliminación de un registro referenciado
- **WHEN** se intenta eliminar un motivo o paciente que tiene turnos asociados
- **THEN** la base rechaza el borrado y conserva los turnos y sus relaciones.
