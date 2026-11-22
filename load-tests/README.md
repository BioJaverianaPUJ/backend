# Pruebas de carga locales con k6

Estos scripts solo permiten destinos de loopback (`localhost`, `127.0.0.1` o `::1`) para reducir el riesgo de ejecutar carga contra producción. No ejecutar desde una URL no local ni con cuentas/datos ajenos a la prueba.

## Inventario verificado

| Operación | Endpoint y parámetros comprobados | Acceso/observación |
|---|---|---|
| Iniciar sesión | `POST /api/v1/auth/login`; `POST /api/v1/auth/refresh-token`; `POST /api/v1/auth/register` | Login/registro emiten JWT; refresh requiere token. Los scripts reciben tokens de prueba mediante entorno. |
| Catálogo/especímenes del mapa | `GET /api/v1/specimens?page&size` | No se encontró un endpoint de mapa/geoespacial; el listado solo pagina especímenes. |
| Feed | `GET /api/v1/posts?page&size` | Autenticado; el servicio filtra publicaciones ocultas y bloqueadas para usuarios no admin. |
| Perfil | `GET /api/v1/users/me` | Autenticado. Se usa también como preflight de token. |
| Observaciones personales | `GET /api/v1/observations?page&size` | Autenticado; listado del usuario actual. |
| Catálogo oficial | `GET /api/v1/species?page&size` | Catálogo ordenado por nombre científico. |
| Usuarios | `GET /api/v1/users?page&size` | Auth global; el controlador no declara `@HasRole(ADMIN)` para este listado. |
| Denuncias | `GET /api/v1/reports/admin/all?page&size` y `/admin/pending?page&size` | Requiere `ADMIN`/`SUPERADMIN`. `reportedUserId` es filtro disponible en `/api/v1/reports/by-reported-user`. |
| Listado administrativo de publicaciones | `GET /api/v1/posts/admin?page&size` | Requiere `ADMIN`/`SUPERADMIN`. |
| Listado administrativo de observaciones | `GET /api/v1/observations/admin?includeHidden&revisionRequested&page&size` | Requiere admin. |
| Baneos | `GET /api/v1/bans?page&size` | Requiere admin; existe además `/api/v1/bans/user/{userId}?page&size`. |
| Gamificación | No hay endpoint ni entidad de gamificación/visitas/logros encontrada en el backend. | No se puede incluir ni medir sin API implementada. |
| Procesamiento de una observación | `POST /api/v1/observations/upload-presign`; `PUT` a la URL S3 devuelta; `POST /api/v1/observations` con key, latitud, longitud y `waitTimeoutSeconds: 0`; consultar `GET /api/v1/observations/{id}/job-status`. | Requiere PostgreSQL, RustFS/S3, RabbitMQ y worker. El flujo usa una cuenta admin de prueba porque el rate limit actual impone enfriamiento a usuarios no admin. |

Las rutas de consulta no ofrecen búsqueda general. Los filtros verificados incluyen `includeHidden`, `revisionRequested`, denuncias pendientes y `reportedUserId`; no se fabrican parámetros de búsqueda adicionales.

## Qué cubre cada escenario

| Escenario | VUs/ritmo por defecto | Carga | Requisito |
|---|---:|---|---|
| `mobile` | 50 VUs por 15 min | Especímenes, feed, perfil y observaciones propias, con pausas aleatorias. | RNF-01 y P95 RNF-04 |
| `admin` | 3 VUs por 15 min | Catálogo, usuarios, denuncias, publicaciones, observaciones y baneos. | RNF-02 y P95 RNF-05 |
| `combined-http` | 50 VUs móviles + 3 VUs admin simultáneos por 15 min | Solo flujos HTTP de `mobileFlow` y `adminFlow`; umbrales P95 móvil ≤ 1000 ms y admin ≤ 2000 ms. | RNF-02 y RNF-05 bajo carga móvil concurrente |
| `observations` | 10 iteraciones/min por 15 min | Presign, PUT de foto, crear job, consultar estado y muestrear RabbitMQ. | RNF-07 |
| `observation-smoke` | 1 VU, 1 iteración | Crea una observación, espera el estado terminal y comprueba que se complete. | Verificación de integración, no evidencia sostenida de RNF-07 |
| `combined` | Los tres anteriores simultáneamente | Carga móvil + web + observaciones. | RNF-01, RNF-02, RNF-04, RNF-05, RNF-07 |

