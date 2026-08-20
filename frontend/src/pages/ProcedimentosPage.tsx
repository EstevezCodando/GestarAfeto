import { useState, useEffect } from 'react'
import { procedimentoService } from '../services/procedimentoService'
import ProcedimentoCard from '../components/ProcedimentoCard'
import type { Procedimento, CriarProcedimentoRequest, TipoProcedimento, TrimestreGestacional } from '../types/Procedimento'

const tipos: TipoProcedimento[] = ['CONSULTA', 'EXAME', 'VACINA', 'ORIENTACAO', 'ULTRASSONOGRAFIA']
const trimestres = ['', 'PRIMEIRO', 'SEGUNDO', 'TERCEIRO']

export default function ProcedimentosPage() {
  const [procedimentos, setProcedimentos] = useState<Procedimento[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<CriarProcedimentoRequest>({ nome: '', descricao: null, tipo: 'CONSULTA', trimestreRecomendado: null, semanaInicialRecomendada: null, semanaFinalRecomendada: null, obrigatorio: true })
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = () => procedimentoService.listar().then(setProcedimentos).catch(e => setError(e.message))
  useEffect(() => { load() }, [])

  const save = async (e: React.FormEvent) => {
    e.preventDefault(); setSaving(true); setError(null)
    try { await procedimentoService.cadastrar(form); setShowForm(false); load() }
    catch (e: any) { setError(e.message) } finally { setSaving(false) }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Procedimentos</h2>
        <button onClick={() => setShowForm(true)} style={{ padding: '.5rem 1rem', background: '#7c3aed', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 600 }}>+ Novo Procedimento</button>
      </div>
      {error && <div style={{ color: '#dc2626', background: '#fee2e2', padding: '.5rem', borderRadius: 4, marginBottom: '1rem', fontSize: '.85rem' }}>{error}</div>}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '1rem' }}>
        {procedimentos.map(p => <ProcedimentoCard key={p.id} procedimento={p} />)}
      </div>

      {showForm && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }} onClick={e => e.target === e.currentTarget && setShowForm(false)}>
          <div style={{ background: 'white', borderRadius: 10, padding: '1.5rem', width: '100%', maxWidth: 480, maxHeight: '90vh', overflowY: 'auto' }}>
            <h2 style={{ marginBottom: '1rem' }}>Novo Procedimento</h2>
            {error && <div style={{ color: '#dc2626', background: '#fee2e2', padding: '.5rem', borderRadius: 4, marginBottom: '.75rem', fontSize: '.85rem' }}>{error}</div>}
            <form onSubmit={save}>
              <div style={{ marginBottom: '1rem' }}><label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Nome *</label><input required value={form.nome} onChange={e => setForm(p => ({ ...p, nome: e.target.value }))} style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6 }} /></div>
              <div style={{ marginBottom: '1rem' }}><label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Descrição</label><textarea value={form.descricao ?? ''} onChange={e => setForm(p => ({ ...p, descricao: e.target.value || null }))} style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6, minHeight: 70 }} /></div>
              <div style={{ marginBottom: '1rem' }}><label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Tipo *</label><select value={form.tipo} onChange={e => setForm(p => ({ ...p, tipo: e.target.value as TipoProcedimento }))} style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6 }}>{tipos.map(t => <option key={t} value={t}>{t}</option>)}</select></div>
              <div style={{ marginBottom: '1rem' }}><label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Trimestre</label><select value={form.trimestreRecomendado ?? ''} onChange={e => setForm(p => ({ ...p, trimestreRecomendado: (e.target.value as TrimestreGestacional) || null }))} style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6 }}>{trimestres.map(t => <option key={t} value={t}>{t || 'Todos'}</option>)}</select></div>
              <div style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
                <div style={{ flex: 1 }}><label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Semana Inicial</label><input type="number" min={1} max={40} value={form.semanaInicialRecomendada ?? ''} onChange={e => setForm(p => ({ ...p, semanaInicialRecomendada: e.target.value ? +e.target.value : null }))} style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6 }} /></div>
                <div style={{ flex: 1 }}><label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Semana Final</label><input type="number" min={1} max={40} value={form.semanaFinalRecomendada ?? ''} onChange={e => setForm(p => ({ ...p, semanaFinalRecomendada: e.target.value ? +e.target.value : null }))} style={{ width: '100%', padding: '.5rem', border: '1px solid #cbd5e1', borderRadius: 6 }} /></div>
              </div>
              <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '.5rem' }}><input type="checkbox" id="obr" checked={form.obrigatorio} onChange={e => setForm(p => ({ ...p, obrigatorio: e.target.checked }))} /><label htmlFor="obr" style={{ fontSize: '.9rem' }}>Obrigatório</label></div>
              <div style={{ display: 'flex', gap: '.5rem' }}>
                <button type="submit" disabled={saving} style={{ padding: '.5rem 1rem', background: '#7c3aed', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 600 }}>{saving ? 'Salvando...' : 'Salvar'}</button>
                <button type="button" onClick={() => setShowForm(false)} style={{ padding: '.5rem 1rem', background: '#e2e8f0', border: 'none', borderRadius: 6, cursor: 'pointer' }}>Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
