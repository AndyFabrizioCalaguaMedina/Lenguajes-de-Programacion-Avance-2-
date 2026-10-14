import { CircleCheck, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import TablaClientes from './components/TablaClientes'
import RegistrarClientes from './components/RegistrarClientes'

export default function ClientesPage() {
    const [mostrarForm, setMostrarForm] = useState(false);
    const [aviso, setAviso] = useState(null); // { texto, ok }
    const [recarga, setRecarga] = useState(0)

    useEffect(() => {
        if (!aviso) return
        const temporizador = setTimeout(() => setAviso(null), 4000)
        return () => clearTimeout(temporizador)
    }, [aviso])

    function handleGuardado() {
        setAviso({ texto: 'Cliente registrado correctamente', ok: true })
        setRecarga((n) => n + 1)
    }
    return (

        <section>

            {aviso && (
                <div
                    role="status"
                    className={`fixed top-4 right-4 z-50 flex items-center gap-2 rounded-lg px-4 py-3 text-sm text-white shadow-lg ${aviso.ok ? 'bg-green-600' : 'bg-red-600'}`}
                >
                    <CircleCheck size={16} />
                    {aviso.texto}
                    <button type="button" onClick={() => setAviso(null)} aria-label="Cerrar aviso">
                    <X size={14} />
                    </button>
                </div>
            )}
            <div className="pb-8 flex justify-between">
                <div>
                    <h1 className="text-slate-800 text-2xl font-bold">
                        Clientes
                    </h1>
                    <p className="text-slate-500 text-sm">
                        Registro y control de clientes
                    </p>
                </div>
                <div>
                    <button
                        onClick={() => setMostrarForm(true)}
                        className='flex gap-2 text-center text-white text-sm font-semibold bg-hcg-navy rounded-xl p-3 '

                    >

                        + Registrar Cliente
                    </button>
                </div>
            </div>
            

            <div className='md:flex items-start gap-4 mt-5'>
                <div className='min-w-0 flex-1'>
                    <TablaClientes
                        recarga={recarga}
                        onCambio={() => setRecarga((n) => n + 1)}
                        onAviso={(texto, ok = true) => setAviso({ texto, ok })}
                    />
                </div>

                {mostrarForm && (
                    <div className='w-96 shrink-0 md:mt-0 mt-6'>
                        <RegistrarClientes
                            onCerrar={() => setMostrarForm(false)}
                            onGuardado={handleGuardado}
                        />

                    </div>
                )}
            </div>

        </section>
    );
}
