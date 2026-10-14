import { ESTADOS, TIPOS } from './constantes'

const PILL = {
  ABIERTA: 'bg-blue-50 text-blue-700',
  EN_PROCESO: 'bg-amber-50 text-amber-700',
  COMPLETADA: 'bg-emerald-50 text-emerald-700',
  CANCELADA: 'bg-red-50 text-red-700',
}
const PILL_TIPO = {
  VENTA: 'bg-hcg-navy/10 text-hcg-navy',
  COMPRA: 'bg-hcg-gold-soft text-amber-800',
  PEDIDO: 'bg-slate-100 text-slate-700',
}

export function PillEstado({ estado }) {
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${PILL[estado] ?? 'bg-gray-100 text-gray-600'}`}>{ESTADOS[estado] ?? estado}</span>
}
export function PillTipo({ tipo }) {
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${PILL_TIPO[tipo] ?? 'bg-gray-100 text-gray-600'}`}>{TIPOS[tipo] ?? tipo}</span>
}

