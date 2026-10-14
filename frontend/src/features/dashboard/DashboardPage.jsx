import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FilePlus2, Workflow, Receipt, Search, BarChart3, Warehouse } from 'lucide-react'
import StackCards from './components/StackCards'
import BarrasAgrupadas from './components/BarrasAgrupadas'
import Barras, { BarrasLista } from '../../components/charts/Barras'
import Tarjeta from '../../components/Tarjeta'
import { request } from '../../services/api'
import { tienePermiso, usuarioActual } from '../../services/sesion'
import { accion, fechaHora, soles } from '../../services/formato'

const ACCESOS = [
    { to: '/cotizaciones/nueva-cotizacion', texto: 'Nueva cotización', icon: FilePlus2, permiso: 'COTIZACIONES_CREAR' },
    { to: '/operaciones', texto: 'Operaciones', icon: Workflow, permiso: 'OPERACIONES_VER' },
    { to: '/boletas', texto: 'Emitir boleta', icon: Receipt, permiso: 'COMPROBANTES_VER' },
    { to: '/bodega', texto: 'Salida de bodega', icon: Warehouse, permiso: 'BODEGA_VER' },
    { to: '/busqueda', texto: 'Búsqueda', icon: Search },
    { to: '/reportes', texto: 'Reportes', icon: BarChart3, permiso: 'REPORTES_VER' },
]

export default function DashboardPage() {
    const [d, setD] = useState(null)
    const [error, setError] = useState('')
    const usuario = usuarioActual()

    useEffect(() => {
        request('/api/stats/dashboard')
            .then(setD)
            .catch((e) => setError(e.message))
    }, [])

    return (
        <section>
            <div className='pb-8'>
                <h1 className='text-hcg-navy text-2xl font-bold'>Dashboard</h1>
                <p className='text-slate-500 text-sm'>
                    Bienvenido, {usuario?.nombreCompleto ?? 'usuario'} · <span className='capitalize'>{new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</span>
                </p>
            </div>

            {error && <p className='text-sm text-red-600'>{error}</p>}
            {!d && !error && <p className='text-sm text-gray-500'>Cargando estadísticas...</p>}

            {d && (
                <div className='flex flex-col gap-6'>
                    <StackCards d={d} />

                    <div className='grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-3'>
                        {ACCESOS.filter((a) => !a.permiso || tienePermiso(a.permiso)).map((a) => {
                            const Icono = a.icon
                            return (
                                <Link key={a.to} to={a.to}
                                    className='flex items-center gap-3 rounded-xl border border-gray-100 bg-white px-4 py-3 text-sm font-medium text-hcg-navy shadow-sm transition hover:border-hcg-gold hover:shadow-md'>
                                    <Icono className='h-5 w-5 text-hcg-gold' />{a.texto}
                                </Link>
                            )
                        })}
                    </div>

                    <div className='grid grid-cols-1 xl:grid-cols-3 gap-6'>
                        <Tarjeta titulo='Cotizaciones de los últimos 6 meses' className='xl:col-span-2'>
                            <BarrasAgrupadas datos={d.cotizacionesPorMes ?? []} />
                        </Tarjeta>
                        <Tarjeta titulo='Cotizaciones por estado'>
                            <BarrasLista
                                filas={d.cotizacionesPorEstado.map((c) => ({ nombre: c.nombre, valor: c.cantidad }))}
                                color='bg-hcg-gold'
                            />
                        </Tarjeta>
                    </div>

                    <div className='grid grid-cols-1 gap-6'>
                        <Tarjeta titulo='Ventas de los últimos 14 días (boletas aceptadas)'>
                            <Barras
                                datos={d.ventasPorDia.map((p) => ({ etiqueta: p.fecha.slice(8) + '/' + p.fecha.slice(5, 7), valor: Number(p.total) }))}
                                formato={soles}
                            />
                        </Tarjeta>
                    </div>

                    {d.actividadReciente.length > 0 && (
                        <Tarjeta titulo='Actividad reciente del equipo'>
                            <ul className='divide-y divide-gray-100'>
                                {d.actividadReciente.map((a) => (
                                    <li key={a.id} className='flex justify-between gap-4 py-2 text-sm'>
                                        <span className='text-gray-700'>
                                            <strong>{a.username ?? 'sistema'}</strong> · {accion(a.accion)}
                                        </span>
                                        <span className='shrink-0 text-gray-400'>{fechaHora(a.fecha)}</span>
                                    </li>
                                ))}
                            </ul>
                            <Link to='/historial' className='mt-3 inline-block text-sm text-hcg-navy hover:underline'>
                                Ver historial completo
                            </Link>
                        </Tarjeta>
                    )}
                </div>
            )}
        </section>
    )
}
