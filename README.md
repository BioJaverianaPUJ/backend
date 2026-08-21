# BioJaveriana - Backend (API)

## Información

Este repositorio contiene el código del backend de **BioJaveriana**, desarrollado en **Java utilizando Spring Boot** y desplegado en un VPS en la nube.

Para obtener información sobre el desarrollo y las contribuciones al proyecto, consulte las secciones correspondientes a continuación.

## Desarrollo

Para el desarrollo local se requieren los siguientes componentes:

- Tener instalada una versión compatible de **Docker (>= 29)**.
- Tener instalada la extensión **Docker Dev Containers** en el entorno de desarrollo.

**Dev Containers** es una herramienta que permite desarrollar dentro de un entorno de Docker preconfigurado. Esto garantiza que todos los desarrolladores utilicen versiones y dependencias consistentes, evitando problemas relacionados con diferencias entre entornos locales.

### Configuración

Clone el repositorio y ábralo utilizando su editor de código compatible con Dev Containers.

Una vez abierto el proyecto, utilice la opción:

> **Dev Containers: Reopen in Container**

El entorno de desarrollo se encargará de crear el contenedor necesario para el backend y de iniciar los servicios requeridos, incluyendo una instancia local de **PostgreSQL**.

La estructura general del entorno es:

```text
┌───────────────────────────────┐
│        Dev Container          │
│                               │
│        Spring Boot API        │
└───────────────┬───────────────┘
                │
                │ PostgreSQL
                ▼
┌───────────────────────────────┐
│       PostgreSQL Container    │
│                               │
│       Base de datos local     │
└───────────────────────────────┘
```

Una vez iniciado el contenedor, el proyecto puede ejecutarse normalmente utilizando las herramientas de desarrollo disponibles dentro del entorno.

### Base de datos

La instancia local de PostgreSQL es administrada mediante Docker Compose. Los datos se almacenan en un volumen de Docker para evitar que se pierdan al reiniciar los contenedores.

Desde el contenedor de desarrollo, PostgreSQL debe ser accesible utilizando el nombre del servicio definido en Docker Compose, en lugar de localhost.

Por ejemplo:
```
postgres:5432
```

Desde el equipo host, la base de datos puede ser accesible mediante:
```
localhost:5432
```

Las credenciales y demás parámetros de conexión utilizados durante el desarrollo se encuentran definidos en la configuración de Docker Compose y de Spring Boot.

## Contribuciones

Para contribuir al repositorio se deben realizar Pull Requests (PR).

Los cambios pequeños y de bajo riesgo pueden realizarse directamente sobre `main`, siempre que esto sea apropiado para el cambio.

Para cambios más grandes, nuevas funcionalidades o modificaciones que puedan afectar otras partes del sistema, se debe crear una rama independiente a partir de `main`.

Las ramas de desarrollo deben mantenerse actualizadas con `main` durante el desarrollo para reducir conflictos al momento de realizar el merge.

Una vez finalizado el desarrollo:

1. Verifique que el proyecto compile correctamente.
2. Ejecute las pruebas disponibles.
3. Actualice la rama con los cambios más recientes de `main`.
4. Cree un Pull Request hacia `main`.
5. Espere la revisión y aprobación correspondiente antes de realizar el merge.

Solamente la rama `main` estará desplegada en el ambiente de producción.

### Convenciones de ramas

Se recomienda utilizar nombres descriptivos para las ramas. Por ejemplo:

```
feature/nombre-de-la-funcionalidad
fix/descripcion-del-error
refactor/descripcion-del-cambio
```

Las ramas deben tener un único propósito y evitar mezclar funcionalidades o correcciones no relacionadas.

> [!WARNING]
> Si encuentras un error en tu rama, corrígelo en una rama de corrección (hotfix/bugfix), haz un Pull Request hacia `main` y luego actualiza tu rama. Ramas marcadas como hotfix/bugfix cuentan con prioridad.