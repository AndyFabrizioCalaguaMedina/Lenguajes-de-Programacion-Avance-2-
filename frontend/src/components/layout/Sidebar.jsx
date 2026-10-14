import { NavLink, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Users, FileText, Folder, Receipt, Warehouse, Search, BarChart3,
  LogOut, UserCog, History, Package, Truck, Workflow,
} from 'lucide-react'
import { logout } from '../../services/api'
import { tienePermiso, usuarioActual } from '../../services/sesion'

const menu = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, permiso: 'DASHBOARD_VER' },
  { to: '/operaciones', label: 'Operaciones', icon: Workflow, permiso: 'OPERACIONES_VER' },
  { to: '/clientes', label: 'Clientes', icon: Users, permiso: 'CLIENTES_VER' },
  { to: '/proveedores', label: 'Proveedores', icon: Truck, permiso: 'PROVEEDORES_VER' },
  { to: '/productos', label: 'Productos', icon: Package, permiso: 'PRODUCTOS_VER' },
  { to: '/cotizaciones', label: 'Cotizaciones', icon: FileText, permiso: 'COTIZACIONES_VER' },
  { to: '/documentos', label: 'Documentos', icon: Folder, permiso: 'DOCUMENTOS_VER' },
  { to: '/boletas', label: 'Boletas / Comprobantes', icon: Receipt, permiso: 'COMPROBANTES_VER' },
  { to: '/bodega', label: 'Salidas de bodega', icon: Warehouse, permiso: 'BODEGA_VER' },
  { to: '/busqueda', label: 'Búsqueda', icon: Search },
  { to: '/reportes', label: 'Reportes', icon: BarChart3, permiso: 'REPORTES_VER' },
]

const sistema = [
  { to: '/usuarios', label: 'Usuarios y personal', icon: UserCog, permiso: 'USUARIOS_VER' },
  { to: '/historial', label: 'Historial de actividad', icon: History, permiso: 'AUDITORIA_VER' },
]

const visibles = (items) => items.filter((i) => !i.permiso || tienePermiso(i.permiso))

function Item({ to, label, icon: Icon }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 border-l-4 px-5 py-2.5 text-sm transition-colors ${
          isActive
            ? 'border-hcg-gold bg-white/10 font-semibold text-white'
            : 'border-transparent text-slate-300 hover:bg-white/5 hover:text-white'
        }`
      }
    >
      <Icon size={18} />
      {label}
    </NavLink>
  )
}

export default function Sidebar() {
  const navigate = useNavigate()
  const usuario = usuarioActual()
  const iniciales = (usuario?.nombreCompleto ?? '?').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()

  function handleLogout() {
    logout()
    navigate('/', { replace: true })
  }

  return (
    <aside className="flex h-screen w-72 shrink-0 flex-col bg-hcg-navy-dark text-white">
      <div className="border-b border-white/10 p-4">
        <div className="rounded-xl bg-white px-3 py-2">
          <img src="/logo-hcg.png" alt="HCG Multiservicios" className="mx-auto h-12 w-auto" />
        </div>
        <p className="mt-2 text-center text-xs tracking-widest text-hcg-gold">GESTIÓN DOCUMENTAL</p>
      </div>

      <nav className="flex-1 overflow-y-auto py-3">
        <p className="px-5 py-2 text-xs font-semibold tracking-widest text-slate-400">MENÚ PRINCIPAL</p>
        {visibles(menu).map((item) => <Item key={item.to} {...item} />)}

        {visibles(sistema).length > 0 && (
          <>
            <p className="mt-5 px-5 py-2 text-xs font-semibold tracking-widest text-slate-400">SISTEMA</p>
            {visibles(sistema).map((item) => <Item key={item.to} {...item} />)}
          </>
        )}
      </nav>

      <div className="flex items-center gap-3 border-t border-white/10 p-4">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-hcg-gold text-sm font-bold text-hcg-navy-dark">
          {iniciales}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold">{usuario?.nombreCompleto ?? 'Usuario'}</p>
          <p className="truncate text-xs text-slate-400">{usuario?.correo}</p>
        </div>
        <button onClick={handleLogout} title="Cerrar sesión" aria-label="Cerrar sesión"
          className="rounded-lg p-2 text-slate-400 hover:bg-white/10 hover:text-white">
          <LogOut size={18} />
        </button>
      </div>
    </aside>
  )
}
