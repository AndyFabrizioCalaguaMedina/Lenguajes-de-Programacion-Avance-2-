// Contenedor blanco reutilizable para secciones de pantalla
export default function Tarjeta({ titulo, children, className = "" }) {
  return (
    <div className={`bg-white p-5 rounded-xl border border-gray-100 shadow-sm ${className}`}>
      {titulo && <h2 className="text-base font-semibold text-slate-800 mb-4">{titulo}</h2>}
      {children}
    </div>
  );
}
