import { useState, useEffect } from 'react'
import { gestanteService } from '../services/gestanteService'
import { alertaService } from '../services/alertaService'
import AlertaCard from '../components/AlertaCard'
import type { Gestante } from '../types/Gestante'
import type { Alerta, PrioridadeAlerta, ResumoAlertas } from '../types/Alerta'

type FiltroPrioridade = PrioridadeAlerta | 'TODAS'

export default function AlertasPage() {
  const [gestantes, setGestantes] = useState<Gestante[]>([])
  const [gestanteId, setGestanteId] = useState<number | null>(null)
  const [alertas, setAlertas] = useState<Alerta[] | null>(null)
  const [resumo, setResumo] = useState<ResumoAlertas | null>(null)
  const [filtro, setFiltro] = useState<FiltroPrioridade>('TODAS')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  useEffect(() => { gestanteService.listar().then(setGestantes) }, [])

  const carregar = async (id: number) => {
    setLoading(true); setError(null); setMsg(null)
    try {
      const [lista, agregado] = await Promise.all([
        alertaService.listar(id),
        alertaService.resumo(id),
      ])
      setAlertas(lista); setResumo(agregado)
    } catch (e: any) {
      setError(e.message); setAlertas(null); setResumo(null)
    } finally {
      setLoading(false)
    }
  }

  /**
   * A reavaliação é assíncrona: o serviço apenas publica o evento e responde 202.
   * O cálculo acontece no microsserviço, então a tela precisa buscar o resultado
   * depois, em vez de recebê-lo nesta chamada. Aqui a página recarrega algumas vezes
   * em curto intervalo até a lista mudar — é a consistência eventual aparecendo na
   * interface, e não um defeito.
   */
  const avaliar = async () => {
    if (!gestanteId) return
    setLoading(true); setError(null); setMsg(null)
    try {
      const aceite = await alertaService.avaliar(gestanteId)
      setMsg(aceite.mensagem)

      const antes = JSON.stringify(await alertaService.resumo(gestanteId))
      for (let tentativa = 0; tentativa < 6; tentativa++) {
        await new Promise(r => setTimeout(r, 500))
        const [lista, agregado] = await Promise.all([
          alertaService.listar(gestanteId),
          alertaService.resumo(gestanteId),
        ])
        setAlertas(lista); setResumo(agregado)
        if (JSON.stringify(agregado) !== antes) {
          setMsg(`Alertas atualizados: ${agregado.totalAtivos} ativo(s).`)
          break
        }
      }
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const agir = async (acao: () => Promise<Alerta>) => {
    setError(null)
    try {
      await acao()
      if (gestanteId) await carregar(gestanteId)
    } catch (e: any) {
      setError(e.message)
    }
  }

  const visiveis = alertas?.filter(a => filtro === 'TODAS' || a.prioridade === filtro) ?? []

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '.35rem' }}>Alertas do Pré-Natal</h2>
      <p style={{ fontSize: '.8rem', color: '#64748b', marginBottom: '1rem' }}>
        Priorização calculada pelo microsserviço de alertas a partir do checklist e da semana gestacional.
      </p>

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div style={{ flex: 1, minWidth: 200 }}>
          <label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Gestante</label>
          <select
            value={gestanteId ?? ''}
            onChange={e => { const id = +e.target.value; setGestanteId(id); carregar(id) }}
            style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6 }}
          >
            <option value="">Selecione...</option>
            {gestantes.map(g => <option key={g.id} value={g.id}>{g.nome}</option>)}
          </select>
        </div>
        <button onClick={avaliar} disabled={!gestanteId || loading} style={{
          padding: '.5rem 1rem', background: '#7c3aed', color: 'white', border: 'none',
          borderRadius: 6, cursor: 'pointer', fontWeight: 600,
        }}>
          Reavaliar Alertas
        </button>
      </div>

      {msg && <div style={{ background: '#dcfce7', color: '#166534', padding: '.5rem .75rem', borderRadius: 4, marginBottom: '.75rem', fontSize: '.85rem' }}>{msg}</div>}
      {error && <div style={{ background: '#fee2e2', color: '#dc2626', padding: '.5rem .75rem', borderRadius: 4, marginBottom: '.75rem', fontSize: '.85rem' }}>{error}</div>}
      {loading && <p style={{ color: '#64748b', fontStyle: 'italic' }}>Carregando...</p>}

      {resumo && (
        <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
          {([
            ['Ativos', resumo.totalAtivos, '#7c3aed', 'TODAS'],
            ['Alta', resumo.alta, '#dc2626', 'ALTA'],
            ['Média', resumo.media, '#d97706', 'MEDIA'],
            ['Baixa', resumo.baixa, '#0284c7', 'BAIXA'],
            ['Não lidos', resumo.naoLidos, '#475569', null],
            ['Resolvidos', resumo.resolvidos, '#16a34a', null],
          ] as const).map(([rotulo, valor, cor, alvo]) => (
            <button
              key={rotulo}
              onClick={() => alvo && setFiltro(alvo as FiltroPrioridade)}
              disabled={!alvo}
              style={{
                background: 'white', borderRadius: 8, padding: '.75rem 1.25rem',
                border: alvo && filtro === alvo ? `2px solid ${cor}` : '1px solid #e2e8f0',
                textAlign: 'center', minWidth: 100, cursor: alvo ? 'pointer' : 'default',
              }}
            >
              <div style={{ fontSize: '1.5rem', fontWeight: 700, color: cor }}>{valor}</div>
              <div style={{ fontSize: '.8rem', color: '#64748b' }}>{rotulo}</div>
            </button>
          ))}
        </div>
      )}

      {alertas && (
        <>
          {visiveis.map(alerta => (
            <AlertaCard
              key={alerta.id}
              alerta={alerta}
              onMarcarLido={() => agir(() => alertaService.marcarLido(alerta.id))}
              onResolver={() => agir(() => alertaService.resolver(alerta.id))}
            />
          ))}
          {visiveis.length === 0 && (
            <p style={{ color: '#64748b' }}>
              {alertas.length === 0
                ? 'Nenhum alerta ativo. Clique em "Reavaliar Alertas" após atualizar o checklist.'
                : 'Nenhum alerta nesta prioridade.'}
            </p>
          )}
        </>
      )}
    </div>
  )
}
