// Datos del usuario que inició sesión (nombre, roles y permisos) para mostrar u ocultar opciones.
// La seguridad real está en el backend: esto solo mejora la pantalla.
const KEY = "hcg_usuario";

export const setUsuario = (u) => {
  try { localStorage.setItem(KEY, JSON.stringify(u)); } catch { /* sin almacenamiento */ }
};
export const clearUsuario = () => {
  try { localStorage.removeItem(KEY); } catch { /* sin almacenamiento */ }
};
export const usuarioActual = () => {
  try { return JSON.parse(localStorage.getItem(KEY)) ?? null; } catch { return null; }
};
export const tienePermiso = (codigo) => {
  const u = usuarioActual();
  if (!u) return false;
  return u.roles?.includes("ADMIN") || u.permisos?.includes(codigo) || false;
};
export const esAdmin = () => usuarioActual()?.roles?.includes("ADMIN") ?? false;
