// Gráfico de barras verticales simple (sin librerías). datos = [{ etiqueta, valor }]
export default function Barras({ datos, formato = (v) => v, color = "#2563eb", alto = 160 }) {
  const max = Math.max(1, ...datos.map((d) => d.valor));
  const ancho = Math.max(datos.length * 28, 280);
  const paso = Math.ceil(datos.length / 10);
  return (
    <div className="overflow-x-auto">
      <svg viewBox={`0 0 ${ancho} ${alto + 28}`} className="w-full min-w-[280px]" role="img">
        {datos.map((d, i) => {
          const h = (d.valor / max) * alto;
          const x = i * (ancho / datos.length);
          const w = ancho / datos.length - 6;
          return (
            <g key={i}>
              <title>{`${d.etiqueta}: ${formato(d.valor)}`}</title>
              <rect x={x + 3} y={alto - h} width={w} height={h} rx="3" fill={color} opacity={d.valor ? 1 : 0.15} />
              {i % paso === 0 && (
                <text x={x + 3 + w / 2} y={alto + 16} fontSize="10" textAnchor="middle" fill="#64748b">
                  {d.etiqueta}
                </text>
              )}
            </g>
          );
        })}
      </svg>
    </div>
  );
}

// Lista con barras horizontales proporcionales. filas = [{ nombre, valor, extra }]
export function BarrasLista({ filas, formato = (v) => v, color = "bg-blue-500", vacio = "Sin datos en este período." }) {
  if (!filas?.length) return <p className="text-sm text-gray-400">{vacio}</p>;
  const max = Math.max(1, ...filas.map((f) => f.valor));
  return (
    <ul className="flex flex-col gap-3">
      {filas.map((f) => (
        <li key={f.nombre} className="text-sm">
          <div className="flex justify-between gap-3">
            <span className="truncate text-gray-700">{f.nombre}</span>
            <span className="shrink-0 font-medium text-gray-900">
              {formato(f.valor)}{f.extra ? <span className="ml-2 text-xs text-gray-400">{f.extra}</span> : null}
            </span>
          </div>
          <div className="mt-1 h-2 rounded-full bg-gray-100">
            <div className={`h-2 rounded-full ${color}`} style={{ width: `${(f.valor / max) * 100}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}
