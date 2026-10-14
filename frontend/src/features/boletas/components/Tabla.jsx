import { useEffect, useState } from "react";
import { abrirPdfBoleta, listarBoletas, reenviarBoleta } from "../boletasApi";

const estilos = {
  ACEPTADA: "bg-green-100 text-green-700",
  PENDIENTE: "bg-yellow-100 text-yellow-700",
  RECHAZADA: "bg-red-100 text-red-700",
  ERROR: "bg-orange-100 text-orange-700",
};

export default function Tabla({ recarga, onCambio }) {
  const [boletas, setBoletas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [estado, setEstado] = useState("");
  const [tipo, setTipo] = useState("");
  const [q, setQ] = useState("");

  useEffect(() => {
    listarBoletas({ q, estado, tipo })
      .then((data) => {
        setBoletas(data.content);
        setError("");
      })
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false));
  }, [recarga, estado, tipo, q]);

  async function pdf(b) {
    try {
      await abrirPdfBoleta(b.serie, b.correlativo);
    } catch (e) {
      alert(e.message);
    }
  }

  async function reenviar(b) {
    try {
      await reenviarBoleta(b.serie, b.correlativo);
    } catch (e) {
      alert(e.message);
    }
    onCambio?.();
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
      <div className="flex gap-3 p-3 border-b border-gray-100">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Buscar por serie, cliente o documento..."
          className="flex-1 text-sm p-2 rounded-lg border border-gray-200"
        />
        <select
          value={tipo}
          onChange={(e) => setTipo(e.target.value)}
          className="text-sm p-2 rounded-lg border border-gray-200"
          aria-label="Tipo de comprobante"
        >
          <option value="">Boletas y facturas</option>
          <option value="03">Solo boletas</option>
          <option value="01">Solo facturas</option>
        </select>
        <select
          value={estado}
          onChange={(e) => setEstado(e.target.value)}
          className="text-sm p-2 rounded-lg border border-gray-200"
        >
          <option value="">Todos los estados</option>
          <option value="ACEPTADA">Aceptada</option>
          <option value="RECHAZADA">Rechazada</option>
          <option value="ERROR">Error</option>
          <option value="PENDIENTE">Pendiente</option>
        </select>
      </div>

      {cargando && <p className="p-4 text-sm text-gray-500">Cargando comprobantes...</p>}
      {error && <p className="p-4 text-sm text-red-600">{error}</p>}
      {!cargando && !error && boletas.length === 0 && (
        <p className="p-4 text-sm text-gray-500">No hay comprobantes registrados.</p>
      )}

      {boletas.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-gray-600">
            <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700">
              <tr>
                {["Tipo", "Número", "Cliente", "Fecha", "Monto", "Estado", ""].map((h) => (
                  <th key={h} scope="col" className="px-6 py-3 font-medium">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {boletas.map((b) => (
                <tr key={b.id} className="border-b border-gray-100 last:border-0 hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <span className={`rounded-md px-2 py-1 text-xs font-semibold ${b.tipoDoc === "01" ? "bg-hcg-navy text-white" : "bg-hcg-gold-soft text-hcg-navy"}`}>
                      {b.tipoDoc === "01" ? "Factura" : "Boleta"}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-medium text-gray-900 whitespace-nowrap">
                    {b.serie}-{String(b.correlativo).padStart(6, "0")}
                  </td>
                  <td className="px-6 py-4">{b.clienteNombre}</td>
                  <td className="px-6 py-4">{new Date(b.fechaEmision).toLocaleDateString("es-PE")}</td>
                  <td className="px-6 py-4">S/ {Number(b.mtoImpVenta).toFixed(2)}</td>
                  <td className="px-6 py-4">
                    <span
                      title={b.descripcionSunat ?? ""}
                      className={`rounded-full px-2.5 py-1 text-xs font-medium ${estilos[b.estado] ?? ""}`}
                    >
                      {b.estado}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {b.estado === "ACEPTADA" && (
                      <button onClick={() => pdf(b)} className="font-medium text-hcg-navy hover:underline">PDF</button>
                    )}
                    {b.estado === "ERROR" && (
                      <button onClick={() => reenviar(b)} className="font-medium text-orange-600 hover:underline">Reenviar</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
