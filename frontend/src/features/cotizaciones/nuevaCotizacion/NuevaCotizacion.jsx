import { useState } from 'react';
import { MoveLeft } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { crearCotizacion } from '../cotizacionesApi';

// Importamos los 3 componentes que creaste
import DatosCotizacion from '../components/DatosCotizacion';
import ProductosDetalle from '../components/ProductosDetalle';
import ResumenLateral from '../components/ResumenLateral';

export default function NuevaCotizacion() {
    const navigate = useNavigate();
    const [guardando, setGuardando] = useState(false);
    const [error, setError] = useState('');
    
    // 1. Estados centralizados (guardan toda la info del formulario)
    const [datosGenerales, setDatosGenerales] = useState({
        cliente: '',
        fechaEmision: '',
        fechaVencimiento: ''
    });

    const [items, setItems] = useState([
        { descripcion: '', cantidad: 1, precioUnitario: 0, subtotal: 0 }
    ]);

    const [observaciones, setObservaciones] = useState('');

    // 2. Cálculos automáticos para el resumen
    const calcularSubtotalGeneral = () => items.reduce((acc, item) => acc + item.subtotal, 0);
    // Los precios unitarios ya incluyen IGV (así los espera el backend)
    const total = calcularSubtotalGeneral();
    const subtotalGeneral = total / 1.18;
    const igv = total - subtotalGeneral;

    async function guardar() {
        setError('');
        if (!datosGenerales.cliente) return setError('Seleccione un cliente');
        const validos = items.filter((i) => i.descripcion.trim());
        if (validos.length === 0) return setError('Agregue al menos un ítem con descripción');
        setGuardando(true);
        try {
            await crearCotizacion({
                clienteId: Number(datosGenerales.cliente),
                vigenciaHasta: datosGenerales.fechaVencimiento || undefined,
                observacion: observaciones.trim() || undefined,
                items: validos.map((i) => ({
                    descripcion: i.descripcion.trim(),
                    cantidad: Number(i.cantidad),
                    precioUnitario: Number(i.precioUnitario),
                })),
            });
            navigate('/cotizaciones');
        } catch (e) {
            const det = e.errors ? ' (' + JSON.stringify(e.errors) + ')' : '';
            setError(e.message + det);
        } finally {
            setGuardando(false);
        }
    }

    return (
        <section className="w-full">
            
            {/* TU CABECERA (Intacta) */}
            <div className='pb-8'>
                <div className='flex items-center mb-4'>
                    <Link className='flex items-center gap-2 text-slate-500 hover:text-slate-800'
                        to="/cotizaciones"
                    >
                        <MoveLeft size={16} />
                        <p>Cotizaciones</p>
                    </Link>
                    <p className='text-slate-800 font-semibold'>
                        <span className='mx-4 text-slate-500'>/</span>
                        Nueva cotización
                    </p>
                </div>
                <h1 className='text-2xl font-bold'>Nueva Cotización</h1>
                <p className='text-slate-500 text-sm'>El código se genera automáticamente al guardar</p>
            </div>

            {/* CONTENIDO PRINCIPAL: Layout de 3 columnas */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                
                {/* Columna Izquierda: Ocupa 2/3 del espacio en pantallas grandes */}
                <div className="lg:col-span-2 flex flex-col gap-6">
                    <DatosCotizacion 
                        datos={datosGenerales} 
                        setDatos={setDatosGenerales} 
                    />
                    
                    <ProductosDetalle 
                        items={items} 
                        setItems={setItems}
                        observaciones={observaciones}
                        setObservaciones={setObservaciones}
                    />
                </div>

                {/* Columna Derecha: Ocupa 1/3 del espacio y se queda fija (sticky) */}
                <div className="lg:col-span-1 sticky top-6">
                    <ResumenLateral 
                        subtotal={subtotalGeneral} 
                        igv={igv} 
                        total={total}
                        onGuardar={guardar}
                        guardando={guardando}
                        error={error}
                    />
                </div>
                
            </div>
            
        </section>
    );
}