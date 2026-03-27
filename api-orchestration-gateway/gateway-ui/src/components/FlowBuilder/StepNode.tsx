import { memo } from 'react'
import { Handle, Position, type NodeProps } from 'reactflow'
import { StepType } from '../../types/flow'
import type { StepNodeData } from './flowGraph'
import './StepNode.css'

const TYPE_CLASS: Record<string, string> = {
  [StepType.HTTP_CALL]: 'step-node--http',
  [StepType.TRANSFORM]: 'step-node--transform',
  [StepType.CONDITION]: 'step-node--condition',
  [StepType.DELAY]: 'step-node--delay',
  [StepType.AGGREGATE]: 'step-node--aggregate',
  [StepType.SCRIPT]: 'step-node--script',
}

const TYPE_ICON: Record<string, string> = {
  [StepType.HTTP_CALL]: '⇄',
  [StepType.TRANSFORM]: '↻',
  [StepType.CONDITION]: '◇',
  [StepType.DELAY]: '⏱',
  [StepType.AGGREGATE]: '⊕',
  [StepType.SCRIPT]: '{ }',
}

function StepNodeComponent({ data, selected }: NodeProps<StepNodeData>) {
  const { step } = data
  const typeClass = TYPE_CLASS[step.type] ?? ''
  const icon = TYPE_ICON[step.type] ?? '•'

  return (
    <div className={`step-node ${typeClass} ${selected ? 'step-node--selected' : ''}`}>
      <Handle type="target" position={Position.Top} className="step-node-handle" />
      <div className="step-node-inner">
        <div className="step-node-icon" aria-hidden>
          {step.type === StepType.CONDITION ? <span className="step-node-diamond">{icon}</span> : icon}
        </div>
        <div className="step-node-body">
          <div className="step-node-name u-truncate" title={step.name}>
            {step.name}
          </div>
          <span className="step-node-type">{step.type}</span>
        </div>
      </div>
      <Handle type="source" position={Position.Bottom} className="step-node-handle" />
    </div>
  )
}

export const StepNode = memo(StepNodeComponent)
