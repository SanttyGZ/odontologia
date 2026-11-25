# Verificación C-11

## Recepción manual en Gmail

Ignacio confirmó el 2026-11-25 que vio los cinco mensajes en **Recibidos** y que sus identificadores coincidían con los intentos. La captura fue descrita por Ignacio. La confirmación de recepción se atribuye al titular del buzón. El resultado SMTP se contrastó aparte en PostgreSQL.

Reloj de la campaña: 2027-01-05 10:00 UTC. Base aislada con datos ficticios.

| Caso | Intento | Resultado SMTP | Recepción manual |
|---|---|---|---|
| Confirmación | e2d97775-5f29-4f32-ad7a-b4cccfaf1b8b | ACEPTADA_SMTP | Recibido |
| Cancelación | 6885260b-224b-4119-b377-6e58f41f9503 | ACEPTADA_SMTP | Recibido |
| Reprogramación | c8ab13f2-5875-4629-bb74-f98c9d0bb1a4 | ACEPTADA_SMTP | Recibido |
| Recordatorio | 00e8d727-a69d-4c35-a274-b23d6fed925e | ACEPTADA_SMTP | Recibido |
| Combinado a 24 horas exactas | e78eb08a-57f4-4788-a715-813b23f3e513 | ACEPTADA_SMTP | Recibido |

La campaña registró un solo intento para el despacho combinado, con tipos `CONFIRMACION+RECORDATORIO`. C-14 sigue pendiente, incluida la apertura funcional de los enlaces y la autogestión en navegador.

## Corrección de la ruta de autogestión

Con el compositor original, la prueba focalizada ejecutó 7 casos y falló en los 6 que esperaban un enlace. El informe mostró `/gestion#credencial-ficticia` donde se esperaba `/gestionar-turno#credencial-ficticia`. El caso sin material de gestión pasó.

Tras corregir la ruta, los 7 casos de composición pasaron. Se comprobó la URL completa para confirmación, reprogramación, recordatorio, combinado y recuperación. Una segunda URL base confirmó que el host y el puerto provienen de la configuración. La cancelación y la confirmación sin material quedaron sin enlace. El combinado conservó un único aviso con un único enlace.

La ejecución directa en WSL usó Temurin 21.0.13.1+2, PostgreSQL 17.12 en un contenedor temporal con puerto de loopback, `APP_BASE_URL=http://127.0.0.1:5173` y `APP_MAIL_ENABLED=false`. Pasaron `test` (118 casos), `integrationTest` (57 casos) y `bootJar`, con salida 0. No se enviaron correos.

| Ejecución directa | Directorio bajo `~/.cache/` en WSL | Salida | SHA-256 de `result.log` redactado |
|---|---|---|---|
| Regresión con la ruta anterior | `c11-ruta.KBnoGDYD` | 1 | `75389a339b588df831ce9d5eec878e81048768cb8b3431fb896ad6fc86d59ab6` |
| Suite con la ruta corregida | `c11-ruta.ACYQdaGu` | 0 | `6f17500e98c7ee501dbf37975fbab064f41a10dd850c62a30e767b26ad41b1fd` |

La comprobación de esta corrección cubre la composición. Una revisión independiente confirmó el alcance del diff, las huellas de ambos registros y que los dos archivos Java coinciden byte a byte con los de la ejecución exitosa. Las ejecuciones quedaron registradas con captura directa. El ejecutable Active Stack disponible carece de `evidence-capture` y `evidence-state`, por lo que sigue pendiente la corroboración formal de la procedencia de esas ejecuciones. **EM-04 y la tarea 4.1 siguen pendientes** hasta abrir el enlace desde un correo recibido en el buzón autorizado con C-14 integrado.

## Pendientes de cierre

- Completar la apertura funcional y la autogestión en navegador cuando se integre C-14. El 2026-11-25 se consultó el remoto: el main de upstream y el HEAD de ignacio/c11 estaban en 4120042ce23782ab2d85ad75dab6e7a96cf754ce, con C-14 pendiente y sin la ruta de autogestión integrada. La tarea 4.1 y C-11 siguen abiertos por ese criterio.

## SMTP AUTH y revisión independiente

