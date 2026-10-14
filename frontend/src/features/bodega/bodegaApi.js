import { request } from "../../services/api";

export const listarSalidas = () => request("/api/warehouse-exits?size=20");
export const registrarSalida = (datos) =>
  request("/api/warehouse-exits", { method: "POST", body: JSON.stringify(datos) });
export const anularSalida = (id) =>
  request(`/api/warehouse-exits/${id}/anular`, { method: "POST" });
export const listarProveedores = () => request("/api/providers?size=100");
export const buscarProducto = (codigo) =>
  request(`/api/products?size=5&q=${encodeURIComponent(codigo)}`);
