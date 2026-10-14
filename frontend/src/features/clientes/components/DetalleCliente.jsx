import { X } from 'lucide-react'

const Dato = ({ label, valor }) => (
    <div>
        <dt className="text-xs uppercase tracking-wide text-gray-500">{label}</dt>
        <dd className="mt-0.5 text-sm text-gray-900 break-words">{valor || '—'}</dd>
    </div>
)

// Ventana con todos los datos del cliente
export default function DetalleCliente({ cliente: c, onCerrar }) {
    const esRuc = c.tipoDocumento === 'RUC'
    return (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4" onClick={onCerrar}>
            <div role="dialog" aria-modal="true" aria-label="Detalle del cliente"
                className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <p className="text-xs font-semibold text-hcg-gold">{c.codigo}</p>
                        <h2 className="text-lg font-bold text-slate-800">{c.nombre}</h2>
                        <span className={`mt-1 inline-block rounded-full px-2.5 py-1 text-xs font-medium ${c.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                            {c.activo ? 'Activo' : 'Inactivo'}
                        </span>
                    </div>
                    <button onClick={onCerrar} aria-label="Cerrar"><X size={18} /></button>
                </div>
                <dl className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <Dato label="Tipo de documento" valor={c.tipoDocumento} />
                    <Dato label={esRuc ? 'RUC' : 'DNI'} valor={esRuc ? c.ruc : c.dni} />
                    {esRuc ? (
                        <>
                            <Dato label="Razón social" valor={c.razonSocial} />
                            <Dato label="Nombre comercial" valor={c.nombreComercial} />
                        </>
                    ) : (
                        <>
                            <Dato label="Nombres" valor={c.nombres} />
                            <Dato label="Apellidos" valor={c.apellidos} />
                        </>
                    )}
                    <Dato label="Teléfono" valor={c.telefono} />
                    <Dato label="Correo" valor={c.correo} />
                    <div className="sm:col-span-2"><Dato label="Dirección" valor={c.direccion} /></div>
                    <Dato label="Registrado" valor={c.fechaRegistro ? new Date(c.fechaRegistro).toLocaleDateString('es-PE') : null} />
                </dl>
            </div>
        </div>
    )
}
