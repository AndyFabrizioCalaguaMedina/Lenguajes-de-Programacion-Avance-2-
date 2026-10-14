const BASE_URL = import.meta.env.VITE_API_URL;
import { setUsuario, clearUsuario } from "./sesion";

const TOKEN_KEY = "hcg_token";

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

// Cierra la sesión borrando el token
export const logout = () => { clearToken(); clearUsuario(); };

// true solo si hay token Y todavía no venció
export function tokenVigente() {
  const token = getToken();
  if (!token) return false;
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(payload));
    return exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

export async function request(ruta, opciones = {}) {
  const headers = { "Content-Type": "application/json" };

  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(BASE_URL + ruta, { ...opciones, headers });
  const body = await res.json().catch(() => null);

  // Sesión vencida: borra el token y vuelve al login
  if (res.status === 401 && ruta !== "/api/auth/login") {
    clearToken();
    window.location.href = "/";
    throw new Error("Sesión expirada");
  }

  if (!res.ok) {
    const error = new Error(body?.message ?? body?.descripcionSunat ?? `Error ${res.status}`)
    error.errors = body?.errors ?? body?.errores
    error.body = body
    throw error
  }

  return body;
}

export async function login(identificador, password) {
  clearToken();

  const body = await request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ identificador, password }),
  });

  setToken(body.token);
  setUsuario(body.usuario);
}

// Descarga un archivo binario (p. ej. PDF) con el token y lo abre en otra pestaña
export async function abrirArchivo(ruta) {
  const res = await fetch(BASE_URL + ruta, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? `Error ${res.status}`);
  }
  const url = URL.createObjectURL(await res.blob());
  window.open(url, "_blank");
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}
