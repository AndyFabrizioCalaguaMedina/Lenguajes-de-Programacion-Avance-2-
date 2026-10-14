import { useEffect, useState } from 'react'
import Tarjeta from '../../components/Tarjeta'
import { request } from '../../services/api'
import { ACCIONES, accion, fechaHora, hace, isoLocal } from '../../services/formato'

export default function HistorialPage() {
  const [usuarios, setUsuarios] = useState([])
  const [username, setUsername] = useState('')
  const [tipo, setTipo] = useState('')
  const [desde, setDesde] = useState(hace(7))
  const [hasta, setHasta] = useState(isoLocal())
  const [page, setPage] = useState(0)
  const [r, setR] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    request('/api/users').then(setUsuarios).catch(() => {})
  }, [])

  useEffect(() => {
    const p = new URLSearchParams({ page, size: 25, desde, hasta })
    if (username) p.set('username', username)
    if (tipo) p.set('accion', tipo)
    request(`/api/audit/search?${p}`)
      .then((d) => { setR(d); setError('') })
      .catch((e) => setError(e.message))
  }, [username, tipo, desde, hasta, page])

  const cambiar = (set) => (e) => { set(e.target.value); setPage(0) }
  const campo = 'rounded-lg border border-gray-200 bg-white p-2 text-sm'

  return (
    <section>
      <div className='pb-6'>
        <h1 className='text-slate-800 text-2xl font-bold'>Historial de actividad</h1>
        <p className='text-slate-500 text-sm'>Qué hizo cada persona en el sistema y cuándo</p>
      </div>

      <div className='mb-6 flex flex-wrap gap-3'>
        <select value={username} onChange={cambiar(setUsername)} className={campo}>
          <option value=''>Todos los usuarios</option>
          {usuarios.map((u) => <option key={u.id} value={u.username}>{u.nombreCompleto} ({u.username})</option>)}
        </select>
        <select value={tipo} onChange={cambiar(setTipo)} className={campo}>
          <option value=''>Todas las acciones</option>
          {Object.entries(ACCIONES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <input type='date' value={desde} max={hasta} onChange={cambiar(setDesde)} className={campo} />
        <input type='date' value={hasta} min={desde} onChange={cambiar(setHasta)} className={campo} />
      </div>

      <Tarjeta>
        {error && <p className='text-sm text-red-600'>{error}</p>}
        {!r && !error && <p className='text-sm text-gray-500'>Cargando...</p>}
        {r && r.content.length === 0 && <p className='text-sm text-gray-400'>No hay actividad con esos filtros.</p>}
        {r && r.content.length > 0 && (
          <div className='overflow-x-auto'>
            <table className='w-full text-left text-sm text-gray-600'>
              <thead className='border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700'>
                <tr>{['Fecha', 'Usuario', 'Acción', 'Detalle'].map((h) => <th key={h} className='px-4 py-3 font-medium'>{h}</th>)}</tr>
              </thead>
              <tbody>
                {r.content.map((a) => (
                  <tr key={a.id} className='border-b border-gray-100 last:border-0'>
                    <td className='px-4 py-3 whitespace-nowrap'>{fechaHora(a.fecha)}</td>
                    <td className='px-4 py-3 font-medium text-gray-900'>{a.username ?? 'sistema'}</td>
                    <td className='px-4 py-3'>{accion(a.accion)}</td>
                    <td className='px-4 py-3 text-xs text-gray-500'>
                      {[a.entidad && `${a.entidad}${a.entidadId ? ' #' + a.entidadId : ''}`, a.valorNuevo].filter(Boolean).join(' · ')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {r && r.totalPages > 1 && (
          <div className='mt-4 flex items-center justify-between text-sm'>
            <button disabled={page === 0} onClick={() => setPage(page - 1)} className='rounded-lg border px-3 py-1 disabled:opacity-40'>Anterior</button>
            <span className='text-gray-500'>Página {r.page + 1} de {r.totalPages} · {r.totalElements} registros</span>
            <button disabled={page + 1 >= r.totalPages} onClick={() => setPage(page + 1)} className='rounded-lg border px-3 py-1 disabled:opacity-40'>Siguiente</button>
          </div>
        )}
      </Tarjeta>
    </section>
  )
}
