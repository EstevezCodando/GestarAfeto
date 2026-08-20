import type { Procedimento } from '../types/Procedimento'

const tipoCores: Record<string, { bg: string; color: string }> = {
  CONSULTA: { bg: '#ede9fe', color: '#5b21b6' },
  EXAME: { bg: '#dbeafe', color: '#1d4ed8' },
  VACINA: { bg: '#dcfce7', color: '#166534' },
  ORIENTACAO: { bg: '#fef3c7', color: '#92400e' },
  ULTRASSONOGRAFIA: { bg: '#fce7f3', color: '#9d174d' },
}

interface Props {
  procedimento: Procedimento
}

export default function ProcedimentoCard({ procedimento }: Props) {
  const cor = tipoCores[procedimento.tipo]
  return (
    <div style={{ background: 'white', borderRadius: 8, padding: '1.25rem', border: '1px solid #e2e8f0', opacity: procedimento.ativo ? 1 : .5 }}>
      <h3 style={{ fontSize: '1rem', marginBottom: '.5rem' }}>{procedimento.nome}</h3>
      <p style={{ fontSize: '.85rem', color: '#64748b', marginBottom: '.75rem' }}>{procedimento.descricao}</p>
      <span style={{ display: 'inline-block', padding: '.2rem .6rem', borderRadius: 999, fontSize: '.75rem', fontWeight: 600, background: cor.bg, color: cor.color }}>{procedimento.tipo}</span>
      {procedimento.trimestreRecomendado && (
        <span style={{ marginLeft: '.5rem', fontSize: '.75rem', color: '#64748b' }}>{procedimento.trimestreRecomendado} trimestre</span>
      )}
      <p style={{ fontSize: '.75rem', color: '#94a3b8', marginTop: '.5rem' }}>
        Semanas {procedimento.semanaInicialRecomendada}–{procedimento.semanaFinalRecomendada} · {procedimento.obrigatorio ? 'Obrigatório' : 'Opcional'}
      </p>
    </div>
  )
}