RNF-06 no está incluido. En el escenario de observaciones, el tiempo de proceso se informa como medición; `PROCESSING_CONFIRM_TIMEOUT_SEC` es solo la ventana elegida para observar el procesamiento y no un umbral de conformidad. Los errores y jobs no completados quedan registrados, no se ocultan.

## Métricas y umbrales

- `http_req_duration`: promedio, mediana, P90, P95 y máximo.
- `http_reqs`: solicitudes y tasa de solicitudes (requests/s).
- `http_req_failed`: fallos HTTP según k6.
- `iterations`, `vus` y `dropped_iterations`: ciclos, concurrencia y llegadas descartadas.
- `observation_processing_ms` y `observation_processing_completed`: tiempo desde la respuesta del POST de creación hasta `COMPLETED`, `FAILED` o el fin de la ventana de observación.
- `rabbit_messages_ready`, `rabbit_messages_unacked` y `rabbit_messages_total`: muestras de la API de administración, tomadas cada segundo por el monitor.
- `rabbit_sampling_failures`: errores al recopilar la cola.
- NDJSON en `load-tests/results/`: muestras con timestamp y tags, incluidas las series RabbitMQ `phase:before/during/after`. Se omiten las etiquetas HTTP `url` y `error` para no persistir URLs presignadas de S3 ni sus firmas temporales.

Los thresholds implementados son únicamente los del SRS: P95 móvil ≤ 1000 ms y P95 de consultas web ≤ 2000 ms. No hay umbral de errores ni de procesamiento inventado.

RabbitMQ se mide mediante su API de administración, no desde k6 leyendo internals: `GET /api/queues/{vhost}/{queue}`. Se reportan `messages_ready`, `messages_unacknowledged` y `messages`. Un VU monitor toma una muestra por segundo (configurable con `RABBITMQ_SAMPLE_INTERVAL_SEC`); el test compara el backlog final con el baseline y conserva las muestras con timestamp. `summarize-rabbit.ps1` informa baseline, máximo observado y valor final de mensajes listos, no reconocidos y totales. Si los mensajes listos superan el baseline en una muestra, informa crecimiento observado. El muestreo no prueba estados ocurridos entre intervalos; el criterio RNF-07 no debe declararse cumplido solo porque la cola final haya vuelto a su baseline.

