# API HCG Multiservicios

La referencia completa y actualizada de endpoints, DTOs, permisos, bodies JSON, CORS,
JWT, persistencia y trazabilidad está en [Auditoría backend/frontend](docs/AUDITORIA_BACKEND_FRONTEND.md).

Base local: http://localhost:8080. Swagger: http://localhost:8080/swagger-ui/index.html.
OpenAPI JSON: http://localhost:8080/v3/api-docs.

CORS por defecto: http://localhost:3000 y http://localhost:5173.
Login público: POST /api/auth/login. Los endpoints de negocio requieren Bearer JWT y permisos.
No hay endpoints DELETE; desactivación/anulación según el recurso.

---
## Salidas de bodega (nuevo) — `/api/warehouse-exits`
Registro de los productos que salen de la bodega (envío a proveedor, devolución, venta, merma). **Descuenta el stock**, guarda el stock anterior y posterior de cada producto y aparece en Documentos (`tipo=SALIDA_BODEGA`). Pensado para registrarse desde el celular, enviando el **código** del producto leído por el escáner.
Permisos: `BODEGA_VER`, `BODEGA_CREAR`, `BODEGA_ANULAR` (el rol ADMIN los tiene).
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/warehouse-exits` | BODEGA_CREAR |
| GET | `/api/warehouse-exits?q=&estado=&destinoTipo=&desde=&hasta=&page=&size=` | BODEGA_VER |
| GET | `/api/warehouse-exits/{id}` | BODEGA_VER (incluye `items`) |
| POST | `/api/warehouse-exits/{id}/anular` | BODEGA_ANULAR — devuelve el stock |

Body de POST:
```json
{ "destinoTipo": "PROVEEDOR", "proveedorId": 3, "motivo": "ENVIO", "observacion": "Lote 12",
  "items": [ { "codigo": "PRD-000001", "cantidad": 5 }, { "productoId": 7, "cantidad": 2 } ] }
