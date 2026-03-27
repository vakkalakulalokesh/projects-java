import { useCallback, useEffect, useMemo, useState, type DragEvent, type MouseEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  addEdge,
  ReactFlowProvider,
  useEdgesState,
  useNodesState,
  useReactFlow,
  type Connection,
  type Edge,
  type Node,
  type OnEdgesChange,
  type OnNodesChange,
} from 'reactflow'
import { ApiError, createFlow, getFlow, triggerFlow, updateFlow, validateFlow } from '../../services/api'
import { StepType, type FlowDefinition } from '../../types/flow'
import {
  createStep,
  flowToNodesAndEdges,
  newFlowDefinition,
  nodesAndEdgesToFlow,
  randomId,
  type StepNodeData,
} from './flowGraph'
import { FlowCanvas } from './FlowCanvas'
import { StepConfigPanel } from './StepConfigPanel'
import './FlowBuilder.css'

const DND_TYPE = 'application/gateway-step-type'

type CanvasAreaProps = {
  nodes: Node<StepNodeData>[]
  edges: Edge[]
  onNodesChange: OnNodesChange<Node<StepNodeData>>
  onEdgesChange: OnEdgesChange
  onConnect: (c: Connection) => void
  onNodeClick: (e: MouseEvent, node: Node<StepNodeData>) => void
  onPaneClick: () => void
  setNodes: (updater: Node<StepNodeData>[] | ((n: Node<StepNodeData>[]) => Node<StepNodeData>[])) => void
  setSelectedId: (id: string | null) => void
}

function FlowCanvasArea({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  onConnect,
  onNodeClick,
  onPaneClick,
  setNodes,
  setSelectedId,
}: CanvasAreaProps) {
  const { screenToFlowPosition } = useReactFlow()

  const onDragOver = useCallback((e: DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }, [])

  const onDrop = useCallback(
    (e: DragEvent) => {
      e.preventDefault()
      const type = e.dataTransfer.getData(DND_TYPE) as StepType
      if (!type || !Object.values(StepType).includes(type)) return
      const pos = screenToFlowPosition({ x: e.clientX, y: e.clientY })
      const sid = randomId('step')
      const step = createStep(type, sid)
      const newNode: Node<StepNodeData> = {
        id: sid,
        type: 'stepNode',
        position: pos,
        data: { step },
      }
      setNodes((nds) => nds.concat(newNode))
      setSelectedId(sid)
    },
    [screenToFlowPosition, setNodes, setSelectedId],
  )

  return (
    <FlowCanvas
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
      onNodeClick={onNodeClick}
      onPaneClick={onPaneClick}
      onDragOver={onDragOver}
      onDrop={onDrop}
    />
  )
}