La regresión reprodujo el fallo con AUTH 454 (recibo `815161fcf6bbaadc823c2a1b29485e0b`). La corrección conserva 454 como transitorio y 535 como permanente. También conserva como permanente un rechazo que solo menciona 454 en el texto. La suite Linux `test integrationTest bootJar` terminó con código 0 (recibo `771090f6e3a08eb9fd6ab4b8de4e0fb3`).

El revisor independiente comprobó el manejo de la excepción de autenticación, el reconocimiento del código de respuesta inicial y la cobertura de regresión. Su revisión de código fue favorable para esta corrección. La suite y su recibo se registran aparte de la revisión de código y de la confirmación manual en Gmail.

## Recuperación después de aceptación SMTP local

El arnés de prueba arrancó el backend en Linux con Temurin 21 y PostgreSQL 17.12 descartable. Usó un transporte JavaMail hacia un servidor SMTP de loopback. El servidor respondió `250` al terminar DATA, el cliente volvió de la llamada SMTP y la JVM terminó abruptamente con código 86 aún dentro del transporte, antes de que `EmisionAviso` pudiera registrar el resultado. Se ejercitaron la cola, la autorización y la persistencia de producción con el transporte de prueba.

Una JVM nueva encontró un único intento abierto en la misma base. Después del sondeo inicial y de otro seis minutos después en el reloj de prueba, quedó un único intento `INCIERTO` con hora de finalización y cero conexiones SMTP. Un intento de reenvío hubiera hecho fallar la prueba. No se usaron credenciales de Gmail ni se enviaron mensajes externos.

| Ejecución | Recibo Active Stack | Salida | SHA-256 de salida redactada |
|---|---|---|---|
| Principal | 3645c3d6d7f342fd71e47476202acb2a | 0 | fbcbd7152414a4bda4d5b9da6c3e99fa434b798f5d6cc1cda8013bc366fc177e |
| Repetición independiente, base propia | b1a5cadc2ca1037f0e76c026dea0803d | 0 | b6b1364e19f199744bb026f26702a053dbf74644524d287b2eb2dc28bd94d087 |

Ambos recibos identifican el árbol `dfaf59df2945197cbaa0624e98d926852564eb6f2fbf652b391dc67c443c22aa` anterior a esta actualización documental. Gradle comprobó los códigos de la JVM 86 y 0 en sus fases respectivas. El redactor ocultó esos valores en las líneas de diagnóstico. El revisor independiente contrastó el SHA-256 de su salida, el punto del corte y los contadores de recuperación. Veredicto favorable para este escenario de EM-02.

Arnés local conservado en WSL `/tmp/c11-recuperacion.Oj7tiwXr`. SHA-256 de `RecuperacionSmtpFixture.java`: `853b2f546e0f33cb5b25288d2b46cfe6723757ed42ccda96b238077064daa925`.

## Revisión final de la rama

Se contrastaron AGENTS.md, CHANGES.md, los artefactos de C-11 y los contratos de C-04/C-05/C-09. Se confirmó que el emisor está deshabilitado por defecto, restringe remitente y destinatario al buzón de prueba, exige STARTTLS con verificación del servidor y recibe el secreto por variable de entorno. La autorización revalida el vencimiento y la revisión bajo el coordinador de agenda. PostgreSQL excluye emisores simultáneos y los intentos inciertos no se reenvían.

La revisión independiente encontró que un fallo explícito al establecer conexión SMTP se trataba como incierto antes de enviar datos. La prueba reprodujo el fallo (recibo `c34125799b8fa4afab660ef1ed95b885`). Ahora `MailConnectException` se clasifica como transitorio y conserva los reintentos de C-05. Un timeout genérico sigue siendo incierto. El revisor dio su conformidad a la corrección y ejecutó en limpio `test integrationTest bootJar` con Temurin 21, PostgreSQL 17.12 aislado, filesystem POSIX y el emisor deshabilitado.

Recibo independiente final `0cbf82d5a590a92353e52b0248c52498`, salida 0. Huella del árbol probado `9c788e20f64dd11d1dde2c86c3be763f1988e3122189afd632140d5f01cf8337`, previa a este registro documental. SHA-256 de salida redactada `3125227a32002264342b1eecaa634ee04a32771f8cce73160053a512d30d7312`. La prueba de corte y la comprobación manual de Gmail se mantienen como evidencia de sus respectivos escenarios. C-11 sigue abierto con 6/7 tareas.