```
`destinoTipo`: PROVEEDOR (requiere `proveedorId`), CLIENTE (requiere `clienteId`) u OTRO (requiere `destinoNombre`). `motivo`: ENVIO, DEVOLUCION, VENTA, MERMA, OTRO. Si el stock no alcanza, responde 400 y no descuenta nada.

---

## Estadísticas, búsqueda e historial

Todas requieren `Authorization: Bearer <token>`.

### `GET /api/stats/dashboard` — permiso `DASHBOARD_VER`
Devuelve: `clientes`, `cotizacionesPendientes` (borrador + enviadas), `cotizacionesAprobadas`, `documentos`, `ventasMes`, `boletasMes`, `boletasAceptadas`, `boletasConProblema` (rechazadas o con error), `salidasMes`, `ventasPorDia` (últimos 14 días: `fecha`, `total`, `cantidad`), `cotizacionesPorEstado` (`nombre`, `cantidad`) y `actividadReciente` (solo si el usuario tiene `AUDITORIA_VER`; si no, lista vacía).

### `GET /api/stats/reportes?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` — permiso `REPORTES_VER`
Por defecto, últimos 30 días; rango máximo 366 días (`RANGO_INVALIDO` / `RANGO_MUY_GRANDE` → 400).
Devuelve: `ventasTotal`, `boletasAceptadas`, `ticketPromedio`, `ventasPorDia`, `boletasPorEstado`, `topProductos` y `topClientes` (`nombre`, `total`, `cantidad`), `cotizacionesPorEstado`, `salidasPorMotivo`, y — solo con `AUDITORIA_VER` — `actividadPorUsuario` y `actividadPorAccion`.
Solo cuentan las boletas ACEPTADAS para ventas.

### `GET /api/search?q=texto`
Busca (mínimo 2 letras) en clientes, proveedores, productos, cotizaciones, documentos y boletas, hasta 5 por sección. Cada sección se incluye solo si el usuario tiene su permiso `*_VER`; si no, viene vacía.

### `GET /api/audit/search` — permiso `AUDITORIA_VER`
Historial de actividad, más reciente primero. Parámetros opcionales: `username`, `accion` (ej. `EMITIR_BOLETA`), `entidad`, `desde`, `hasta`, `page`, `size` (máx. 100). Respuesta paginada: `content`, `page`, `size`, `totalElements`, `totalPages`.
(`GET /api/audit` sigue devolviendo la lista simple de los registros más recientes.)

### Personal y permisos (ya existentes)
- `POST /api/users` (crear usuario con `roles`), `PATCH /api/users/{id}/status` (`{"activo":false}`), `PUT /api/users/{id}/password`.
- `GET /api/roles`, `GET /api/permissions`, `POST /api/roles`, `PUT /api/roles/{id}/permissions` (`{"permisos":["CLIENTES_VER", ...]}`).
- El frontend crea un rol `EMP_<USUARIO>` por cada empleado con permisos personalizados.


## Operaciones (ventas, compras y pedidos) — `/api/operations`

Permisos: `OPERACIONES_VER`, `OPERACIONES_CREAR`, `OPERACIONES_EDITAR` (el ADMIN tiene todos).

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/operations` | Lista paginada. Filtros: `q`, `tipo` (VENTA/COMPRA/PEDIDO), `estado` (ABIERTA/EN_PROCESO/COMPLETADA/CANCELADA), `proveedorId`, `clienteId`, `desde`, `hasta` (AAAA-MM-DD), `page`, `size` |
| GET | `/api/operations/summary` | Totales por estado y por tipo (tarjetas de la pantalla) |
| GET | `/api/operations/{id}` | Detalle con la línea de tiempo (`eventos`) |
| POST | `/api/operations` | Crea. Body: `tipo`, `titulo`, `descripcion?`, `clienteId?`, `proveedorId?`, `contraparteNombre?`, `monto?`, `fechaInicio?`. Genera código `OP-AAAA-0001` |
| PUT | `/api/operations/{id}` | Edita los datos |
| POST | `/api/operations/{id}/events` | Agrega un hito: `tipo` (SOLICITUD, COTIZACION, ENVIO, ACEPTACION, DESPACHO, COMPROBANTE, DOCUMENTO, NOTA), `titulo`, `detalle?`, `referencia?`. Pasa ABIERTA → EN_PROCESO |
| PATCH | `/api/operations/{id}/status` | Body `{"estado":"EN_PROCESO\|COMPLETADA\|CANCELADA"}`. Las cerradas no se modifican |

El dashboard (`/api/stats/dashboard`) ahora incluye `cotizacionesPorMes` (6 meses) y `operacionesActivas`.

## Clientes, productos y comprobantes (novedades)

### `DELETE /api/clients/{id}` — permiso `CLIENTES_EDITAR`
Elimina el cliente (204). Si tiene cotizaciones, operaciones o salidas asociadas responde `409 CLIENT_IN_USE`: en ese caso se usa `PATCH /api/clients/{id}/status` para desactivarlo.

### `POST /api/products/import` — permiso `PRODUCTOS_CREAR`
Carga masiva. El frontend lee el Excel y envía las filas (máx. 1000 por llamada):
```json
{ "actualizarExistentes": false,
  "filas": [ { "fila": 2, "producto": { "codigo": "PRD-001", "nombre": "Cable", "precioVenta": 10.5, "stock": 20 } } ] }
```
Respuesta: `{ "total", "creados", "actualizados", "errores": [ { "fila", "codigo", "mensaje" } ] }`. Cada fila se guarda por separado: una fila con error no frena las demás. Con `actualizarExistentes: true` un código ya registrado se actualiza (exige además `PRODUCTOS_EDITAR`); con `false` se informa como error.

### Factura y boleta — `POST /api/v1/boletas/emitir`
Nuevo campo `tipoDoc`: `"03"` boleta (por defecto, serie `BBB1`) o `"01"` factura (serie `FFF1`, creada en la migración V8). La factura exige `cliente.tipoDoc = "6"` con RUC de 11 dígitos y razón social (`FACTURA_REQUIERE_RUC`). `GET /api/v1/boletas?tipo=01|03` filtra por tipo.
