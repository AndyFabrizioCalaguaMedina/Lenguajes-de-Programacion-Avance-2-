import { useRef, useState } from 'react'
import { Download, FileSpreadsheet, Upload, X } from 'lucide-react'
import { readSheet } from 'read-excel-file/browser'
import writeExcelFile from 'write-excel-file/browser'
import Tarjeta from '../../components/Tarjeta'
import { request } from '../../services/api'
import { soles } from '../../services/formato'
import { COLUMNAS_EXCEL, plantillaFilas, filasDesdeHoja } from './importarExcel'

const MAX_POR_ENVIO = 500

// Panel para cargar productos masivamente: descargar plantilla, elegir .xlsx, revisar y confirmar.
export default function ImportarProductos({ onCerrar, onTerminado }) {
  const inputRef = useRef(null)
  const [archivo, setArchivo] = useState('')
  const [filas, setFilas] = useState([])
  const [errores, setErrores] = useState([])
  const [actualizar, setActualizar] = useState(false)
  const [leyendo, setLeyendo] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [aviso, setAviso] = useState(null)
  const [resultado, setResultado] = useState(null)

  async function descargarPlantilla() {
    const encabezado = COLUMNAS_EXCEL.map((c) => ({ value: c.titulo, fontWeight: 'bold', backgroundColor: '#0f2b4f', color: '#ffffff' }))
    const ejemplo = plantillaFilas.map((fila) => fila.map((v) => (typeof v === 'number' ? { value: v, type: Number } : { value: v, type: String })))
    await writeExcelFile([encabezado, ...ejemplo], { columns: COLUMNAS_EXCEL.map((c) => ({ width: c.ancho })) })
      .toFile('plantilla-productos-hcg.xlsx')
  }

  async function elegirArchivo(e) {
    const f = e.target.files?.[0]
    e.target.value = ''
    if (!f) return
    setAviso(null)
    setResultado(null)
    setFilas([])
    setErrores([])
    setArchivo(f.name)
    if (!/\.xlsx$/i.test(f.name)) {
      setAviso('El archivo debe ser Excel moderno (.xlsx). Si es .xls o .csv, ábrelo en Excel y guárdalo como .xlsx.')
      return
    }
    setLeyendo(true)
    try {
      const hoja = await readSheet(f)
      const r = filasDesdeHoja(hoja)
      if (r.error) setAviso(r.error)
      setFilas(r.filas)
      setErrores(r.errores)
    } catch {
      setAviso('No se pudo leer el archivo. Verifica que sea un .xlsx válido y no esté protegido con contraseña.')
    } finally {
      setLeyendo(false)
    }
  }

  async function importar() {
    setEnviando(true)
    setAviso(null)
    const total = { total: 0, creados: 0, actualizados: 0, errores: [] }
    try {
      for (let i = 0; i < filas.length; i += MAX_POR_ENVIO) {
        const r = await request('/api/products/import', {
          method: 'POST',
          body: JSON.stringify({ filas: filas.slice(i, i + MAX_POR_ENVIO), actualizarExistentes: actualizar }),
        })
        total.total += r.total
        total.creados += r.creados
        total.actualizados += r.actualizados
        total.errores.push(...r.errores)
      }
      setResultado(total)
      setFilas([])
      onTerminado?.()
    } catch (err) {
      // Si algo falló a mitad de camino, lo ya guardado se informa igual
      if (total.total > 0) setResultado(total)
      setAviso(err.message)
    } finally {
      setEnviando(false)
    }
  }

  const hayFilas = filas.length > 0

  return (
    <Tarjeta className="mb-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="flex items-center gap-2 text-base font-semibold text-slate-800">
            <FileSpreadsheet size={18} className="text-hcg-gold" /> Importar productos desde Excel
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Descarga la plantilla, completa una fila por producto y súbela. Solo son obligatorios el nombre y el precio de venta.
          </p>
        </div>
        <button onClick={onCerrar} aria-label="Cerrar" className="text-gray-500 hover:text-gray-800"><X size={18} /></button>
      </div>

      <div className="mt-4 flex flex-wrap gap-3">
        <button type="button" onClick={descargarPlantilla}
          className="flex items-center gap-2 rounded-xl border border-hcg-navy px-4 py-2 text-sm font-semibold text-hcg-navy hover:bg-hcg-navy/5">
          <Download size={16} /> Descargar plantilla
        </button>
        <button type="button" onClick={() => inputRef.current?.click()} disabled={leyendo || enviando}
          className="flex items-center gap-2 rounded-xl bg-hcg-navy px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
          <Upload size={16} /> {leyendo ? 'Leyendo...' : 'Elegir archivo .xlsx'}
        </button>
        <input ref={inputRef} type="file" accept=".xlsx" className="hidden" onChange={elegirArchivo} />
        {archivo && <span className="self-center text-sm text-gray-600">{archivo}</span>}
      </div>

      {aviso && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{aviso}</p>}

      {errores.length > 0 && (
        <div className="mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
          <p className="font-semibold">{errores.length} fila(s) con problemas que no se enviarán:</p>
          <ul className="mt-1 max-h-32 list-disc overflow-y-auto pl-5">
            {errores.map((e) => <li key={e.fila}>Fila {e.fila}: {e.mensaje}</li>)}
          </ul>
        </div>
      )}

      {hayFilas && (
        <div className="mt-4">
          <p className="mb-2 text-sm font-semibold text-slate-800">
            {filas.length} producto(s) listos para importar
            {filas.length > 8 && <span className="font-normal text-gray-500"> (se muestran los primeros 8)</span>}
          </p>
          <div className="overflow-x-auto rounded-lg border border-gray-200">
            <table className="w-full text-left text-sm text-gray-600">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs uppercase text-gray-700">
                <tr>{['Fila', 'Código', 'Nombre', 'Categoría', 'Precio venta', 'Stock'].map((h) => <th key={h} className="px-4 py-2 font-medium">{h}</th>)}</tr>
              </thead>
              <tbody>
                {filas.slice(0, 8).map((f) => (
                  <tr key={f.fila} className="border-b border-gray-100 last:border-0">
                    <td className="px-4 py-2">{f.fila}</td>
                    <td className="px-4 py-2">{f.producto.codigo ?? 'Automático'}</td>
                    <td className="px-4 py-2">{f.producto.nombre}</td>
                    <td className="px-4 py-2">{f.producto.categoria ?? '—'}</td>
                    <td className="px-4 py-2">{soles(f.producto.precioVenta)}</td>
                    <td className="px-4 py-2">{f.producto.stock ?? 0}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <label className="mt-4 flex items-start gap-2 text-sm text-gray-700">
            <input type="checkbox" checked={actualizar} onChange={(e) => setActualizar(e.target.checked)} className="mt-0.5" />
            <span>
              Si el código ya existe, <strong>actualizar</strong> ese producto (precios, stock, etc.). Si no marcas esto, esas filas se omiten y se informan.
            </span>
          </label>

          <button type="button" onClick={importar} disabled={enviando}
            className="mt-4 rounded-xl bg-hcg-gold px-5 py-2 text-sm font-semibold text-hcg-navy disabled:opacity-50">
            {enviando ? 'Importando...' : `Importar ${filas.length} producto(s)`}
          </button>
        </div>
      )}

      {resultado && (
        <div className="mt-4 rounded-lg border border-green-200 bg-green-50 p-3 text-sm text-green-800">
          <p className="font-semibold">
            Importación terminada: {resultado.creados} creado(s), {resultado.actualizados} actualizado(s)
            {resultado.errores.length > 0 && `, ${resultado.errores.length} con error`}.
          </p>
          {resultado.errores.length > 0 && (
            <ul className="mt-2 max-h-40 list-disc overflow-y-auto pl-5 text-red-700">
              {resultado.errores.map((e, i) => <li key={i}>Fila {e.fila}{e.codigo ? ` (${e.codigo})` : ''}: {e.mensaje}</li>)}
            </ul>
          )}
        </div>
      )}
    </Tarjeta>
  )
}
