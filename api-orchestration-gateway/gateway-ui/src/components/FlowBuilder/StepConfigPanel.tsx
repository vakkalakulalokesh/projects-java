import { useEffect, useState } from 'react'
import { StepType, type RetryConfig, type StepDefinition } from '../../types/flow'
import './StepConfigPanel.css'

export interface StepConfigPanelProps {
  step: StepDefinition | null
  otherStepIds: { id: string; name: string }[]
  onApply: (step: StepDefinition) => void
  onClose: () => void
}

function headersToLines(h: Record<string, string> | undefined): string {
  if (!h) return ''
  return Object.entries(h)
    .map(([k, v]) => `${k}: ${v}`)
    .join('\n')
}

function linesToHeaders(text: string): Record<string, string> {
  const out: Record<string, string> = {}
  text.split('\n').forEach((line) => {
    const idx = line.indexOf(':')
    if (idx > 0) {
      const k = line.slice(0, idx).trim()
      const v = line.slice(idx + 1).trim()
      if (k) out[k] = v
    }
  })
  return out
}

function recordToLines(r: Record<string, string> | undefined): string {
  if (!r) return ''
  return Object.entries(r)
    .map(([k, v]) => `${k}=${v}`)
    .join('\n')
}

function linesToRecord(text: string): Record<string, string> {
  const out: Record<string, string> = {}
  text.split('\n').forEach((line) => {
    const idx = line.indexOf('=')
    if (idx > 0) {
      const k = line.slice(0, idx).trim()
      const v = line.slice(idx + 1).trim()
      if (k) out[k] = v
    }
  })
  return out
}

