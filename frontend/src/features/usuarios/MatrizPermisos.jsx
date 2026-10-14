// Cuadrícula de permisos: una fila por módulo y una columna por acción.
const ACCIONES = ['VER', 'CREAR', 'EDITAR', 'ANULAR']
const MODULOS = {
  DASHBOARD: 'Dashboard', CLIENTES: 'Clientes', PROVEEDORES: 'Proveedores', PRODUCTOS: 'Productos',
  COTIZACIONES: 'Cotizaciones', COMPROBANTES: 'Boletas / Comprobantes', DOCUMENTOS: 'Documentos',
  BODEGA: 'Salidas de bodega', REPORTES: 'Reportes', USUARIOS: 'Usuarios', AUDITORIA: 'Historial de actividad',
  OPERACIONES: 'Operaciones', PEDIDOS: 'Pedidos', COMPRAS: 'Compras', VENTAS: 'Ventas',
}

const PRESETS = {
  ninguno: () => [],
  consulta: (todos) => todos.filter((c) => c.endsWith('_VER') && !c.startsWith('USUARIOS') && !c.startsWith('AUDITORIA')),
  operador: (todos) => todos.filter((c) =>
    /^(CLIENTES|PRODUCTOS|COTIZACIONES|COMPROBANTES|DOCUMENTOS|BODEGA)_(VER|CREAR)$/.test(c)
    || ['DASHBOARD_VER', 'PROVEEDORES_VER'].includes(c)),
}

export default function MatrizPermisos({ permisos, seleccion, onChange }) {
  const existentes = new Set(permisos.map((p) => p.codigo))
  const modulos = [...new Set(permisos.map((p) => p.modulo))]
  const todos = permisos.map((p) => p.codigo)
  const activo = new Set(seleccion)

  const alternar = (codigo) => {
    const s = new Set(activo)
    if (s.has(codigo)) s.delete(codigo); else s.add(codigo)
    onChange([...s])
  }

  return (
    <div>
      <div className='mb-3 flex flex-wrap gap-2 text-xs'>
        <span className='text-gray-500'>Plantillas:</span>
        {[['consulta', 'Solo consulta'], ['operador', 'Operador (ver y crear)'], ['ninguno', 'Ninguno']].map(([k, t]) => (
          <button key={k} type='button' onClick={() => onChange(PRESETS[k](todos))}
            className='rounded-full border border-gray-300 px-3 py-1 text-gray-700 hover:bg-gray-50'>{t}</button>
        ))}
      </div>
      <div className='overflow-x-auto'>
        <table className='w-full text-sm'>
          <thead>
            <tr className='text-left text-xs uppercase text-gray-500'>
              <th className='py-2 pr-4'>Módulo</th>
              {ACCIONES.map((a) => <th key={a} className='px-3 py-2 text-center'>{a}</th>)}
            </tr>
          </thead>
          <tbody>
            {modulos.map((m) => (
              <tr key={m} className='border-t border-gray-100'>
                <td className='py-2 pr-4 text-gray-800'>{MODULOS[m] ?? m}</td>
                {ACCIONES.map((a) => {
                  const codigo = `${m}_${a}`
                  return (
                    <td key={a} className='px-3 py-2 text-center'>
                      {existentes.has(codigo)
                        ? <input type='checkbox' checked={activo.has(codigo)} onChange={() => alternar(codigo)} />
                        : <span className='text-gray-300'>·</span>}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
