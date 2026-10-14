// Barras agrupadas por mes (aprobadas / pendientes / rechazadas) con CSS puro
const SERIES = [
    ['aprobadas', 'Aprobadas', 'bg-emerald-500'],
    ['pendientes', 'Pendientes', 'bg-hcg-gold'],
    ['rechazadas', 'Rechazadas', 'bg-red-400'],
]
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

export default function BarrasAgrupadas({ datos }) {
    const max = Math.max(1, ...datos.flatMap((m) => SERIES.map(([k]) => m[k])))
    return (
        <div>
            <div className='mb-3 flex gap-4 text-xs text-gray-600'>
                {SERIES.map(([k, t, c]) => (
                    <span key={k} className='flex items-center gap-1.5'><span className={`h-2.5 w-2.5 rounded-sm ${c}`} />{t}</span>
                ))}
            </div>
            <div className='flex h-48 items-end gap-3 border-b border-gray-200'>
                {datos.map((m) => (
                    <div key={m.mes} className='flex h-full flex-1 items-end justify-center gap-1'>
                        {SERIES.map(([k, t, c]) => (
                            <div key={k} title={`${t}: ${m[k]}`} className={`w-full max-w-5 rounded-t ${c}`}
                                style={{ height: `${(m[k] / max) * 100}%`, minHeight: m[k] ? 3 : 0 }} />
                        ))}
                    </div>
                ))}
            </div>
            <div className='mt-1 flex gap-3'>
                {datos.map((m) => (
                    <span key={m.mes} className='flex-1 text-center text-xs capitalize text-gray-500'>{MESES[Number(m.mes.slice(5)) - 1]}</span>
                ))}
            </div>
        </div>
    )
}