El runner imprime al final una síntesis RabbitMQ en los escenarios `observations` y `combined`. También se puede repetir manualmente:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\summarize-rabbit.ps1 -MetricsPath .\load-tests\results\<archivo>-metrics.ndjson
```

## Preparación aislada

1. Instalar k6 y tener Docker Desktop/Engine activo. En la comprobación final Docker Engine respondió, pero k6 no estaba en PATH y los puertos locales verificados (API 8000, PostgreSQL 15432, AMQP 5673, RabbitMQ Management 15673 y S3 19000) no estaban escuchando; por tanto no se ejecutó carga.
2. Copiar `load-tests/.env.example` a `load-tests/.env`. Establecer contraseñas nuevas solo para esta pila local, y guardar tokens de cuentas creadas en la base aislada. El archivo `.env` y los resultados están excluidos por `.gitignore`.
3. Levantar solamente la pila descartable de rendimiento:

   ```powershell
   docker compose -p biojaveriana-loadtest --env-file load-tests/.env -f load-tests/compose.yml up -d
   ```

   Usa volúmenes con nombre propios y puertos separados (PostgreSQL 15432, AMQP 5673, Management 15673, S3 19000 y consola RustFS 19001 por defecto). No inicia ni modifica servicios del compose de desarrollo/producción.
4. Crear un bucket vacío llamado `biojaveriana-loadtest` en la consola local RustFS, autenticando con `RUSTFS_TEST_ACCESS_KEY` / `RUSTFS_TEST_SECRET_KEY` de `.env`. No apuntar el API a un bucket compartido.
5. Iniciar el backend contra esa base de pruebas; por ejemplo, desde PowerShell, con valores de prueba:

   ```powershell
   $env:POSTGRES_URL = "jdbc:postgresql://127.0.0.1:15432/biojaveriana_loadtest"
   $env:POSTGRES_USER = "loadtest"
   $env:POSTGRES_PASSWORD = "<LOADTEST_DB_PASSWORD>"
   $env:RABBITMQ_HOST = "127.0.0.1"
   $env:RABBITMQ_PORT = "5673"
   $env:RABBITMQ_USER = "loadtest"
   $env:RABBITMQ_PASSWORD = "<RABBITMQ_PASSWORD>"
   $env:S3_ENDPOINT = "http://127.0.0.1:19000"
   $env:S3_BUCKET = "biojaveriana-loadtest"
   $env:S3_ACCESS_KEY = "<RUSTFS_TEST_ACCESS_KEY>"
   $env:S3_SECRET_KEY = "<RUSTFS_TEST_SECRET_KEY>"
   $env:SERVER_PORT = "8000"
   .\gradlew.bat bootRun
   ```

6. Usar `/api/v1/auth/register` para registrar una cuenta móvil de prueba. Registrar por separado una cuenta de prueba admin, y asignarle rol `ADMIN` únicamente dentro de la base aislada. Ejemplo (reemplazar el correo por el exacto usado al registrar):

   ```powershell
   docker compose -p biojaveriana-loadtest --env-file load-tests/.env -f load-tests/compose.yml exec postgres psql -U loadtest -d biojaveriana_loadtest -c "UPDATE users SET role='ADMIN' WHERE email='load-admin@example.test';"
   ```

   Login de ambas cuentas vía `/api/v1/auth/login` y guardar los JWT en `MOBILE_TOKEN` y `ADMIN_TOKEN`. El preflight exige que el token móvil pertenezca a `USER` y que el token administrativo sea `ADMIN` o `SUPERADMIN`.
7. El escenario de observaciones requiere una fotografía de prueba real (JPEG/PNG/WEBP/TIFF) accesible desde k6. Guardarla localmente, establecer `TEST_IMAGE_PATH`, `TEST_IMAGE_CONTENT_TYPE`, coordenadas de prueba y una ventana positiva `PROCESSING_CONFIRM_TIMEOUT_SEC`. Iniciar el worker con acceso a la RabbitMQ y RustFS aislados antes de probar procesamiento; el worker puede descargar BioCLIP-2 y requiere recursos de CPU/GPU suficientes. Desde la raíz del repositorio, en una terminal separada y con dependencias del worker ya instaladas:

   ```powershell
   $env:RABBITMQ_HOST = "127.0.0.1"
   $env:RABBITMQ_PORT = "5673"
   $env:RABBITMQ_USER = "loadtest"
   $env:RABBITMQ_PASSWORD = "<RABBITMQ_PASSWORD>"
   $env:S3_ENDPOINT = "http://127.0.0.1:19000"
   $env:S3_BUCKET = "biojaveriana-loadtest"
   $env:S3_ACCESS_KEY = "<RUSTFS_TEST_ACCESS_KEY>"
   $env:S3_SECRET_KEY = "<RUSTFS_TEST_SECRET_KEY>"
   $env:DEVICE = "cpu"
   $env:MODEL_PRECISION = "float32"
   python -m worker.main
   ```

   El worker usa `RABBITMQ_EXCHANGE=observation.jobs.exchange` y las colas/routing keys declaradas por su configuración. Confirmar que cargó el modelo y que inició consumidores antes del escenario.
8. En `.env`, configurar la dirección de administración `http://127.0.0.1:15673`, vhost `/`, cola `observation.jobs` y las credenciales de RabbitMQ. La cola de entrada es la métrica de backlog de trabajos.

Si un servicio no inicia, hay que resolverlo antes de la carga. No se debe cambiar `API_BASE_URL` ni `RABBITMQ_MANAGEMENT_URL` a un host remoto: los scripts y el runner bloquean destinos que no sean loopback.

