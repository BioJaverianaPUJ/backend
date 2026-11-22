# Informe de ejecución de carga — 2026-11-22

## Resultado

**No se inició ninguna prueba de carga.** El preflight encontró requisitos faltantes que impedirían ejecutar los escenarios completos o validar su resultado. No hay latencias, percentiles, tasa de errores ni throughput de carga que reportar. No se declaran RNF cumplidos.

No se enviaron observaciones ni se cargaron archivos. Solo se consultó la API de administración de RabbitMQ para verificar el estado de la cola.

## Estado del preflight

| Componente/requisito | Observación |
|---|---|
| PostgreSQL de prueba | Contenedor `postgres` en ejecución. |
| RabbitMQ | Contenedor en ejecución; la API de administración respondió. |
| Cola `observation.jobs` | 0 consumidores, 0 mensajes listos y 0 mensajes no reconocidos al consultar. |
| RustFS | Contenedor en ejecución; puertos S3 19000 y consola 19001 escuchando. No se verificó la existencia del bucket. |
| Backend | Puerto local 8000 escuchando. No se pudo comprobar la autenticación porque faltan los tokens de prueba. |
| k6 | No está instalado o no está disponible en `PATH`. |
| Configuración de carga | `MOBILE_TOKEN`, `ADMIN_TOKEN` y `PROCESSING_CONFIRM_TIMEOUT_SEC` no están configurados. |
| Fotografía de prueba | `TEST_IMAGE_PATH` vacío o no apunta a un archivo existente. |
| Worker | No se observan consumidores en la cola; el flujo de procesamiento no está listo. |

Los valores secretos no se leyeron ni se incluyeron en este informe.

## Escenarios

| Escenario | Estado |
|---|---|
| `mobile` | No ejecutado: k6 y token móvil faltantes. |
| `admin` | No ejecutado: k6 y token administrativo faltantes. |
| `observations` | No ejecutado: k6, fotografía, ventana de observación y worker/consumidor faltantes. |
| `combined` | No ejecutado: faltan requisitos de los escenarios anteriores. |

## Siguiente paso

1. Instalar k6 y confirmar que `k6` esté disponible en `PATH`.
2. Obtener tokens de cuentas de prueba con roles `USER` y `ADMIN` y configurarlos en `load-tests/.env`.
3. Configurar `TEST_IMAGE_PATH` a una fotografía local válida y asignar un valor positivo a `PROCESSING_CONFIRM_TIMEOUT_SEC`.
4. Crear/verificar el bucket aislado `biojaveriana-loadtest` y arrancar el worker contra RabbitMQ/RustFS locales. Confirmar que `observation.jobs` tenga al menos un consumidor.
5. Repetir el preflight y, cuando esté listo, ejecutar desde la raíz del repositorio:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\load-tests\run.ps1 -Scenario combined
   ```

El escenario combinado conserva los valores de carga predeterminados del SRS: 50 usuarios móviles, 3 administradores y 10 observaciones por minuto durante 15 minutos. La conclusión sobre RNF-07 requiere revisar las muestras RabbitMQ del resultado; este preflight no aporta evidencia de rendimiento.
