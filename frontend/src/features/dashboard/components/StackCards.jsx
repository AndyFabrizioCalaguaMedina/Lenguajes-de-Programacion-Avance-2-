import { Link } from 'react-router-dom'
import { Users, HourglassCog, FileText, Receipt, Banknote, Warehouse, Workflow } from 'lucide-react'
import { soles } from '../../../services/formato'

// Tarjetas clicables: cada una lleva al módulo (con el filtro ya aplicado cuando corresponde)
export default function StackCards({ d }) {
    const cards = [
        { icon: Banknote, number: soles(d.ventasMes), label: "Ventas del mes", span: `${d.boletasAceptadas} boletas aceptadas`,
          to: '/boletas', tono: 'emerald' },
        { icon: Receipt, number: d.boletasMes, label: "Boletas del mes",
          span: d.boletasConProblema ? `${d.boletasConProblema} rechazadas o con error` : "sin problemas",
          to: '/boletas', tono: 'blue', alerta: d.boletasConProblema > 0 },
        { icon: HourglassCog, number: d.cotizacionesPendientes, label: "Cotizaciones pendientes",
          span: `${d.cotizacionesAprobadas} aprobadas`, to: '/cotizaciones', tono: 'amber' },
        { icon: Workflow, number: d.operacionesActivas ?? 0, label: "Operaciones activas", span: "abiertas y en proceso",
          to: '/operaciones?estado=EN_PROCESO', tono: 'navy' },
        { icon: Users, number: d.clientes, label: "Clientes activos", span: "registrados", to: '/clientes', tono: 'purple' },
        { icon: FileText, number: d.documentos, label: "Documentos", span: "en el repositorio", to: '/documentos', tono: 'sky' },
        { icon: Warehouse, number: d.salidasMes, label: "Salidas de bodega", span: "este mes", to: '/bodega', tono: 'orange' },
    ]
    const TONOS = {
        emerald: 'bg-emerald-100 text-emerald-700', blue: 'bg-blue-100 text-blue-700', amber: 'bg-hcg-gold-soft text-amber-700',
        navy: 'bg-hcg-navy/10 text-hcg-navy', purple: 'bg-purple-100 text-purple-700', sky: 'bg-sky-100 text-sky-700',
        orange: 'bg-orange-100 text-orange-700',
    }

    return (
        <div className="grid grid-cols-2 lg:grid-cols-4 xl:grid-cols-7 gap-4">
            {cards.map((item) => {
                const Icono = item.icon
                return (
                    <Link key={item.label} to={item.to}
                        className="bg-white rounded-xl p-4 grid gap-1.5 shadow-sm border border-gray-100 transition hover:shadow-md hover:border-hcg-gold">
                        <Icono className={`p-2.5 w-11 h-11 rounded-xl ${TONOS[item.tono]}`} />
                        <h2 className="text-xl font-bold text-hcg-navy break-words">{item.number}</h2>
                        <p className="text-sm text-gray-600">{item.label}</p>
                        <span className={`text-xs ${item.alerta ? 'text-red-600' : 'text-gray-400'}`}>{item.span}</span>
                    </Link>
                )
            })}
        </div>
    )
}
