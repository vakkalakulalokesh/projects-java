import { useNavigate } from 'react-router-dom'
import './Header.css'

export interface HeaderProps {
  title: string
}

export function Header({ title }: HeaderProps) {
  const navigate = useNavigate()

  return (
    <header className="header">
      <h1 className="page-title header-title">{title}</h1>
      <div className="header-actions">
        <input
          type="search"
          className="header-search input"
          placeholder="Search flows, executions…"
          aria-label="Search"
        />
        <button type="button" className="btn btn-primary btn-sm" onClick={() => navigate('/flows/new')}>
          New Flow
        </button>
        <button type="button" className="btn btn-sm" onClick={() => navigate('/library')}>
          Trigger
        </button>
      </div>
    </header>
  )
}
