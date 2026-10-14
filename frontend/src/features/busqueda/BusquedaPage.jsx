import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Search } from 'lucide-react'
import Tarjeta from '../../components/Tarjeta'
import { request } from '../../services/api'
import { fecha, soles } from '../../services/formato'

const SECCIONES = [
  { clave: 'clientes', titulo: 'Clientes', ruta: '/clientes',
    fila: (c) => [c.codigo, c.nombres || c.razonSocial || c.nombre, c.ruc || c.dni] },
  { clave: 'proveedores', titulo: 'Proveedores', ruta: null,
    fila: (p) => [p.ruc, p.razonSocial || p.nombre, p.nombreComercial] },
  { clave: 'productos', titulo: 'Productos', ruta: null,
    fila: (p) => [p.codigo, p.nombre, p.categoria] },
  { clave: 'cotizaciones', titulo: 'Cotizaciones', ruta: '/cotizaciones',
    fila: (c) => [c.numero, c.clienteNombre, `${soles(c.total)} · ${c.estado}`] },
  { clave: 'documentos', titulo: 'Documentos', ruta: '/documentos',
    fila: (d) => [d.tipo, d.numero, `${d.contraparteNombre ?? '—'} · ${fecha(d.fecha + 'T00:00:00')}`] },
  { clave: 'boletas', titulo: 'Boletas', ruta: '/boletas',
    fila: (b) => [`${b.serie}-${b.correlativo}`, b.clienteNombre, `${soles(b.mtoImpVenta)} · ${b.estado}`] },
]

export default function BusquedaPage() {
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const [r, setR] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const texto = q.trim()
    const t = setTimeout(() => {
      setParams(texto ? { q: texto } : {}, { replace: true })
      if (texto.length < 2) { setR(null); return }
      request(`/api/search?q=${encodeURIComponent(texto)}`)
        .then((d) => { setR(d); setError('') })
        .catch((e) => setError(e.message))
    }, 300)
    return () => clearTimeout(t)
  }, [q, setParams])

  const total = r ? SECCIONES.reduce((n, s) => n + (r[s.clave]?.length ?? 0), 0) : 0

  return (
    <section>
      <div className='pb-6'>
        <h1 className='text-slate-800 text-2xl font-bold'>Búsqueda</h1>
        <p className='text-slate-500 text-sm'>Busca en clientes, productos, cotizaciones, documentos y boletas a la vez</p>
      </div>

      <div className='relative mb-6'>
        <Search size={18} className='absolute left-3 top-3 text-gray-400' />
        <input autoFocus value={q} onChange={(e) => setQ(e.target.value)}
          placeholder='Nombre, RUC/DNI, número de documento, código...'
          className='w-full rounded-xl border border-gray-200 bg-white p-3 pl-10 text-sm' />
      </div>

      {error && <p className='text-sm text-red-600'>{error}</p>}
      {!r && !error && <p className='text-sm text-gray-400'>Escribe al menos 2 letras.</p>}
      {r && total === 0 && <p className='text-sm text-gray-500'>Sin resultados para «{r.q}».</p>}

      <div className='grid grid-cols-1 lg:grid-cols-2 gap-6'>
        {r && SECCIONES.filter((s) => r[s.clave]?.length > 0).map((s) => (
          <Tarjeta key={s.clave} titulo={`${s.titulo} (${r[s.clave].length})`}>
            <ul className='divide-y divide-gray-100'>
              {r[s.clave].map((x, i) => {
                const [a, b, c] = s.fila(x)
                return (
                  <li key={i} className='py-2 text-sm'>
                    <span className='font-mono text-gray-500'>{a}</span>{' '}
                    <span className='text-gray-800'>{b}</span>
                    {c && <span className='block text-xs text-gray-400'>{c}</span>}
                  </li>
                )
              })}
            </ul>
            {s.ruta && <Link to={s.ruta} className='mt-3 inline-block text-sm text-hcg-navy hover:underline'>Ir a {s.titulo}</Link>}
          </Tarjeta>
        ))}
      </div>
    </section>
  )
}
