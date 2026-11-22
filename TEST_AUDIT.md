# Auditoria automatizada del backend

Fecha: 2026-11-21

## Resumen de ejecucion

Ejecutado:

- `.\gradlew.bat test`
- Resultado inicial: 142 pruebas, 141 aprobadas y 1 fallida por intento de conexion a PostgreSQL real desde `ApiApplicationTests`.
- Resultado final: 148 pruebas, 145 aprobadas, 3 omitidas y 0 fallidas.
- Las 3 pruebas omitidas son pruebas Docker/Testcontainers de PostgreSQL porque Docker no estaba disponible en el entorno.
- La prueba `ApiApplicationTests` fue corregida para usar el perfil aislado `test` con H2 en modo PostgreSQL.

## Cobertura nueva

| Requisito | Cobertura nueva | Tipo | Estado |
|---|---|---|---|
| RNF-14 | BCrypt costo 12, sal unica, coincidencia y rechazo de clave incorrecta | Prueba unitaria | Implementada y ejecutada |
| RNF-20 | Cadena maliciosa contra derivadas de `SpeciesRepository`; confirma que no modifica datos | Prueba JPA aislada | Implementada y ejecutada |
| RNF-21/RNF-22 | Restricciones unicas, identificadores UUID y restricciones de votos/reacciones en PostgreSQL | Testcontainers PostgreSQL | Implementada, omitida sin Docker |
| RNF-11/RNF-12/RNF-23/RNF-06/RNF-07 | No se afirman cumplidas sin base real, Docker, RabbitMQ o worker disponibles | Integracion externa | Pendiente |

## Cobertura existente

La suite existente contiene 142 pruebas distribuidas en controladores, servicios, autenticacion, JWT, usuarios, publicaciones, comentarios, especies, especimenes, observaciones y almacenamiento S3. La revision encontro cobertura unitaria/MVC real para:

- Login, registro, refresh y validaciones HTTP.
- Servicios de usuarios, publicaciones, comentarios y observaciones.
- Estados y reintentos de `ObservationJobService`.
- Publicacion presignada y operaciones S3.
- Servicios de especies y especimenes.

No fueron halladas pruebas existentes de Spring Data/JPA con PostgreSQL real ni pruebas de RabbitMQ real.

## Hallazgos de codigo

1. El contexto de prueba intentaba conectarse a PostgreSQL con usuario/password locales por defecto y no podia determinar el dialecto. Ahora `ApiApplicationTests` usa el perfil `test`.
2. El pool configurado no declara `maximum-pool-size`; Hikari usa su valor por defecto. RNF-23 necesita fijar y documentar el limite explicitamente y contrastarlo con `max_connections` de PostgreSQL.
3. No existe una entidad ni implementacion para zonas visitadas, logros o avance de logros. Por ello RNF-21 no puede verificarlos.
4. Solo `scientific_name` es unico en `Species`; `common_name` no tiene restriccion unica en el modelo actual.
5. `Report` no declara restricciones unicas para evitar denuncias duplicadas del mismo usuario sobre el mismo contenido. La deduplicacion debe comprobarse en base de datos, no solo en el servicio.
6. Las consultas observadas usan parametros nombrados o derivadas de Spring Data. No se encontraron SQL concatenado con entradas del cliente.
7. `ObservationJob` expira en 24 horas por configuracion, mientras RNF-06 exige cambiar a FAILED a los 120 segundos sin resultado. No se encontro un scheduler que ejecute esa transicion a 120 segundos.
8. RNF-15 es contradictorio: el SRS establece bloqueo despues de 20 fallos en 15 minutos y tambien indica VF-01 como funcionalidad futura. El codigo no implementa bloqueo de intentos.

## Pendientes

No automatizables o pendientes desde el backend:

- RNF-01 y RNF-02: carga con 50 usuarios mobiles y 3 administradores durante 15 minutos.
- RNF-03: memoria, reinicios y alarma de memoria de RabbitMQ.
- RNF-04 y RNF-05: percentiles p95 bajo carga.
- RNF-06/RNF-07: medicion real del worker, 120 segundos y throughput de 10 observaciones/minuto.
- RNF-13: monitor externo mensual.
- RNF-16, RNF-17 y RNF-18: despliegue y documentacion.
- RNF-09 y RNF-10: arquitectura, diagramas y revision tecnologica.
- RNF-11/RNF-12: reinicio persistente, concurrencia y integridad real con PostgreSQL/RabbitMQ.
- RNF-23: conexion real de carga y liberacion bajo presion.
- RNF-15: pendiente de aclaracion por ambiguedad VF-01.
