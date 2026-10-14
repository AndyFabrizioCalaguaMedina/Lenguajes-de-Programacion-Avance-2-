import { useEffect, useState } from 'react'
import Barras, { BarrasLista } from '../../components/charts/Barras'
import Dona from '../../components/charts/Dona'
import Tarjeta from '../../components/Tarjeta'
import { request } from '../../services/api'
import { accion, hace, isoLocal, soles } from '../../services/formato'

function Kpi({ titulo, valor }) {
  return (
    <div className='bg-white rounded-xl p-5 shadow'>
      <p className='text-sm text-gray-500'>{titulo}</p>
      <p className='text-2xl font-bold text-slate-800'>{valor}</p>
    </div>
  )
}

export default function ReportesPage() {
  const [desde, setDesde] = useState(hace(29))
  const [hasta, setHasta] = useState(isoLocal())
  const [r, setR] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    request(`/api/stats/reportes?desde=${desde}&hasta=${hasta}`)
      .then((d) => { setR(d); setError('') })
      .catch((e) => { setError(e.message); setR(null) })
  }, [desde, hasta])

  function exportarCsv() {
    const filas = ['fecha,total,boletas', ...r.ventasPorDia.map((p) => `${p.fecha},${p.total},${p.cantidad}`)]
    const url = URL.createObjectURL(new Blob([filas.join('\n')], { type: 'text/csv' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `ventas_${desde}_${hasta}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  const COLORES = { BORRADOR: '#94a3b8', ENVIADA: '#c79a3b', APROBADA: '#10b981', RECHAZADA: '#f87171', FACTURADA: '#0f2b4f' }
  const cot = r?.cotizacionesPorEstado ?? []
  const cotTotal = cot.reduce((a, c) => a + c.cantidad, 0)
  const cotAprob = cot.filter((c) => c.nombre === 'APROBADA' || c.nombre === 'FACTURADA').reduce((a, c) => a + c.cantidad, 0)
  const tasa = cotTotal ? Math.round((cotAprob / cotTotal) * 100) + '%' : '—'
  const nombresEstado = (l) => l.map((c) => ({ nombre: c.nombre, valor: c.cantidad }))

  return (
    <section>
      <div className='pb-6 flex flex-wrap items-end justify-between gap-4'>
        <div>
          <h1 className='text-hcg-navy text-2xl font-bold'>Reportes</h1>
          <p className='text-slate-500 text-sm'>Ventas, cotizaciones, bodega y actividad del equipo</p>
        </div>
        <div className='flex flex-wrap items-end gap-3 text-sm'>
          <label className='flex flex-col text-xs text-gray-600'>Desde
            <input type='date' value={desde} max={hasta} onChange={(e) => setDesde(e.target.value)}
              className='rounded-lg border border-gray-200 bg-white p-2 text-sm' />
          </label>
          <label className='flex flex-col text-xs text-gray-600'>Hasta
            <input type='date' value={hasta} min={desde} onChange={(e) => setHasta(e.target.value)}
              className='rounded-lg border border-gray-200 bg-white p-2 text-sm' />
          </label>
          {[['7 días', 6], ['30 días', 29], ['90 días', 89]].map(([t, n]) => (
            <button key={n} onClick={() => { setDesde(hace(n)); setHasta(isoLocal()) }}
              className='rounded-full border border-gray-300 bg-white px-3 py-2 text-xs text-gray-700 hover:border-hcg-gold'>{t}</button>
          ))}
          {r && (
            <button onClick={exportarCsv} className='rounded-lg bg-hcg-navy px-3 py-2 text-sm font-semibold text-white'>
              Exportar ventas (CSV)
            </button>
          )}
        </div>
      </div>

      {error && <p className='text-sm text-red-600'>{error}</p>}
      {!r && !error && <p className='text-sm text-gray-500'>Cargando reportes...</p>}

      {r && (
        <div className='flex flex-col gap-6'>
          <div className='grid grid-cols-1 md:grid-cols-4 gap-4'>
            <Kpi titulo='Ventas (boletas aceptadas)' valor={soles(r.ventasTotal)} />
            <Kpi titulo='Boletas aceptadas' valor={r.boletasAceptadas} />
            <Kpi titulo='Ticket promedio' valor={soles(r.ticketPromedio)} />
            <Kpi titulo='Tasa de aceptación de cotizaciones' valor={tasa} />
          </div>

          <Tarjeta titulo='Ventas por día'>
            <Barras
              datos={r.ventasPorDia.map((p) => ({ etiqueta: p.fecha.slice(8) + '/' + p.fecha.slice(5, 7), valor: Number(p.total) }))}
              formato={soles} color='#059669'
            />
          </Tarjeta>

          <div className='grid grid-cols-1 xl:grid-cols-2 gap-6'>
            <Tarjeta titulo='Productos y servicios más vendidos'>
              <BarrasLista filas={r.topProductos.map((p) => ({ nombre: p.nombre, valor: Number(p.total), extra: `${p.cantidad} uds` }))}
                formato={soles} color='bg-emerald-500' />
            </Tarjeta>
            <Tarjeta titulo='Mejores clientes'>
              <BarrasLista filas={r.topClientes.map((p) => ({ nombre: p.nombre, valor: Number(p.total), extra: `${p.cantidad} boletas` }))}
                formato={soles} color='bg-purple-500' />
            </Tarjeta>
            <Tarjeta titulo='Boletas por estado'>
              <BarrasLista filas={nombresEstado(r.boletasPorEstado)} color='bg-blue-500' />
            </Tarjeta>
            <Tarjeta titulo='Cotizaciones por estado'>
              <Dona filas={cot.map((c) => ({ nombre: c.nombre, valor: c.cantidad, color: COLORES[c.nombre] ?? '#64748b' }))} />
            </Tarjeta>
            <Tarjeta titulo='Salidas de bodega por motivo'>
              <BarrasLista filas={nombresEstado(r.salidasPorMotivo)} color='bg-orange-500' />
            </Tarjeta>
            {r.actividadPorUsuario.length > 0 && (
              <Tarjeta titulo='Actividad por empleado (acciones registradas)'>
                <BarrasLista filas={nombresEstado(r.actividadPorUsuario)} color='bg-slate-600' />
              </Tarjeta>
            )}
          </div>

          {r.actividadPorAccion.length > 0 && (
            <Tarjeta titulo='Acciones más frecuentes'>
              <BarrasLista filas={r.actividadPorAccion.slice(0, 8).map((c) => ({ nombre: accion(c.nombre), valor: c.cantidad }))}
                color='bg-slate-500' />
            </Tarjeta>
          )}
        </div>
      )}
    </section>
  )
}
