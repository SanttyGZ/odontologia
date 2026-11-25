# Verificación de C-09 `autogestion-turnos-api`

Fecha: 2026-11-25. Alcance verificado: API de consulta, cancelación y reprogramación de autogestión; sin SMTP ni cambios de interfaz.

## Resultados

| Comprobación | Resultado |
|---|---|
| Pruebas unitarias (`test`) | 68 pasaron; 0 fallos, errores u omisiones. |
| Pruebas de integración (`integrationTest`) | 49 pasaron; 0 fallos, errores u omisiones. |
| Artefacto (`bootJar`) | Generado: `backend/build/libs/odontologia-0.0.1-SNAPSHOT.jar`. |
| Validación OpenSpec estricta | Pasa: `openspec validate autogestion-turnos-api --strict --no-interactive`. |
| Validación posterior al archivo | 12/12 especificaciones y 13/13 changes archivados pasan `openspec validate --strict`. |
| Archivo | OpenSpec archivó el change el 2026-11-25 e incorporó 6 requisitos a `openspec/specs/appointment-self-management/spec.md`. |
| `git diff --check` | Pasa, sin diferencias de whitespace reportadas. |

La cobertura C-09 comprueba las reglas temporales y de disponibilidad, la identidad estable del turno, los contratos HTTP y sus errores genéricos, el ETag y `If-Match`, la revocación al cancelar, la conservación de credencial al reprogramar, la preparación e invalidación de avisos, el rollback conjunto y dos solicitudes sincronizadas con la misma revisión.

## Entorno y evidencia

La compilación y las pruebas se ejecutaron en Temurin 21.0.13.1, Gradle 9.9.2 y PostgreSQL 17.12 (`sha256:223efeb196329b55b3b9a13db144956411dbc18e727e59f681c3e0aede6475cf`). PostgreSQL corrió en un contenedor temporal sin volumen persistente; las bases de integración se crearon y eliminaron dentro de ese contenedor. Las credenciales de prueba y `APP_BASE_URL=http://127.0.0.1:5173` se pasaron por un archivo temporal fuera del repositorio, que se eliminó al terminar. No se modificó `.env` ni la configuración local.

La primera corrida completa omitió `APP_BASE_URL`, variable que aporta `scripts/with-local-db.py`; por eso fallaron los cuatro casos de `PostgresConnectivityTest` durante la validación inicial del entorno. Se corrigió el entorno temporal y se repitió la suite completa: los 117 tests pasaron y `bootJar` finalizó con código 0.

Capturas Active Stack, código de salida 0:

- `5ba7a64394aeefbd4d5df9fbbb9536e9` — pruebas unitarias focalizadas, requisito AG-01.
- `6bd0eca09be80ec30215417cb0d8fd21` — integración C-09 con PostgreSQL, requisito AG-05.
- `0a0582ff6c21b8ff7559ac6bb7643449` — suite completa y `bootJar`, requisito AG-06.
