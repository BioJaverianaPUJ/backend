# Auditoría 2.0 — pruebas automatizadas y carga

**Fecha de revisión:** 22 de noviembre de 2026  
**Alcance:** ejecución actual de la suite Gradle y revisión de las ejecuciones k6 ya guardadas. No se repitieron pruebas de carga.

## Resumen ejecutivo

- **Pruebas automatizadas:** `.\gradlew.bat test` compiló el proyecto y ejecutó 148 pruebas: **147 pasaron, 1 falló, 0 fueron omitidas**. Testcontainers pudo iniciar PostgreSQL; el fallo está en una aserción de `PostgresSchemaIntegrityTest`.
- **Pruebas de carga:** las evidencias existentes muestran los resultados móvil, combinado y de observaciones descritos abajo. Los P95 observados para RNF-04 y RNF-05 están bajo sus respectivos límites en estas corridas locales.
- **Alcance de RNF-07:** 150/150 observaciones finalizaron con un worker sintético. **NO se probó BioCLIP-2 real**, por lo que estos resultados no demuestran el rendimiento del modelo ni el cumplimiento end-to-end con el worker real.
- No se afirma que el sistema completo cumpla todos los RNF.

## Entorno, comandos y método

### Suite automatizada

Comando ejecutado desde la raíz del backend:

```powershell
.\gradlew.bat test
```

Entorno observado en el reporte: Java 25.0.5 y Testcontainers con la imagen `postgres:18` (PostgreSQL 18.7). `ApiApplicationTests` activa el perfil `test`, que usa H2 en modo PostgreSQL; la prueba de integridad de esquema usa un contenedor PostgreSQL real. El resultado actual sustituye el estado histórico consignado en `TEST_AUDIT.md`, que correspondía a una ejecución anterior con pruebas omitidas por falta de Docker.

### Pruebas de carga

No se ejecutó k6 durante esta auditoría. Se leyeron los archivos de `load-tests/results/` y se contrastaron con los escenarios de `load-tests/k6/`:

- `mobile.js`: 50 VUs durante 15 minutos por defecto; umbral P95 móvil ≤ 1.000 ms.
- `combined-http.js`: móvil y administración concurrentes, 50 y 3 VUs durante 15 minutos por defecto; umbrales P95 móvil ≤ 1.000 ms y administrativo ≤ 2.000 ms.
- `observations.js`: 10 llegadas por minuto durante 15 minutos por defecto y monitorización RabbitMQ.
- `common.js`: autenticación/preflight, operaciones HTTP, procesamiento de observaciones y muestras RabbitMQ.

Los scripts restringen API, Management de RabbitMQ y URL presignada S3 a loopback. Los registros de estabilidad identifican la pila `biojaveriana-loadtest-*`; los datos no corresponden a producción.

## Resultados de pruebas automatizadas

| Grupo de pruebas | Casos | Resultado |
|---|---:|---|
| Contexto de aplicación | 1 | 1 pasó |
| Autenticación y JWT | 12 | 12 pasaron |
| Comentarios | 16 | 16 pasaron |
| Integración PostgreSQL y seguridad de parámetros | 4 | 3 pasaron, 1 falló |
| Observaciones y jobs | 19 | 19 pasaron |
| Publicaciones | 18 | 18 pasaron |
| S3 y presigned URLs | 15 | 15 pasaron |
| Seguridad de contraseñas | 2 | 2 pasaron |
| Especies y especímenes | 31 | 31 pasaron |
| Usuarios | 30 | 30 pasaron |
| **Total** | **148** | **147 pasaron, 1 falló, 0 omitidas** |

### Fallo pendiente de diagnóstico

Falló `PostgresSchemaIntegrityTest.rnF21UniqueConstraintsRejectDuplicateUserAndSpeciesValues()`. La aserción esperaba `DataIntegrityViolationException`, pero recibió `UncategorizedSQLException` con SQLSTATE `25P02` y el mensaje de PostgreSQL “current transaction is aborted”. El SQL del fallo corresponde a un `INSERT INTO users`.

La prueba, anotada con `@DataJpaTest`, realiza varias inserciones duplicadas dentro del mismo método/transacción. El error observado es compatible con que una violación previa haya dejado la transacción PostgreSQL abortada y que una inserción posterior ya no pueda comprobarse de forma independiente. Esto apunta a un problema de aislamiento de las aserciones; **no demuestra por sí solo que la restricción de base de datos esté ausente**. Sin embargo, la aserción falló y la unicidad de email/especie no queda acreditada por esta ejecución. No se modificó ni volvió a ejecutar la prueba.

Las otras dos pruebas de `PostgresSchemaIntegrityTest` pasaron: presencia de restricciones únicas para votos/reacciones e identificadores UUID en columnas de clave primaria. También pasó `RepositoryParameterSafetyTest`. No hay en esta suite una prueba de integración que ejecute RabbitMQ real o BioCLIP-2.

## Resultados de carga por RNF

