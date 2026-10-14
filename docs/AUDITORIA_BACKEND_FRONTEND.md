# Auditoría backend e integración React/Vite

Fecha: 8 de octubre de 2026. Base URL: `http://localhost:8080`.

## Alcance y evidencia

Se revisaron todos los controllers, DTOs, servicios principales, seguridad y migraciones V1–V5. La copia de trabajo no contiene `.git`, por lo que no se dispone de diff ni historial Git. Las pruebas usan H2 en modo PostgreSQL, Flyway, MockMvc y APIsPERU simulado: no equivalen a una prueba contra Supabase ni a una emisión real ante SUNAT. No hay `DB_PASSWORD` configurada en esta sesión y no se hicieron escrituras remotas.

Resultado: BUILD SUCCESS, 60 pruebas, 0 fallos, 0 errores, 0 omitidas. Ejecución final: 8 de octubre de 2026, 17:11 (America/Lima). Java 21 portátil oficial, Maven Wrapper; log en backend-audit-test.log.

## Contrato HTTP

- POST de catálogos, cotizaciones, documentos, usuarios y roles: 201 + DTO JSON. PUT/PATCH/GET: 200 + DTO JSON.
- Login y emisión/reenvío/conversión de boleta: 200 en éxito. SUNAT rechaza: 400; fallo de comunicación: 502. Una boleta puede quedar persistida en ERROR aun con respuesta 502.
- Cambio/restablecimiento de contraseña: 204, sin cuerpo. No llamar `response.json()` cuando el status sea 204.
- DTOs desacoplados de entidades: no se serializan proxies JPA, relaciones circulares ni hashes de contraseñas. Los campos opcionales pueden ser null.
- Listados paginados: `PageResponse<T> = {content, page, size, totalElements, totalPages}`. Page desde 0, size por defecto 20 y máximo 100. Usuarios, roles, permisos y auditoría devuelven arrays.
- `CotizacionDto.items` y `ComprobanteDto.detalles` son null en listados; el GET individual devuelve los detalles. PATCH de estado de cotización también devuelve items null.
- Error común: `ApiError = {success:false, message, errorCode, errors?}`; 400 validación, 401 token/credenciales, 403 permiso, 404 inexistente, 409 conflicto, 500 error interno. CORS con origen prohibido lo rechaza el filtro antes del controller y no usa ApiError.
- XML se devuelve como `application/xml`. PDF de boleta (`application/pdf`) se genera en APIsPERU (`/invoice/pdf`); solo boletas ACEPTADAS (409 si no, 404 si no existe).

## Inventario completo de endpoints de negocio

Todos requieren JWT salvo POST login. Los nombres DTO corresponden exactamente a las clases Java. `—` indica sin body.

