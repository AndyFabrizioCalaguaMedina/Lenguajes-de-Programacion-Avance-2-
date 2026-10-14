export const soles = (n) =>
  "S/ " + Number(n ?? 0).toLocaleString("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export const fecha = (d) => (d ? new Date(d).toLocaleDateString("es-PE") : "—");

export const fechaHora = (d) =>
  d ? new Date(d).toLocaleString("es-PE", { dateStyle: "short", timeStyle: "short" }) : "—";

// Fecha local en formato AAAA-MM-DD (para inputs type="date")
export const isoLocal = (d = new Date()) => {
  const z = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return z.toISOString().slice(0, 10);
};

export const hace = (dias) => {
  const d = new Date();
  d.setDate(d.getDate() - dias);
  return isoLocal(d);
};

// Nombres legibles para las acciones que registra la auditoría
export const ACCIONES = {
  LOGIN_OK: "Inició sesión",
  LOGIN_FALLIDO: "Intento de acceso fallido",
  LOGIN_BLOQUEADO: "Acceso bloqueado",
  LOGIN_USUARIO_INACTIVO: "Intento con usuario inactivo",
  EMITIR_BOLETA: "Emitió una boleta",
  REENVIAR_BOLETA: "Reenvió una boleta",
  ESTADO_BOLETA: "Cambió estado de boleta",
  CREAR_COTIZACION: "Creó una cotización",
  EDITAR_COTIZACION: "Editó una cotización",
  ESTADO_COTIZACION: "Cambió estado de cotización",
  COTIZACION_A_BOLETA: "Convirtió cotización en boleta",
  CREAR_DOCUMENTO: "Registró un documento",
  EDITAR_DOCUMENTO: "Editó un documento",
  ANULAR_DOCUMENTO: "Anuló un documento",
  CLIENTE_CREADO: "Creó un cliente",
  CLIENTE_EDITADO: "Editó un cliente",
  CLIENTE_DESACTIVADO: "Desactivó un cliente",
  PROVEEDOR_CREADO: "Creó un proveedor",
  PROVEEDOR_EDITADO: "Editó un proveedor",
  PROVEEDOR_DESACTIVADO: "Desactivó un proveedor",
  REGISTRAR_SALIDA_BODEGA: "Registró salida de bodega",
  ANULAR_SALIDA_BODEGA: "Anuló salida de bodega",
  USUARIO_CREADO: "Creó un usuario",
  USUARIO_EDITADO: "Editó un usuario",
  USUARIO_DESACTIVADO: "Cambió estado de usuario",
  ROLES_ASIGNADOS: "Asignó roles",
  PERMISOS_ASIGNADOS: "Cambió permisos de un rol",
  PASSWORD_CAMBIADA: "Cambió su contraseña",
  PASSWORD_RESTABLECIDA: "Restableció una contraseña",
  OPERACION_CREADA: "Creó una operación",
  OPERACION_EDITADA: "Editó una operación",
  OPERACION_EVENTO: "Registró avance en una operación",
  OPERACION_ESTADO: "Cambió estado de una operación",
  ROL_CREADO: "Creó un rol",
};
export const accion = (c) => ACCIONES[c] ?? c;
