import type { Alerta, PrioridadeAlerta, TipoAlerta } from '../types/Alerta'

const prioridadeEstilo: Record<PrioridadeAlerta, { bg: string; color: string; borda: string; rotulo: string }> = {
  ALTA: { bg: '#fee2e2', color: '#991b1b', borda: '#dc2626', rotulo: 'Alta' },
  MEDIA: { bg: '#fef3c7', color: '#92400e', borda: '#d97706', rotulo: 'Média' },
  BAIXA: { bg: '#e0f2fe', color: '#075985', borda: '#0284c7', rotulo: 'Baixa' },
}

const tipoRotulo: Record<TipoAlerta, string> = {
  PROCEDIMENTO_ATRASADO: 'Atrasado',
  PROCEDIMENTO_PENDENTE: 'Pendente agora',
  JANELA_PROXIMA: 'Em breve',
  REVISAO_SOLICITADA: 'Revisão solicitada',
}

interface Props {
  alerta: Alerta
  onMarcarLido: () => void
  onResolver: () => void
}

export default function AlertaCard({ alerta, onMarcarLido, onResolver }: Props) {
  const estilo = prioridadeEstilo[alerta.prioridade]
  const naoLido = alerta.status === 'ABERTO'

  return (
    <div style={{
      display: 'flex', alignItems: 'flex-start', gap: '.75rem', padding: '.75rem',
      background: 'white', borderRadius: 6, border: '1px solid #e2e8f0',
      borderLeft: `4px solid ${estilo.borda}`, marginBottom: '.5rem',
    }}>
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '.5rem', flexWrap: 'wrap' }}>
          <strong style={{ fontSize: '.9rem', fontWeight: naoLido ? 700 : 500 }}>{alerta.titulo}</strong>
          {naoLido && (
            <span style={{
              width: 8, height: 8, borderRadius: '50%', background: estilo.borda,
              display: 'inline-block',
            }} title="Não lido" />
          )}
        </div>
        <div style={{ fontSize: '.8rem', color: '#475569', marginTop: '.3rem' }}>{alerta.mensagem}</div>
        <div style={{ fontSize: '.75rem', color: '#64748b', marginTop: '.3rem' }}>
          {tipoRotulo[alerta.tipo]}
          {alerta.semanaGestacionalReferencia !== null && <> · semana {alerta.semanaGestacionalReferencia}</>}
          {alerta.dataReferencia && <> · previsto para {alerta.dataReferencia}</>}
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '.35rem' }}>
        <span style={{
          padding: '.2rem .6rem', borderRadius: 999, fontSize: '.75rem', fontWeight: 600,
          background: estilo.bg, color: estilo.color,
        }}>
          {estilo.rotulo}
        </span>
        {naoLido && (
          <button onClick={onMarcarLido} style={{
            padding: '.25rem .5rem', background: '#475569', color: 'white', border: 'none',
            borderRadius: 4, cursor: 'pointer', fontSize: '.75rem',
          }}>
            Marcar lido
          </button>
        )}
        <button onClick={onResolver} style={{
          padding: '.25rem .5rem', background: '#16a34a', color: 'white', border: 'none',
          borderRadius: 4, cursor: 'pointer', fontSize: '.75rem',
        }}>
          ✓ Resolver
        </button>
      </div>
    </div>
  )
}
