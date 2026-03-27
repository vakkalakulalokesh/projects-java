import { useCallback, useMemo, type MouseEvent as ReactMouseEvent } from 'react'
import ReactFlow, {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  type Connection,
  type Edge,
  type Node,
  type OnConnect,
  type OnEdgesChange,
  type OnNodesChange,
} from 'reactflow'
import 'reactflow/dist/style.css'
import type { StepNodeData } from './flowGraph'
import { StepNode } from './StepNode'
import './FlowCanvas.css'

const nodeTypes = { stepNode: StepNode }

export interface FlowCanvasProps {
  nodes: Node<StepNodeData>[]
  edges: Edge[]
  onNodesChange: OnNodesChange
  onEdgesChange: OnEdgesChange
  onConnect: OnConnect
  onNodeClick: (_: ReactMouseEvent, node: Node<StepNodeData>) => void
  onPaneClick: () => void
  onDragOver: (e: React.DragEvent) => void
  onDrop: (e: React.DragEvent) => void
}

export function FlowCanvas({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  onConnect,
  onNodeClick,
  onPaneClick,
  onDragOver,
  onDrop,
}: FlowCanvasProps) {
  const defaultEdgeOptions = useMemo(
    () => ({
      animated: true,
      style: { stroke: '#94a3b8', strokeWidth: 2 },
    }),
    [],
  )

  const isValidConnection = useCallback((connection: Connection) => {
    return !!connection.source && !!connection.target && connection.source !== connection.target
  }, [])

  return (
    <div className="flow-canvas-wrap">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={onNodeClick}
        onPaneClick={onPaneClick}
        onDragOver={onDragOver}
        onDrop={onDrop}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        minZoom={0.2}
        maxZoom={1.5}
        defaultEdgeOptions={defaultEdgeOptions}
        connectionLineStyle={{ stroke: '#6366f1', strokeWidth: 2 }}
        isValidConnection={isValidConnection}
        proOptions={{ hideAttribution: true }}
      >
        <Background variant={BackgroundVariant.Dots} gap={18} size={1} color="#cbd5e1" />
        <Controls className="flow-controls" />
        <MiniMap
          className="flow-minimap"
          nodeStrokeWidth={3}
          zoomable
          pannable
          maskColor="rgba(15, 23, 42, 0.08)"
        />
      </ReactFlow>
    </div>
  )
}