| Verbo | URL | DTO entrada | DTO salida | Permiso |
|---|---|---|---|---|
| POST | /api/auth/login | LoginRequest | LoginResponse | Público |
| GET | /api/auth/me | — | UsuarioDto | Autenticado |
| POST | /api/auth/change-password | ChangePasswordRequest | — (204) | Autenticado |
| GET | /api/users | — | List<UsuarioDto> | USUARIOS_VER |
| GET | /api/users/{id} | — | UsuarioDto | USUARIOS_VER |
| POST | /api/users | UsuarioCreateRequest | UsuarioDto | USUARIOS_CREAR |
| PUT | /api/users/{id} | UsuarioUpdateRequest | UsuarioDto | USUARIOS_EDITAR |
| PATCH | /api/users/{id}/status | EstadoRequest | UsuarioDto | USUARIOS_EDITAR |
| PUT | /api/users/{id}/roles | RolesRequest | UsuarioDto | ADMIN |
| PUT | /api/users/{id}/password | PasswordResetRequest | — (204) | ADMIN |
| GET | /api/roles | — | List<RolDto> | USUARIOS_VER |
| GET | /api/permissions | — | List<PermisoDto> | USUARIOS_VER |
| POST | /api/roles | RolCreateRequest | RolDto | ADMIN |
| PUT | /api/roles/{id}/permissions | PermisosRequest | RolDto | ADMIN |
| GET | /api/audit | — | List<AuditoriaDto> | AUDITORIA_VER |
| GET | /api/clients | — | PageResponse<ClienteDto> | CLIENTES_VER |
| GET | /api/clients/{id} | — | ClienteDto | CLIENTES_VER |
| POST | /api/clients | ClienteRequest | ClienteDto | CLIENTES_CREAR |
| PUT | /api/clients/{id} | ClienteRequest | ClienteDto | CLIENTES_EDITAR |
| PATCH | /api/clients/{id}/status | EstadoRequest | ClienteDto | CLIENTES_EDITAR |
| GET | /api/providers | — | PageResponse<ProveedorDto> | PROVEEDORES_VER |
| GET | /api/providers/{id} | — | ProveedorDto | PROVEEDORES_VER |
| POST | /api/providers | ProveedorRequest | ProveedorDto | PROVEEDORES_CREAR |
| PUT | /api/providers/{id} | ProveedorRequest | ProveedorDto | PROVEEDORES_EDITAR |
| PATCH | /api/providers/{id}/status | EstadoRequest | ProveedorDto | PROVEEDORES_EDITAR |
| GET | /api/products | — | PageResponse<ProductoDto> | PRODUCTOS_VER |
| GET | /api/products/{id} | — | ProductoDto | PRODUCTOS_VER |
| POST | /api/products | ProductoRequest | ProductoDto | PRODUCTOS_CREAR |
| PUT | /api/products/{id} | ProductoRequest | ProductoDto | PRODUCTOS_EDITAR |
| PATCH | /api/products/{id}/status | EstadoRequest | ProductoDto | PRODUCTOS_EDITAR |
| GET | /api/services | — | PageResponse<ProductoDto> | PRODUCTOS_VER |
| GET | /api/services/{id} | — | ProductoDto | PRODUCTOS_VER |
| POST | /api/services | ProductoRequest | ProductoDto | PRODUCTOS_CREAR |
| PUT | /api/services/{id} | ProductoRequest | ProductoDto | PRODUCTOS_EDITAR |
| PATCH | /api/services/{id}/status | EstadoRequest | ProductoDto | PRODUCTOS_EDITAR |
| GET | /api/quotations | — | PageResponse<CotizacionDto> | COTIZACIONES_VER |
| GET | /api/quotations/{id} | — | CotizacionDto | COTIZACIONES_VER |
| POST | /api/quotations | CotizacionRequest | CotizacionDto | COTIZACIONES_CREAR |
| PUT | /api/quotations/{id} | CotizacionRequest | CotizacionDto | COTIZACIONES_EDITAR |
| PATCH | /api/quotations/{id}/status | CotizacionEstadoRequest | CotizacionDto | COTIZACIONES_EDITAR |
| POST | /api/quotations/{id}/convertir-boleta | — | BoletaResponse | COTIZACIONES_EDITAR + COMPROBANTES_CREAR |
| GET | /api/documents | — | PageResponse<DocumentoDto> | DOCUMENTOS_VER |
| GET | /api/documents/{id} | — | DocumentoDto | DOCUMENTOS_VER |
| POST | /api/documents | DocumentoRequest | DocumentoDto | DOCUMENTOS_CREAR |
| PUT | /api/documents/{id} | DocumentoRequest | DocumentoDto | DOCUMENTOS_EDITAR |
| POST | /api/documents/{id}/anular | — | DocumentoDto | DOCUMENTOS_ANULAR |
| POST | /api/v1/boletas/emitir | EmitirBoletaRequest | BoletaResponse | COMPROBANTES_CREAR |
| GET | /api/v1/boletas | — | PageResponse<ComprobanteDto> | COMPROBANTES_VER |
| GET | /api/v1/boletas/{serie}-{correlativo} | — | ComprobanteDto | COMPROBANTES_VER |
| GET | /api/v1/boletas/{serie}-{correlativo}/xml | — | String XML | COMPROBANTES_VER |
| GET | /api/v1/boletas/{serie}-{correlativo}/pdf | — | application/pdf (409/404/502 ApiError) | COMPROBANTES_VER |
| POST | /api/v1/boletas/{serie}-{correlativo}/reenviar | — | BoletaResponse | COMPROBANTES_CREAR |

Rutas auxiliares: GET `/swagger-ui.html` (redirección), GET `/swagger-ui/**` (HTML/JS/CSS), GET `/v3/api-docs` y `/v3/api-docs/**` (OpenAPI/configuración JSON; formato YAML en `/v3/api-docs.yaml`, protegido al no incluirse expresamente en las rutas públicas). OPTIONS `/api/**` sirve preflight CORS sin JWT. `/error` es público y lo usa el manejo interno de errores.

