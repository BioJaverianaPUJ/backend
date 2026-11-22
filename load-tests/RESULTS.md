# Resultados de pruebas de carga y estabilidad

## Objetivo y alcance

Documentar las ejecuciones de carga y estabilidad registradas para BioJaveriana, comparando los percentiles observados con los umbrales de RNF-04 y RNF-05 y describiendo el resultado limitado de RNF-07. Las ejecuciones corresponden al entorno local aislado y no son una certificación del sistema en producción.

**Fecha de las ejecuciones:** 22 de noviembre de 2026.

## Entorno de pruebas y aislamiento

Los scripts k6 validan que la API, la API de administración RabbitMQ y la URL presignada de almacenamiento apunten a `localhost` o loopback; se niegan a usar destinos externos. El registro de estabilidad identifica contenedores con prefijo `biojaveriana-loadtest-` para PostgreSQL, RabbitMQ y RustFS. Por tanto, la evidencia corresponde a la pila local de carga, no a producción.

Los archivos de resumen JSON de k6 incluyen `setup_data`; no se copian aquí sus valores. Deben tratarse como datos sensibles y revisarse/sanitizarse antes de compartirlos.

## Metodología y escenarios

- **Móvil (`k6/mobile.js`):** escenario `constant-vus`, con 50 VUs y 15 minutos por defecto; umbral configurado para P95 móvil ≤ 1.000 ms.
- **Carga combinada HTTP (`k6/combined-http.js`):** escenarios móvil y administrativo simultáneos, con 50 y 3 VUs y 15 minutos por defecto; umbrales P95 móvil ≤ 1.000 ms y administrativo ≤ 2.000 ms.
- **Observaciones (`k6/observations.js`):** `constant-arrival-rate` de 10 iteraciones por minuto durante 15 minutos por defecto. El flujo carga la imagen, crea la observación y consulta el estado hasta completarse, fallar o agotar el timeout configurado. Un VU adicional muestrea RabbitMQ.
- Las métricas de completitud, tiempo de procesamiento y RabbitMQ se definen en `k6/common.js`.

Los resúmenes y scripts fueron inspeccionados directamente para preparar este informe. No se ejecutaron pruebas nuevas.

## Tabla de resultados por RNF

| Requisito | Resultado observado | Interpretación |
|---|---|---|
| RNF-01 | 50 VUs móviles durante 15 minutos; 22.505 solicitudes; 22.505/22.505 checks exitosos; P95 26,03 ms. | Ejecución realizada. El P95 observado está por debajo de 1 s; no constituye una garantía fuera de esta corrida. |
| RNF-02 | 50 VUs móviles y 3 administrativos durante 15 minutos, en dos ejecuciones. Solicitudes: 24.159 y 24.137. P95 móvil: 26,65 ms y 27,86 ms; administrativo: 29,51 ms y 30,39 ms. | Ejecuciones realizadas. En ambas, los P95 observados están por debajo de los umbrales; no constituye una garantía fuera de estas corridas. |
| RNF-03 | 31 muestras durante la carga combinada. RabbitMQ reportó cero alarmas; PostgreSQL, RabbitMQ y RustFS reportaron `RestartCount=0` y `OOMKilled=false`. | No se observaron esos eventos en las muestras/contenedores registrados. No demuestra ausencia de eventos entre muestras ni cubre componentes no incluidos en el TXT. |
| RNF-04 | P95 móvil de 26,03 ms en la prueba móvil y de 26,65 ms / 27,86 ms en las combinadas. | Por debajo de 1 s en las ejecuciones observadas; no es una declaración de cumplimiento integral. |
| RNF-05 | P95 administrativo de 29,51 ms y 30,39 ms en las combinadas. | Por debajo de 2 s en las ejecuciones observadas; no es una declaración de cumplimiento integral. |
| RNF-07 | 150/150 observaciones completadas, 10/min durante 15 minutos, con worker sintético. Media aproximada 2,01 s; P95 2,03 s. 955 muestras `during` por cada métrica RabbitMQ; máximo de `ready`, `unacked` y total: 0. | Solo evidencia del flujo de infraestructura con worker sintético. **No se declara RNF-07 cumplido end-to-end: NO se probó BioCLIP-2 real ni se valida el rendimiento del modelo.** |

Los valores de latencia a dos decimales reproducen las cifras reportadas truncando decimales adicionales. Los archivos de resumen conservan mayor precisión.

## Detalle por requisito

### RNF-01 — Carga móvil

La ejecución móvil registrada alcanzó 50 VUs y duró aproximadamente 15 minutos. Se contabilizaron **22.505 solicitudes** y **22.505 checks exitosos, sin checks fallidos**. El P95 de duración HTTP fue **26,03 ms**, inferior a 1 segundo.

### RNF-02 — Carga web concurrente con móvil

Se registraron dos ejecuciones de `combined-http`, cada una con 50 VUs móviles y 3 administrativos concurrentes, durante aproximadamente 15 minutos:

| Ejecución | Solicitudes | P95 móvil | P95 administrativo |
|---|---:|---:|---:|
| 1 | 24.159 | 26,65 ms | 29,51 ms |
| 2 | 24.137 | 27,86 ms | 30,39 ms |

En ambas, los checks registrados fueron exitosos y no se reportaron solicitudes HTTP fallidas. El P95 móvil fue inferior a 1 segundo y el P95 administrativo inferior a 2 segundos.

### RNF-03 — Estabilidad observada

