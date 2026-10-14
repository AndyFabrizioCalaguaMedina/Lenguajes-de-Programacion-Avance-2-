// Gráfico de dona con SVG. filas: [{ nombre, valor, color }]
export default function Dona({ filas, centro }) {
  const total = filas.reduce((a, f) => a + f.valor, 0)
  const R = 40
  const C = 2 * Math.PI * R
  let acumulado = 0
  return (
    <div className='flex flex-wrap items-center gap-6'>
      <svg viewBox='0 0 100 100' className='h-40 w-40 -rotate-90'>
        <circle cx='50' cy='50' r={R} fill='none' stroke='#e5e7eb' strokeWidth='14' />
        {total > 0 && filas.map((f) => {
          const largo = (f.valor / total) * C
          const el = (
            <circle key={f.nombre} cx='50' cy='50' r={R} fill='none' stroke={f.color} strokeWidth='14'
              strokeDasharray={`${largo} ${C - largo}`} strokeDashoffset={-acumulado} />
          )
          acumulado += largo
          return el
        })}
        <text x='50' y='50' className='rotate-90 origin-center fill-slate-800 text-[14px] font-bold' textAnchor='middle' dominantBaseline='central'>
          {centro ?? total}
        </text>
      </svg>
      <ul className='space-y-1.5 text-sm'>
        {filas.map((f) => (
          <li key={f.nombre} className='flex items-center gap-2 text-gray-700'>
            <span className='h-2.5 w-2.5 rounded-full' style={{ background: f.color }} />
            {f.nombre} <strong className='text-slate-800'>{f.valor}</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}
