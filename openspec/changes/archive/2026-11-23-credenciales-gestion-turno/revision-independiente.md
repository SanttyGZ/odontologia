# Revisión independiente de C-04 y C-05

**Estado** — veredicto favorable emitido el 2026-11-23 por el agente independiente `revision_independiente`. La tarea 2.3 de C-04 requiere revisión independiente de quien implementó. La delegación de solo lectura se habilitó localmente con autorización expresa para esta revisión.

## Resultado

- Base probada `cdd650a759abd007b2fcdaffac5799073f919a64`. Huella SHA-256 del árbol al comenzar y terminar `526aa78c484848e350a95dbb34cd0426348b4e88b386d277f3087a0ca3f5f983`. El agente revisor no modificó el árbol.
- Active Stack, corrida `27f6a145964c2cf4060f855365aeeff2`. Gradle `test integrationTest` completo con Temurin 21 en Linux y PostgreSQL aislado, salida 0 en 53 s. SHA-256 de la salida redactada `0d19151ddf0a6649a37366da314de9ed1e778b67f6607688f5cd0436d8de7c0c`, contrastado por el revisor y el coordinador.
- La revisión inicial pidió corregir conflicto de evento, destinatario confirmado, contenido inmutable del intento, lectura del archivo de claves y cobertura de vencimiento y resultado incierto. La revisión posterior comprobó las correcciones y la cancelación coordinada con revocación de un enlace cuyo intento ya estaba abierto.
- El veredicto abarca el contrato de C-04/C-05, incluida recuperación HTTP autenticada, CSRF, persistencia y rollback, vigencia y cancelación, concurrencia, reintentos y composición de prueba. Quedan para sus changes las rutas públicas y reglas de borde consumidoras (C-08/C-09), envío real (C-11) y navegador (C-14).
- El registro de cierre y el marcado de tareas son posteriores al árbol probado. Los archivos de implementación y pruebas no se modificaron después de la captura.

## Material y comprobaciones

- Comparar el árbol de trabajo con la base de revisión `cdd650a`. Incluir los archivos nuevos de gestión, comunicaciones, las migraciones V2 y V3 y las pruebas de integración. Rechazar cualquier evidencia referida a una huella de árbol distinta.
- Contrastar el resultado con [la especificación de C-04](specs/appointment-management-credentials/spec.md) y [la de C-05](../2026-11-23-intenciones-notificacion/specs/appointment-notification-intents/spec.md), junto con sus diseños aprobados y el ajuste de recuperación.
- Revisar que la ruta interna exija rol de odontólogo y CSRF, que el fallo de preparación propague rollback y que la respuesta y los registros no expongan el secreto. Revisar además el manejo de clave ausente, inapropiada para lectura o comprometida.
- Revisar la conservación de identidad, programación e historial al recuperar acceso, el orden entre recuperación y autorización y el bloqueo de reintentos del aviso anterior. Comprobar que el ensayo de rollback empieza dentro de la transacción que contiene las escrituras relacionadas.
- Repetir la suite completa en Linux con Temurin 21 y PostgreSQL de pruebas. Capturar la ejecución con Active Stack desde el mismo checkout. Verificar la huella del árbol, el código de salida y el SHA-256 de la copia redactada. Revisar qué escenarios ejercita realmente cada prueba antes de emitir un resultado.

## Resultado a registrar por el revisor

Identidad del revisor, fecha, huella del árbol, recibos independientes, hallazgos por escenario y veredicto. Si queda una diferencia de alcance o de evidencia, dejar la revisión pendiente. La entrega del correo y la interfaz del navegador pertenecen a C-11 y C-14.
