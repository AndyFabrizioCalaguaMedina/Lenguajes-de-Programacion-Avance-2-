import { useCallback, useEffect, useState } from 'react'
import Tarjeta from '../../components/Tarjeta'
import MatrizPermisos from './MatrizPermisos'
import { request } from '../../services/api'
import { esAdmin } from '../../services/sesion'
import { fechaHora } from '../../services/formato'

const campo = 'w-full rounded-lg border border-gray-200 p-2 text-sm'
const vacio = { nombreCompleto: '', username: '', correo: '', password: '', rol: 'PERSONALIZADO' }
const post = (ruta, body) => request(ruta, { method: 'POST', body: JSON.stringify(body) })
const put = (ruta, body) => request(ruta, { method: 'PUT', body: JSON.stringify(body) })

export default function UsuariosPage() {
  const admin = esAdmin()
  const [usuarios, setUsuarios] = useState([])
  const [roles, setRoles] = useState([])
  const [permisos, setPermisos] = useState([])
  const [form, setForm] = useState(vacio)
  const [seleccion, setSeleccion] = useState([])
  const [mostrar, setMostrar] = useState(false)
  const [mensaje, setMensaje] = useState(null)
  const [editando, setEditando] = useState(null) // { rol, permisos, usuarios }
  const [cargando, setCargando] = useState(false)

  const cargar = useCallback(async () => {
    try {
      const [u, r, p] = await Promise.all([request('/api/users'), request('/api/roles'), request('/api/permissions')])
      setUsuarios(u); setRoles(r); setPermisos(p)
    } catch (e) { setMensaje({ ok: false, texto: e.message }) }
  }, [])

  useEffect(() => {
    Promise.all([request('/api/users'), request('/api/roles'), request('/api/permissions')])
      .then(([u, r, p]) => { setUsuarios(u); setRoles(r); setPermisos(p) })
      .catch((e) => setMensaje({ ok: false, texto: e.message }))
  }, [])

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })
  const rolesElegibles = roles.filter((r) => r.nombre !== 'ADMIN')

  async function crear(e) {
    e.preventDefault()
    setCargando(true); setMensaje(null)
    try {
      let nombreRol = form.rol
      if (form.rol === 'PERSONALIZADO') {
        nombreRol = 'EMP_' + form.username.toUpperCase().replace(/[^A-Z0-9]/g, '_')
        let rol = roles.find((r) => r.nombre === nombreRol)
        if (!rol) rol = await post('/api/roles', { nombre: nombreRol, descripcion: `Permisos de ${form.nombreCompleto}` })
        await put(`/api/roles/${rol.id}/permissions`, { permisos: seleccion })
      }
      await post('/api/users', {
        username: form.username.trim(), nombreCompleto: form.nombreCompleto.trim(),
        correo: form.correo.trim(), password: form.password, roles: [nombreRol],
      })
      setMensaje({ ok: true, texto: `Usuario ${form.username} creado.` })
      setForm(vacio); setSeleccion([]); setMostrar(false)
      cargar()
    } catch (err) {
      const det = err.errors ? ' ' + JSON.stringify(err.errors) : ''
      setMensaje({ ok: false, texto: err.message + det })
    } finally { setCargando(false) }
  }

  async function cambiarEstado(u) {
    try { await request(`/api/users/${u.id}/status`, { method: 'PATCH', body: JSON.stringify({ activo: !u.activo }) }); cargar() }
    catch (e) { setMensaje({ ok: false, texto: e.message }) }
  }

  async function restablecer(u) {
    const clave = prompt(`Nueva contraseña para ${u.username} (mínimo 8 caracteres):`)
    if (!clave) return
    try { await put(`/api/users/${u.id}/password`, { password: clave }); setMensaje({ ok: true, texto: `Contraseña de ${u.username} restablecida.` }) }
    catch (e) { setMensaje({ ok: false, texto: e.message }) }
  }

  function editarPermisos(u) {
    const rol = roles.find((r) => u.roles.includes(r.nombre) && r.nombre !== 'ADMIN')
    if (!rol) return setMensaje({ ok: false, texto: 'Este usuario es administrador: ya tiene todos los permisos.' })
    setEditando({ rol, permisos: [...rol.permisos], compartido: usuarios.filter((x) => x.roles.includes(rol.nombre)).length })
  }

  async function guardarPermisos() {
    try {
      await put(`/api/roles/${editando.rol.id}/permissions`, { permisos: editando.permisos })
      setMensaje({ ok: true, texto: `Permisos del rol ${editando.rol.nombre} actualizados.` })
      setEditando(null); cargar()
    } catch (e) { setMensaje({ ok: false, texto: e.message }) }
  }

  return (
    <section>
      <div className='pb-6 flex flex-wrap items-center justify-between gap-4'>
        <div>
          <h1 className='text-slate-800 text-2xl font-bold'>Usuarios y personal</h1>
          <p className='text-slate-500 text-sm'>Registra empleados y define qué puede hacer cada uno</p>
        </div>
        {admin && (
          <button onClick={() => setMostrar(!mostrar)} className='rounded-xl bg-hcg-navy p-3 text-sm font-semibold text-white'>
            {mostrar ? 'Cerrar' : '+ Registrar personal'}
          </button>
        )}
      </div>

      {mensaje && <p className={`mb-4 text-sm ${mensaje.ok ? 'text-green-700' : 'text-red-600'}`}>{mensaje.texto}</p>}

      {admin && mostrar && (
        <Tarjeta titulo='Nuevo usuario' className='mb-6'>
          <form onSubmit={crear} className='flex flex-col gap-4'>
            <div className='grid grid-cols-1 md:grid-cols-2 gap-3'>
              <input className={campo} placeholder='Nombre completo *' value={form.nombreCompleto} onChange={set('nombreCompleto')} required />
              <input className={campo} placeholder='Usuario * (letras, números, . _ -)' value={form.username} onChange={set('username')} required minLength={3} />
              <input className={campo} type='email' placeholder='Correo *' value={form.correo} onChange={set('correo')} required />
              <input className={campo} type='password' placeholder='Contraseña inicial * (mín. 8)' value={form.password} onChange={set('password')} required minLength={8} />
            </div>

            <div>
              <label className='mb-1 block text-xs text-gray-600'>Rol</label>
              <select className={campo} value={form.rol} onChange={set('rol')}>
                <option value='PERSONALIZADO'>Permisos personalizados (elegir abajo)</option>
                {rolesElegibles.map((r) => <option key={r.id} value={r.nombre}>{r.nombre} — {r.permisos.length} permisos</option>)}
                <option value='ADMIN'>ADMIN — acceso total</option>
              </select>
            </div>

            {form.rol === 'PERSONALIZADO' && (
              <MatrizPermisos permisos={permisos} seleccion={seleccion} onChange={setSeleccion} />
            )}

            <button disabled={cargando} className='self-start rounded-xl bg-hcg-navy px-4 py-2 text-sm font-semibold text-white disabled:opacity-50'>
              {cargando ? 'Guardando...' : 'Crear usuario'}
            </button>
          </form>
        </Tarjeta>
      )}

      {editando && (
        <Tarjeta titulo={`Permisos del rol ${editando.rol.nombre}`} className='mb-6'>
          {editando.compartido > 1 && (
            <p className='mb-3 text-xs text-amber-700'>Este rol lo usan {editando.compartido} usuarios: el cambio los afecta a todos.</p>
          )}
          <MatrizPermisos permisos={permisos} seleccion={editando.permisos}
            onChange={(p) => setEditando({ ...editando, permisos: p })} />
          <div className='mt-4 flex gap-3'>
            <button onClick={guardarPermisos} className='rounded-xl bg-hcg-navy px-4 py-2 text-sm font-semibold text-white'>Guardar permisos</button>
            <button onClick={() => setEditando(null)} className='rounded-xl border px-4 py-2 text-sm text-gray-600'>Cancelar</button>
          </div>
        </Tarjeta>
      )}

      <Tarjeta>
        <div className='overflow-x-auto'>
          <table className='w-full text-left text-sm text-gray-600'>
            <thead className='border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700'>
              <tr>{['Nombre', 'Usuario', 'Correo', 'Rol', 'Estado', 'Último acceso', ''].map((h) => <th key={h} className='px-4 py-3 font-medium'>{h}</th>)}</tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id} className='border-b border-gray-100 last:border-0'>
                  <td className='px-4 py-3 font-medium text-gray-900'>{u.nombreCompleto}</td>
                  <td className='px-4 py-3'>{u.username}</td>
                  <td className='px-4 py-3'>{u.correo}</td>
                  <td className='px-4 py-3'>{[...u.roles].join(', ')}</td>
                  <td className='px-4 py-3'>
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${u.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                      {u.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td className='px-4 py-3 whitespace-nowrap'>{fechaHora(u.ultimoLogin)}</td>
                  <td className='px-4 py-3 whitespace-nowrap'>
                    {admin && (
                      <span className='flex gap-3 text-xs font-medium'>
                        <button onClick={() => editarPermisos(u)} className='text-hcg-navy hover:underline'>Permisos</button>
                        <button onClick={() => restablecer(u)} className='text-gray-600 hover:underline'>Clave</button>
                        <button onClick={() => cambiarEstado(u)} className={u.activo ? 'text-red-600 hover:underline' : 'text-green-700 hover:underline'}>
                          {u.activo ? 'Desactivar' : 'Activar'}
                        </button>
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Tarjeta>
    </section>
  )
}
