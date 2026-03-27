import { NavLink } from 'react-router-dom'
import { useState } from 'react'
import './Sidebar.css'

const NAV = [
  { to: '/', label: 'Dashboard', icon: '◆' },
  { to: '/flows/new', label: 'Flow Builder', icon: '⬡' },
  { to: '/library', label: 'Flow Library', icon: '▤' },
  { to: '/executions', label: 'Executions', icon: '◎' },
  { to: '/circuit-breakers', label: 'Circuit Breakers', icon: '◉' },
]

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <aside className={`sidebar ${collapsed ? 'sidebar--collapsed' : ''}`}>
      <div className="sidebar-brand">
        <span className="sidebar-logo" aria-hidden>
          ⬢
        </span>
        {!collapsed && (
          <div className="sidebar-brand-text">
            <span className="sidebar-title">API Gateway</span>
            <span className="sidebar-sub">Orchestration</span>
          </div>
        )}
      </div>

      <nav className="sidebar-nav" aria-label="Main">
        {NAV.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) => `sidebar-link ${isActive ? 'sidebar-link--active' : ''}`}
            isActive={item.to === '/flows/new' ? (_match, loc) => loc.pathname.startsWith('/flows') : undefined}
          >
            <span className="sidebar-link-icon" aria-hidden>
              {item.icon}
            </span>
            {!collapsed && <span className="sidebar-link-label">{item.label}</span>}
          </NavLink>
        ))}
      </nav>

      <button
        type="button"
        className="sidebar-collapse btn btn-ghost"
        onClick={() => setCollapsed((c) => !c)}
        aria-expanded={!collapsed}
        aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
      >
        {collapsed ? '⟩' : '⟨'}
      </button>
    </aside>
  )
}
