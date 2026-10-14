import { useEffect, useState } from 'react';
import { listarClientes } from '../../clientes/clientesApi';

export default function DatosCotizacion({ datos, setDatos }) {
  const [clientes, setClientes] = useState([]);
  useEffect(() => {
    listarClientes({ activo: "true" }).then((d) => setClientes(d.content)).catch(() => {});
  }, []);

  return (
    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <h2 className="text-lg font-semibold text-gray-800 mb-4">Datos de la cotización</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* Selector de Cliente que ocupa toda la fila */}
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-600 mb-1">Cliente *</label>
          <select 
            className="w-full border border-gray-300 rounded-lg p-2.5 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-hcg-gold"
            value={datos.cliente}
            onChange={(e) => setDatos({...datos, cliente: e.target.value})}
          >
            <option value="">Seleccionar cliente...</option>
            {clientes.map((c) => (
              <option key={c.id} value={c.id}>{c.nombres || c.razonSocial}</option>
            ))}
          </select>
        </div>

        {/* Fechas en dos columnas */}
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Fecha de emisión *</label>
          <input 
            type="date" 
            className="w-full border border-gray-300 rounded-lg p-2.5 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-hcg-gold"
            value={datos.fechaEmision}
            onChange={(e) => setDatos({...datos, fechaEmision: e.target.value})}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-600 mb-1">Fecha de vencimiento</label>
          <input 
            type="date" 
            className="w-full border border-gray-300 rounded-lg p-2.5 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-hcg-gold"
            value={datos.fechaVencimiento}
            onChange={(e) => setDatos({...datos, fechaVencimiento: e.target.value})}
          />
        </div>
      </div>
    </div>
  );
}