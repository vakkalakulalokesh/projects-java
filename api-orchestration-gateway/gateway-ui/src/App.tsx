import { Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { CircuitBreakers } from './components/CircuitBreakers/CircuitBreakers'
import { Dashboard } from './components/Dashboard/Dashboard'
import { ExecutionTrace } from './components/ExecutionTrace/ExecutionTrace'
import { FlowBuilder } from './components/FlowBuilder/FlowBuilder'
import { FlowLibrary } from './components/FlowLibrary/FlowLibrary'
import { Header } from './components/Layout/Header'
import { Sidebar } from './components/Layout/Sidebar'
import './App.css'

function titleForPath(pathname: string): string {
  if (pathname === '/') return 'Dashboard'
  if (pathname.startsWith('/flows')) return 'Flow Builder'
  if (pathname.startsWith('/library')) return 'Flow Library'
  if (pathname.startsWith('/executions')) return 'Executions'
  if (pathname.startsWith('/circuit-breakers')) return 'Circuit Breakers'
  return 'API Gateway'
}

function Layout() {
  const { pathname } = useLocation()
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-main">
        <Header title={titleForPath(pathname)} />
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/flows/:id" element={<FlowBuilder />} />
        <Route path="/library" element={<FlowLibrary />} />
        <Route path="/executions" element={<ExecutionTrace />} />
        <Route path="/executions/:id" element={<ExecutionTrace />} />
        <Route path="/circuit-breakers" element={<CircuitBreakers />} />
      </Route>
    </Routes>
  )
}
