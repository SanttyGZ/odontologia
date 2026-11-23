# Spec Delta

## MODIFIED Requirements

### Requirement: LR-03 Conservación del estado existente

Desde C-02, el arranque SHALL aplicar únicamente migraciones versionadas e inicialización revisadas, sin recrear el esquema, borrar registros ni sobrescribir configuración existente. Repetir el arranque SHALL conservar los datos y no reaplicar migraciones ya registradas. Un esquema no reconocido o una migración incompatible SHALL producir un error sin limpieza, reparación ni adopción automática.

#### Scenario: Comprobaciones repetidas
- **WHEN** se repiten el arranque y la comprobación de conexión sobre una base ya migrada e inicializada
- **THEN** no se reaplican migraciones y el esquema y los registros existentes permanecen sin cambios.

#### Scenario: Primera instalación revisada
- **WHEN** se inicia sobre una base vacía explícitamente habilitada con la configuración inicial acordada
- **THEN** se aplican las migraciones versionadas y se inicializa la configuración necesaria sin cargar ejemplos optativos.

#### Scenario: Esquema no reconocido o migración incompatible
- **WHEN** el arranque encuentra un esquema no reconocido o una migración cuyo historial no coincide con los archivos vigentes
- **THEN** informa el fallo y no limpia, repara ni sobrescribe automáticamente el estado previo.

### Requirement: LR-06 Zona horaria sin inferencias

El contrato local SHALL exigir una zona IANA explícita para el tiempo del consultorio, sin derivarla del equipo ni adoptar un fixture de prueba por omisión. La zona configurada SHALL coincidir con la persistida; una diferencia SHALL impedir completar el arranque sin modificar la zona existente. Las zonas de pruebas aisladas SHALL identificarse como fixtures y no aprobar la zona del consultorio.

#### Scenario: Zona de prueba explícita
- **WHEN** una comprobación aislada utiliza una zona de prueba indicada por el ejecutor
- **THEN** su resultado identifica el contexto de prueba y no crea configuración persistida en la instalación del consultorio.

#### Scenario: Zona inválida o ausente
- **WHEN** el arranque o comprobación exige una zona y su configuración está ausente o no es un identificador IANA válido
- **THEN** se informa el error sin sustituirla silenciosamente por la zona del sistema ni por Etc/UTC.

#### Scenario: Diferencia respecto de la zona persistida
- **WHEN** el arranque recibe una zona válida diferente de la del consultorio ya inicializado
- **THEN** falla de forma identificable y conserva la zona persistida y los intervalos de todos los registros.
