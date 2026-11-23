# agenda-write-serialization Specification

## Purpose
Definir un contrato compartido para coordinar cambios de disponibilidad y turnos, evitando decisiones sobre datos anteriores a una escritura concurrente y resultados parcialmente confirmados.

## Requirements

### Requirement: AW-01 Coordinación común por agenda

Toda operación que altere motivos, horario, pausas, bloqueos u ocupación de turnos SHALL participar en una coordinación transaccional común por agenda. Una segunda escritura SHALL esperar o fallar de forma identificable sin decidir sobre disponibilidad mientras la primera conserva la coordinación.

#### Scenario: Dos escrituras de la misma agenda
- **WHEN** una escritura conserva la coordinación y otra intenta modificar disponibilidad de la misma agenda
- **THEN** la segunda no inicia su validación dependiente del estado hasta obtener la coordinación o informa que no pudo obtenerla.

### Requirement: AW-02 Validación con estado vigente

Después de obtener la coordinación, la escritura SHALL consultar nuevamente los datos persistidos que condicionan su decisión. Ninguna consulta previa de disponibilidad SHALL equivaler a una reserva ni sustituir esa validación; los datos leídos antes de esperar SHALL descartarse para decidir.

#### Scenario: Cambio confirmado durante la espera
- **WHEN** la primera transacción modifica ocupación o configuración y confirma mientras otra espera
- **THEN** la segunda valida utilizando el cambio confirmado antes de persistir su resultado.

#### Scenario: Cambio revertido durante la espera
- **WHEN** la primera transacción revierte y libera la coordinación
- **THEN** la segunda valida contra el estado persistido sin observar cambios revertidos.

### Requirement: AW-03 Atomicidad y conservación del original

Una escritura coordinada SHALL confirmar todos sus cambios de base en una sola transacción o revertirlos completamente. Un conflicto o fallo de reprogramación SHALL conservar identificador, intervalo, revisión y estado originales. La coordinación SHALL liberarse al confirmar o revertir.

#### Scenario: Fallo después de modificar varios registros
- **WHEN** una escritura cambia un turno y otro registro relacionado y luego ocurre un error antes de confirmar
- **THEN** ninguno de sus cambios queda persistido y una operación posterior puede adquirir la coordinación.

#### Scenario: Destino de reprogramación en conflicto
- **WHEN** una prueba intenta mover un turno a un intervalo confirmado ocupado
- **THEN** se rechaza el cambio y se conserva íntegra la reserva original.

### Requirement: AW-04 Conflictos y esperas finitas

La coordinación SHALL limitar las esperas por bloqueos y distinguir una agenda inexistente, una espera agotada, un conflicto de integridad y un error técnico. Ningún resultado fallido SHALL presentarse como escritura confirmada ni provocar un reintento automático de una operación de negocio.

#### Scenario: Espera agotada
- **WHEN** otra transacción retiene la coordinación más que el límite configurado
- **THEN** la operación termina con un conflicto de espera identificable, revierte todos sus cambios y no queda esperando indefinidamente.

#### Scenario: Agenda inexistente
- **WHEN** se solicita una escritura coordinada para un UUID de agenda inexistente
- **THEN** la operación informa la ausencia y no ejecuta cambios ni simula una coordinación exitosa.

### Requirement: AW-05 Límite transaccional sin servicios externos

La transacción coordinada SHALL limitarse a trabajo de base y validaciones locales; no SHALL conservar bloqueos mientras espera SMTP, servicios externos o intervención humana. Los cambios posteriores que agreguen credenciales o intenciones durables SHALL poder participar en la misma transacción sin confirmar por separado.

#### Scenario: Composición con otra escritura persistente
- **WHEN** un consumidor ejecuta dos escrituras de base dentro de la operación coordinada y la segunda falla
- **THEN** ambas se revierten y no queda confirmada por separado la primera.

#### Scenario: Uso del contrato por los changes siguientes
- **WHEN** se revisa la integración de configuración, reserva o autogestión con el contrato
- **THEN** las llamadas externas quedan fuera del período de bloqueo y los cambios persistentes relacionados comparten confirmación o rollback.
