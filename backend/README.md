# Voluntia

API REST de VoluntaRed para gestionar voluntarios, organizaciones y campañas ambientales y sociales. Incluye los CRUD y el acceso autónomo por perfil, usando las tecnologías y capas de los manuales proporcionados.

## Ejecutar en Windows

Requisitos: Java 17, PostgreSQL activo en localhost:5432 y Node.js 22 LTS o 24 para Angular.

Desde la carpeta del backend:

```powershell
.\preparar-bd.cmd
$env:ADMIN_PASSWORD = 'reemplaza-con-tu-clave-de-administración'
.\iniciar.cmd
```

Desde una segunda terminal en la carpeta hermana `Voluntia_Angular`:

```powershell
npm.cmd ci
npm.cmd start
```

Abre http://localhost:4200. Angular usa Spring Boot en http://localhost:8081/api. En IntelliJ, importa el `pom.xml`, selecciona JDK 17 y configura las variables de entorno en la ejecución de `VoluntiaApplication`.

El administrador entra como `admin@voluntia.local` en Angular. Usa `ADMIN_EMAIL` para cambiar el correo reservado. El acceso con ese correo requiere `ADMIN_PASSWORD`. Las cuentas administrativas aprovisionadas en PostgreSQL con otro correo usan su propia contraseña BCrypt. Para un origen Angular diferente, configura `ANGULAR_ORIGINS` en Spring Boot como una lista de orígenes separados por comas y ajusta la URL en Angular si cambia el backend.

## Base de datos

El backend usa `VoluntiaDB` en PostgreSQL y valida el esquema al iniciar. `preparar-bd.cmd` crea la base si falta, aplica `database/schema.sql` y ejecuta la migración repetible `database/integracion-angular.sql`. Antes de migrar una instalación existente, guarda un respaldo con pgAdmin o `pg_dump`. También aplica `database/roles.sql` para crear el catálogo de roles y validar su referencia desde `usuario.rol`. Las campañas anteriores reciben la categoría `Social` como valor inicial. Las cuentas existentes con contraseña BCrypt conservan su acceso.

Configura `DB_URL`, `DB_USER` y `DB_PASSWORD` en Spring Boot si cambias la conexión. El script de preparación local usa `DB_USER` y `DB_PASSWORD` y apunta a `VoluntiaDB` en localhost. Para pruebas Maven, prepara una base separada con el esquema seguido de la migración y pasa su URL mediante `DB_URL` antes de ejecutar `.mvnw.cmd test`.

## Funciones disponibles

- Catálogo Angular por texto, ubicación y categoría. Las organizaciones publican y gestionan sus propias campañas.
- Registro, inicio y cierre de sesión con cookie del servidor, Spring Security, BCrypt y protección CSRF. El backend verifica cada rol y la propiedad de los recursos.
- Inscripción, cancelación, historial, asistencia y control de cupos. Angular conserva las inscripciones canceladas y las asistencias.
- Resultados de impacto por campaña y dashboard por organización o consolidado.
- Administración de cuentas con baja lógica. La baja de una organización cancela sus campañas futuras y conserva su historial. La de un voluntario libera sus inscripciones pendientes y conserva sus asistencias.

## Estructura

```text
src/main/java/com/voluntared/voluntia/
  entity/       Entidades JPA y relaciones
  repository/   Interfaces JpaRepository
  service/      Reglas de negocio y transacciones
  dto/          Datos de entrada/salida sin exponer contraseñas
  controller/   Controladores de la API REST
  config/       Acceso por perfil con Spring Security
src/main/resources/
  static/css/   Diseño visual
  static/js/    Confirmaciones, búsqueda y control de envío
database/       Scripts PostgreSQL
scripts/        Preparación e inicio para Windows
```

## API

| Recurso | Acceso y operaciones |
| --- | --- |
| /api/auth/sesion | GET devuelve la cuenta actual o null |
| /api/auth/ingresar, /api/auth/registro, /api/auth/salir | POST para iniciar sesión, crear una cuenta de voluntario u organización y cerrar sesión |
| /api/mi-cuenta | PUT del perfil propio de voluntario u organización |
| /api/campanias | Catálogo público con q, ubicacion y categoria. gestion=true requiere organización o administración. POST publica como organización |
| /api/campanias/{id} | GET público. PUT y DELETE disponibles para la propietaria o administración |
| /api/organizaciones/{id}, /api/organizaciones/{id}/campanias | Perfil y próximas campañas públicas |
| /api/voluntarios, /api/organizaciones | Administración de cuentas. DELETE por ID desactiva la cuenta |
| /api/mis-participaciones, /api/campanias/{id}/mi-inscripcion | Historial, inscripción y cancelación para el voluntario autenticado |
| /api/campanias/{id}/participantes, /api/campanias/{id}/participantes/{inscripcionId} | Lista, asistencia y retiro para la propietaria o administración |
| /api/impacto, /api/campanias/{id}/impacto | Dashboard por rol y lectura o actualización de resultados por campaña |

## Configuración y pruebas

Usa una base de prueba para ejecutar las pruebas que persisten datos.

```powershell
.\iniciar.cmd -Pruebas
```

Las pruebas de integración usan PostgreSQL. Las pruebas transaccionales revierten sus registros; la prueba de concurrencia crea cuentas y campañas de prueba. Verifican los CRUD, registro, inicio de sesión, perfil propio, sesión tras editar el correo, permisos sobre cuentas y campañas ajenas, participación, retiro sin eliminar cuentas, conservación de asistencia, API, CSRF y solicitudes simultáneas.

## Alcance

Spring Boot expone la API JSON bajo /api. La interfaz reside en el proyecto Voluntia_Angular. El asistente y el mapa quedan reservados para una etapa posterior.
