# Odontologia — Base de conocimiento

**Estado**: redacción completa de los diez documentos canónicos y este índice. Documentos 01–07 aprobados; 08–10 redactados en bloque autorizado, con propuestas y pendientes explícitos.
**Fecha**: 2026-11-18.
**Proyecto**: `odontologia` es el identificador del repositorio; el nombre comercial sigue pendiente.

La estructura y el stack fueron aprobados por el usuario mediante «Aprobar stack y estructura; comenzar la redacción». Posteriormente aprobó 07 y autorizó «avance con todos los siguientes documentos», por lo que 08–10 se completaron sin nuevas pausas de revisión individual. Cada archivo registra su estado; no se presume aprobación de propuestas nuevas. La validación con odontólogos sigue pendiente.

**Actualización C-01, 2026-11-22:** entorno mínimo implementado, verificado y
archivado; [README de ejecución](../README.md) y
[registro de comprobaciones](../openspec/changes/archive/2026-11-22-foundation-setup/verification.md).
Las referencias históricas a ausencia de implementación o versiones pendientes
se precisan con las decisiones posteriores de [09](09_decisiones_y_supuestos.md)
y el estado técnico de [10](10_preguntas_abiertas.md). Todavía no hay funciones de
negocio ni aceptación de las historias del MVP.

**Actualización C-02, 2026-11-22.** El modelo inicial, las migraciones y la coordinación de escrituras están implementados. Las decisiones y pruebas se registran en el [change archivado](../openspec/changes/archive/2026-11-22-core-models/verification.md).

**Actualización C-03, 2026-11-22.** El acceso interno del odontólogo está implementado y verificado. El contrato de sesión y protección de las rutas internas se registra en el [change archivado](../openspec/changes/archive/2026-11-22-acceso-interno-odontologo/design.md).

## Fuentes y precedencia

1. [Discovery aprobado](../discovery/discovery.md): requisitos, alcance y reglas acordados.
2. [Decisiones posteriores de entrega y stack](../discovery/propuesta-tecnica-kb.md): primera versión local para un único consultorio y combinación técnica aprobada.
3. [Estudio de mercado](../discovery/informe-mercado.md): antecedente de investigación; sus propuestas no amplían por sí mismas el MVP.

Las decisiones explícitas posteriores precisan el Discovery. Los criterios nuevos y supuestos se identifican como propuestas hasta su revisión.

## Índice de archivos

Los diez documentos están disponibles. Sus estados distinguen redacción completa de aprobación de decisiones.

| Archivo | Contenido | Estado |
|---|---|---|
| [01_vision_y_objetivos.md](01_vision_y_objetivos.md) | Propósito, actores, alcance, objetivos y aceptación de la entrega local. | Aprobado por el usuario el 2026-11-18. |
| [02_descripcion_general.md](02_descripcion_general.md) | Stack, componentes, ejecución local, integración externa y mapa orientativo de API. | Aprobado por el usuario el 2026-11-18. |
| [03_actores_y_roles.md](03_actores_y_roles.md) | Actores, permisos, rutas públicas y autogestión limitada al turno. | Aprobado por el usuario el 2026-11-18. |
| [04_modelo_de_datos.md](04_modelo_de_datos.md) | Entidades, relaciones, restricciones, notificaciones y datos de demostración. | Aprobado por el usuario el 2026-11-18. |
| [05_reglas_de_negocio.md](05_reglas_de_negocio.md) | Reglas identificadas por dominio, estados, trazabilidad y decisiones pendientes. | Aprobado por el usuario el 2026-11-18. |
| [06_funcionalidades.md](06_funcionalidades.md) | Historias de usuario por épica, criterios de aceptación y dependencias abiertas. | Aprobado por el usuario el 2026-11-18. |
| [07_flujos_principales.md](07_flujos_principales.md) | Flujos completos, participantes, resultados, errores y decisiones pendientes. | Aprobado por el usuario el 2026-11-18. |
| [08_arquitectura_propuesta.md](08_arquitectura_propuesta.md) | Patrones, estructura, transacciones, concurrencia, correo, acceso y contrato de entorno. | Redactado en bloque; propuestas técnicas pendientes de revisión. |
| [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) | Diez decisiones aprobadas, seis propuestas técnicas y cinco supuestos por validar. | Consolidado; no aprueba PT ni valida SU por inferencia. |
| [10_preguntas_abiertas.md](10_preguntas_abiertas.md) | Doce PB, once pendientes técnicos AT, seis pendientes de producto AP y aclaraciones históricas. | Registro consolidado; preguntas pendientes. |

## Orden de lectura para desarrollo

Comenzar por [visión y objetivos](01_vision_y_objetivos.md) y las fuentes anteriores; continuar en este orden:

1. Dominio: [01](01_vision_y_objetivos.md) y [03](03_actores_y_roles.md).
2. Datos y reglas: [04](04_modelo_de_datos.md) y [05](05_reglas_de_negocio.md).
3. Comportamiento: [06](06_funcionalidades.md) y [07](07_flujos_principales.md).
4. Implementación propuesta: [02](02_descripcion_general.md) y [08](08_arquitectura_propuesta.md).
5. Decisiones y pendientes: [09](09_decisiones_y_supuestos.md) y [10](10_preguntas_abiertas.md) antes de iniciar el trabajo correspondiente.

El [README de ejecución](../README.md) contiene los comandos reales incorporados en C-01. El registro `state.kb` conserva el origen interactivo de esta KB y su inventario documental. La fundación está completada y la política visual vigente es `off`. Las propuestas y preguntas restantes conservan su estado salvo las decisiones posteriores registradas en 09 y 10.

## Resumen técnico para la fundación

| Campo | Definición vigente |
|---|---|
| Problema | Reducir el trabajo de reservar, confirmar y reprogramar turnos del odontólogo independiente. |
| Tipo de sistema | Aplicación web; objetivo comercial futuro SaaS. |
| Dominio | Odontología: agenda y datos básicos de pacientes. |
| Escala de esta entrega | Un consultorio, un odontólogo interno y pacientes concurrentes sin cuenta; volumen numérico no informado. |
| Stack | Java 21 Temurin, Spring Boot, Gradle Wrapper, React, TypeScript, Vite, CSS, PostgreSQL local y Spring Mail/SMTP. |
| Infraestructura necesaria | Backend, base local y conectividad/cuenta SMTP; no implica alojamiento pago o público. |

## Alcance de la verificación documental

Comprobar los diez archivos, enlaces locales, estados y coherencia entre 40 RN, doce PB, doce US, 48 CA-US, seis CT y nueve FL. Verificar que PT, SU, AT y AP estén identificados y que las 48 casillas de aplicación sigan sin marcar. Esta comprobación de documentos no acredita funcionamiento, entrega de email ni controles de seguridad implementados.

## Resumen ejecutivo

El producto busca reducir el trabajo de reservar, confirmar y reprogramar turnos de un odontólogo independiente. La primera entrega será una aplicación web local para un consultorio, con agenda, datos básicos de pacientes y emails reales a destinatarios de prueba. El objetivo comercial SaaS se mantiene para una evolución posterior; el problema y la disposición a pagar siguen sin validación directa con odontólogos.
