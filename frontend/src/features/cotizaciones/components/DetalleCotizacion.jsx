import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { cambiarEstadoCotizacion, convertirEnBoleta, obtenerCotizacion } from "../cotizacionesApi";
import { tienePermiso } from "../../../services/sesion";
import { fecha, soles } from "../../../services/formato";

// Siguiente paso permitido por el backend: BORRADOR→ENVIADA→APROBADA|RECHAZADA; APROBADA→boleta
const PASOS = {
  BORRADOR: [{ estado: "ENVIADA", texto: "Marcar como enviada" }],
  ENVIADA: [{ estado: "APROBADA", texto: "Aprobar" }, { estado: "RECHAZADA", texto: "Rechazar", peligro: true }],
  APROBADA: [{ estado: "RECHAZADA", texto: "Rechazar", peligro: true }],
};

export default function DetalleCotizacion({ id, onCerrar, onCambio }) {
  const [c, setC] = useState(null);
  const [mensaje, setMensaje] = useState(null);
  const [trabajando, setTrabajando] = useState(false);

  useEffect(() => {
    obtenerCotizacion(id)
      .then(setC)
      .catch((e) => setMensaje({ ok: false, texto: e.message }));
  }, [id]);

  async function ejecutar(accion, textoOk) {
    setTrabajando(true);
    setMensaje(null);
    try {
      await accion();
      setMensaje({ ok: true, texto: textoOk });
      setC(await obtenerCotizacion(id));
    } catch (e) {
      setMensaje({ ok: false, texto: e.message });
    } finally {
      setTrabajando(false);
      onCambio?.();
    }
  }

  const puedeEditar = tienePermiso("COTIZACIONES_EDITAR");
  const puedeFacturar = puedeEditar && tienePermiso("COMPROBANTES_CREAR");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onCerrar}>
      <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}>
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-800">{c ? `Cotización ${c.numero}` : "Cotización"}</h2>
            {c && <p className="text-sm text-gray-500">{c.clienteNombre} · {fecha(c.fecha + "T00:00:00")} · {c.estado}</p>}
          </div>
          <X size={18} className="cursor-pointer text-gray-500" onClick={onCerrar} />
        </div>

        {!c && !mensaje && <p className="text-sm text-gray-500">Cargando...</p>}

        {c && (
          <>
            <table className="mb-4 w-full text-left text-sm text-gray-600">
              <thead className="border-b border-gray-200 text-xs uppercase text-gray-700">
                <tr>
                  <th className="py-2">Descripción</th><th className="py-2 text-right">Cant.</th>
                  <th className="py-2 text-right">P. unit.</th><th className="py-2 text-right">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {c.items.map((i) => (
                  <tr key={i.item} className="border-b border-gray-100">
                    <td className="py-2">{i.descripcion}</td>
                    <td className="py-2 text-right">{i.cantidad}</td>
                    <td className="py-2 text-right">{soles(i.precioUnitario)}</td>
                    <td className="py-2 text-right">{soles(i.subtotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="mb-4 ml-auto w-56 text-sm">
              <div className="flex justify-between text-gray-500"><span>Subtotal</span><span>{soles(c.subtotal)}</span></div>
              <div className="flex justify-between text-gray-500"><span>IGV (18%)</span><span>{soles(c.igv)}</span></div>
              <div className="mt-1 flex justify-between border-t pt-1 font-bold text-gray-900"><span>Total</span><span>{soles(c.total)}</span></div>
            </div>

            {c.observacion && <p className="mb-4 text-sm text-gray-600">Observación: {c.observacion}</p>}

            <div className="flex flex-wrap gap-3">
              {puedeEditar && (PASOS[c.estado] ?? []).map((p) => (
                <button key={p.estado} disabled={trabajando}
                  onClick={() => ejecutar(() => cambiarEstadoCotizacion(id, p.estado), `Estado: ${p.estado}.`)}
                  className={`rounded-xl px-4 py-2 text-sm font-semibold disabled:opacity-50 ${p.peligro ? "border border-red-300 text-red-600" : "bg-hcg-navy text-white"}`}>
                  {p.texto}
                </button>
              ))}
              {puedeFacturar && c.estado === "APROBADA" && (
                <button disabled={trabajando}
                  onClick={() => ejecutar(() => convertirEnBoleta(id), "Boleta emitida y aceptada por SUNAT.")}
                  className="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
                  Emitir boleta con esta cotización
                </button>
              )}
            </div>
          </>
        )}

        {mensaje && <p className={`mt-4 text-sm ${mensaje.ok ? "text-green-700" : "text-red-600"}`}>{mensaje.texto}</p>}
      </div>
    </div>
  );
}
