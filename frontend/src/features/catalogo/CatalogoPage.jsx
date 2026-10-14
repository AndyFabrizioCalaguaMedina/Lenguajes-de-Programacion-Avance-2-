import { useCallback, useEffect, useState } from 'react'
import Tarjeta from '../../components/Tarjeta'
import { request } from '../../services/api'
import { tienePermiso } from '../../services/sesion'
import ImportarProductos from './ImportarProductos'

const campo = 'w-full rounded-lg border border-gray-200 p-2 text-sm'

// Pantalla genérica de catálogo (listar, buscar, crear, editar, activar/desactivar).
// config: { titulo, subtitulo, api, permiso, campos:[{k,label,tipo,req}], columnas:[{label,ver}], nuevoTexto }
export default function CatalogoPage({ config }) {
  const { titulo, subtitulo, api, permiso, campos, columnas, nuevoTexto, importable } = config
  const [filas, setFilas] = useState([])
  const [q, setQ] = useState('')
  const [recarga, setRecarga] = useState(0)
  const [form, setForm] = useState(null) // null = cerrado; { id?, ...valores }
  const [mensaje, setMensaje] = useState(null)
  const [guardando, setGuardando] = useState(false)
  const [importando, setImportando] = useState(false)
  const puedeCrear = tienePermiso(`${permiso}_CREAR`)
  const puedeEditar = tienePermiso(`${permiso}_EDITAR`)

  useEffect(() => {
    const t = setTimeout(() => {
      request(`${api}?size=100${q.trim() ? `&q=${encodeURIComponent(q.trim())}` : ''}`)
        .then((d) => setFilas(d.content))
        .catch((e) => setMensaje({ ok: false, texto: e.message }))
    }, 250)
    return () => clearTimeout(t)
  }, [api, q, recarga])

  const refrescar = useCallback(() => setRecarga((n) => n + 1), [])

  function abrirNuevo() {
    setMensaje(null)
    setForm(Object.fromEntries(campos.map((c) => [c.k, ''])))
  }

  function abrirEditar(fila) {
    setMensaje(null)
    setForm({ id: fila.id, ...Object.fromEntries(campos.map((c) => [c.k, fila[c.k] ?? ''])) })
  }

  async function guardar(e) {
    e.preventDefault()
    setGuardando(true)
    setMensaje(null)
    const body = {}
    campos.forEach((c) => {
      const v = form[c.k]
      if (v === '' || v == null) { body[c.k] = null; return }
      body[c.k] = c.tipo === 'number' ? Number(v) : String(v).trim()
    })
    try {
      await request(form.id ? `${api}/${form.id}` : api, { method: form.id ? 'PUT' : 'POST', body: JSON.stringify(body) })
      setMensaje({ ok: true, texto: form.id ? 'Cambios guardados.' : 'Registro creado.' })
      setForm(null)
      refrescar()
    } catch (err) {
      const det = err.errors ? ' ' + JSON.stringify(err.errors) : ''
      setMensaje({ ok: false, texto: err.message + det })
    } finally { setGuardando(false) }
  }

  async function cambiarEstado(fila) {
    try {
      await request(`${api}/${fila.id}/status`, { method: 'PATCH', body: JSON.stringify({ activo: !fila.activo }) })
      refrescar()
    } catch (e) { setMensaje({ ok: false, texto: e.message }) }
  }

  return (
    <section>
      <div className='pb-6 flex flex-wrap items-center justify-between gap-4'>
        <div>
          <h1 className='text-slate-800 text-2xl font-bold'>{titulo}</h1>
          <p className='text-slate-500 text-sm'>{subtitulo}</p>
        </div>
        {puedeCrear && (
          <div className='flex flex-wrap gap-2'>
            {importable && (
              <button onClick={() => { setForm(null); setImportando(true) }}
                className='rounded-xl border border-hcg-navy p-3 text-sm font-semibold text-hcg-navy hover:bg-hcg-navy/5'>
                Importar Excel
              </button>
            )}
            <button onClick={() => { setImportando(false); abrirNuevo() }} className='rounded-xl bg-hcg-navy p-3 text-sm font-semibold text-white'>{nuevoTexto}</button>
          </div>
        )}
      </div>

      {mensaje && <p className={`mb-4 text-sm ${mensaje.ok ? 'text-green-700' : 'text-red-600'}`}>{mensaje.texto}</p>}

      {importando && <ImportarProductos onCerrar={() => setImportando(false)} onTerminado={refrescar} />}

      {form && (
        <Tarjeta titulo={form.id ? 'Editar registro' : nuevoTexto.replace('+ ', '')} className='mb-6'>
          <form onSubmit={guardar} className='flex flex-col gap-4'>
            <div className='grid grid-cols-1 md:grid-cols-3 gap-3'>
              {campos.map((c) => (
                <label key={c.k} className='flex flex-col gap-1 text-xs text-gray-600'>
                  {c.label}{c.req ? ' *' : ''}
                  <input className={campo} type={c.tipo === 'number' ? 'number' : c.tipo ?? 'text'}
                    step={c.tipo === 'number' ? c.paso ?? '1' : undefined} min={c.tipo === 'number' ? 0 : undefined}
                    value={form[c.k]} required={c.req}
                    onChange={(e) => setForm({ ...form, [c.k]: e.target.value })} />
                </label>
              ))}
            </div>
            <div className='flex gap-3'>
              <button disabled={guardando} className='rounded-xl bg-hcg-navy px-4 py-2 text-sm font-semibold text-white disabled:opacity-50'>
                {guardando ? 'Guardando...' : 'Guardar'}
              </button>
              <button type='button' onClick={() => setForm(null)} className='rounded-xl border px-4 py-2 text-sm text-gray-600'>Cancelar</button>
            </div>
          </form>
        </Tarjeta>
      )}

      <Tarjeta>
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder='Buscar...'
          className='mb-4 w-full rounded-lg border border-gray-200 p-2 text-sm md:w-80' />
        {filas.length === 0 && <p className='text-sm text-gray-400'>No hay registros.</p>}
        {filas.length > 0 && (
          <div className='overflow-x-auto'>
            <table className='w-full text-left text-sm text-gray-600'>
              <thead className='border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700'>
                <tr>
                  {[...columnas.map((c) => c.label), 'Estado', ''].map((h) => <th key={h} className='px-4 py-3 font-medium'>{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {filas.map((f) => (
                  <tr key={f.id} className='border-b border-gray-100 last:border-0'>
                    {columnas.map((c) => <td key={c.label} className='px-4 py-3'>{c.ver(f)}</td>)}
                    <td className='px-4 py-3'>
                      <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${f.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {f.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className='px-4 py-3 whitespace-nowrap'>
                      {puedeEditar && (
                        <span className='flex gap-3 text-xs font-medium'>
                          <button onClick={() => abrirEditar(f)} className='text-hcg-navy hover:underline'>Editar</button>
                          <button onClick={() => cambiarEstado(f)} className={f.activo ? 'text-red-600 hover:underline' : 'text-green-700 hover:underline'}>
                            {f.activo ? 'Desactivar' : 'Activar'}
                          </button>
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Tarjeta>
    </section>
  )
}
