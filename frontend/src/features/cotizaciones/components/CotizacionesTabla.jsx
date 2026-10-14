import { useEffect, useState } from "react";
import { listarCotizaciones } from "../cotizacionesApi";
import { View } from "lucide-react";
import DetalleCotizacion from "./DetalleCotizacion";
import { soles } from "../../../services/formato";
import Spinner from "../../../components/Spinner";

const PILL = {
    BORRADOR: "bg-slate-100 text-slate-700", ENVIADA: "bg-hcg-gold-soft text-amber-800",
    APROBADA: "bg-emerald-50 text-emerald-700", RECHAZADA: "bg-red-50 text-red-700", FACTURADA: "bg-hcg-navy/10 text-hcg-navy",
};
const POR_PAGINA = 10;

export default function CotizacionesTabla({ cotizaciones, setCotizaciones, filtro = {} }) {
    const [pagina, setPagina] = useState(0);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState("");
    const [verId, setVerId] = useState(null);
    const [recarga, setRecarga] = useState(0);

    useEffect(() => {
        listarCotizaciones()
            .then((data) => {
                // data.content actualiza el estado del PADRE (CotizacionesPage)
                setCotizaciones(data.content || []);
            })
            .catch((e) => setError(e.message))
            .finally(() => setCargando(false));
    }, [setCotizaciones, recarga]);

    if (cargando) return (
        <div className="w-full py-12 flex items-center justify-center">
            <Spinner />
        </div>
    );

    if (error) return <p className="text-red-500 p-4">{error}</p>;

    const texto = (filtro.q ?? "").trim().toLowerCase();
    const filtradas = cotizaciones.filter((c) =>
        (!filtro.estado || c.estado === filtro.estado)
        && (!texto || `${c.numero} ${c.clienteNombre}`.toLowerCase().includes(texto))
        && (!filtro.desde || c.fecha >= filtro.desde)
        && (!filtro.hasta || c.fecha <= filtro.hasta));
    const paginas = Math.max(1, Math.ceil(filtradas.length / POR_PAGINA));
    const actual = Math.min(pagina, paginas - 1);
    const visibles = filtradas.slice(actual * POR_PAGINA, (actual + 1) * POR_PAGINA);

    return (
        <section className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
            <table className="w-full text-left text-sm text-gray-600">
                <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700">
                    <tr>
                        <th className="px-6 py-3 font-medium">CODIGO</th>
                        <th className="px-6 py-3 font-medium">CLIENTE</th>
                        <th className="px-6 py-3 font-medium">FECHA</th>
                        <th className="px-6 py-3 font-medium">MONTO</th>
                        <th className="px-6 py-3 font-medium">ESTADO</th>
                        <th className="px-6 py-3 font-medium text-center">ACCIONES</th>
                    </tr>
                </thead>
                <tbody>
                    {visibles.map((c) => (
                        <tr
                            key={c.id}
                            className="border-b border-gray-100 last:border-0 hover:bg-gray-50"
                        >
                            <td className="px-6 py-4 font-mono text-xs text-hcg-navy">{c.numero}</td>
                            <td className="px-6 py-4">{c.clienteNombre}</td>
                            <td className="px-6 py-4">{c.fecha}</td>
                            <td className="px-6 py-4">{soles(c.total)}</td>
                            <td className="px-6 py-4"><span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${PILL[c.estado] ?? "bg-gray-100 text-gray-600"}`}>{c.estado}</span></td>
                            <td className="px-4 py-3">
                                <div className="flex gap-2 justify-center text-center">
                                    <View size={16} onClick={() => setVerId(c.id)} className="text-hcg-navy hover:text-hcg-navy/55 cursor-pointer" />
                                </div>
                            </td>
                        </tr>
                    ))}
                    {visibles.length === 0 && (
                        <tr><td colSpan={6} className="px-6 py-8 text-center text-gray-400">Sin cotizaciones para estos filtros.</td></tr>
                    )}
                </tbody>
            </table>
            <div className="flex items-center justify-between border-t border-gray-100 px-6 py-3 text-sm text-gray-500">
                <span>{filtradas.length} resultado(s)</span>
                <div className="flex items-center gap-2">
                    <button disabled={actual === 0} onClick={() => setPagina(actual - 1)} className="rounded border px-3 py-1 disabled:opacity-40">Anterior</button>
                    <span>{actual + 1} / {paginas}</span>
                    <button disabled={actual + 1 >= paginas} onClick={() => setPagina(actual + 1)} className="rounded border px-3 py-1 disabled:opacity-40">Siguiente</button>
                </div>
            </div>
            {verId && (
                <DetalleCotizacion id={verId} onCerrar={() => setVerId(null)} onCambio={() => setRecarga((n) => n + 1)} />
            )}
        </section>
    );
}