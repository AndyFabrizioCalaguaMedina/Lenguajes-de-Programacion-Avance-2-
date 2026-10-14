import { request } from "../../services/api";

export const listarCotizaciones = () => request("/api/quotations?size=100");

export const obtenerCotizacion = (id) => request(`/api/quotations/${id}`);

export const crearCotizacion = (datos) =>
  request("/api/quotations", { method: "POST", body: JSON.stringify(datos) });

export const cambiarEstadoCotizacion = (id, estado) =>
  request(`/api/quotations/${id}/status`, { method: "PATCH", body: JSON.stringify({ estado }) });

export const convertirEnBoleta = (id) =>
  request(`/api/quotations/${id}/convertir-boleta`, { method: "POST" });
