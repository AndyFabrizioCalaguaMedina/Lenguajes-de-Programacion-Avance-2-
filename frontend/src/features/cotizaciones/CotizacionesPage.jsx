import { useState } from "react";
import { Link } from "react-router-dom";
import CotizacionesTabla from "./components/CotizacionesTabla";

const ESTADOS = ["", "BORRADOR", "ENVIADA", "APROBADA", "RECHAZADA", "FACTURADA"];

export default function CotizacionesPage() {
    const [cotizaciones, setCotizaciones] = useState([]);
    const [estado, setEstado] = useState("");
    const [q, setQ] = useState("");
    const [desde, setDesde] = useState("");
    const [hasta, setHasta] = useState("");

    const cuenta = (e) => (e ? cotizaciones.filter((c) => c.estado === e).length : cotizaciones.length);
    const campo = "rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm focus:border-hcg-gold focus:outline-none";

    return (
        <section>
            <div className='pb-6 flex justify-between items-center'>
                <div>
                    <h1 className='text-hcg-navy text-2xl font-bold'>Cotizaciones</h1>
                    <p className='text-slate-500 text-sm'>
                        <span className="font-semibold text-slate-700">{cotizaciones.length}</span>{' '}
                        {cotizaciones.length === 1 ? 'cotización' : 'cotizaciones'} en total
                    </p>
                </div>
                <Link
                    to="/cotizaciones/nueva-cotizacion"
                    className='flex gap-2 items-center text-white text-sm font-semibold bg-hcg-navy hover:bg-hcg-navy-soft transition-colors rounded-xl px-4 py-2.5'
                >
                    + Nueva cotización
                </Link>
            </div>

            <div className="mb-4 flex flex-wrap gap-2">
                {ESTADOS.map((e) => (
                    <button key={e} onClick={() => setEstado(e)}
                        className={`rounded-full border px-3.5 py-1.5 text-sm transition ${estado === e ? "border-hcg-navy bg-hcg-navy text-white" : "border-gray-300 bg-white text-gray-700 hover:border-hcg-gold"}`}>
                        {e ? e.charAt(0) + e.slice(1).toLowerCase() : "Todas"} <span className="opacity-70">({cuenta(e)})</span>
                    </button>
                ))}
            </div>

            <div className="mb-4 flex flex-wrap gap-2">
                <input className={`${campo} min-w-60 flex-1`} placeholder="Buscar por código o cliente…" value={q} onChange={(e) => setQ(e.target.value)} />
                <input type="date" className={campo} value={desde} onChange={(e) => setDesde(e.target.value)} title="Desde" />
                <input type="date" className={campo} value={hasta} onChange={(e) => setHasta(e.target.value)} title="Hasta" />
            </div>

            <CotizacionesTabla
                cotizaciones={cotizaciones}
                setCotizaciones={setCotizaciones}
                filtro={{ estado, q, desde, hasta }}
            />
        </section>
    );
}
