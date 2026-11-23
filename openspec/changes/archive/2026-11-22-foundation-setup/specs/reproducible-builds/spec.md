# Spec Delta

## Purpose

Permitir que los integrantes del equipo obtengan dependencias y compilen el backend y el frontend desde un checkout limpio siguiendo un contrato común y reproducible.

## ADDED Requirements

### Requirement: RB-01 Herramientas fijadas y compatibles

El proyecto SHALL declarar versiones estables exactas de sus herramientas directas, compatibles con Java 21 Temurin y el stack aprobado. SHALL indicar cómo seleccionar esas herramientas sin modificar instalaciones globales ni archivos personales.

#### Scenario: Preparación de un checkout limpio
- **WHEN** un integrante sigue los requisitos de herramientas documentados
- **THEN** puede identificar las versiones exactas y seleccionar el entorno necesario para compilar ambos componentes.

#### Scenario: Herramienta incompatible
- **WHEN** se intenta compilar con una versión incompatible de Java o Node.js
- **THEN** la comprobación de requisitos o el comando de compilación falla de forma identificable, sin sustituir silenciosamente la versión acordada.

### Requirement: RB-02 Dependencias reproducibles

El proyecto SHALL conservar el Wrapper de construcción del backend con verificación de distribución y los archivos de bloqueo de dependencias del backend y frontend. La instalación documentada SHALL respetar esos bloqueos y no actualizar versiones implícitamente.

#### Scenario: Instalación con bloqueos existentes
- **WHEN** un integrante instala las dependencias con los comandos documentados desde un checkout limpio
- **THEN** se resuelven las versiones registradas y no se modifican los archivos de bloqueo.

#### Scenario: Desacuerdo entre manifiesto y bloqueo
- **WHEN** el manifiesto del frontend y su archivo de bloqueo no coinciden
- **THEN** el comando documentado de instalación reproducible informa un error en lugar de regenerar el bloqueo automáticamente.

### Requirement: RB-03 Compilación independiente de ambos componentes

El proyecto SHALL proporcionar comandos separados para compilar backend y frontend y comprobar los tipos del frontend. Esas compilaciones SHALL producir artefactos locales sin requerir una conexión a PostgreSQL ni ejecutar operaciones de negocio.

#### Scenario: Compilación sin servidor de base de datos
- **WHEN** se ejecutan las compilaciones documentadas con las herramientas y dependencias disponibles, sin PostgreSQL en ejecución
- **THEN** se obtienen el artefacto ejecutable del backend y los archivos estáticos del frontend; las pruebas de integración de base se ejecutan mediante un comando separado.

#### Scenario: Error de tipos
- **WHEN** el frontend contiene una asignación que viola su configuración estricta de tipos
- **THEN** la comprobación de tipos falla y el comando de build documentado no presenta esa ejecución como exitosa.

### Requirement: RB-04 Documentación ejecutable y límites de aceptación

La documentación SHALL identificar el directorio de trabajo, requisitos y resultado esperado de cada comando de preparación, build y comprobación. SHALL distinguir las comprobaciones de C-01 del empaquetado integrado y la aceptación funcional de C-16.

#### Scenario: Reproducción por otro integrante
- **WHEN** otro integrante sigue la documentación desde un checkout limpio
- **THEN** conoce los comandos de backend y frontend, dónde quedan sus artefactos y cuáles comprobaciones requieren una base de prueba.
