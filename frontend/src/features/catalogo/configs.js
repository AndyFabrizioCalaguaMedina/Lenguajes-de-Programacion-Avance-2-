import { soles } from '../../services/formato'

export const PRODUCTOS = {
  titulo: 'Productos',
  subtitulo: 'Catálogo de productos y control de stock',
  api: '/api/products',
  permiso: 'PRODUCTOS',
  nuevoTexto: '+ Nuevo producto',
  importable: true,
  campos: [
    { k: 'codigo', label: 'Código (opcional, se genera solo)' },
    { k: 'nombre', label: 'Nombre', req: true },
    { k: 'categoria', label: 'Categoría' },
    { k: 'descripcion', label: 'Descripción' },
    { k: 'precioVenta', label: 'Precio de venta (con IGV)', tipo: 'number', paso: '0.01', req: true },
    { k: 'precioCompra', label: 'Precio de compra', tipo: 'number', paso: '0.01' },
    { k: 'stock', label: 'Stock', tipo: 'number' },
    { k: 'stockMinimo', label: 'Stock mínimo', tipo: 'number' },
    { k: 'unidadMedida', label: 'Unidad (ej. NIU)' },
  ],
  columnas: [
    { label: 'Código', ver: (f) => f.codigo },
    { label: 'Nombre', ver: (f) => f.nombre },
    { label: 'Categoría', ver: (f) => f.categoria ?? '—' },
    { label: 'Precio', ver: (f) => soles(f.precioVenta) },
    { label: 'Stock', ver: (f) => (f.stockBajo ? `${f.stock} ⚠ bajo` : f.stock ?? '—') },
  ],
}

export const PROVEEDORES = {
  titulo: 'Proveedores',
  subtitulo: 'Empresas a las que compras o envías productos',
  api: '/api/providers',
  permiso: 'PROVEEDORES',
  nuevoTexto: '+ Nuevo proveedor',
  campos: [
    { k: 'ruc', label: 'RUC', req: true },
    { k: 'razonSocial', label: 'Razón social', req: true },
    { k: 'nombreComercial', label: 'Nombre comercial' },
    { k: 'direccion', label: 'Dirección' },
    { k: 'telefono1', label: 'Teléfono 1' },
    { k: 'telefono2', label: 'Teléfono 2' },
    { k: 'correo', label: 'Correo', tipo: 'email' },
    { k: 'contacto', label: 'Persona de contacto' },
  ],
  columnas: [
    { label: 'RUC', ver: (f) => f.ruc },
    { label: 'Razón social', ver: (f) => f.razonSocial },
    { label: 'Teléfono', ver: (f) => f.telefono1 ?? '—' },
    { label: 'Correo', ver: (f) => f.correo ?? '—' },
  ],
}
