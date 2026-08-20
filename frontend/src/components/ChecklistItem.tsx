import type { ItemChecklist } from '../types/Checklist'

const badgeColors: Record<string, { bg: string; color: string }> = {
  PENDENTE: { bg: '#fef9c3', color: '#92400e' },
  REALIZADO: { bg: '#dcfce7', color: '#166534' },
  NAO_SE_APLICA: { bg: '#f1f5f9', color: '#64748b' },
  PRECISA_REVISAR: { bg: '#fee2e2', color: '#991b1b' },
}

interface Props {
  item: ItemChecklist
  onRealizar: () => void
  onRevisar: () => void
}

export default function ChecklistItem({ item, onRealizar, onRevisar }: Props) {
  const badge = badgeColors[item.status]
  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '.75rem', padding: '.75rem', background: 'white', borderRadius: 6, border: '1px solid #e2e8f0', marginBottom: '.5rem' }}>
      <div style={{ flex: 1 }}>
        <strong style={{ fontSize: '.9rem' }}>{item.procedimento.nome}</strong>
        <div style={{ fontSize: '.8rem', color: '#64748b', marginTop: '.2rem' }}>
          {item.procedimento.tipo}
          {item.dataRealizacao && <> · Realizado em: {item.dataRealizacao}</>}
        </div>
        {item.observacao && <div style={{ fontSize: '.8rem', color: '#475569', marginTop: '.2rem' }}>Obs: {item.observacao}</div>}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '.35rem' }}>
        <span style={{ padding: '.2rem .6rem', borderRadius: 999, fontSize: '.75rem', fontWeight: 600, background: badge.bg, color: badge.color }}>{item.status}</span>
        {item.status !== 'REALIZADO' && (
          <button onClick={onRealizar} style={{ padding: '.25rem .5rem', background: '#16a34a', color: 'white', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: '.75rem' }}>✓ Realizado</button>
        )}
        {item.status !== 'PRECISA_REVISAR' && (
          <button onClick={onRevisar} style={{ padding: '.25rem .5rem', background: '#d97706', color: 'white', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: '.75rem' }}>⚠ Revisar</button>
        )}
      </div>
    </div>
  )
}
