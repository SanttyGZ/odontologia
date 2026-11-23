# Spec Delta

## Purpose

Definir una ejecución local explícita y comprobable para desarrollar el sistema y conectar el backend a PostgreSQL de prueba preservando datos y decisiones pendientes del dominio.

## ADDED Requirements

### Requirement: LR-01 Configuración local explícita

La aplicación SHALL obtener de configuración explícita la URL base, direcciones de escucha, puertos y conexión a PostgreSQL. Los servidores de desarrollo SHALL escuchar únicamente en loopback por defecto y fallar si el puerto elegido está ocupado, sin escoger otro silenciosamente.

#### Scenario: Arranque con configuración local válida
- **WHEN** se inicia el entorno con la configuración local documentada y sus puertos disponibles
- **THEN** los procesos escuchan en las direcciones y puertos acordados y la URL de navegación coincide con el modo de ejecución documentado.

#### Scenario: Puerto ocupado
- **WHEN** otro proceso ocupa el puerto configurado del backend o frontend
- **THEN** el arranque correspondiente informa el conflicto sin cambiar automáticamente a un puerto diferente.

### Requirement: LR-02 Conexión PostgreSQL comprobable

El backend SHALL permitir comprobar su arranque y conexión contra PostgreSQL real mediante una consulta de solo lectura. Una conexión inválida o indisponible SHALL producir un resultado fallido identificable y nunca sustituirse por una base en memoria.

#### Scenario: Base de prueba accesible
- **WHEN** se ejecuta la comprobación documentada con credenciales y una base de prueba explícitamente habilitadas
- **THEN** se abre una conexión real, se ejecuta una consulta de solo lectura y se informa un resultado exitoso sin registrar las credenciales.

#### Scenario: Base inaccesible
- **WHEN** la base configurada no está disponible o rechaza la conexión
- **THEN** la comprobación termina como fallida dentro de un tiempo finito e identifica el problema sin exponer secretos.

### Requirement: LR-03 Conservación del estado existente

En C-01, el arranque y las comprobaciones SHALL evitar crear, eliminar o poblar tablas, migrar esquemas o modificar registros existentes. La preparación de migraciones de negocio SHALL quedar para C-02, después de revisar su esquema e inicialización.

#### Scenario: Comprobaciones repetidas
- **WHEN** se repiten el arranque y la comprobación de conexión de C-01 sobre la misma base de prueba
- **THEN** el esquema y sus datos permanecen sin cambios, incluida la ausencia de inicialización automática de metadatos de migraciones.

### Requirement: LR-04 Proxy de desarrollo

Durante el desarrollo, el frontend SHALL reenviar las solicitudes de `/api` al backend local configurado y conservar su ruta y respuesta. SHALL permitir observar la indisponibilidad del backend sin presentar una página HTML o datos ficticios como una respuesta exitosa de API.

#### Scenario: Solicitud reenviada
- **WHEN** el navegador solicita una ruta `/api` desde el origen de desarrollo y el backend está disponible
- **THEN** el backend recibe esa ruta y el navegador recibe su estado y respuesta a través del proxy, incluso si la ruta todavía responde 404 en C-01.

#### Scenario: Backend detenido
- **WHEN** se solicita una ruta `/api` mientras el backend está detenido
- **THEN** el fallo de conexión resulta observable y no se convierte en una respuesta exitosa del frontend.

### Requirement: LR-05 Configuración sin secretos versionados

El proyecto SHALL proporcionar una plantilla documentada sin credenciales reales y excluir los valores locales sensibles del control de versiones. La ausencia de credenciales requeridas SHALL impedir presentar como exitosa una comprobación de conexión; no SHALL existir una contraseña predeterminada de la aplicación.

#### Scenario: Configuración compartida con el equipo
- **WHEN** un integrante consulta la plantilla y documentación versionadas
- **THEN** identifica qué valores aportar localmente y cómo cargarlos sin encontrar credenciales reales ni valores sensibles en ejemplos o registros.

#### Scenario: Falta una credencial requerida
- **WHEN** se ejecuta la comprobación sin la configuración de conexión requerida
- **THEN** se informa el dato de configuración ausente, sin conectarse con una identidad predeterminada implícita.

### Requirement: LR-06 Zona horaria sin inferencias

El contrato local SHALL exigir una zona IANA explícita cuando se configure el tiempo del consultorio; no SHALL derivarla de la computadora. En C-01, una zona usada solo como fixture de prueba SHALL identificarse como tal y no persistirse ni aprobarse como zona del consultorio.

#### Scenario: Zona de prueba explícita
- **WHEN** una comprobación de C-01 utiliza una zona de prueba indicada por el ejecutor
- **THEN** su resultado identifica el contexto de prueba y no crea configuración persistida del consultorio.

#### Scenario: Zona inválida o ausente
- **WHEN** el arranque o comprobación exige una zona y su configuración está ausente o no es un identificador IANA válido
- **THEN** se informa el error sin sustituirla silenciosamente por la zona del sistema.
