import { TriangleAlert } from 'lucide-react'
import Tabla from './components/Tabla';
import Registrar from './components/Registrar';
import { useState } from 'react';

export default function BoletasPage() {
    const [mostrarForm, setMostrarForm] = useState(false);
    const [recarga, setRecarga] = useState(0);
    const refrescar = () => setRecarga((n) => n + 1);

    return (
        <section>
            <div className="pb-8 flex justify-between">
                <div>
                    <h1 className="text-slate-800 text-2xl font-bold">
                        Comprobantes
                    </h1>
                    <p className="text-slate-500 text-sm">
                        Boletas y facturas electrónicas emitidas
                    </p>
                </div>
                <div>
                    <button
                        onClick={() => setMostrarForm(true)} 
                        className='flex gap-2 text-center text-white text-sm font-semibold bg-hcg-navy rounded-xl p-3 '

                    >
                    
                    + Emitir comprobante
                    </button>
                </div>
            </div>
            <div className='flex items-center py-3 bg-[#FFFBEB] rounded-xl border-l-3 border-l-amber-400'>
                <div className='mx-4'>
                    <TriangleAlert size={16} className='text-yellow-600'/>
                    
                </div>
                <div>
                    <h2 className='text-yellow-600 text-sm font-bold'>Sistema de registro y control interno</h2>
                    <p className='text-yellow-600 text-sm'>Este módulo permite registrar y hacer seguimiento de los comprobantes emitidos. No reemplaza la emisión tributaria oficial a través de SUNAT ni los sistemas de facturación electrónica certificados.</p>
                </div>
            </div>

            <div className='md:flex items-start gap-4 mt-5'>
                <div className='min-w-0 flex-1'>
                    <Tabla recarga={recarga} onCambio={refrescar} />
                </div>

                {mostrarForm && (
                <div className='w-96 shrink-0 md:mt-0 mt-6'>
                    <Registrar 
                        onCerrar={() => setMostrarForm(false)}
                        onEmitida={refrescar}
                    />

                </div>
                )}
            </div>

            
        </section>
    );
}