**No hay ningún @DeleteMapping.** No se añadió borrado físico: clientes/proveedores/productos/servicios/usuarios se desactivan con PATCH status. Documentos manuales se anulan con POST anular. Cotizaciones tienen transiciones de estado y boletas fiscales no tienen edición/borrado general. DELETE con token sobre una ruta mapeada sin ese método responde 405 JSON; sin token, 401. `ComprobanteController` y `OperacionController` son archivos vacíos: no crean rutas `/api/comprobantes` ni `/api/operaciones`. Los comprobantes implementados se exponen por BoletaController.

Filtros GET:

- clients/providers/products/services: `q`, `activo`, `page`, `size`.
- quotations/boletas: `q`, `estado`, `page`, `size`.
- documents: `q`, `tipo`, `estado`, `desde`, `hasta`, `page`, `size`; fechas YYYY-MM-DD.
- audit: `page=0`, `size=50` (máximo 200).

## DTOs principales de salida

| DTO | Campos |
|---|---|
| LoginResponse | token, tipo (Bearer), expiraEnSegundos, usuario (UsuarioDto) |
| CotizacionDto | id, numero, clienteId, clienteNombre, telefono, fecha, vigenciaHasta, estado, moneda, subtotal, igv, total, observacion, comprobanteId, usuarioId, creadoEn, items |
| CotizacionDto.Item | item, productoId, descripcion, unidad, cantidad, precioUnitario, subtotal |
| DocumentoDto | id, tipo, numero, fecha, origen, origenId, contraparteNombre, contraparteDoc, monto, moneda, estado, descripcion, archivoUrl, usuarioId, creadoEn |
| ComprobanteDto | id, tipoDoc, serie, correlativo (string), fechaEmision, moneda, clienteId, clienteTipoDoc, clienteNumDoc, clienteNombre, clienteDireccion, mtoOperGravadas, mtoIgv, mtoImpVenta, observacion, estado, codigoSunat, descripcionSunat, hash, usuarioId, creadoEn, detalles |
| ComprobanteDto.Detalle | item, codProducto, descripcion, unidad, cantidad, valorUnitario, precioUnitario, valorVenta, igv |
| BoletaResponse | success, id, estado, serie, correlativo, hash, codigoRespuestaSunat, descripcionSunat, errores, pdfUrl, xml, cdrZip, sunatResponse (campos opcionales omitidos si null) |
| ClienteDto | id, codigo, tipoDocumento, numeroDocumento, ruc, dni, razonSocial, nombreComercial, nombres, apellidos, nombre, direccion, telefono, correo, activo, fechaRegistro |
| ProveedorDto | id, ruc, razonSocial, nombreComercial, nombre, direccion, telefono1, telefono2, correo, contacto, activo, fechaRegistro |
| ProductoDto | id, codigo, tipo, nombre, descripcion, categoria, precioVenta, precioCompra, stock, stockMinimo, unidadMedida, activo, stockBajo |
| AuditoriaDto | id, usuarioId, username, accion, entidad, entidadId, valorAnterior, valorNuevo, fecha |

## Bodies para Postman / Swagger

Enviar `Content-Type: application/json` y, salvo login, `Authorization: Bearer <token>`. Reemplazar identificadores con los devueltos por POST.

Login — POST /api/auth/login:
```json
{"identificador":"admin","password":"TU_PASSWORD_CONFIGURADA"}
```
El campo identificador acepta username o correo. Guardar `token` de la respuesta; en Swagger pulsar Authorize e introducir únicamente el token.

Cliente — POST /api/clients o PUT /api/clients/{id}:
```json
{"tipoDocumento":"DNI","dni":"12345678","nombres":"Juan","apellidos":"Pérez","direccion":"Lima","telefono":"999888777","correo":"juan@example.com"}
```
También se permite SIN_DOCUMENTO; para RUC se exige ruc válido y razonSocial. PUT envía el DTO completo, no es actualización parcial.

Proveedor — POST /api/providers o PUT /api/providers/{id}:
```json
{"ruc":"20100070970","razonSocial":"Proveedor de prueba","direccion":"Lima","telefono1":"999888777","correo":"proveedor@example.com","contacto":"Ana"}
```
RUC ilustrativo, valida formato/dígito; no se verifica titularidad externa. Un RUC/DNI ya registrado genera 409.

Producto — POST /api/products o PUT /api/products/{id}:
```json
{"nombre":"Aceite","descripcion":"Aceite para mantenimiento","categoria":"Insumos","precioVenta":118.00,"precioCompra":80.00,"stock":10,"stockMinimo":2,"unidadMedida":"NIU"}
```
Código opcional; el servidor genera PRD-xxxxxx.

