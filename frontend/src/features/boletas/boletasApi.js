import { request, abrirArchivo } from "../../services/api";

export const listarBoletas = ({ q = "", estado = "", tipo = "", page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams({ page, size });
  if (q) params.set("q", q);
  if (estado) params.set("estado", estado);
  if (tipo) params.set("tipo", tipo);
  return request(`/api/v1/boletas?${params}`);
};

export const emitirBoleta = (datos) =>
  request("/api/v1/boletas/emitir", { method: "POST", body: JSON.stringify(datos) });

export const reenviarBoleta = (serie, correlativo) =>
  request(`/api/v1/boletas/${serie}-${correlativo}/reenviar`, { method: "POST" });

export const abrirPdfBoleta = (serie, correlativo) =>
  abrirArchivo(`/api/v1/boletas/${serie}-${correlativo}/pdf`);