export function StepConfigPanel({ step, otherStepIds, onApply, onClose }: StepConfigPanelProps) {
  const [draft, setDraft] = useState<StepDefinition | null>(step)
  const [headersText, setHeadersText] = useState('')
  const [extractorsText, setExtractorsText] = useState('')
  const [mappingsText, setMappingsText] = useState('')
  const [compOpen, setCompOpen] = useState(false)

  useEffect(() => {
    setDraft(step)
    if (!step) {
      setHeadersText('')
      setExtractorsText('')
      setMappingsText('')
      return
    }
    if (step.type === StepType.HTTP_CALL) {
      const c = step.config as { headers?: Record<string, string>; responseExtractors?: Record<string, string> }
      setHeadersText(headersToLines(c.headers))
      setExtractorsText(recordToLines(c.responseExtractors))
    }
    if (step.type === StepType.TRANSFORM) {
      const c = step.config as { mappings?: Record<string, string> }
      setMappingsText(recordToLines(c.mappings))
    }
  }, [step])

  if (!step || !draft) {
    return (
      <aside className="step-panel step-panel--empty">
        <p className="u-text-secondary">Select a step to configure</p>
      </aside>
    )
  }

  const updateConfig = (partial: Record<string, unknown>) => {
    setDraft({ ...draft, config: { ...draft.config, ...partial } })
  }

  const handleSave = () => {
    let config: StepDefinition['config'] = { ...draft.config }
    if (draft.type === StepType.HTTP_CALL) {
      config = {
        ...config,
        headers: linesToHeaders(headersText),
        responseExtractors: linesToRecord(extractorsText),
      }
    }
    if (draft.type === StepType.TRANSFORM) {
      config = { ...config, mappings: linesToRecord(mappingsText) }
    }
    onApply({ ...draft, config })
  }

  const handleCancel = () => {
    setDraft(step)
    onClose()
  }

  return (
    <aside className="step-panel">
      <div className="step-panel-head">
        <h2 className="step-panel-title">Step config</h2>
        <button type="button" className="btn btn-ghost btn-sm" onClick={onClose} aria-label="Close panel">
          ×
        </button>
      </div>

      <div className="step-panel-scroll">
        <label className="label" htmlFor="step-name">
          Name
        </label>
        <input
          id="step-name"
          className="input u-mt-sm"
          value={draft.name}
          onChange={(e) => setDraft({ ...draft, name: e.target.value })}
        />

        {draft.type === StepType.HTTP_CALL && (
          <div className="step-panel-section">
            <label className="label">URL</label>
            <input
              className="input u-mt-sm"
              value={(draft.config as { url?: string }).url ?? ''}
              onChange={(e) => updateConfig({ url: e.target.value })}
            />
            <label className="label u-mt-md">Method</label>
            <select
              className="input u-mt-sm"
              value={(draft.config as { method?: string }).method ?? 'GET'}
              onChange={(e) => updateConfig({ method: e.target.value })}
            >
              {(['GET', 'POST', 'PUT', 'DELETE', 'PATCH'] as const).map((m) => (
                <option key={m} value={m}>
                  {m}
                </option>
              ))}
            </select>
            <label className="label u-mt-md">Headers (one per line, Key: Value)</label>
            <textarea
              className="input step-panel-textarea u-mt-sm"
              rows={4}
              value={headersText}
              onChange={(e) => setHeadersText(e.target.value)}
            />
            <label className="label u-mt-md">Body (JSON)</label>
            <textarea
              className="input step-panel-textarea u-mt-sm"
              rows={5}
              value={(draft.config as { body?: string }).body ?? ''}
              onChange={(e) => updateConfig({ body: e.target.value })}
            />
            <label className="label u-mt-md">Response extractors (field=expr per line)</label>
            <textarea
              className="input step-panel-textarea u-mt-sm"
              rows={3}
              value={extractorsText}
              onChange={(e) => setExtractorsText(e.target.value)}
            />
          </div>
        )}

        {draft.type === StepType.TRANSFORM && (
          <div className="step-panel-section">
            <label className="label">Mappings (output=expression per line)</label>
            <textarea
              className="input step-panel-textarea u-mt-sm"
              rows={8}
              value={mappingsText}
              onChange={(e) => setMappingsText(e.target.value)}
            />
          </div>
        )}

        {draft.type === StepType.CONDITION && (
          <div className="step-panel-section">
            <label className="label">Expression</label>
            <input
              className="input u-mt-sm"
              value={(draft.config as { expression?: string }).expression ?? ''}
              onChange={(e) => updateConfig({ expression: e.target.value })}
            />
            <label className="label u-mt-md">On true → step</label>
            <select
              className="input u-mt-sm"
              value={(draft.config as { onTrueStepId?: string }).onTrueStepId ?? ''}
              onChange={(e) => updateConfig({ onTrueStepId: e.target.value })}
            >
              <option value="">—</option>
              {otherStepIds.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.id})
                </option>
              ))}
            </select>
            <label className="label u-mt-md">On false → step</label>
            <select
              className="input u-mt-sm"
              value={(draft.config as { onFalseStepId?: string }).onFalseStepId ?? ''}
              onChange={(e) => updateConfig({ onFalseStepId: e.target.value })}
            >
              <option value="">—</option>
              {otherStepIds.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.id})
                </option>
              ))}
            </select>
          </div>
        )}

        {draft.type === StepType.DELAY && (
          <div className="step-panel-section">
            <label className="label">Duration (ms)</label>
            <input
              type="number"
              className="input u-mt-sm"
              value={(draft.config as { durationMs?: number }).durationMs ?? 0}
              onChange={(e) => updateConfig({ durationMs: Number(e.target.value) })}
            />
          </div>
        )}

        {draft.type === StepType.AGGREGATE && (
          <div className="step-panel-section">
            <label className="label">Wait for steps</label>
            <div className="step-panel-checklist u-mt-sm">
              {otherStepIds.map((s) => {
                const ids = (draft.config as { waitForStepIds?: string[] }).waitForStepIds ?? []
                const checked = ids.includes(s.id)
                return (
                  <label key={s.id} className="step-panel-check">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => {
                        const next = checked ? ids.filter((x) => x !== s.id) : [...ids, s.id]
                        updateConfig({ waitForStepIds: next })
                      }}
                    />
                    <span>
                      {s.name} <span className="u-text-secondary">({s.id})</span>
                    </span>
                  </label>
                )
              })}
            </div>
            <label className="label u-mt-md">Merge strategy</label>
            <select
              className="input u-mt-sm"
              value={(draft.config as { mergeStrategy?: string }).mergeStrategy ?? 'MERGE_OBJECTS'}
              onChange={(e) => updateConfig({ mergeStrategy: e.target.value })}
            >
              <option value="MERGE_OBJECTS">Merge objects</option>
              <option value="ARRAY">Array</option>
              <option value="FIRST_WINS">First wins</option>
              <option value="LAST_WINS">Last wins</option>
            </select>
          </div>
        )}

        {draft.type === StepType.SCRIPT && (
          <div className="step-panel-section">
            <label className="label">Language</label>
            <select
              className="input u-mt-sm"
              value={(draft.config as { language?: string }).language ?? 'javascript'}
              onChange={(e) => updateConfig({ language: e.target.value })}
            >
              <option value="javascript">JavaScript</option>
              <option value="groovy">Groovy</option>
              <option value="python">Python</option>
            </select>
            <label className="label u-mt-md">Code</label>
            <textarea
              className="input step-panel-textarea step-panel-code u-mt-sm"
              rows={12}
              value={(draft.config as { code?: string }).code ?? ''}
              onChange={(e) => updateConfig({ code: e.target.value })}
            />
          </div>
        )}

        <div className="step-panel-section">
          <h3 className="step-panel-subtitle">Retry</h3>
          <div className="step-panel-row">
            <div>
              <label className="label">Max attempts</label>
              <input
                type="number"
                className="input u-mt-sm"
                value={draft.retry?.maxAttempts ?? 1}
                min={1}
                onChange={(e) =>
                  setDraft({
                    ...draft,
                    retry: {
                      maxAttempts: Number(e.target.value),
                      strategy: draft.retry?.strategy ?? 'EXPONENTIAL',
                      delayMs: draft.retry?.delayMs ?? 500,
                    },
                  })
                }
              />
            </div>
            <div>
              <label className="label">Strategy</label>
              <select
                className="input u-mt-sm"
                value={draft.retry?.strategy ?? 'EXPONENTIAL'}
                onChange={(e) =>
                  setDraft({
                    ...draft,
                    retry: {
                      maxAttempts: draft.retry?.maxAttempts ?? 3,
                      strategy: e.target.value as RetryConfig['strategy'],
                      delayMs: draft.retry?.delayMs ?? 500,
                    },
                  })
                }
              >
                <option value="FIXED">Fixed</option>
                <option value="EXPONENTIAL">Exponential</option>
                <option value="LINEAR">Linear</option>
              </select>
            </div>
            <div>
              <label className="label">Delay (ms)</label>
              <input
                type="number"
                className="input u-mt-sm"
                value={draft.retry?.delayMs ?? 0}
                min={0}
                onChange={(e) =>
                  setDraft({
                    ...draft,
                    retry: {
                      maxAttempts: draft.retry?.maxAttempts ?? 3,
                      strategy: draft.retry?.strategy ?? 'EXPONENTIAL',
                      delayMs: Number(e.target.value),
                    },
                  })
                }
              />
            </div>
          </div>
        </div>

        <div className="step-panel-section">
          <label className="label">Timeout (ms)</label>
          <input
            type="number"
            className="input u-mt-sm"
            value={draft.timeoutMs ?? 0}
            min={0}
            onChange={(e) => setDraft({ ...draft, timeoutMs: Number(e.target.value) })}
          />
        </div>

        <div className="step-panel-section">
          <button
            type="button"
            className="step-panel-collapse btn btn-ghost btn-sm"
            onClick={() => setCompOpen((o) => !o)}
          >
            Compensation {compOpen ? '▼' : '▶'}
          </button>
          {compOpen && (
            <div className="u-mt-md">
              <label className="step-panel-check">
                <input
                  type="checkbox"
                  checked={!!draft.compensation?.enabled}
                  onChange={(e) =>
                    setDraft({
                      ...draft,
                      compensation: { ...draft.compensation, enabled: e.target.checked },
                    })
                  }
                />
                <span>Enable compensation</span>
              </label>
              <label className="label u-mt-md">Compensation step</label>
              <select
                className="input u-mt-sm"
                value={draft.compensation?.stepId ?? ''}
                onChange={(e) =>
                  setDraft({
                    ...draft,
                    compensation: { ...draft.compensation, enabled: true, stepId: e.target.value },
                  })
                }
              >
                <option value="">—</option>
                {otherStepIds.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      </div>

      <div className="step-panel-footer">
        <button type="button" className="btn btn-primary" onClick={handleSave}>
          Save
        </button>
        <button type="button" className="btn" onClick={handleCancel}>
          Cancel
        </button>
      </div>
    </aside>
  )
}
