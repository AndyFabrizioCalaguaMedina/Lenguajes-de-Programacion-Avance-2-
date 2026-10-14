import { Save, Info } from 'lucide-react';

import { Link } from 'react-router-dom';

export default function ResumenLateral({ subtotal, igv, total, onGuardar, guardando, error }) {
  return (
    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <h2 className="text-lg font-semibold text-gray-800 mb-5">Resumen</h2>
      
      <div className="flex flex-col gap-3 text-sm mb-6">
        <div className="flex justify-between text-gray-500">
          <span>Subtotal</span>
          <span className="font-medium text-gray-800">S/ {subtotal.toFixed(2)}</span>
        </div>
        <div className="flex justify-between text-gray-500">
          <span>IGV (18%)</span>
          <span className="font-medium text-gray-800">S/ {igv.toFixed(2)}</span>
        </div>
        
        <div className="flex justify-between items-center mt-2 pt-4 border-t border-gray-200">
          <span className="font-bold text-gray-800">Total</span>
          <span className="text-xl font-bold text-gray-900">S/ {total.toFixed(2)}</span>
        </div>
      </div>

      {/* Botones de acción */}
      <div className="flex flex-col gap-3">
        {error && <p className="text-xs text-red-600">{error}</p>}
        <button
          onClick={onGuardar}
          disabled={guardando}
          className="w-full flex items-center justify-center gap-2 bg-hcg-navy hover:bg-hcg-navy-soft disabled:opacity-50 text-white font-medium py-2.5 rounded-lg text-sm transition-colors">
          <Save size={16} />
          {guardando ? 'Guardando...' : 'Guardar cotización'}
        </button>
        
        <Link to="/cotizaciones" className="w-full text-center text-gray-400 hover:text-gray-600 font-medium py-2 text-sm transition-colors">
          Cancelar
        </Link>
      </div>

      {/* Alerta informativa */}
      <div className="mt-6 bg-slate-50 rounded-lg p-3 flex gap-3 border border-slate-100">
        <Info size={16} className="text-blue-500 shrink-0 mt-0.5" />
        <p className="text-xs text-slate-500 leading-relaxed">
          Esta cotización es un documento interno. No reemplaza comprobantes tributarios oficiales (SUNAT).
        </p>
      </div>
    </div>
  );
}