Servicio — POST /api/services o PUT /api/services/{id}:
```json
{"nombre":"Mantenimiento","precioVenta":59.00,"categoria":"Taller","unidadMedida":"ZZ"}
```
No enviar stock, stockMinimo ni precioCompra en servicios. Código automático SRV-xxxxxx.

Desactivar catálogo o usuario — PATCH /{id}/status:
```json
{"activo":false}
```

Cotización — POST /api/quotations o PUT /api/quotations/{id}:
```json
{
  "clienteNombre":"Juan Pérez",
  "telefono":"999888777",
  "vigenciaHasta":"2026-10-31",
  "observacion":"Precios incluyen IGV",
  "items":[{"descripcion":"Mantenimiento","unidad":"ZZ","cantidad":2,"precioUnitario":59.00}]
}
```
Alternativa: clienteId en lugar de clienteNombre; productoId permite resolver nombre/unidad/precio de catálogo. Moneda actual de cotización: PEN, no es campo del request. Resultado esperado: total 118.00, subtotal 100.00, igv 18.00; número COT-xxxxxx. Solo se edita en BORRADOR. Cantidad/precio aceptan hasta 2 decimales; el total no puede superar 9999999999.99.

Transición — PATCH /api/quotations/{id}/status:
```json
{"estado":"ENVIADA"}
```
Después ENVIADA → APROBADA o RECHAZADA; APROBADA también admite RECHAZADA. FACTURADA se asigna solo por conversión. POST convertir-boleta no lleva body. Requiere configurar APIsPERU y puede emitir un comprobante real. Si falla la comunicación conserva APROBADA y enlaza la boleta para reenviarla en lugar de volver a crearla.

Documento manual — POST /api/documents o PUT /api/documents/{id}:
```json
{"tipo":"FACTURA","numero":"F001-100","fecha":"2026-10-08","contraparteNombre":"Proveedor de prueba","contraparteDoc":"20100070970","monto":250.50,"moneda":"PEN","descripcion":"Registro de factura recibida","archivoUrl":"https://example.com/factura.pdf"}
```
Tipos: BOLETA, FACTURA, COTIZACION, ORDEN_COMPRA, GUIA, OTRO. Este endpoint registra metadatos, no emite una factura fiscal ni sube archivos. El servidor fija origen MANUAL, usuario y estado REGISTRADO. origenId es null en registros manuales. POST anular no lleva body; los automáticos se actualizan desde su origen.

Boleta — POST /api/v1/boletas/emitir:
```json
{"serie":"BBB1","fechaEmision":"2026-10-08T10:00:00-05:00","tipoMoneda":"PEN","cliente":{"tipoDoc":"1","numDoc":"12345678","rznSocial":"Juan Pérez","direccion":"Lima"},"items":[{"codProducto":"SRV-000001","descripcion":"Mantenimiento","unidad":"ZZ","cantidad":2,"precioUnitario":59.00}],"observacion":"Prueba en entorno de pruebas del proveedor"}
```
El correlativo lo asigna el backend en una transacción con bloqueo de serie: no enviar uno inventado. Ítems obligatorios; precioUnitario incluye IGV y valorUnitario es alternativa sin IGV. Se validan cantidades/precios y totales coincidentes. Totales opcionales: es preferible omitir mtoOperGravadas, mtoIGV, mtoImpVenta. No ejecutar contra proveedor de producción para probar la integración.

Otros bodies:
```json
{"username":"vendedor","nombreCompleto":"Vendedor de prueba","correo":"vendedor@example.com","password":"PasswordUnica123","roles":["EMPLEADO"]}
```
UsuarioCreateRequest, POST users. PUT users usa `{"nombreCompleto":"Vendedor","correo":"vendedor@example.com"}`; roles usa `{"roles":["EMPLEADO"]}`; reset password usa `{"password":"PasswordNueva123"}`. ChangePasswordRequest usa `{"actual":"PasswordAnterior123","nueva":"PasswordNueva123"}`. POST roles usa `{"nombre":"VENDEDOR","descripcion":"Ventas"}`. PUT permissions usa `{"permisos":["CLIENTES_VER","COTIZACIONES_VER","COTIZACIONES_CREAR"]}`.

## Fetch desde React/Vite

