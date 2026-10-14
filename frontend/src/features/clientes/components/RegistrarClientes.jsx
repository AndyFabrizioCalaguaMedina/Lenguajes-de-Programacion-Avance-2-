import { X } from 'lucide-react'
import { useState } from 'react'
import { crearCliente } from '../clientesApi'

const inputClass = 'text-sm text-gray-600 p-1 rounded-lg border border-gray-200'

const formInicial = {
    ruc: '',
    razonSocial: '',
    nombreComercial: '',
    dni: '',
    nombres: '',
    apellidos: '',
    direccion: '',
    telefono: '',
    correo: '',
}

const soloNumeros = ['ruc', 'dni', 'telefono']

// Un campo = etiqueta + input + mensaje de error
function Campo({ id, label, error, ...inputProps }) {
    return (
        <div className="flex flex-col gap-2">
            <label htmlFor={id} className="text-xs text-gray-900">{label}</label>
            <input id={id} name={id} className={inputClass} {...inputProps} />
            {error && <p className="text-xs text-red-600">{error}</p>}
        </div>
    )
}

export default function RegistrarClientes({ onCerrar, onGuardado }) {

    const [tipoDocumento, setTipoDocumento] = useState('RUC')
    const [form, setForm] = useState(formInicial)
    const [errores, setErrores] = useState({})
    const [errorGeneral, setErrorGeneral] = useState('')
    const [guardando, setGuardando] = useState(false)

    function handleChange(e) {
        const { name, value } = e.target
        const limpio = soloNumeros.includes(name) ? value.replace(/\D/g, '') : value
        setForm({ ...form, [name]: limpio })
    }

    // Props comunes de cada campo: su valor, su onChange y su error
    const campo = (name) => ({
        value: form[name],
        onChange: handleChange,
        error: errores[name],
    })

    // Solo se envían los campos del tipo elegido
    function armarDatos() {
        const base = {
            tipoDocumento,
            direccion: form.direccion.trim(),
            telefono: form.telefono,
            correo: form.correo.trim(),
        }
        if (tipoDocumento === 'RUC') {
            return {
                ...base,
                ruc: form.ruc,
                razonSocial: form.razonSocial.trim(),
                nombreComercial: form.nombreComercial.trim() || null,
            }
        }
        return {
            ...base,
            dni: form.dni,
            nombres: form.nombres.trim(),
            apellidos: form.apellidos.trim(),
        }
    }

    async function handleSubmit(e) {
        e.preventDefault()
        setErrores({})
        setErrorGeneral('')
        setGuardando(true)
        try {
            await crearCliente(armarDatos())
            onGuardado?.()
            onCerrar()
        } catch (err) {
            setErrores(err.errors ?? {})
            setErrorGeneral(err.message)
        } finally {
            setGuardando(false)
        }
    }

    return (
        <form
            onSubmit={handleSubmit}
            className="p-4 border border-gray-200 rounded-lg bg-white flex flex-col gap-5"
        >
            <div className="flex justify-between items-center">
                <h1 className="text-base font-semibold text-slate-800">Registrar Cliente</h1>
                <button type="button" onClick={onCerrar} aria-label="Cerrar">
                    <X size={16} />
                </button>
            </div>

            <div className="flex flex-col gap-2">
                <label htmlFor="tipoDoc" className="text-xs text-gray-900">Tipo de documento *</label>
                <select
                    id="tipoDoc"
                    value={tipoDocumento}
                    onChange={(e) => {
                        setTipoDocumento(e.target.value)
                        setErrores({})
                    }}
                    className={inputClass}
                >
                    <option value="RUC">RUC</option>
                    <option value="DNI">DNI</option>
                </select>
            </div>

            {/* Renderizado condicional según elección */}
            {tipoDocumento === 'RUC' && (
                <>
                    <Campo id="ruc" label="Número de RUC *" required
                        maxLength={11} inputMode="numeric" placeholder="20600000001"
                        {...campo('ruc')} />
                    <Campo id="razonSocial" label="Razón Social *" required
                        {...campo('razonSocial')} />
                    <Campo id="nombreComercial" label="Nombre Comercial"
                        {...campo('nombreComercial')} />
                </>
            )}

            {tipoDocumento === 'DNI' && (
                <>
                    <Campo id="dni" label="Número de DNI *" required
                        maxLength={8} inputMode="numeric" placeholder="77778888"
                        {...campo('dni')} />
                    <Campo id="nombres" label="Nombres *" required
                        {...campo('nombres')} />
                    <Campo id="apellidos" label="Apellidos *" required
                        {...campo('apellidos')} />
                </>
            )}

            <Campo id="direccion" label="Dirección *" required
                {...campo('direccion')} />
            <Campo id="telefono" label="Teléfono *" required type="tel"
                maxLength={9} inputMode="numeric"
                {...campo('telefono')} />
            <Campo id="correo" label="Correo *" required type="email"
                placeholder="example@dominio.com"
                {...campo('correo')} />

            {errorGeneral && (
                <p role="alert" className="text-sm text-red-600">{errorGeneral}</p>
            )}

            <div className="flex justify-end gap-3">
                <button
                    type="button"
                    onClick={onCerrar}
                    className="text-sm text-slate-600 px-4 py-2 rounded-lg border border-gray-200"
                >
                    Cancelar
                </button>
                <button
                    type="submit"
                    disabled={guardando}
                    className="text-sm text-white font-semibold px-4 py-2 rounded-lg bg-hcg-navy disabled:opacity-60"
                >
                    {guardando ? 'Guardando...' : 'Guardar'}
                </button>
            </div>
        </form>
    )
}