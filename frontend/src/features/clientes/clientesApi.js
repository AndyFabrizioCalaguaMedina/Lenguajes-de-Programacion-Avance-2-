import { request } from '../../services/api'

const RUTA = '/api/clients'

export const crearCliente = (datos) =>
    request(RUTA, { method: 'POST', body: JSON.stringify(datos) })

export const listarClientes = ({ q = '', activo = '', page = 0, size = 100 } = {}) => {
    const params = new URLSearchParams({ page, size })
    if (q) params.set('q', q)
    if (activo !== '') params.set('activo', activo)
    return request(`${RUTA}?${params}`)
}

export const cambiarEstadoCliente = (id, activo) =>
    request(`${RUTA}/${id}/status`, { method: 'PATCH', body: JSON.stringify({ activo }) })

export const eliminarCliente = (id) => request(`${RUTA}/${id}`, { method: 'DELETE' })