### Simular el worker sin BioCLIP-2

`load-tests/fake_worker.py` sustituye solo al consumidor BioCLIP: consume de `observation.jobs` y responde por `observation.jobs.exchange` con routing key y cola `observation.jobs.response`. Usa los nombres que verifica el backend y el worker real. No ejecuta inferencia ni necesita RustFS; el escenario k6 `observations` aún necesita RustFS y una imagen de prueba porque crea observaciones mediante la API y sube la imagen.

Instala únicamente la dependencia del simulador, en un entorno Python de pruebas:

```powershell
python -m pip install -r .\load-tests\requirements.txt
```

Inicia primero RabbitMQ y el backend para que existan el exchange y las colas. En una terminal PowerShell separada, configura las variables del mismo `.env` aislado y ejecuta:

```powershell
$env:RABBITMQ_HOST = "127.0.0.1"
$env:RABBITMQ_PORT = "5673" # Debe coincidir con LOADTEST_RABBIT_PORT
$env:RABBITMQ_VHOST = "/"
$env:RABBITMQ_USER = "loadtest"
$env:RABBITMQ_PASSWORD = "<RABBITMQ_PASSWORD_DE_LOADTESTS>"
$env:FAKE_WORKER_DELAY_SECONDS = "0"
python -B .\load-tests\fake_worker.py
```

`RABBITMQ_PASSWORD` es el valor de pruebas guardado localmente; no lo guardes en el script ni en el repositorio. `FAKE_WORKER_DELAY_SECONDS` acepta cero o una demora finita no negativa. `-B` evita escribir bytecode en el directorio del proyecto. El simulador registra contadores acumulados de mensajes consumidos, respuestas publicadas y errores, sin registrar credenciales ni cuerpos de mensajes.

Al recibir cada job, espera la demora configurada, publica una respuesta sintética `success: true` con el mismo `observationId` y tipo `UNKNOWN`, y solo entonces confirma el mensaje de entrada con ACK. La publicación usa confirmación del broker y `mandatory`; si no se confirma, el simulador no ACKea y termina, cerrando la conexión para que RabbitMQ pueda volver a entregar el mensaje pendiente. Un mensaje de entrada malformado recibe NACK sin reencolar. Detén el simulador con **Ctrl+C**; los mensajes que sigan sin ACK se reencolan al cerrar la conexión.

Este modo permite medir la ruta API→RabbitMQ→respuesta→estado en backend sin BioCLIP, pero no valida el costo ni el rendimiento de inferencia real.

## Ejecución

Después de configurar `load-tests/.env`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\run.ps1 -Scenario mobile
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\run.ps1 -Scenario admin
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\run.ps1 -Scenario observations
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\run.ps1 -Scenario observation-smoke
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\run.ps1 -Scenario combined-http
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\run.ps1 -Scenario combined
```

Los comandos usan `Bypass` solo en ese proceso de PowerShell; no cambian la política permanente del equipo. `mobile` y `admin` se pueden ejecutar sin la fotografía/worker, aunque el archivo `.env.example` incluye el grupo de variables para los otros escenarios. `observations`, `observation-smoke` y `combined` requieren los servicios, credenciales, imagen y ventana de observación; se puede usar el fake worker para omitir BioCLIP-2.

`combined-http` ejecuta únicamente consultas móviles y administrativas; necesita `API_BASE_URL`, `MOBILE_TOKEN` y `ADMIN_TOKEN`, pero no requiere imagen, worker, RustFS ni RabbitMQ.

`observation-smoke` reutiliza `prepareObservations`, `observationFlow` y `finishObservations` de `common.js`, con un solo VU y una iteración compartida. Requiere `ADMIN_TOKEN`, `TEST_IMAGE_PATH`, `TEST_IMAGE_CONTENT_TYPE`, coordenadas, `PROCESSING_CONFIRM_TIMEOUT_SEC`, la API y RustFS; inicia RabbitMQ y el fake worker antes de ejecutarlo. La respuesta del fake worker es sintética: esta prueba no carga ni valida BioCLIP-2.

El smoke no ejecuta `summarize-rabbit.ps1`: `finishObservations` toma muestras `before` y `after`, pero el resumen requiere también muestras `during`. Para consultar la cola de entrada manualmente, en una sesión con `RABBITMQ_USER`, `RABBITMQ_PASSWORD` y `RABBITMQ_MANAGEMENT_URL` cargadas desde el `.env` aislado:

```powershell
$pair = "{0}:{1}" -f $env:RABBITMQ_USER, $env:RABBITMQ_PASSWORD
$basicAuth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$queueUrl = "{0}/api/queues/%2F/observation.jobs" -f $env:RABBITMQ_MANAGEMENT_URL.TrimEnd("/")
Invoke-RestMethod -Uri $queueUrl -Headers @{ Authorization = "Basic $basicAuth" } |
  Select-Object name, messages, messages_ready, messages_unacknowledged, consumers
