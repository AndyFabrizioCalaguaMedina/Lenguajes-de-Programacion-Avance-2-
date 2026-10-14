// Lectura flexible del Excel de productos: reconoce los encabezados sin importar tildes, mayúsculas ni el orden de las columnas.

export const COLUMNAS_EXCEL = [
  { titulo: 'Código', clave: 'codigo', ancho: 14, alias: ['codigo', 'cod', 'sku'] },
  { titulo: 'Nombre', clave: 'nombre', ancho: 34, alias: ['nombre', 'producto', 'descripcion del producto'] },
  { titulo: 'Categoría', clave: 'categoria', ancho: 18, alias: ['categoria', 'rubro', 'linea'] },
  { titulo: 'Descripción', clave: 'descripcion', ancho: 34, alias: ['descripcion', 'detalle'] },
  { titulo: 'Precio de venta (con IGV)', clave: 'precioVenta', ancho: 24, alias: ['precio de venta', 'precio venta', 'precio', 'pventa', 'precio de venta con igv'] },
  { titulo: 'Precio de compra', clave: 'precioCompra', ancho: 18, alias: ['precio de compra', 'precio compra', 'costo', 'pcompra'] },
  { titulo: 'Stock', clave: 'stock', ancho: 10, alias: ['stock', 'cantidad', 'existencias'] },
  { titulo: 'Stock mínimo', clave: 'stockMinimo', ancho: 14, alias: ['stock minimo', 'minimo', 'stock min'] },
  { titulo: 'Unidad', clave: 'unidadMedida', ancho: 10, alias: ['unidad', 'unidad de medida', 'um', 'und'] },
]

// Filas de ejemplo de la plantilla (en el mismo orden que COLUMNAS_EXCEL)
export const plantillaFilas = [
  ['PRD-001', 'Cable eléctrico 2.5mm (rollo 100m)', 'Electricidad', 'Cable THW 2.5mm color negro', 185.5, 140, 20, 5, 'NIU'],
  ['', 'Tubo PVC 1/2" x 3m', 'Gasfitería', '', 12.9, 8.5, 150, 30, 'NIU'],
]

const normalizar = (t) =>
  String(t ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9 ]/g, ' ').replace(/\s+/g, ' ').trim()

const vacio = (v) => v === null || v === undefined || String(v).trim() === ''

function aNumero(v) {
  if (typeof v === 'number') return v
  const t = String(v).trim().replace(/^s\/\.?\s*/i, '').replace(/\s/g, '')
  // "1.234,50" → 1234.50 ; "12,5" → 12.5
  const limpio = t.includes(',') && t.includes('.') ? t.replace(/\./g, '').replace(',', '.') : t.replace(',', '.')
  return Number(limpio)
}

const texto = (v) => (vacio(v) ? null : String(v).trim())

// hoja: array de filas (la primera es el encabezado). Devuelve { filas, errores, error? }
export function filasDesdeHoja(hoja) {
  if (!hoja || hoja.length < 2) {
    return { filas: [], errores: [], error: 'El archivo no tiene filas de datos. Usa la plantilla y completa al menos un producto.' }
  }
  const encabezado = hoja[0].map(normalizar)
  const indice = {}
  for (const col of COLUMNAS_EXCEL) {
    const i = encabezado.findIndex((h) => col.alias.includes(h))
    if (i >= 0) indice[col.clave] = i
  }
  if (indice.nombre === undefined || indice.precioVenta === undefined) {
    return {
      filas: [],
      errores: [],
      error: 'No se encontraron las columnas "Nombre" y "Precio de venta" en la primera fila. Descarga la plantilla para ver el formato.',
    }
  }

  const filas = []
  const errores = []
  hoja.slice(1).forEach((celdas, n) => {
    const fila = n + 2 // número de fila en Excel
    if (celdas.every(vacio)) return
    const dato = (clave) => (indice[clave] === undefined ? null : celdas[indice[clave]])
    const problemas = []

    const nombre = texto(dato('nombre'))
    if (!nombre) problemas.push('falta el nombre')

    const precioRaw = dato('precioVenta')
    let precioVenta = null
    if (vacio(precioRaw)) problemas.push('falta el precio de venta')
    else {
      precioVenta = aNumero(precioRaw)
      if (!Number.isFinite(precioVenta) || precioVenta < 0) problemas.push('el precio de venta no es un número válido')
    }

    const opcionalNum = (clave, etiqueta, entero) => {
      const v = dato(clave)
      if (vacio(v)) return null
      const n = aNumero(v)
      if (!Number.isFinite(n) || n < 0 || (entero && !Number.isInteger(n))) {
        problemas.push(`${etiqueta} no es válido`)
        return null
      }
      return n
    }
    const precioCompra = opcionalNum('precioCompra', 'el precio de compra', false)
    const stock = opcionalNum('stock', 'el stock', true)
    const stockMinimo = opcionalNum('stockMinimo', 'el stock mínimo', true)

    if (problemas.length) {
      errores.push({ fila, mensaje: problemas.join(', ') })
      return
    }
    filas.push({
      fila,
      producto: {
        codigo: texto(dato('codigo')),
        nombre,
        categoria: texto(dato('categoria')),
        descripcion: texto(dato('descripcion')),
        precioVenta: Math.round(precioVenta * 100) / 100,
        precioCompra: precioCompra === null ? null : Math.round(precioCompra * 100) / 100,
        stock,
        stockMinimo,
        unidadMedida: texto(dato('unidadMedida')),
      },
    })
  })

  if (filas.length === 0 && errores.length === 0) {
    return { filas, errores, error: 'El archivo no tiene filas con datos.' }
  }
  return { filas, errores }
}
