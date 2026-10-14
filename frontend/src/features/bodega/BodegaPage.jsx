import { useEffect, useRef, useState } from "react";
import { Trash2 } from "lucide-react";
import { listarClientes } from "../clientes/clientesApi";
import {
  anularSalida, buscarProducto, listarProveedores, listarSalidas, registrarSalida,
} from "./bodegaApi";

const campo = "text-sm text-gray-700 p-2 rounded-lg border border-gray-200";
const MOTIVOS = ["ENVIO", "DEVOLUCION", "VENTA", "MERMA", "OTRO"];

export default function BodegaPage() {
  const [destinoTipo, setDestinoTipo] = useState("PROVEEDOR");
  const [destinoId, setDestinoId] = useState("");
  const [destinoNombre, setDestinoNombre] = useState("");
  const [motivo, setMotivo] = useState("ENVIO");
  const [proveedores, setProveedores] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [items, setItems] = useState([]); // { codigo, descripcion, cantidad }
  const [codigo, setCodigo] = useState("");
  const [mensaje, setMensaje] = useState(null);
  const [salidas, setSalidas] = useState([]);
  const [recarga, setRecarga] = useState(0);
  const inputRef = useRef(null);

  useEffect(() => {
    listarProveedores().then((d) => setProveedores(d.content)).catch(() => {});
    listarClientes({ activo: "true" }).then((d) => setClientes(d.content)).catch(() => {});
  }, []);

  useEffect(() => {
    listarSalidas().then((d) => setSalidas(d.content)).catch(() => {});
  }, [recarga]);

  // El lector de códigos (o la cámara del celular con un lector) escribe el código y envía Enter
  async function agregarCodigo(e) {
    e.preventDefault();
    const cod = codigo.trim();
    if (!cod) return;
    setMensaje(null);
    const existente = items.findIndex((i) => i.codigo === cod);
    if (existente >= 0) {
      setItems((p) => p.map((it, i) => (i === existente ? { ...it, cantidad: it.cantidad + 1 } : it)));
    } else {
      try {
        const r = await buscarProducto(cod);
        const prod = r.content.find((p) => p.codigo === cod);
        if (!prod) throw new Error(`No existe el producto con código ${cod}`);
        setItems((p) => [...p, { codigo: prod.codigo, descripcion: prod.nombre ?? prod.descripcion, cantidad: 1 }]);
      } catch (err) {
        setMensaje({ ok: false, texto: err.message });
      }
    }
    setCodigo("");
    inputRef.current?.focus();
  }

  async function guardar() {
    setMensaje(null);
    const body = {
      destinoTipo,
      motivo,
      items: items.map((i) => ({ codigo: i.codigo, cantidad: Number(i.cantidad) })),
    };
    if (destinoTipo === "PROVEEDOR") body.proveedorId = Number(destinoId);
    if (destinoTipo === "CLIENTE") body.clienteId = Number(destinoId);
    if (destinoTipo === "OTRO") body.destinoNombre = destinoNombre.trim();
    try {
      const r = await registrarSalida(body);
      setMensaje({ ok: true, texto: `Salida ${r.numero} registrada. El stock se descontó.` });
      setItems([]);
      setRecarga((n) => n + 1);
    } catch (err) {
      const det = err.errors ? " " + JSON.stringify(err.errors) : "";
      setMensaje({ ok: false, texto: err.message + det });
    }
  }

  async function anular(s) {
    if (!confirm(`¿Anular la salida ${s.numero}? Se devolverá el stock.`)) return;
    try {
      await anularSalida(s.id);
      setRecarga((n) => n + 1);
    } catch (err) {
      alert(err.message);
    }
  }

  const listo = items.length > 0 && (destinoTipo === "OTRO" ? destinoNombre.trim() : destinoId);

  return (
    <section>
      <div className="pb-8">
        <h1 className="text-slate-800 text-2xl font-bold">Salidas de bodega</h1>
        <p className="text-slate-500 text-sm">Escanee o escriba el código de cada producto que sale de la bodega</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        <div className="lg:col-span-2 bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex flex-col gap-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <select value={destinoTipo} className={campo}
              onChange={(e) => { setDestinoTipo(e.target.value); setDestinoId(""); }}>
              <option value="PROVEEDOR">Proveedor</option>
              <option value="CLIENTE">Cliente</option>
              <option value="OTRO">Otro destino</option>
            </select>

            {destinoTipo === "OTRO" ? (
              <input value={destinoNombre} onChange={(e) => setDestinoNombre(e.target.value)}
                placeholder="Nombre del destino" className={campo} />
            ) : (
              <select value={destinoId} onChange={(e) => setDestinoId(e.target.value)} className={campo}>
                <option value="">Seleccionar...</option>
                {(destinoTipo === "PROVEEDOR" ? proveedores : clientes).map((x) => (
                  <option key={x.id} value={x.id}>{x.nombres || x.razonSocial || x.nombre}</option>
                ))}
              </select>
            )}

            <select value={motivo} onChange={(e) => setMotivo(e.target.value)} className={campo}>
              {MOTIVOS.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>

          <form onSubmit={agregarCodigo}>
            <input ref={inputRef} autoFocus value={codigo} onChange={(e) => setCodigo(e.target.value)}
              placeholder="Código del producto (ej. PRD-000001) y Enter"
              className={`${campo} w-full text-base`} />
          </form>

          {items.length === 0 ? (
            <p className="text-sm text-gray-400">Aún no hay productos en esta salida.</p>
          ) : (
            <ul className="divide-y divide-gray-100">
              {items.map((it, idx) => (
                <li key={it.codigo} className="flex items-center gap-3 py-2 text-sm">
                  <span className="font-mono text-gray-500">{it.codigo}</span>
                  <span className="flex-1 text-gray-800">{it.descripcion}</span>
                  <input type="number" min="1" value={it.cantidad}
                    onChange={(e) => setItems((p) => p.map((x, i) => (i === idx ? { ...x, cantidad: e.target.value } : x)))}
                    className={`${campo} w-20 text-center`} />
                  <Trash2 size={16} className="cursor-pointer text-red-500"
                    onClick={() => setItems((p) => p.filter((_, i) => i !== idx))} />
                </li>
              ))}
            </ul>
          )}

          {mensaje && (
            <p className={`text-sm ${mensaje.ok ? "text-green-700" : "text-red-600"}`}>{mensaje.texto}</p>
          )}

          <button disabled={!listo} onClick={guardar}
            className="text-white text-sm font-semibold bg-hcg-navy rounded-xl p-3 disabled:opacity-40">
            Registrar salida
          </button>
        </div>

        <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm">
          <h2 className="text-base font-semibold text-slate-800 mb-3">Últimas salidas</h2>
          {salidas.length === 0 && <p className="text-sm text-gray-400">Sin salidas registradas.</p>}
          <ul className="flex flex-col gap-3">
            {salidas.map((s) => (
              <li key={s.id} className="text-sm border-b border-gray-100 pb-2">
                <div className="flex justify-between">
                  <span className="font-medium text-gray-800">{s.numero}</span>
                  <span className={s.estado === "ANULADA" ? "text-red-600" : "text-green-700"}>{s.estado}</span>
                </div>
                <p className="text-gray-500">{s.destinoNombre} · {s.motivo}</p>
                {s.estado !== "ANULADA" && (
                  <button onClick={() => anular(s)} className="text-xs text-red-600 hover:underline">Anular</button>
                )}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
