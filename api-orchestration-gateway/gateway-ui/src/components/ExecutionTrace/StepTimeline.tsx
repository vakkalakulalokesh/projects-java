import { useState } from 'react'
import type { StepExecutionResponse } from '../../types/execution'
import { formatDuration, formatJSON } from '../../utils/formatters'
import { StatusBadge } from '../shared/StatusBadge'
import './StepTimeline.css'

const STEP_ICONS: Record<string, string> = {
  HTTP_CALL: '⇄',
  TRANSFORM: '↻',
  CONDITION: '◇',
  DELAY: '⏱',
  AGGREGATE: '⊕',
  SCRIPT: '{ }',
}

export interface StepTimelineProps {
  steps: StepExecutionResponse[]
}

export function StepTimeline({ steps }: StepTimelineProps) {
  const [openIo, setOpenIo] = useState<Record<string, boolean>>({})

  const toggleIo = (id: string) => {
    setOpenIo((o) => ({ ...o, [id]: !o[id] }))
  }

  const ordered = [...steps].sort((a, b) => {
    if (a.compensation && !b.compensation) return 1
    if (!a.compensation && b.compensation) return -1
    return 0
  })

  return (
    <ol className="step-timeline">
      {ordered.map((step, idx) => {
        const lineDone = step.status === 'COMPLETED' || step.status === 'FAILED' || step.status === 'SKIPPED'
        const icon = STEP_ICONS[step.stepType] ?? '•'
        const isComp = !!step.compensation
        return (
          <li
            key={`${step.stepId}-${idx}`}
            className={`step-timeline-item ${isComp ? 'step-timeline-item--comp' : ''} ${lineDone ? 'step-timeline-item--done' : ''}`}
          >
            <div className="step-timeline-rail">
              <span className={`step-timeline-line ${lineDone ? 'step-timeline-line--solid' : 'step-timeline-line--dashed'}`} />
              <span className={`step-timeline-dot ${step.status === 'RUNNING' ? 'step-timeline-dot--pulse' : ''}`} />
            </div>
            <div className="step-timeline-card card">
              <div className="step-timeline-card-head">
                <span className="step-timeline-icon" aria-hidden>
                  {icon}
                </span>
                <div className="step-timeline-titles">
                  <span className="step-timeline-name">{step.stepName}</span>
                  <span className="step-timeline-type">{step.stepType}</span>
                </div>
                <StatusBadge status={step.status} size="sm" />
                <span className="step-timeline-duration">{formatDuration(step.durationMs)}</span>
              </div>
              {step.attempt != null && step.maxAttempts != null && (
                <div className="step-timeline-retry">
                  Attempt {step.attempt} of {step.maxAttempts}
                </div>
              )}
              {step.errorMessage && <div className="step-timeline-error">{step.errorMessage}</div>}
              <div className="step-timeline-io">
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => toggleIo(`${step.stepId}-io`)}>
                  Input / output {openIo[`${step.stepId}-io`] ? '▲' : '▼'}
                </button>
                {openIo[`${step.stepId}-io`] && (
                  <div className="step-timeline-json">
                    <div>
                      <span className="step-timeline-json-label">Input</span>
                      <pre>{formatJSON(step.inputData ?? {})}</pre>
                    </div>
                    <div>
                      <span className="step-timeline-json-label">Output</span>
                      <pre>{formatJSON(step.outputData ?? {})}</pre>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
