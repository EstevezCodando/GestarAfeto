import { Routes, Route, NavLink } from 'react-router-dom'
import GestantesPage from './pages/GestantesPage'
import ProcedimentosPage from './pages/ProcedimentosPage'
import ChecklistGestantePage from './pages/ChecklistGestantePage'
import AlertasPage from './pages/AlertasPage'

export default function App() {
  return (
    <div>
      <header style={{ background: '#7c3aed', color: 'white', padding: '1rem 2rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>GestarAfeto</h1>
        <p style={{ fontSize: '.85rem', opacity: .85 }}>Acompanhamento Organizacional do Pré-Natal</p>
      </header>

      <div style={{ background: '#fef3c7', borderLeft: '4px solid #f59e0b', padding: '.75rem 2rem', fontSize: '.85rem' }}>
        ⚠️ <strong>Aviso:</strong> Esta ferramenta é de apoio organizacional e <strong>não substitui</strong> acompanhamento médico profissional.
      </div>

      <nav style={{ background: 'white', borderBottom: '1px solid #e2e8f0', padding: '0 2rem', display: 'flex' }}>
        {[
          { to: '/', label: 'Gestantes' },
          { to: '/procedimentos', label: 'Procedimentos' },
          { to: '/checklist', label: 'Checklist' },
          { to: '/alertas', label: 'Alertas' },
        ].map(({ to, label }) => (
          <NavLink key={to} to={to} end={to === '/'} style={({ isActive }) => ({
            padding: '.75rem 1.25rem', textDecoration: 'none', fontSize: '.9rem',
            color: isActive ? '#7c3aed' : '#64748b',
            borderBottom: isActive ? '3px solid #7c3aed' : '3px solid transparent',
          })}>
            {label}
          </NavLink>
        ))}
      </nav>

      <main style={{ padding: '1.5rem 2rem', maxWidth: '1100px', margin: '0 auto' }}>
        <Routes>
          <Route path="/" element={<GestantesPage />} />
          <Route path="/procedimentos" element={<ProcedimentosPage />} />
          <Route path="/checklist" element={<ChecklistGestantePage />} />
          <Route path="/alertas" element={<AlertasPage />} />
        </Routes>
      </main>
    </div>
  )
}
