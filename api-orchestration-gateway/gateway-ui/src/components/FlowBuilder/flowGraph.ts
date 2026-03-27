import type { Edge, Node } from 'reactflow'
import { StepType, type EdgeDefinition, type FlowDefinition, type StepDefinition } from '../../types/flow'

export type StepNodeData = {
  step: StepDefinition
}

const PALETTE_Y = 80

export function defaultPosition(index: number): { x: number; y: number } {
  return { x: 120 + (index % 4) * 240, y: PALETTE_Y + Math.floor(index / 4) * 140 }
}

export function defaultConfigForType(type: StepType): StepDefinition['config'] {
  switch (type) {
    case StepType.HTTP_CALL:
      return { url: 'https://', method: 'GET', headers: {}, body: '' }
    case StepType.TRANSFORM:
      return { mappings: {} }
    case StepType.CONDITION:
      return { expression: 'true', onTrueStepId: '', onFalseStepId: '' }
    case StepType.DELAY:
      return { durationMs: 1000 }
    case StepType.AGGREGATE:
      return { waitForStepIds: [], mergeStrategy: 'MERGE_OBJECTS' }
    case StepType.SCRIPT:
      return { language: 'javascript', code: '// script' }
    default:
      return {}
  }
}

export function createStep(type: StepType, id: string): StepDefinition {
  return {
    id,
    name: `${type.replace(/_/g, ' ')}`,
    type,
    config: defaultConfigForType(type),
    retry: { maxAttempts: 3, strategy: 'EXPONENTIAL', delayMs: 500 },
    timeoutMs: 30_000,
  }
}

function getPositions(flow: FlowDefinition): Record<string, { x: number; y: number }> {
  const meta = flow.metadata as { nodePositions?: Record<string, { x: number; y: number }> } | undefined
  return meta?.nodePositions ?? {}
}

export function flowToNodesAndEdges(flow: FlowDefinition): { nodes: Node<StepNodeData>[]; edges: Edge[] } {
  const positions = getPositions(flow)
  const nodes: Node<StepNodeData>[] = flow.steps.map((step, i) => ({
    id: step.id,
    type: 'stepNode',
    position: positions[step.id] ?? defaultPosition(i),
    data: { step },
  }))
  const edges: Edge[] = flow.edges.map((e) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    sourceHandle: e.sourceHandle ?? undefined,
    targetHandle: e.targetHandle ?? undefined,
    label: e.label,
    animated: true,
    style: { stroke: '#94a3b8', strokeWidth: 2 },
  }))
  return { nodes, edges }
}

export function nodesAndEdgesToFlow(
  name: string,
  description: string | undefined,
  nodes: Node<StepNodeData>[],
  edges: Edge[],
  extra?: Partial<FlowDefinition>,
): FlowDefinition {
  const nodePositions: Record<string, { x: number; y: number }> = {}
  nodes.forEach((n) => {
    nodePositions[n.id] = { x: n.position.x, y: n.position.y }
  })
  const steps: StepDefinition[] = nodes.map((n) => n.data.step)
  const edgeDefs: EdgeDefinition[] = edges.map((e) => ({
    id: e.id,
    source: e.source,
    target: e.target,
    sourceHandle: e.sourceHandle ?? null,
    targetHandle: e.targetHandle ?? null,
    label: typeof e.label === 'string' ? e.label : undefined,
  }))
  return {
    ...extra,
    name,
    description,
    steps,
    edges: edgeDefs,
    metadata: {
      ...(extra?.metadata ?? {}),
      nodePositions,
    },
  }
}

export function randomId(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

export function newFlowDefinition(): FlowDefinition {
  const a = createStep(StepType.HTTP_CALL, randomId('step'))
  return {
    name: 'Untitled flow',
    description: '',
    steps: [a],
    edges: [],
    metadata: { nodePositions: { [a.id]: defaultPosition(0) } },
  }
}