```

Los valores por defecto son los requisitos sostenidos. Para una prueba corta previa se pueden fijar `DURATION`, `MOBILE_VUS`, `ADMIN_VUS` y `OBSERVATION_RATE`; ese resultado es solo smoke/preflight y no evidencia RNF-01/02/07. En observaciones, la tasa de llegada se mantiene 15 minutos; después la ventana de espera configurable permite observar las últimas solicitudes y el monitor de RabbitMQ sigue tomando muestras antes de la lectura final.

## Datos y limpieza

- Ejecutar únicamente con la pila `biojaveriana-loadtest`, usuarios de prueba y bucket separado.
- Cada envío genera una key nueva bajo `observations/{userId}/...`; crea filas de observación/job persistentes y no hay endpoint de borrado de observaciones en la API.
- No borrar datos por API ni ejecutar SQL de limpieza en una base compartida. Después de guardar resultados, apagar y eliminar solo los volúmenes de este proyecto aislado:

  ```powershell
  docker compose -p biojaveriana-loadtest --env-file load-tests/.env -f load-tests/compose.yml down -v
  ```

  `-v` elimina permanentemente los datos de esta pila de carga. Confirmar el nombre del proyecto y archivo compose antes de ejecutarlo.
- No incluye eliminación de contenedores/volúmenes de otros proyectos.

## Trazabilidad y limitaciones

| RNF | Escenario | Estado actual |
|---|---|---|
| RNF-01 | Mobile | Escenario configurado; pendiente de ejecución sostenida 50/15 min. |
| RNF-02 | Admin | Escenario configurado; pendiente de ejecución sostenida 3/15 min simultánea con móvil. |
| RNF-04 | Mobile | Umbral P95 configurado; pendiente de evidencia real. |
| RNF-05 | Admin | Umbral P95 configurado; pendiente de evidencia real. |
| RNF-07 | Observations/combined | Generación y métricas de cola preparadas; pendiente worker real, prueba y análisis de tendencia del backlog. |
| Gamificación | No disponible | No se encontró endpoint/capacidad implementada; no se puede medir. |
| Mapa | Parcial/no disponible | Hay listado de especímenes, pero no endpoint de mapa o búsqueda geoespacial en el backend revisado. |
| RNF-06 | Excluido | No se evalúa ni se declara conforme en esta fase. |

## Riesgos antes de una ejecución oficial

- El modelo BioCLIP-2, su arranque y el recurso CPU/GPU influyen en las observaciones; el resultado incluye el worker y no solo el backend.
- La prueba escribe observaciones, jobs y objetos; no es una prueba de solo lectura y puede acumular datos/almacenamiento.
- El rate limit excluye roles admin/superadmin; por eso el escenario C utiliza un usuario administrador aislado. No representa envío de observaciones por cuentas normales.
- `constant-arrival-rate` puede registrar `dropped_iterations` si el presign, la subida, la inferencia o el polling mantienen ocupados demasiados VUs. Revisar esta métrica antes de afirmar que se entregaron diez por minuto.
- Las series de cola requieren Management Plugin disponible y permisos para consultar la cola.
- Sin ejecución real no se declara conforme ningún RNF ni se reporta throughput alcanzado.