```javascript
const base = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
async function api(path, {token, body, method = 'GET'} = {}) {
  const response = await fetch(`${base}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined ? {'Content-Type': 'application/json'} : {}),
      ...(token ? {Authorization: `Bearer ${token}`} : {})
    },
    ...(body !== undefined ? {body: JSON.stringify(body)} : {})
  });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) throw Object.assign(new Error(data?.message ?? data?.descripcionSunat ?? 'Error API'),
      {status: response.status, data});
  return data;
}
const session = await api('/api/auth/login', {
  method: 'POST', body: {identificador: 'admin', password: 'TU_PASSWORD'}
});
const quotation = await api('/api/quotations', {
  method: 'POST', token: session.token,
  body: {clienteNombre: 'Juan Pérez', items: [{descripcion: 'Mantenimiento', cantidad: 2, precioUnitario: 59}]}
});
const documents = await api('/api/documents?tipo=COTIZACION', {token: session.token});
```
El helper anterior es para endpoints JSON; XML se consume con `response.text()`. No introducir DB_PASSWORD, HCG_JWT_SECRET ni APISPERU_TOKEN en variables VITE_ (son públicas en el navegador).

## Trazabilidad y transacciones

- `documento` representa el registro central y el estado actual, no una fila nueva por cada edición. `registrarAutomatico` hace upsert lógico por tipo + número y conserva id/creadoEn/usuario creador.
- Cotización POST/PUT registra origen COTIZACION, origen_id con id de cotización, monto total, estado y usuarioId. PATCH estado sincroniza documento. Crear/editar y auditoria usan la misma transacción REQUIRED: si falla, se revierte todo.
- Documento manual POST inserta en documento y crea evento CREAR_DOCUMENTO; PUT crea EDITAR_DOCUMENTO; anular crea ANULAR_DOCUMENTO en auditoria. El usuario del registro identifica al creador; el actor de cada modificación está en auditoria.
- Boleta: reserva correlativo + comprobante + documento PENDIENTE + evento EMITIR_BOLETA en una transacción. Envía a APIsPERU fuera de transacción. Luego guarda resultado + estado de documento + evento ESTADO_BOLETA en otra transacción.
- Fallo de proveedor no revierte la reserva ya confirmada: conserva evidencia ERROR/PENDIENTE y permite reenviar con mismo número. Si falla guardar el resultado después de que SUNAT lo aceptó, puede quedar PENDIENTE; se requiere reconciliación con proveedor antes de reenviar.
- Conversión de cotización guarda su vínculo/estado y evento COTIZACION_A_BOLETA en una misma transacción final.
- documento.usuario_id y auditoria.usuario_id son nullable y tienen FK a usuario con ON DELETE SET NULL. Null es válido; un id no nulo inexistente debe fallar y revertir la operación. El converter JWT consulta usuario activo en BD antes de llegar al controller, reduciendo referencias inválidas.
- cotizacion.usuario_id y comprobante_id se agregaron como BIGINT en V5 sin FK: no asumir que todas las relaciones tienen integridad referencial. No se añadieron constraints sobre datos remotos sin conocer los registros actuales.
- No hay triggers SQL que creen documento/auditoria. Inserciones directas en Supabase u otros consumidores no pasan por estos servicios y no generan trazabilidad automáticamente.
- Los eventos de edición de cotización/documento contienen un resumen (número/tipo), no una copia completa de todos los importes/campos previos. La auditoría existente no permite reconstruir cada versión íntegra.

## Tipos SQL y verificación real

IDs principales: IDENTITY (Integer en catálogos/cotización, Long en documento/comprobante/usuario). Montos activos: BigDecimal ↔ NUMERIC(12,2). Columnas heredadas FLOAT como presupuesto/precio_unit/precio_total no se usan para los nuevos montos. Fecha de documento/cotización: LocalDate ↔ DATE; creado_en/actualizado_en/fecha_emision: LocalDateTime ↔ TIMESTAMP sin zona. JSON usa ISO-8601. La fecha automática de cotización usa America/Lima; otros timestamps usan zona del JVM, por lo que hay que ejecutar backend en la zona esperada (p. ej. `-Duser.timezone=America/Lima`).

Consultas de lectura para Supabase: `docs/verificacion_supabase.sql`. Comprueban columnas, identidades, FK, migraciones, registros sin documento y desajustes. Usarlas después de ejecutar POST/PUT desde un entorno de pruebas con credenciales reales. IDs pueden tener saltos por rollback (normal en PostgreSQL). No se afirma haber ejecutado estas consultas remotamente.

## Cambios aplicados

- CorsConfig nuevo: WebMvcConfigurer y CorsConfigurationSource compartiendo orígenes, headers y métodos. SecurityConfig conserva `.cors(...)` y elimina su bean CORS duplicado.
- SecurityConfig: GET Swagger/OpenAPI público; la API sigue protegida por JWT y permisos por URL/@PreAuthorize.
- pom.xml: springdoc-openapi-starter-webmvc-ui 3.1.1. OpenApiConfig: esquema bearerAuth y login público. Versión seleccionada según [documentación oficial de springdoc](https://springdoc.org/) (línea 3.x compatible con Boot 4).
- application.properties/.env.example: dos puertos CORS; secreto JWT sin valor por defecto; bootstrap sin contraseña automática. Es obligatorio configurar HCG_JWT_SECRET (≥32 bytes) para arrancar; primer admin solo se crea al configurar sus variables y con tabla usuario vacía.
- DocumentoService: rechaza colisiones de tipo/número de otro origen en vez de sobrescribir evidencia.
- BoletaController: body obligatorio + @Valid. EmitirBoletaRequest/ItemBoletaDto: ítems obligatorios, no nulos, descripción, cantidades y precios válidos. Precio cero ya no se sustituye por 118.
- ApisPeruBoletaService: rechaza totales distintos de los calculados y total fuera de NUMERIC(12,2). Transporte SimpleClientHttpRequestFactory con timeout de conexión 10 s y lectura 30 s; evita el fallo de inicialización loopback del cliente JDK observado en este Windows.
- CotizacionRequest: elementos null rechazados. CotizacionService: límite total NUMERIC(12,2); auditoría de conversión dentro de su transacción final.
- ComprobanteService: creación y evento auditables atómicos; agrega ESTADO_BOLETA al guardar resultado.
- Repositories y migraciones: sin cambios. No se cambió el modelo de borrado ni se implementaron emisión de facturas/PDF.
- Pruebas añadidas: CORS real con ambos puertos/métodos y origen rechazado, Swagger público/esquema JWT, usuario nulo y actualización del mismo documento, rollback real por FK inválida, rechazo de colisión de origen, boleta vacía/precios inválidos/totales manipulados.

## Límites pendientes

No se validó contra Supabase ni SUNAT real. La conversión/reenvío no implementa idempotencia completa para solicitudes concurrentes: dos peticiones simultáneas a convertir-boleta pueden emitir más de un comprobante. Un reenvío aceptado no sincroniza automáticamente la cotización previamente enlazada a ERROR a FACTURADA. Flyway tiene validate-on-migrate=false y baseline-on-migrate=true: comprobar historial/esquema real antes de atribuirle consistencia. Estos límites deben considerarse antes de usar emisión fiscal en producción.


## Código de CorsConfig


```java
package com.hcg.multiservicios.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Una misma politica CORS para Spring MVC y el filtro de Spring Security. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] origins;
    private static final String[] METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};
    private static final String[] HEADERS = {"Authorization", "Content-Type", "Accept"};

    public CorsConfig(@Value("${hcg.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String origins) {
        this.origins = Arrays.stream(origins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods(METHODS)
                .allowedHeaders(HEADERS).allowCredentials(false).maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origins));
        config.setAllowedMethods(List.of(METHODS));
        config.setAllowedHeaders(List.of(HEADERS));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```


## Validación final

`mvnw.cmd test`: BUILD SUCCESS (60 pruebas). AuthSecurityIntegrationTest 17; BackendAuditIntegrationTest 3; CatalogosIntegrationTest 17; ComprobanteBoletaIntegrationTest 6; CotizacionDocumentoIntegrationTest 8; BoletaIntegrationTest 4; DocumentoValidatorsTest 4; BackendApplicationTests 1. Flyway aplicó V1–V5 y Hibernate validó las entidades en H2. Se probaron login/JWT real, permisos, JSON sin hash, CORS preflight, Swagger público, POST/GET/PUT/PATCH, rechazo de DELETE no implementado, montos, registros centrales, origen, usuario nulo y rollback por FK. La emisión fiscal está simulada.

Para ejecutar en su terminal necesita JDK 21 y JAVA_HOME configurado; el JDK usado para esta auditoría es portátil en temporales y no cambia la configuración del sistema. Defina DB_URL, DB_USER, DB_PASSWORD y HCG_JWT_SECRET para arrancar contra Supabase. Configure HCG_ADMIN_* si aún no existe usuario. La aplicación no carga automáticamente un archivo .env: las variables deben estar exportadas al proceso Java.
