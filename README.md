# HCG Multiservicios – Sistema de gestión

Backend: Spring Boot (Java 21) + PostgreSQL + JPA + Flyway. Frontend: React + Vite (en construcción según el diseño de Figma).

## Backend

```bash
cd backend
# 1. Defina las variables de .env.example en su entorno (al menos DB_*, HCG_JWT_SECRET y HCG_ADMIN_*)
# 2. Pruebas (usan H2 en memoria, no requieren PostgreSQL)
./mvnw test
# 3. Ejecutar
./mvnw spring-boot:run
```

### Base de datos
- No se crea ninguna base nueva: use la que ya tiene en `DB_URL`.
- Hibernate **solo valida** (`ddl-auto=validate`); el esquema lo gestiona Flyway con migraciones **aditivas** en `src/main/resources/db/migration`.
- Si su BD ya tiene las tablas del diagrama y no tiene historial de Flyway, se toma como versión 1 y solo se aplican V2 en adelante. Haga un respaldo antes del primer arranque.
- V1 = esquema del diagrama actual (`IF NOT EXISTS`). V2 = usuarios, roles, permisos y auditoría.

### Seguridad (Fase 1)
- `POST /api/auth/login` → JWT (HS256). El token solo identifica al usuario: roles y permisos se leen de la BD en cada petición, por lo que desactivar un usuario o cambiar permisos surte efecto de inmediato.
- Roles: `ADMIN` (todos los permisos, siempre) y `EMPLEADO` (solo lo que el admin asigne). Se pueden crear más roles con `POST /api/roles`.
- Permisos con formato `MODULO_ACCION` (p. ej. `CLIENTES_VER`, `VENTAS_CREAR`). Lista: `GET /api/permissions`.
- Bloqueo temporal tras 5 intentos fallidos; contraseñas con BCrypt; auditoría de login, usuarios, roles y permisos en `/api/audit`.
- Errores siempre con el formato `{"success":false,"message":"...","errorCode":"..."}`.

| Endpoint | Permiso |
|---|---|
| `GET /api/users`, `GET /api/users/{id}` | `USUARIOS_VER` |
| `POST /api/users` | `USUARIOS_CREAR` |
| `PUT /api/users/{id}`, `PATCH /api/users/{id}/status` | `USUARIOS_EDITAR` |
| `PUT /api/users/{id}/roles`, `PUT /api/users/{id}/password`, `POST /api/roles`, `PUT /api/roles/{id}/permissions` | rol `ADMIN` |
| `GET /api/roles`, `GET /api/permissions` | `USUARIOS_VER` |
| `GET /api/audit` | `AUDITORIA_VER` |
| `GET /api/auth/me`, `POST /api/auth/change-password` | cualquier usuario autenticado |

## Catálogos (Fase 2)

Clientes, proveedores y productos/servicios sobre las tablas existentes (`cliente`, `proveedores`, `producto`). La migración `V3` solo **agrega** columnas e índices; no cambia ni borra nada. Productos y servicios comparten la tabla `producto` y se distinguen por la columna `tipo` (`PRODUCTO` / `SERVICIO`).

- Validación de **RUC** (11 dígitos, prefijo válido y dígito verificador) y **DNI** (8 dígitos), en el backend con mensajes por campo.
- RUC/DNI únicos (409 `DUPLICATE_RUC` / `DUPLICATE_DNI`); códigos autogenerados `CLI-000001`, `PRD-000001`, `SRV-000001`.
- Búsqueda `?q=` (código, RUC, DNI, nombre, razón social…), filtro `?activo=true|false` y paginación `?page=&size=`.
- Activar/desactivar con `PATCH /{id}/status` (no se borran registros).
- Los precios nuevos usan `precio_venta`/`precio_compra` `NUMERIC(12,2)`; la columna original `precio` (FLOAT) queda intacta.
- Los servicios no admiten stock, stock mínimo ni precio de compra.

| Recurso | Endpoints | Permisos |
|---|---|---|
| Clientes | `/api/clients` (GET, GET /{id}, POST, PUT /{id}, PATCH /{id}/status) | `CLIENTES_VER/CREAR/EDITAR` |
| Proveedores | `/api/providers` (igual) | `PROVEEDORES_VER/CREAR/EDITAR` |
| Productos | `/api/products` (igual) | `PRODUCTOS_VER/CREAR/EDITAR` |
| Servicios | `/api/services` (igual) | `PRODUCTOS_VER/CREAR/EDITAR` |

El historial de operaciones del cliente (columna "Operaciones" del diseño) se completa en la Fase 3, cuando existan cotizaciones y pedidos.

## Auditoría e integración del frontend

La referencia actualizada está en [docs/AUDITORIA_BACKEND_FRONTEND.md](docs/AUDITORIA_BACKEND_FRONTEND.md). CORS permite los puertos 3000 y 5173; Swagger está en /swagger-ui/index.html. Configure HCG_JWT_SECRET explícitamente. El bootstrap de administrador requiere HCG_ADMIN_USERNAME, HCG_ADMIN_EMAIL y HCG_ADMIN_PASSWORD; no tiene credenciales automáticas.

