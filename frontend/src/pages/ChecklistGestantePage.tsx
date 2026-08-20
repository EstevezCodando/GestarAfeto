import { useState, useEffect } from 'react'
import { gestanteService } from '../services/gestanteService'
import { checklistService } from '../services/checklistService'
import ChecklistItem from '../components/ChecklistItem'
import type { Gestante } from '../types/Gestante'
import type { ItemChecklist } from '../types/Checklist'

export default function ChecklistGestantePage() {
  const [gestantes, setGestantes] = useState<Gestante[]>([])
  const [gestanteId, setGestanteId] = useState<number | null>(null)
  const [itens, setItens] = useState<ItemChecklist[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  useEffect(() => { gestanteService.listar().then(setGestantes) }, [])

  const carregar = async (id: number) => {
    setLoading(true); setError(null); setMsg(null)
    try { setItens(await checklistService.listar(id)) } catch (e: any) { setError(e.message); setItens(null) } finally { setLoading(false) }
  }

  const gerar = async () => {
    if (!gestanteId) return
    setLoading(true); setError(null); setMsg(null)
    try { setItens(await checklistService.gerar(gestanteId)); setMsg('Checklist gerado com sucesso!') }
    catch (e: any) { setError(e.message) } finally { setLoading(false) }
  }

  const atualizar = async (itemId: number, fn: () => Promise<ItemChecklist>) => {
    setError(null)
    try { const updated = await fn(); setItens(prev => prev!.map(i => i.id === itemId ? updated : i)) }
    catch (e: any) { setError(e.message) }
  }

  const pendentes = itens?.filter(i => i.status === 'PENDENTE').length ?? 0
  const realizados = itens?.filter(i => i.status === 'REALIZADO').length ?? 0
  const revisao = itens?.filter(i => i.status === 'PRECISA_REVISAR').length ?? 0

  return (
    <div>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1rem' }}>Checklist Pré-Natal</h2>

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div style={{ flex: 1, minWidth: 200 }}>
          <label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Gestante</label>
          <select value={gestanteId ?? ''} onChange={e => { const id = +e.target.value; setGestanteId(id); carregar(id) }} style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6 }}>
            <option value="">Selecione...</option>
            {gestantes.map(g => <option key={g.id} value={g.id}>{g.nome}</option>)}
          </select>
        </div>
        <button onClick={gerar} disabled={!gestanteId || loading} style={{ padding: '.5rem 1rem', background: '#7c3aed', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 600 }}>Gerar Checklist</button>
      </div>

      {msg && <div style={{ background: '#dcfce7', color: '#166534', padding: '.5rem .75rem', borderRadius: 4, marginBottom: '.75rem', fontSize: '.85rem' }}>{msg}</div>}
      {error && <div style={{ background: '#fee2e2', color: '#dc2626', padding: '.5rem .75rem', borderRadius: 4, marginBottom: '.75rem', fontSize: '.85rem' }}>{error}</div>}
      {loading && <p style={{ color: '#64748b', fontStyle: 'italic' }}>Carregando...</p>}

      {itens && (
        <>
          <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
            {[['Total', itens.length, '#7c3aed'], ['Pendentes', pendentes, '#d97706'], ['Realizados', realizados, '#16a34a'], ['Para Revisar', revisao, '#dc2626']].map(([l, n, c]) => (
              <div key={String(l)} style={{ background: 'white', borderRadius: 8, padding: '.75rem 1.25rem', border: '1px solid #e2e8f0', textAlign: 'center', minWidth: 100 }}>
                <div style={{ fontSize: '1.5rem', fontWeight: 700, color: String(c) }}>{n}</div>
                <div style={{ fontSize: '.8rem', color: '#64748b' }}>{l}</div>
              </div>
            ))}
          </div>
          {itens.map(item => (
            <ChecklistItem
              key={item.id}
              item={item}
              onRealizar={() => atualizar(item.id, () => checklistService.marcarRealizado(item.id))}
              onRevisar={() => atualizar(item.id, () => checklistService.marcarParaRevisar(item.id))}
            />
          ))}
          {itens.length === 0 && <p style={{ color: '#64748b' }}>Nenhum item. Clique em "Gerar Checklist".</p>}
        </>
      )}
    </div>
  )
}
