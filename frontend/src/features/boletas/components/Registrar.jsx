import { useState } from "react";
import { X, Plus, Trash2 } from "lucide-react";
import { emitirBoleta } from "../boletasApi";

const campo = "text-sm text-gray-600 p-1 rounded-lg border border-gray-200";
const itemVacio = () => ({ descripcion: "", cantidad: 1, precioUnitario: "" });

export default function Registrar({ onCerrar, onEmitida }) {
  const [tipoComprobante, setTipoComprobante] = useState("03"); // 03 boleta, 01 factura
  const [tipoDoc, setTipoDoc] = useState("1");
  const [numDoc, setNumDoc] = useState("");
  const [nombre, setNombre] = useState("");
  const [items, setItems] = useState([itemVacio()]);
  const [observacion, setObservacion] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState(null);

  const esFactura = tipoComprobante === "01";
  const nombreComp = esFactura ? "factura" : "boleta";

  function elegirTipo(t) {
    setTipoComprobante(t);
    setMensaje(null);
    // La factura solo se emite a RUC
    if (t === "01") setTipoDoc("6");
    else if (tipoDoc === "6") setTipoDoc("1");
  }

  const total = items.reduce((t, i) => t + (Number(i.cantidad) || 0) * (Number(i.precioUnitario) || 0), 0);

  const cambiarItem = (idx, k, v) =>
    setItems((prev) => prev.map((it, i) => (i === idx ? { ...it, [k]: v } : it)));

  async function enviar(e) {
    e.preventDefault();
    setEnviando(true);
    setMensaje(null);
    try {
      const r = await emitirBoleta({
        tipoDoc: tipoComprobante,
        cliente: { tipoDoc, numDoc: numDoc.trim(), rznSocial: nombre.trim() },
        items: items.map((i, n) => ({
          codProducto: `ITEM-${n + 1}`,
          descripcion: i.descripcion.trim(),
          cantidad: Number(i.cantidad),
          precioUnitario: Number(i.precioUnitario),
        })),
        observacion: observacion.trim() || undefined,
      });
      setMensaje({ ok: true, texto: `${esFactura ? "Factura" : "Boleta"} ${r.serie}-${r.correlativo} aceptada por SUNAT.` });
      setItems([itemVacio()]);
      setObservacion("");
      onEmitida?.();
    } catch (err) {
      const detalle = err.errors?.length ? ` (${[].concat(err.errors).join(", ")})` : "";
      setMensaje({ ok: false, texto: err.message + detalle });
      onEmitida?.(); // si quedó guardada como rechazada/error, que aparezca en la tabla
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} className="p-4 border border-gray-200 rounded-lg bg-white flex flex-col gap-4">
      <div className="flex justify-between items-center">
        <h1 className="text-base font-semibold text-slate-800">Emitir {nombreComp}</h1>
        <X onClick={onCerrar} size={16} className="cursor-pointer" />
      </div>

      <div role="tablist" aria-label="Tipo de comprobante" className="grid grid-cols-2 gap-1 rounded-xl bg-gray-100 p-1">
        {[["03", "Boleta"], ["01", "Factura"]].map(([v, etiqueta]) => (
          <button key={v} type="button" role="tab" aria-selected={tipoComprobante === v} onClick={() => elegirTipo(v)}
            className={`rounded-lg py-1.5 text-sm font-semibold transition ${tipoComprobante === v ? "bg-hcg-navy text-white shadow" : "text-gray-600 hover:text-hcg-navy"}`}>
            {etiqueta}
          </button>
        ))}
      </div>
      {esFactura && (
        <p className="-mt-2 text-xs text-gray-500">La factura se emite solo a clientes con RUC (11 dígitos).</p>
      )}

      <div className="flex gap-2">
        <div className="flex flex-col gap-1 w-24">
          <label className="text-xs text-gray-900">Doc.</label>
          <select value={tipoDoc} onChange={(e) => setTipoDoc(e.target.value)} className={campo} disabled={esFactura}>
            {!esFactura && <option value="1">DNI</option>}
            <option value="6">RUC</option>
            {!esFactura && <option value="0">Sin doc.</option>}
          </select>
        </div>
        <div className="flex flex-col gap-1 flex-1">
          <label className="text-xs text-gray-900">Número</label>
          <input value={numDoc} onChange={(e) => setNumDoc(e.target.value)} className={campo}
            placeholder={tipoDoc === "6" ? "20123456789" : "12345678"} required={tipoDoc !== "0"}
            inputMode="numeric" maxLength={tipoDoc === "6" ? 11 : 8}
            pattern={esFactura ? "\\d{11}" : undefined} title={esFactura ? "El RUC debe tener 11 dígitos" : undefined} />
        </div>
      </div>

      <div className="flex flex-col gap-1">
        <label className="text-xs text-gray-900">Nombre / Razón social *</label>
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} className={campo} required />
      </div>

      <div className="flex flex-col gap-2">
        <label className="text-xs text-gray-900">Ítems (precio con IGV) *</label>
        {items.map((it, idx) => (
          <div key={idx} className="flex gap-1 items-center">
            <input value={it.descripcion} onChange={(e) => cambiarItem(idx, "descripcion", e.target.value)}
              className={`${campo} flex-1 min-w-0`} placeholder="Descripción" required />
            <input type="number" min="1" step="1" value={it.cantidad}
              onChange={(e) => cambiarItem(idx, "cantidad", e.target.value)}
              className={`${campo} w-14`} title="Cantidad" required />
            <input type="number" min="0" step="0.01" value={it.precioUnitario}
              onChange={(e) => cambiarItem(idx, "precioUnitario", e.target.value)}
              className={`${campo} w-20`} placeholder="S/" title="Precio unitario" required />
            {items.length > 1 && (
              <Trash2 size={14} className="cursor-pointer text-red-500"
                onClick={() => setItems((p) => p.filter((_, i) => i !== idx))} />
            )}
          </div>
        ))}
        <button type="button" onClick={() => setItems((p) => [...p, itemVacio()])}
          className="flex items-center gap-1 text-xs text-hcg-navy self-start">
          <Plus size={12} /> Agregar ítem
        </button>
      </div>

      <div className="flex flex-col gap-1">
        <label className="text-xs text-gray-900">Observación</label>
        <input value={observacion} onChange={(e) => setObservacion(e.target.value)} className={campo} />
      </div>

      <p className="text-sm font-semibold text-slate-800">Total: S/ {total.toFixed(2)}</p>

      {mensaje && (
        <p className={`text-xs ${mensaje.ok ? "text-green-700" : "text-red-600"}`}>{mensaje.texto}</p>
      )}

      <button disabled={enviando} type="submit"
        className="text-white text-sm font-semibold bg-hcg-navy rounded-xl p-2 disabled:opacity-50">
        {enviando ? "Emitiendo..." : `Emitir ${nombreComp}`}
      </button>
    </form>
  );
}
