import { useEffect, useState } from "react";
import { Eye, Power, Trash2 } from "lucide-react";
import { cambiarEstadoCliente, eliminarCliente, listarClientes } from "../clientesApi";
import { tienePermiso } from "../../../services/sesion";
import Spinner from "../../../components/Spinner";
import DetalleCliente from "./DetalleCliente";

const TAM = 10;

export default function TablaClientes({ recarga, onCambio, onAviso }) {
  const [clientes, setClientes] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [total, setTotal] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [q, setQ] = useState("");
  const [activo, setActivo] = useState("");
  const [detalle, setDetalle] = useState(null);
  const [porEliminar, setPorEliminar] = useState(null);
  const [trabajando, setTrabajando] = useState(false);
  const puedeEditar = tienePermiso("CLIENTES_EDITAR");

  useEffect(() => {
    const t = setTimeout(() => {
      listarClientes({ q: q.trim(), activo, page: pagina, size: TAM })
        .then((data) => {
          setClientes(data.content);
          setTotalPaginas(data.totalPages);
          setTotal(data.totalElements);
          setError("");
        })
        .catch((e) => setError(e.message))
        .finally(() => setCargando(false));
    }, 250);
    return () => clearTimeout(t);
  }, [recarga, q, activo, pagina]);

  async function alternarEstado(c) {
    try {
      await cambiarEstadoCliente(c.id, !c.activo);
      onAviso?.(c.activo ? "Cliente desactivado" : "Cliente activado");
      onCambio?.();
    } catch (e) {
      onAviso?.(e.message, false);
    }
  }

  async function confirmarEliminar() {
    setTrabajando(true);
    try {
      await eliminarCliente(porEliminar.id);
      onAviso?.("Cliente eliminado");
      setPorEliminar(null);
      if (clientes.length === 1 && pagina > 0) setPagina(pagina - 1);
      onCambio?.();
    } catch (e) {
      onAviso?.(e.message, false);
      setPorEliminar(null);
    } finally {
      setTrabajando(false);
    }
  }

  return (
    <section className="rounded-lg border border-gray-200 bg-white shadow-sm">
      <div className="flex flex-wrap gap-3 border-b border-gray-100 p-3">
        <input
          value={q}
          onChange={(e) => { setQ(e.target.value); setPagina(0); }}
          placeholder="Buscar por nombre, RUC, DNI o código..."
          className="min-w-52 flex-1 rounded-lg border border-gray-200 p-2 text-sm"
        />
        <select
          value={activo}
          onChange={(e) => { setActivo(e.target.value); setPagina(0); }}
          className="rounded-lg border border-gray-200 p-2 text-sm"
        >
          <option value="">Todos</option>
          <option value="true">Activos</option>
          <option value="false">Inactivos</option>
        </select>
      </div>

      {cargando && <div className="flex w-full items-center justify-center py-12"><Spinner /></div>}
      {error && <p className="p-4 text-sm text-red-600">{error}</p>}
      {!cargando && !error && clientes.length === 0 && (
        <p className="p-6 text-sm text-gray-500">No hay clientes que coincidan.</p>
      )}

      {!cargando && clientes.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-gray-600">
            <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700">
              <tr>
                {["Código", "Nombre / Razón social", "RUC / DNI", "Teléfono", "Estado", "Acciones"].map((h) => (
                  <th key={h} scope="col" className={`px-6 py-3 font-medium ${h === "Acciones" ? "text-center" : ""}`}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id} className={`border-b border-gray-100 last:border-0 hover:bg-gray-50 ${c.activo ? "" : "opacity-60"}`}>
                  <td className="px-6 py-4 whitespace-nowrap">{c.codigo}</td>
                  <td className="px-6 py-4 font-medium text-gray-900">{c.nombre}</td>
                  <td className="px-6 py-4">{c.ruc || c.dni}</td>
                  <td className="px-6 py-4">{c.telefono || "—"}</td>
                  <td className="px-6 py-4">
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${c.activo ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                      {c.activo ? "Activo" : "Inactivo"}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-center gap-3">
                      <button title="Ver detalle" aria-label="Ver detalle" onClick={() => setDetalle(c)}
                        className="text-hcg-navy hover:text-hcg-navy/60"><Eye size={17} /></button>
                      {puedeEditar && (
                        <>
                          <button title={c.activo ? "Desactivar" : "Activar"} aria-label={c.activo ? "Desactivar" : "Activar"}
                            onClick={() => alternarEstado(c)}
                            className={c.activo ? "text-amber-600 hover:text-amber-500" : "text-green-600 hover:text-green-500"}>
                            <Power size={17} />
                          </button>
                          <button title="Eliminar" aria-label="Eliminar" onClick={() => setPorEliminar(c)}
                            className="text-red-500 hover:text-red-400"><Trash2 size={17} /></button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {total > 0 && (
        <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3 text-sm text-gray-600">
          <span>{total} cliente(s)</span>
          <div className="flex items-center gap-2">
            <button disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}
              className="rounded-lg border px-3 py-1 disabled:opacity-40">Anterior</button>
            <span>Página {pagina + 1} de {Math.max(totalPaginas, 1)}</span>
            <button disabled={pagina + 1 >= totalPaginas} onClick={() => setPagina(pagina + 1)}
              className="rounded-lg border px-3 py-1 disabled:opacity-40">Siguiente</button>
          </div>
        </div>
      )}

      {detalle && <DetalleCliente cliente={detalle} onCerrar={() => setDetalle(null)} />}

      {porEliminar && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
          <div role="alertdialog" aria-modal="true" className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
            <h2 className="text-lg font-bold text-slate-800">¿Eliminar cliente?</h2>
            <p className="mt-2 text-sm text-gray-600">
              Se eliminará <strong>{porEliminar.nombre}</strong>. Si ya tiene cotizaciones, operaciones o salidas
              no se podrá borrar; en ese caso desactívalo para que deje de aparecer en los formularios.
            </p>
            <div className="mt-5 flex justify-end gap-3">
              <button onClick={() => setPorEliminar(null)} disabled={trabajando}
                className="rounded-xl border px-4 py-2 text-sm text-gray-700">Cancelar</button>
              <button onClick={confirmarEliminar} disabled={trabajando}
                className="rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
                {trabajando ? "Eliminando..." : "Sí, eliminar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
