# Proposal

## Why

El proyecto tiene requisitos y roadmap, pero todavía no cuenta con código fuente ni un entorno reproducible de compilación y ejecución. C-01 prepara una base común para que los cinco integrantes implementen los changes siguientes sin divergencias de herramientas o configuración local.

## What Changes

- Crear `backend/` con Java 21 Temurin, Spring Boot y Gradle Wrapper, y `frontend/` con React, TypeScript estricto, Vite y CSS.
- Proponer versiones estables exactas, bloqueo de dependencias y comandos documentados de instalación, compilación y comprobación.
- Proponer Spring JDBC y Flyway como contrato inicial de AT-02; mantener el esquema físico, las migraciones de negocio y el protocolo concurrente para C-02.
- Definir configuración local explícita: direcciones de escucha, puertos, URL base, variables de conexión a PostgreSQL y tratamiento de la zona pendiente del consultorio, sin secretos versionados.
- Preparar desarrollo con proxy de Vite a Spring Boot y una comprobación de arranque y conexión de solo lectura sobre PostgreSQL de prueba.
- Presentar AT-01, AT-02, AT-08 y AT-10 para revisión HIGH antes de escribir configuración ejecutable. Los valores de `design.md` son propuestas; no quedan aprobados por generar estos documentos.

## Capabilities

### New Capabilities

- `reproducible-builds`: instalación y compilación reproducibles del backend y frontend con herramientas fijadas, comprobaciones explícitas y comandos documentados.
- `local-runtime`: ejecución local con configuración verificable, proxy de desarrollo y conexión a PostgreSQL sin modificar datos existentes.

### Modified Capabilities

Ninguna. El inventario del CLI no contiene especificaciones consolidadas previas.

## Impact

- Cambios futuros previstos en `backend/`, `frontend/`, `.gitignore` y documentación de ejecución. En esta ronda se escriben únicamente los artefactos de planificación de `foundation-setup`.
- Dependencias propuestas: Spring Web MVC, Spring JDBC, driver PostgreSQL, Flyway y herramientas de prueba. No se incorpora autenticación ni SMTP en C-01.
- Sin endpoints de negocio, tablas del dominio, pacientes ni datos reales. Sin cambios en servicios globales, dotfiles personales ni bases existentes.
- El empaquetado integrado definitivo de React servido por Spring Boot y la aceptación completa del producto corresponden a C-16.
- C-01 permanece pendiente de implementación y aprobación HIGH. El roadmap y las decisiones históricas de la KB conservan su estado.
