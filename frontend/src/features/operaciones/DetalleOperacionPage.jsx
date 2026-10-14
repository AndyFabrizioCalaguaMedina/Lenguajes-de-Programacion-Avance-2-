import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Tarjeta from '../../components/Tarjeta'
import Spinner from '../../components/Spinner'
import { request } from '../../services/api'
import { tienePermiso } from '../../services/sesion'
import { fecha, fechaHora, soles } from '../../services/formato'
import { EVENTOS } from './constantes'
import { PillEstado, PillTipo } from './Pills'

const campo = 'rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm focus:border-hcg-gold focus:outline-none'

export default function DetalleOperacionPage() {
  const { id } = useParams()
  const [op, setOp] = useState(null)
  const [error, setError] = useState(null)
  const [ev, setEv] = useState({ tipo: 'NOTA', titulo: '', detalle: '', referencia: '' })
  const [enviando, setEnviando] = useState(false)
  const puedeEditar = tienePermiso('OPERACIONES_EDITAR')

  useEffect(() => {
    request(`/api/operations/${id}`).then(setOp).catch((e) => setError(e.message))
  }, [id])

  const cerrada = op && (op.estado === 'COMPLETADA' || op.estado === 'CANCELADA')

  async function agregar(e) {
    e.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      const body = { tipo: ev.tipo, titulo: ev.titulo.trim(), detalle: ev.detalle.trim() || null, referencia: ev.referencia.trim() || null }
      setOp(await request(`/api/operations/${id}/events`, { method: 'POST', body: JSON.stringify(body) }))
      setEv({ tipo: 'NOTA', titulo: '', detalle: '', referencia: '' })
    } catch (err) { setError(err.message) } finally { setEnviando(false) }
  }

  async function estado(nuevo) {
    if (nuevo === 'CANCELADA' && !window.confirm('¿Cancelar esta operación? No podrá reabrirse.')) return
    setError(null)
    try {
      setOp(await request(`/api/operations/${id}/status`, { method: 'PATCH', body: JSON.stringify({ estado: nuevo }) }))
    } catch (err) { setError(err.message) }
  }

  if (!op) return <div className='flex justify-center py-16'>{error ? <p className='text-red-600'>{error}</p> : <Spinner />}</div>

  return (
    <div className='space-y-5'>
      <div>
        <Link to='/operaciones' className='text-sm text-gray-500 hover:text-hcg-navy'>← Operaciones</Link>
        <div className='mt-1 flex flex-wrap items-center gap-3'>
          <h1 className='text-2xl font-semibold text-hcg-navy'>Detalle de operación</h1>
          <span className='font-mono text-sm text-gray-500'>{op.codigo}</span>
          <PillTipo tipo={op.tipo} /> <PillEstado estado={op.estado} />
        </div>
      </div>

      {error && <p className='rounded-lg bg-red-50 p-3 text-sm text-red-700'>{error}</p>}

      <div className='grid gap-5 lg:grid-cols-3'>
        <div className='space-y-5 lg:col-span-2'>
          <Tarjeta titulo='Línea de tiempo'>
            <ol className='relative ml-3 border-l-2 border-hcg-gold/40'>
              {op.eventos.map((e) => (
                <li key={e.id} className='mb-5 ml-5'>
                  <span className='absolute -left-[7px] mt-1.5 h-3 w-3 rounded-full bg-hcg-gold ring-4 ring-white' />
                  <div className='flex flex-wrap items-baseline gap-2'>
                    <span className='rounded bg-hcg-navy/10 px-2 py-0.5 text-xs font-medium text-hcg-navy'>{EVENTOS[e.tipo] ?? e.tipo}</span>
                    <span className='font-medium text-slate-800'>{e.titulo}</span>
                    <span className='text-xs text-gray-400'>{fechaHora(e.fecha)}</span>
                  </div>
                  {e.detalle && <p className='mt-1 text-sm text-gray-600'>{e.detalle}</p>}
                  {e.referencia && <p className='mt-0.5 font-mono text-xs text-gray-500'>Ref: {e.referencia}</p>}
                </li>
              ))}
            </ol>
          </Tarjeta>

          {puedeEditar && !cerrada && (
            <Tarjeta titulo='Registrar avance'>
              <form onSubmit={agregar} className='grid gap-3 sm:grid-cols-2'>
                <select className={campo} value={ev.tipo} onChange={(e) => setEv({ ...ev, tipo: e.target.value })}>
                  {Object.entries(EVENTOS).map(([k, t]) => <option key={k} value={k}>{t}</option>)}
                </select>
                <input required className={campo} placeholder='Título' value={ev.titulo} onChange={(e) => setEv({ ...ev, titulo: e.target.value })} />
                <input className={`${campo} sm:col-span-2`} placeholder='Detalle (opcional)' value={ev.detalle} onChange={(e) => setEv({ ...ev, detalle: e.target.value })} />
                <input className={campo} placeholder='Referencia (ej. COT-2026-001)' value={ev.referencia} onChange={(e) => setEv({ ...ev, referencia: e.target.value })} />
                <button disabled={enviando} className='rounded-lg bg-hcg-navy px-4 py-2 text-sm text-white hover:bg-hcg-navy-soft disabled:opacity-60'>Agregar a la línea de tiempo</button>
              </form>
            </Tarjeta>
          )}
        </div>

        <div className='space-y-5'>
          <Tarjeta titulo='Resumen'>
            <dl className='space-y-2 text-sm'>
              <Fila k='Título' v={op.titulo} />
              <Fila k='Descripción' v={op.descripcion} />
              <Fila k='Contraparte' v={op.contraparteNombre} />
              <Fila k='Monto' v={op.monto != null ? soles(op.monto) : null} />
              <Fila k='Inicio' v={fecha(op.fechaInicio)} />
              <Fila k='Cierre' v={op.fechaCierre ? fecha(op.fechaCierre) : null} />
            </dl>
          </Tarjeta>
          {puedeEditar && !cerrada && (
            <Tarjeta titulo='Cambiar estado'>
              <div className='flex flex-col gap-2'>
                {op.estado === 'ABIERTA' && <button onClick={() => estado('EN_PROCESO')} className='rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800'>Marcar en proceso</button>}
                <button onClick={() => estado('COMPLETADA')} className='rounded-lg border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm text-emerald-800'>Completar</button>
                <button onClick={() => estado('CANCELADA')} className='rounded-lg border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700'>Cancelar</button>
              </div>
            </Tarjeta>
          )}
        </div>
      </div>
    </div>
  )
}

function Fila({ k, v }) {
  return (
    <div className='flex justify-between gap-3 border-b border-gray-50 pb-1.5'>
      <dt className='text-gray-500'>{k}</dt>
      <dd className='text-right text-slate-800'>{v || '—'}</dd>
    </div>
  )
}
