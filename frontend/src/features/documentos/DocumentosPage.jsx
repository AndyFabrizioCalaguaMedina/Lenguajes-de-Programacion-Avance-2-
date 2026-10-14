import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { request } from "../../services/api";

const TIPOS = ["", "BOLETA", "FACTURA", "COTIZACION", "ORDEN_COMPRA", "OTRO"];
const ETIQUETA = { "": "Todos", BOLETA: "Boleta", FACTURA: "Factura", COTIZACION: "Cotización", ORDEN_COMPRA: "Orden de compra", OTRO: "Otros" };
const PILL = { ACTIVO: "bg-emerald-50 text-emerald-700", EMITIDO: "bg-emerald-50 text-emerald-700", ANULADO: "bg-red-50 text-red-700" };

export default function DocumentosPage() {
  const [docs, setDocs] = useState([]);
  const [tipo, setTipo] = useState("");
  const [q, setQ] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    const params = new URLSearchParams({ size: 50 });
    if (tipo) params.set("tipo", tipo);
    if (q) params.set("q", q);
    request(`/api/documents?${params}`)
      .then((d) => { setDocs(d.content); setError(""); })
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false));
  }, [tipo, q]);

  return (
    <section>
      <div className="pb-8 flex justify-between">
        <div>
          <h1 className="text-hcg-navy text-2xl font-bold">Documentos</h1>
          <p className="text-slate-500 text-sm">Repositorio centralizado de documentación comercial</p>
        </div>
        <Link to="/boletas"
          className="flex gap-2 text-center text-white text-sm font-semibold bg-hcg-navy rounded-xl p-3">
          + Emitir boleta
        </Link>
      </div>

      <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
        <div className="flex gap-3 p-3 border-b border-gray-100">
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Buscar..."
            className="flex-1 text-sm p-2 rounded-lg border border-gray-200" />
        </div>
        <div className="flex flex-wrap gap-2 p-3 border-b border-gray-100">
          {TIPOS.map((t) => (
            <button key={t} onClick={() => setTipo(t)}
              className={`rounded-full border px-3.5 py-1.5 text-sm transition ${tipo === t ? "border-hcg-navy bg-hcg-navy text-white" : "border-gray-300 bg-white text-gray-700 hover:border-hcg-gold"}`}>
              {ETIQUETA[t]}
            </button>
          ))}
        </div>

        {cargando && <p className="p-4 text-sm text-gray-500">Cargando documentos...</p>}
        {error && <p className="p-4 text-sm text-red-600">{error}</p>}
        {!cargando && !error && docs.length === 0 && (
          <p className="p-4 text-sm text-gray-500">No hay documentos registrados.</p>
        )}

        {docs.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-600">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700">
                <tr>
                  {["Tipo", "Número", "Fecha", "Contraparte", "Monto", "Estado"].map((h) => (
                    <th key={h} className="px-6 py-3 font-medium">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {docs.map((d) => (
                  <tr key={d.id} className="border-b border-gray-100 last:border-0 hover:bg-gray-50">
                    <td className="px-6 py-4 font-medium text-gray-900">{ETIQUETA[d.tipo] ?? d.tipo}</td>
                    <td className="px-6 py-4 font-mono text-xs text-hcg-navy">{d.numero}</td>
                    <td className="px-6 py-4">{new Date(d.fecha + "T00:00:00").toLocaleDateString("es-PE")}</td>
                    <td className="px-6 py-4">{d.contraparteNombre ?? "—"}</td>
                    <td className="px-6 py-4">
                      {d.monto != null ? `${d.moneda === "USD" ? "$" : "S/"} ${Number(d.monto).toFixed(2)}` : "—"}
                    </td>
                    <td className="px-6 py-4"><span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${PILL[d.estado] ?? "bg-gray-100 text-gray-600"}`}>{d.estado}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}
