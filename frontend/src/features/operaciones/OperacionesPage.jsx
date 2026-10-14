import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Tarjeta from '../../components/Tarjeta'
import Spinner from '../../components/Spinner'
import { request } from '../../services/api'
import { tienePermiso } from '../../services/sesion'
import { fecha, soles } from '../../services/formato'
import { ESTADOS, TIPOS } from './constantes'
import { PillEstado, PillTipo } from './Pills'

const campo = 'rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm focus:border-hcg-gold focus:outline-none'
const VACIO = { tipo: 'VENTA', titulo: '', descripcion: '', clienteId: '', proveedorId: '', contraparteNombre: '', monto: '' }

function Kpi({ titulo, valor, activo, onClick, acento }) {
  return (
    <button type='button' onClick={onClick}
      className={`rounded-xl border bg-white p-4 text-left shadow-sm transition hover:shadow-md ${activo ? 'border-hcg-gold ring-1 ring-hcg-gold' : 'border-gray-100'}`}>
      <div className='text-xs font-medium uppercase tracking-wide text-gray-500'>{titulo}</div>
      <div className={`mt-1 text-2xl font-semibold ${acento ?? 'text-hcg-navy'}`}>{valor ?? '—'}</div>
    </button>
  )
}

export default function OperacionesPage() {
  const [params, setParams] = useSearchParams()
  const filtros = {
    q: params.get('q') ?? '', tipo: params.get('tipo') ?? '', estado: params.get('estado') ?? '',
    proveedorId: params.get('proveedorId') ?? '', clienteId: params.get('clienteId') ?? '',
    desde: params.get('desde') ?? '', hasta: params.get('hasta') ?? '',
  }
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState(null)
  const [resumen, setResumen] = useState(null)
  const [clientes, setClientes] = useState([])
  const [proveedores, setProveedores] = useState([])
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)
  const [form, setForm] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const puedeCrear = tienePermiso('OPERACIONES_CREAR')

  const cambiar = (k, v) => {
    const p = new URLSearchParams(params)
    if (v) p.set(k, v); else p.delete(k)
    setParams(p, { replace: true })
    setPagina(0)
  }

  useEffect(() => {
    request('/api/clients?size=200').then((d) => setClientes(d.content)).catch(() => {})
    request('/api/providers?size=200').then((d) => setProveedores(d.content)).catch(() => {})
  }, [])

  useEffect(() => {
    request('/api/operations/summary').then(setResumen).catch(() => {})
  }, [recarga])

  const consulta = new URLSearchParams({ page: pagina, size: 15 })
  Object.entries(filtros).forEach(([k, v]) => { if (v) consulta.set(k, v) })
  const clave = consulta.toString()

  useEffect(() => {
    const t = setTimeout(() => {
      request(`/api/operations?${clave}`).then((d) => { setDatos(d); setError(null) })
        .catch((e) => setError(e.message))
    }, 250)
    return () => clearTimeout(t)
  }, [clave, recarga])

  async function guardar(e) {
    e.preventDefault()
    setGuardando(true)
    setError(null)
    const body = {
      tipo: form.tipo, titulo: form.titulo.trim(), descripcion: form.descripcion.trim() || null,
      clienteId: form.tipo === 'COMPRA' || !form.clienteId ? null : Number(form.clienteId),
      proveedorId: form.tipo === 'VENTA' || !form.proveedorId ? null : Number(form.proveedorId),
      contraparteNombre: form.contraparteNombre.trim() || null,
      monto: form.monto === '' ? null : Number(form.monto),
    }
    try {
      await request('/api/operations', { method: 'POST', body: JSON.stringify(body) })
      setForm(null)
      setRecarga((n) => n + 1)
    } catch (err) {
      setError(err.message + (err.errors ? ' ' + JSON.stringify(err.errors) : ''))
    } finally { setGuardando(false) }
  }

  const nombreCliente = (id) => clientes.find((c) => c.id === id)?.nombre
  const nombreProveedor = (id) => proveedores.find((p) => p.id === id)?.razonSocial

  return (
    <div className='space-y-5'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <div>
          <h1 className='text-2xl font-semibold text-hcg-navy'>Operaciones</h1>
          <p className='text-sm text-gray-500'>Ventas, compras y pedidos con su línea de tiempo.</p>
        </div>
        {puedeCrear && (
          <button onClick={() => { setError(null); setForm({ ...VACIO }) }}
            className='rounded-lg bg-hcg-navy px-4 py-2 text-sm font-medium text-white hover:bg-hcg-navy-soft'>
            + Nueva operación
          </button>
        )}
      </div>

      <div className='grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-7'>
        <Kpi titulo='Todas' valor={resumen?.total} activo={!filtros.estado && !filtros.tipo} onClick={() => setParams({}, { replace: true })} />
        <Kpi titulo='Abiertas' valor={resumen?.abiertas} activo={filtros.estado === 'ABIERTA'} onClick={() => cambiar('estado', 'ABIERTA')} acento='text-blue-700' />
        <Kpi titulo='En proceso' valor={resumen?.enProceso} activo={filtros.estado === 'EN_PROCESO'} onClick={() => cambiar('estado', 'EN_PROCESO')} acento='text-amber-600' />
        <Kpi titulo='Completadas' valor={resumen?.completadas} activo={filtros.estado === 'COMPLETADA'} onClick={() => cambiar('estado', 'COMPLETADA')} acento='text-emerald-600' />
        <Kpi titulo='Ventas' valor={resumen?.ventas} activo={filtros.tipo === 'VENTA'} onClick={() => cambiar('tipo', 'VENTA')} />
        <Kpi titulo='Compras' valor={resumen?.compras} activo={filtros.tipo === 'COMPRA'} onClick={() => cambiar('tipo', 'COMPRA')} />
        <Kpi titulo='Pedidos' valor={resumen?.pedidos} activo={filtros.tipo === 'PEDIDO'} onClick={() => cambiar('tipo', 'PEDIDO')} />
      </div>

      <Tarjeta>
        <div className='mb-4 flex flex-wrap gap-2'>
          <input className={`${campo} min-w-52 flex-1`} placeholder='Buscar por código, título o contraparte…' value={filtros.q} onChange={(e) => cambiar('q', e.target.value)} />
          <select className={campo} value={filtros.tipo} onChange={(e) => cambiar('tipo', e.target.value)}>
            <option value=''>Todos los tipos</option>
            {Object.entries(TIPOS).map(([k, t]) => <option key={k} value={k}>{t}</option>)}
          </select>
          <select className={campo} value={filtros.estado} onChange={(e) => cambiar('estado', e.target.value)}>
            <option value=''>Todos los estados</option>
            {Object.entries(ESTADOS).map(([k, t]) => <option key={k} value={k}>{t}</option>)}
          </select>
          <select className={campo} value={filtros.proveedorId} onChange={(e) => cambiar('proveedorId', e.target.value)}>
            <option value=''>Todos los proveedores</option>
            {proveedores.map((p) => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}
          </select>
          <select className={campo} value={filtros.clienteId} onChange={(e) => cambiar('clienteId', e.target.value)}>
            <option value=''>Todos los clientes</option>
            {clientes.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
          <input type='date' className={campo} value={filtros.desde} onChange={(e) => cambiar('desde', e.target.value)} title='Desde' />
          <input type='date' className={campo} value={filtros.hasta} onChange={(e) => cambiar('hasta', e.target.value)} title='Hasta' />
        </div>

        {error && <p className='mb-3 rounded-lg bg-red-50 p-3 text-sm text-red-700'>{error}</p>}

        {!datos ? <div className='flex justify-center py-10'><Spinner /></div> : (
          <>
            <div className='overflow-x-auto'>
              <table className='w-full text-sm'>
                <thead>
                  <tr className='text-left text-xs uppercase text-gray-500'>
                    <th className='py-2 pr-3'>Código</th><th className='pr-3'>Tipo</th><th className='pr-3'>Título</th>
                    <th className='pr-3'>Contraparte</th><th className='pr-3'>Inicio</th><th className='pr-3 text-right'>Monto</th><th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {datos.content.map((o) => (
                    <tr key={o.id} className='border-t border-gray-100 hover:bg-slate-50'>
                      <td className='py-2.5 pr-3 font-mono text-xs'>
                        <Link to={`/operaciones/${o.id}`} className='text-hcg-navy underline-offset-2 hover:underline'>{o.codigo}</Link>
                      </td>
                      <td className='pr-3'><PillTipo tipo={o.tipo} /></td>
                      <td className='pr-3 text-slate-800'>{o.titulo}</td>
                      <td className='pr-3 text-gray-600'>{nombreCliente(o.clienteId) ?? nombreProveedor(o.proveedorId) ?? o.contraparteNombre ?? '—'}</td>
                      <td className='pr-3 text-gray-600'>{fecha(o.fechaInicio)}</td>
                      <td className='pr-3 text-right'>{o.monto != null ? soles(o.monto) : '—'}</td>
                      <td><PillEstado estado={o.estado} /></td>
                    </tr>
                  ))}
                  {datos.content.length === 0 && <tr><td colSpan={7} className='py-8 text-center text-gray-400'>Sin operaciones para estos filtros.</td></tr>}
                </tbody>
              </table>
            </div>
            <div className='mt-4 flex items-center justify-between text-sm text-gray-500'>
              <span>{datos.totalElements} resultado(s)</span>
              <div className='flex gap-2'>
                <button disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)} className='rounded border px-3 py-1 disabled:opacity-40'>Anterior</button>
                <span className='py-1'>{pagina + 1} / {Math.max(datos.totalPages, 1)}</span>
                <button disabled={pagina + 1 >= datos.totalPages} onClick={() => setPagina((p) => p + 1)} className='rounded border px-3 py-1 disabled:opacity-40'>Siguiente</button>
              </div>
            </div>
          </>
        )}
      </Tarjeta>

      {form && (
        <div className='fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4'>
          <form onSubmit={guardar} className='max-h-[90vh] w-full max-w-lg space-y-3 overflow-y-auto rounded-xl bg-white p-6 shadow-xl'>
            <h2 className='text-lg font-semibold text-hcg-navy'>Nueva operación</h2>
            <select className={`${campo} w-full`} value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
              {Object.entries(TIPOS).map(([k, t]) => <option key={k} value={k}>{t}</option>)}
            </select>
            <input required className={`${campo} w-full`} placeholder='Título' value={form.titulo} onChange={(e) => setForm({ ...form, titulo: e.target.value })} />
            <textarea className={`${campo} w-full`} rows={3} placeholder='Descripción' value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} />
            {form.tipo !== 'COMPRA' && (
              <select className={`${campo} w-full`} value={form.clienteId} onChange={(e) => setForm({ ...form, clienteId: e.target.value })}>
                <option value=''>Cliente (opcional)</option>
                {clientes.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
            )}
            {form.tipo !== 'VENTA' && (
              <select className={`${campo} w-full`} value={form.proveedorId} onChange={(e) => setForm({ ...form, proveedorId: e.target.value })}>
                <option value=''>Proveedor (opcional)</option>
                {proveedores.map((p) => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}
              </select>
            )}
            <input className={`${campo} w-full`} placeholder='Contraparte (obligatoria si no eliges cliente ni proveedor)' value={form.contraparteNombre} onChange={(e) => setForm({ ...form, contraparteNombre: e.target.value })} />
            <input type='number' step='0.01' min='0' className={`${campo} w-full`} placeholder='Monto estimado (S/)' value={form.monto} onChange={(e) => setForm({ ...form, monto: e.target.value })} />
            {error && <p className='rounded-lg bg-red-50 p-2 text-sm text-red-700'>{error}</p>}
            <div className='flex justify-end gap-2'>
              <button type='button' onClick={() => setForm(null)} className='rounded-lg border px-4 py-2 text-sm'>Cancelar</button>
              <button disabled={guardando} className='rounded-lg bg-hcg-navy px-4 py-2 text-sm text-white disabled:opacity-60'>{guardando ? 'Guardando…' : 'Crear'}</button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
