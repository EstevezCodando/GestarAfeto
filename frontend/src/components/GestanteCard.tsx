import type { Gestante } from '../types/Gestante'

interface Props {
  gestante: Gestante
  onEdit: () => void
  onDelete: () => void
}

export default function GestanteCard({ gestante, onEdit, onDelete }: Props) {
  return (
    <div style={{ background: 'white', borderRadius: 8, padding: '1.25rem', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,.08)' }}>
      <h3 style={{ marginBottom: '.5rem' }}>{gestante.nome}</h3>
      <p style={{ fontSize: '.85rem', color: '#64748b' }}>📞 {gestante.telefone || '—'}</p>
      <p style={{ fontSize: '.85rem', color: '#64748b' }}>✉️ {gestante.email || '—'}</p>
      <p style={{ fontSize: '.85rem', color: '#64748b' }}>🍼 DPP: {gestante.dataProvavelParto || '—'}</p>
      <p style={{ fontSize: '.75rem', color: '#94a3b8', marginTop: '.5rem' }}>
        Cadastro: {gestante.dataCadastro?.slice(0, 10)}
      </p>
      <div style={{ display: 'flex', gap: '.5rem', marginTop: '.75rem' }}>
        <button onClick={onEdit} style={{ padding: '.3rem .6rem', background: '#7c3aed', color: 'white', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: '.8rem' }}>Editar</button>
        <button onClick={onDelete} style={{ padding: '.3rem .6rem', background: '#dc2626', color: 'white', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: '.8rem' }}>Remover</button>
      </div>
    </div>
  )
}
