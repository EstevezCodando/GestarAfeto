import { useState, useEffect } from 'react'
import { gestanteService } from '../services/gestanteService'
import GestanteCard from '../components/GestanteCard'
import type { Gestante, CriarGestanteRequest } from '../types/Gestante'

const emptyForm: CriarGestanteRequest = { nome: '', dataNascimento: null, telefone: null, email: null, dataUltimaMenstruacao: null, dataProvavelParto: null, observacoes: null }

export default function GestantesPage() {
  const [gestantes, setGestantes] = useState<Gestante[]>([])
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState<Gestante | null>(null)
  const [form, setForm] = useState<CriarGestanteRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const load = () => gestanteService.listar().then(setGestantes).catch(e => setError(e.message))

  useEffect(() => { load() }, [])

  const openNew = () => { setEditing(null); setForm(emptyForm); setError(null); setShowForm(true) }
  const openEdit = (g: Gestante) => {
    setEditing(g)
    setForm({ nome: g.nome, dataNascimento: g.dataNascimento, telefone: g.telefone, email: g.email, dataUltimaMenstruacao: g.dataUltimaMenstruacao, dataProvavelParto: g.dataProvavelParto, observacoes: g.observacoes })
    setError(null); setShowForm(true)
  }
  const del = async (id: number) => {
    if (!confirm('Remover gestante?')) return
    try { await gestanteService.remover(id); load() } catch (e: any) { alert(e.message) }
  }
  const save = async (e: React.FormEvent) => {
    e.preventDefault(); setSaving(true); setError(null)
    try {
      if (editing) await gestanteService.atualizar(editing.id, form)
      else await gestanteService.cadastrar(form)
      setShowForm(false); load()
    } catch (e: any) { setError(e.message) } finally { setSaving(false) }
  }
  const f = (k: keyof CriarGestanteRequest) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm(p => ({ ...p, [k]: e.target.value || null }))

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Gestantes</h2>
        <button onClick={openNew} style={{ padding: '.5rem 1rem', background: '#7c3aed', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 600 }}>+ Nova Gestante</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '1rem' }}>
        {gestantes.map(g => <GestanteCard key={g.id} gestante={g} onEdit={() => openEdit(g)} onDelete={() => del(g.id)} />)}
        {gestantes.length === 0 && <p style={{ color: '#64748b' }}>Nenhuma gestante cadastrada.</p>}
      </div>

      {showForm && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }} onClick={e => e.target === e.currentTarget && setShowForm(false)}>
          <div style={{ background: 'white', borderRadius: 10, padding: '1.5rem', width: '100%', maxWidth: 500, maxHeight: '90vh', overflowY: 'auto' }}>
            <h2 style={{ marginBottom: '1rem' }}>{editing ? 'Editar Gestante' : 'Nova Gestante'}</h2>
            {error && <div style={{ color: '#dc2626', background: '#fee2e2', padding: '.5rem', borderRadius: 4, marginBottom: '.75rem', fontSize: '.85rem' }}>{error}</div>}
            <form onSubmit={save}>
              {([['nome', 'Nome *', 'text'], ['dataNascimento', 'Data de Nascimento', 'date'], ['telefone', 'Telefone', 'text'], ['email', 'E-mail', 'email'], ['dataUltimaMenstruacao', 'Data da Última Menstruação', 'date'], ['dataProvavelParto', 'Data Provável do Parto', 'date']] as const).map(([k, label, type]) => (
                <div key={k} style={{ marginBottom: '1rem' }}>
                  <label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>{label}</label>
                  <input type={type} required={k === 'nome'} value={(form[k] as string) ?? ''} onChange={f(k)} style={{ width: '100%', padding: '.5rem .75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '.9rem' }} />
                </div>
              ))}
              <div style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '.85rem', fontWeight: 600, marginBottom: '.3rem' }}>Observações</label>
                <textarea value={form.observacoes ?? ''} onChange={f('observacoes')} style={{ width: '100%', padding: '.5rem .75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '.9rem', minHeight: 80 }} />
              </div>
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