El registro de monitorización contiene **31 muestras** entre las 05:14:38 y las 05:31:26 del 22 de noviembre, abarcando la segunda ejecución combinada. En las muestras, RabbitMQ reportó que no había alarmas locales ni del clúster. PostgreSQL, RabbitMQ y RustFS mostraron `RestartCount=0` y `OOMKilled=false`.

Esto describe lo observado en esas muestras y en esos contenedores; no equivale a monitorización continua de todos los componentes.

### RNF-04 y RNF-05 — Latencia

- **RNF-04:** el P95 móvil fue 26,03 ms en la ejecución móvil y 26,65 ms y 27,86 ms en las dos ejecuciones combinadas. Los valores observados son inferiores al límite de 1.000 ms.
- **RNF-05:** el P95 administrativo fue 29,51 ms y 30,39 ms en las dos ejecuciones combinadas. Los valores observados son inferiores al límite de 2.000 ms.

Los resultados son evidencia de estas corridas locales, no de todas las condiciones posibles ni de producción.

### RNF-07 — Procesamiento de observaciones

El escenario configurado en `k6/observations.js` envió 10 observaciones por minuto durante 15 minutos. El resumen registra **150 completadas de 150**. El procesamiento medido desde la creación hasta el estado final tuvo:

- **Media:** 2.017,33 ms (2,01 s truncado a dos decimales).
- **P95:** 2.033 ms (2,03 s truncado a dos decimales).

El NDJSON registra **955 muestras `during` para cada una** de las métricas `rabbit_messages_ready`, `rabbit_messages_unacked` y `rabbit_messages_total`. Además, cada métrica tiene una muestra `before` y una `after`. El máximo observado fue **0** para las tres métricas en todo el registro.

**Limitación importante:** esta ejecución usó un worker sintético. Valida el recorrido de carga, publicación, procesamiento simulado y consulta de estado junto con la observación de RabbitMQ; **NO se probó BioCLIP-2 real y no se valida el rendimiento, estabilidad ni los resultados de inferencia del modelo**. Por ello, RNF-07 no se declara cumplido para el worker real.

## Evidencias: archivos consultados

### Resúmenes y registros de resultados

- Móvil: `results/mobile-20261122-031551.json`; [results/mobile-20261122-031551-metrics.ndjson](./results/mobile-20261122-031551-metrics.ndjson).
- Combinada, primera ejecución: `results/combined-http-20261122-040217.json`; [results/combined-http-20261122-040217-metrics.ndjson](./results/combined-http-20261122-040217-metrics.ndjson).
- Combinada, segunda ejecución: `results/combined-http-20261122-051455.json`; [results/combined-http-20261122-051455-metrics.ndjson](./results/combined-http-20261122-051455-metrics.ndjson).
- Observaciones: `results/observations-20261122-131513.json`; [results/observations-20261122-131513-metrics.ndjson](./results/observations-20261122-131513-metrics.ndjson).
- Estabilidad: [results/rnf03-monitor-20261122-051438.txt](./results/rnf03-monitor-20261122-051438.txt).

Los escenarios y la definición de las métricas se encuentran en [k6/mobile.js](./k6/mobile.js), [k6/combined-http.js](./k6/combined-http.js), [k6/observations.js](./k6/observations.js) y [k6/common.js](./k6/common.js).

## Limitaciones y riesgos

- Las mediciones son de una pila local aislada y no representan carga, red, datos ni recursos de producción.
- Las muestras de estabilidad son periódicas: no prueban qué ocurrió entre muestras. El TXT solo cubre los contenedores que enumera.
- El resumen k6 de observaciones registra resultados del flujo HTTP/estado y métricas de cola, pero no identifica por sí mismo qué implementación de worker consumió cada mensaje. El uso del worker sintético corresponde al contexto de esta ejecución.
- Las respuestas sintéticas no evalúan clasificación, calidad, latencia ni consumo de recursos de BioCLIP-2.
- Los JSON de resumen contienen `setup_data`; deben protegerse y sanitizarse antes de compartirlos. No se reproducen sus valores en este informe.

## Requisitos pendientes de validar

- **RNF-07 con el worker real BioCLIP-2:** repetir el escenario con el modelo real para medir su procesamiento y validar el flujo end-to-end. Hasta entonces, el resultado con worker sintético no demuestra el rendimiento del modelo ni permite declarar RNF-07 integralmente cumplido.
- **RNF-03 de forma continua y para toda la aplicación:** ampliar la monitorización para cubrir todos los componentes relevantes y detectar eventos entre muestras.
- Los resultados disponibles no demuestran el cumplimiento integral de todos los RNF ni de condiciones de producción. RNF-06 no forma parte de estas ejecuciones.

## Conclusiones

Las corridas registradas alcanzaron las cargas definidas para los escenarios móvil y combinado. Los P95 medidos quedaron por debajo de los umbrales de RNF-04 y RNF-05 en estas ejecuciones locales. Durante las 31 muestras de estabilidad no se reportaron alarmas de RabbitMQ ni reinicios/OOM en los tres contenedores enumerados. El escenario de observaciones completó 150/150 jobs y mantuvo en cero las métricas observadas de RabbitMQ con un worker sintético.

Estos resultados no permiten afirmar que el sistema completo cumple todos los RNF. En particular, RNF-07 permanece pendiente de validar con BioCLIP-2 real; los demás hallazgos se limitan a las ejecuciones y evidencias citadas.
