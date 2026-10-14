import { Plus, X } from 'lucide-react';

export default function ProductosDetalle({ items, setItems, observaciones, setObservaciones }) {
  
  // Agregar una nueva fila vacía
  const handleAgregarItem = () => {
    setItems([...items, { descripcion: '', cantidad: 1, precioUnitario: 0, subtotal: 0 }]);
  };

  // Eliminar una fila usando su índice
  const handleRemoveItem = (indexToRemove) => {
    const nuevosItems = items.filter((_, index) => index !== indexToRemove);
    setItems(nuevosItems);
  };

  // Actualizar los valores cuando el usuario escribe en los inputs
  const handleItemChange = (index, campo, valor) => {
    const nuevosItems = [...items];
    
    // Actualizamos el campo específico (descripcion, cantidad o precioUnitario)
    nuevosItems[index][campo] = valor;
    
    // Si modificamos cantidad o precio, recalculamos el subtotal de esa fila
    if (campo === 'cantidad' || campo === 'precioUnitario') {
      const cant = parseFloat(nuevosItems[index].cantidad) || 0;
      const precio = parseFloat(nuevosItems[index].precioUnitario) || 0;
      nuevosItems[index].subtotal = cant * precio;
    }
    
    setItems(nuevosItems);
  };

  return (
    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-lg font-semibold text-gray-800">Productos / Servicios</h2>
        <button 
          onClick={handleAgregarItem}
          className="flex items-center gap-2 bg-hcg-navy hover:bg-hcg-navy-soft text-white px-4 py-2 rounded-lg text-sm transition-colors"
        >
          <Plus size={16} />
          Agregar ítem
        </button>
      </div>

      <div className="grid grid-cols-12 gap-3 text-xs font-bold text-gray-500 mb-3 px-2">
        <div className="col-span-5">DESCRIPCIÓN</div>
        <div className="col-span-2 text-center">CANT.</div>
        <div className="col-span-2 text-center">P. UNIT. (S/ con IGV)</div>
        <div className="col-span-2 text-right">SUBTOTAL</div>
        <div className="col-span-1"></div>
      </div>

      <div className="flex flex-col gap-3">
        {items.map((item, index) => (
          <div key={index} className="grid grid-cols-12 gap-3 items-center">
            <div className="col-span-5">
              <input 
                type="text" 
                placeholder="Descripción del producto..."
                className="w-full border border-gray-300 rounded-lg p-2.5 text-sm"
                value={item.descripcion}
                onChange={(e) => handleItemChange(index, 'descripcion', e.target.value)}
              />
            </div>
            <div className="col-span-2">
              <input 
                type="number" 
                min="1"
                className="w-full border border-gray-300 rounded-lg p-2.5 text-sm text-center"
                value={item.cantidad}
                onChange={(e) => handleItemChange(index, 'cantidad', e.target.value)}
              />
            </div>
            <div className="col-span-2">
              <input 
                type="number" 
                min="0"
                step="0.01"
                className="w-full border border-gray-300 rounded-lg p-2.5 text-sm text-center"
                value={item.precioUnitario}
                onChange={(e) => handleItemChange(index, 'precioUnitario', e.target.value)}
              />
            </div>
            <div className="col-span-2 text-right font-medium text-sm text-gray-800">
              S/ {item.subtotal.toFixed(2)}
            </div>
            <div className="col-span-1 flex justify-center">
              {/* Botón para eliminar la fila */}
              <button 
                onClick={() => handleRemoveItem(index)}
                className="text-gray-300 hover:text-red-500 transition-colors"
                title="Eliminar fila"
              >
                <X size={18} />
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-8">
        <label className="block text-sm font-medium text-gray-600 mb-2">Observaciones</label>
        <textarea 
          className="w-full border border-gray-300 rounded-lg p-3 text-sm text-gray-700 resize-none focus:outline-none focus:ring-2 focus:ring-hcg-gold"
          rows="3" 
          placeholder="Condiciones de pago, tiempo de entrega, notas adicionales..."
          value={observaciones}
          onChange={(e) => setObservaciones(e.target.value)}
        />
      </div>
    </div>
  );
}