function FlowBuilderInner() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isNew = !id || id === 'new'

  const [flowName, setFlowName] = useState('Untitled flow')
  const [flowDescription, setFlowDescription] = useState('')
  const [flowId, setFlowId] = useState<string | null>(isNew ? null : id ?? null)
  const [flowMeta, setFlowMeta] = useState<Partial<FlowDefinition>>({})
  const [nodes, setNodes, onNodesChange] = useNodesState<Node<StepNodeData>>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [validation, setValidation] = useState<string[]>([])
  const [statusMsg, setStatusMsg] = useState<string | null>(null)
  const [loading, setLoading] = useState(!isNew)

  const loadFlow = useCallback(async () => {
    if (isNew || !id) {
      const def = newFlowDefinition()
      setFlowName(def.name)
      setFlowDescription(def.description ?? '')
      const { nodes: n, edges: e } = flowToNodesAndEdges(def)
      setNodes(n)
      setEdges(e)
      setLoading(false)
      return
    }
    setLoading(true)
    try {
      const res = await getFlow(id)
      setFlowId(res.id)
      setFlowName(res.name)
      setFlowDescription(res.description ?? '')
      setFlowMeta({
        version: res.version,
        status: res.status,
        tags: res.tags,
        id: res.id,
      })
      const { nodes: n, edges: e } = flowToNodesAndEdges(res)
      setNodes(n)
      setEdges(e)
      setStatusMsg(null)
    } catch (err) {
      const a = err as ApiError
      setStatusMsg(a.message ?? 'Failed to load flow')
    } finally {
      setLoading(false)
    }
  }, [id, isNew, setEdges, setNodes])

  useEffect(() => {
    void loadFlow()
  }, [loadFlow])

  const selectedStep = useMemo(() => {
    if (!selectedId) return null
    const n = nodes.find((x) => x.id === selectedId)
    return n?.data.step ?? null
  }, [nodes, selectedId])

  const otherSteps = useMemo(
    () =>
      nodes
        .filter((n) => n.id !== selectedId)
        .map((n) => ({ id: n.id, name: n.data.step.name })),
    [nodes, selectedId],
  )

  const onConnect = useCallback(
    (params: Connection) =>
      setEdges((eds) =>
        addEdge(
          {
            ...params,
            id: randomId('edge'),
            animated: true,
            style: { stroke: '#94a3b8', strokeWidth: 2 },
          },
          eds,
        ),
      ),
    [setEdges],
  )

  const onNodeClick = useCallback((_: MouseEvent, node: Node<StepNodeData>) => {
    setSelectedId(node.id)
  }, [])

  const onPaneClick = useCallback(() => setSelectedId(null), [])

  const buildDefinition = useCallback((): FlowDefinition => {
    return nodesAndEdgesToFlow(flowName, flowDescription, nodes, edges, flowMeta)
  }, [edges, flowDescription, flowMeta, flowName, nodes])

  const handleSave = async () => {
    setStatusMsg(null)
    setValidation([])
    const def = buildDefinition()
    try {
      if (flowId) {
        await updateFlow(flowId, {
          name: def.name,
          description: def.description,
          definition: {
            name: def.name,
            description: def.description,
            steps: def.steps,
            edges: def.edges,
            tags: def.tags,
            metadata: def.metadata,
            version: def.version,
          },
        })
        setStatusMsg('Saved')
      } else {
        const created = await createFlow({
          name: def.name,
          description: def.description,
          definition: {
            name: def.name,
            description: def.description,
            steps: def.steps,
            edges: def.edges,
            tags: def.tags,
            metadata: def.metadata,
          },
        })
        setFlowId(created.id)
        setFlowMeta((m) => ({ ...m, id: created.id }))
        navigate(`/flows/${created.id}`, { replace: true })
        setStatusMsg('Created')
      }
    } catch (err) {
      const a = err as ApiError
      setStatusMsg(a.message ?? 'Save failed')
    }
  }

  const handleValidate = async () => {
    setValidation([])
    const def = buildDefinition()
    try {
      const res = await validateFlow(flowId ?? 'draft', def)
      setValidation([...(res.errors ?? []), ...(res.warnings?.map((w) => `⚠ ${w}`) ?? [])])
      if (res.valid && (!res.errors || res.errors.length === 0)) {
        setValidation(['Flow definition is valid'])
      }
    } catch (err) {
      const a = err as ApiError
      setValidation([a.message ?? 'Validation request failed'])
    }
  }

  const handleTrigger = async () => {
    if (!flowId) {
      setStatusMsg('Save the flow before triggering')
      return
    }
    try {
      const ex = await triggerFlow(flowId, {})
      navigate(`/executions/${ex.id}`)
    } catch (err) {
      const a = err as ApiError
      setStatusMsg(a.message ?? 'Trigger failed')
    }
  }

  const applyStep = useCallback(
    (step: StepNodeData['step']) => {
      setNodes((nds) =>
        nds.map((n) =>
          n.id === step.id
            ? {
                ...n,
                data: { step },
              }
            : n,
        ),
      )
      setSelectedId(null)
    },
    [setNodes],
  )

  if (loading) {
    return <div className="flow-builder-loading">Loading flow…</div>
  }

  return (
    <div className="flow-builder">
      <div className="flow-builder-toolbar card">
        <input
          className="input flow-builder-name"
          value={flowName}
          onChange={(e) => setFlowName(e.target.value)}
          placeholder="Flow name"
          aria-label="Flow name"
        />
        <input
          className="input flow-builder-desc"
          value={flowDescription}
          onChange={(e) => setFlowDescription(e.target.value)}
          placeholder="Description"
          aria-label="Flow description"
        />
        <div className="flow-builder-toolbar-actions">
          <button type="button" className="btn btn-primary btn-sm" onClick={() => void handleSave()}>
            Save
          </button>
          <button type="button" className="btn btn-sm" onClick={() => void handleValidate()}>
            Validate
          </button>
          <button type="button" className="btn btn-sm" onClick={() => void handleTrigger()}>
            Trigger
          </button>
        </div>
        {statusMsg && <span className="flow-builder-status">{statusMsg}</span>}
      </div>

      <div className="flow-builder-body">
        <aside className="flow-palette card">
          <h3 className="flow-palette-title">Steps</h3>
          <p className="flow-palette-hint u-text-secondary">Drag onto canvas</p>
          <div className="flow-palette-list">
            {(Object.keys(StepType) as (keyof typeof StepType)[]).map((k) => {
              const t = StepType[k]
              return (
                <div
                  key={t}
                  className="flow-palette-item"
                  draggable
                  onDragStart={(e) => {
                    e.dataTransfer.setData(DND_TYPE, t)
                    e.dataTransfer.effectAllowed = 'move'
                  }}
                >
                  <span className="flow-palette-badge">{t}</span>
                </div>
              )
            })}
          </div>
        </aside>

        <div className="flow-builder-center">
          <FlowCanvasArea
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            onPaneClick={onPaneClick}
            setNodes={setNodes}
            setSelectedId={setSelectedId}
          />
          <div className="flow-validation card">
            <div className="flow-validation-head">Validation</div>
            {validation.length === 0 ? (
              <p className="flow-validation-empty u-text-secondary">Run validate to see messages</p>
            ) : (
              <ul className="flow-validation-list">
                {validation.map((v, i) => (
                  <li key={`${i}-${v}`}>{v}</li>
                ))}
              </ul>
            )}
          </div>
        </div>

        <StepConfigPanel
          step={selectedStep}
          otherStepIds={otherSteps}
          onApply={applyStep}
          onClose={() => setSelectedId(null)}
        />
      </div>
    </div>
  )
}

export function FlowBuilder() {
  return (
    <ReactFlowProvider>
      <FlowBuilderInner />
    </ReactFlowProvider>
  )
}