| RNF | Evidencia y resultado verificado | Interpretación |
|---|---|---|
| RNF-01 | 50 VUs móviles; 22.505 solicitudes; 22.505 checks pasaron y 0 fallaron; P95 26,03 ms. | Carga prevista observada; P95 inferior a 1 s en esta ejecución. |
| RNF-02 | Dos ejecuciones con 50 VUs móviles y 3 administrativos. Corrida 1: 24.159 solicitudes; P95 móvil 26,65 ms y admin 29,51 ms. Corrida 2: 24.137 solicitudes; P95 móvil 27,86 ms y admin 30,39 ms. Ambas muestran 0 tasa de fallos HTTP. | En ambas ejecuciones, los P95 observados están bajo los límites de RNF-04 y RNF-05. |
| RNF-03 | Registro de 31 muestras durante la carga combinada. RabbitMQ reportó que no había alarmas. PostgreSQL, RabbitMQ y RustFS mostraron `RestartCount=0` y `OOMKilled=false`. | No se observaron alarmas, reinicios ni OOM en los datos muestreados. No prueba ausencia entre muestras ni cubre servicios no incluidos en el TXT. |
| RNF-04 | P95 móvil: 26,03 ms en la corrida móvil y 26,65 / 27,86 ms en las combinadas. | Inferior a 1.000 ms en las ejecuciones registradas; no es una declaración de cumplimiento integral. |
| RNF-05 | P95 administrativo en las dos combinadas: 29,51 / 30,39 ms. | Inferior a 2.000 ms en las ejecuciones registradas; no es una declaración de cumplimiento integral. |
| RNF-07 | 150/150 observaciones completadas con worker sintético, a 10 por minuto durante 15 minutos. Media: 2.017,33 ms (2,01 s truncado); P95: 2.033 ms (2,03 s). Para cada métrica RabbitMQ hubo 955 muestras `during`; máximo `ready`, `unacked` y total: 0. | Valida el recorrido de infraestructura simulado. **NO se probó BioCLIP-2 real ni se midió el rendimiento del modelo. RNF-07 no se declara cumplido end-to-end.** |

Los P95 y la media a dos decimales se muestran truncados para reproducir los valores reportados; los JSON conservan más precisión. Para RabbitMQ, el NDJSON contiene además una muestra `before` y otra `after` por métrica, aparte de las 955 muestras `during`.

## Evidencias y archivos exactos

### Pruebas automatizadas

- Reporte HTML Gradle: `build/reports/tests/test/index.html`.
- XML del fallo: `build/test-results/test/TEST-com.biojaveriana.api.integration.PostgresSchemaIntegrityTest.xml`.
- XML de la suite: los demás `build/test-results/test/TEST-*.xml`.
- Prueba con fallo: `src/test/java/com/biojaveriana/api/integration/PostgresSchemaIntegrityTest.java`.
- Configuración H2: `src/test/resources/application-test.yaml`.

### Pruebas de carga

- RNF-01: `load-tests/results/mobile-20261122-031551.json` y `load-tests/results/mobile-20261122-031551-metrics.ndjson`.
- RNF-02, primera corrida: `load-tests/results/combined-http-20261122-040217.json` y `load-tests/results/combined-http-20261122-040217-metrics.ndjson`.
- RNF-02, segunda corrida: `load-tests/results/combined-http-20261122-051455.json` y `load-tests/results/combined-http-20261122-051455-metrics.ndjson`.
- RNF-03: `load-tests/results/rnf03-monitor-20261122-051438.txt`.
- RNF-07: `load-tests/results/observations-20261122-131513.json` y `load-tests/results/observations-20261122-131513-metrics.ndjson`.
- Escenarios: `load-tests/k6/mobile.js`, `load-tests/k6/combined-http.js`, `load-tests/k6/observations.js` y `load-tests/k6/common.js`.
- Informe complementario de carga: `load-tests/RESULTS.md`.

Los resúmenes JSON de k6 contienen `setup_data`. Protegerlos como datos sensibles y sanitizarlos antes de compartirlos; este informe no reproduce tokens ni credenciales.

## Limitaciones, riesgos y requisitos pendientes

1. **Fallo de prueba:** corregir el aislamiento/gestión transaccional de `rnF21UniqueConstraintsRejectDuplicateUserAndSpeciesValues()` y volver a ejecutar la suite. Hasta entonces, las comprobaciones de unicidad que no llegaron a ejecutarse correctamente quedan pendientes.
2. **RNF-07 con modelo real:** correr la validación con BioCLIP-2 real para medir inferencia, latencia, recursos y flujo end-to-end. Los resultados actuales usan respuestas sintéticas.
3. **RNF-03:** ampliar la monitorización para cubrir continuamente todos los servicios relevantes; 31 muestras no detectan necesariamente eventos entre intervalos.
4. **Representatividad:** las pruebas de carga son locales y aisladas. No extrapolar sus latencias a producción ni a otros volúmenes/configuraciones.
5. **Cobertura de integración externa:** la suite no demuestra el comportamiento de RabbitMQ real, RustFS real bajo carga, el worker real o el sistema desplegado completo.
6. **RNF-06** no fue incluido en estas ejecuciones.

## Conclusiones

La suite automatizada compiló y ejecutó 148 casos, con 147 aprobados y un fallo concreto en una prueba de integridad PostgreSQL; por ello, el resultado global de `test` es **fallido**, no aprobado. La causa observada apunta a una transacción abortada durante varias aserciones de unicidad en el mismo test, pero debe corregirse y verificarse antes de considerar completa esa cobertura.

Las pruebas de carga existentes muestran P95 móvil y administrativo por debajo de sus umbrales en las ejecuciones locales registradas, y estabilidad sin eventos reportados en las muestras de infraestructura disponibles. RNF-07 solo tiene evidencia con worker sintético. **No hay evidencia suficiente para afirmar que el sistema completo cumple todos los RNF.**
