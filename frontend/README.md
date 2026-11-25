# Voluntia

Frontend de Voluntia para buscar y gestionar campañas de voluntariado ambiental y social. Usa Angular 20 standalone, TypeScript, CSS, Angular Material, formularios reactivos, Angular Router y servicios HTTP con RxJS.

## Ejecutar en Windows

Arranca primero PostgreSQL y el backend Spring Boot en http://localhost:8081. Desde esta carpeta, con Node.js 22 LTS o Node.js 24 y npm:

```powershell
npm.cmd ci
npm.cmd start
```

Abre http://localhost:4200. Para generar el build de producción, ejecuta `npm.cmd run build`. Para las pruebas con Chrome instalado, ejecuta `npm.cmd test -- --watch=false --browsers=ChromeHeadless`.

## Conexión con la API

Los componentes llaman a `VoluntiaService` y `SesionService`. Los modelos en `src/app/models/voluntia.ts` describen los datos que esperan las pantallas. Angular envía las solicitudes a http://localhost:8081/api. Los cambios de URL se configuran en `src/app/services/configuracion.ts`. Usa el correo configurado mediante `ADMIN_EMAIL` y la clave `ADMIN_PASSWORD` del backend para ingresar como administrador.

| Recurso bajo la URL base | Operaciones que usa Angular |
| --- | --- |
| `/auth/sesion`, `/auth/ingresar`, `/auth/registro`, `/auth/salir` | GET de sesión, POST de ingreso, registro y cierre. La sesión devuelve una cuenta o `null`. |
| `/mi-cuenta` | PUT del perfil autenticado. |
| `/campanias`, `/campanias/:id` | Listar, obtener, publicar, editar y cancelar. El listado admite `q`, `ubicacion`, `categoria` y `gestion=true`. |
| `/organizaciones`, `/voluntarios` | GET de listas para administración, PUT y DELETE de una cuenta por ID. |
| `/organizaciones/:id`, `/organizaciones/:id/campanias` | GET del perfil público y sus próximas campañas. |
| `/mis-participaciones`, `/campanias/:id/mi-inscripcion` | GET de historial e inscripción actual, POST de inscripción y DELETE de cancelación. |
| `/campanias/:id/participantes`, `/campanias/:id/participantes/:inscripcionId` | GET del listado, PUT para marcar asistencia y DELETE para retirar. |
| `/impacto`, `/campanias/:id/impacto` | GET del consolidado disponible según el rol, GET y PUT de resultados por campaña. |

El contrato de frontend contempla errores JSON con `mensaje`, respuestas sin contenido para bajas y cancelaciones, y solicitudes con credenciales para sesiones del servidor. Spring Security aplica los permisos y valida el token CSRF en las operaciones de escritura. El origen local autorizado por defecto es http://localhost:4200.

El asistente y el mapa tienen espacios reservados para una integración futura.
