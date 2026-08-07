import { useEffect, useRef } from 'preact/hooks'
import { useSignal } from '@preact/signals'
import { evaluate as feel_evaluate } from '@bpmn-io/feelin'

/**
 * Renders a Camunda Forms (form-js) JSON schema as plain HTML form fields
 * that obey the project's global `form { display: grid; ... }` layout.
 *
 * @param schema     form-js JSON schema (object with `components` array)
 * @param data       initial form data ({ key: value })
 * @param disabled   render read-only
 * @param on_submit  ({ data, errors }) => void
 * @param on_ready   (controls) => void — exposes { submit() }
 */
export const CamundaForm = ({ schema, data, disabled, on_submit, on_ready }) => {
  const form_data = useSignal(data ?? {})
  const form_ref = useRef(null)

  // Reset state whenever the schema changes (i.e. switching tasks).
  useEffect(() => {
    form_data.value = { ...(data ?? {}) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [schema])

  const set = (key, value) => {
    form_data.value = { ...form_data.value, [key]: value }
  }

  const submit = () => {
    const errors = collect_errors(schema, form_data.value)
    on_submit?.({ data: form_data.value, errors })
  }

  useEffect(() => {
    on_ready?.({ submit })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!schema || !Array.isArray(schema.components)) return null

  return (
    <form
      ref={form_ref}
      class="camunda-form"
      onSubmit={(e) => { e.preventDefault(); submit() }}
    >
      {schema.components.map((c) => {
        if (is_hidden(c, form_data.value)) return null
        return (
          <Component
            key={c.id ?? c.key}
            component={c}
            value={form_data.value[c.key]}
            on_change={(v) => set(c.key, v)}
            disabled={disabled || feel_or_value(c.disabled, form_data.value, false)}
          />
        )
      })}
    </form>
  )
}

const Component = ({ component: c, value, on_change, disabled }) => {
  const is_disabled = disabled || c.disabled
  const required = !!c.validate?.required

  switch (c.type) {
    case 'text':
      return <FullWidth><Markdown md={c.text} /></FullWidth>

    case 'spacer':
      return <FullWidth><div class="form-spacer" style="height: var(--spacing-1)" /></FullWidth>

    case 'separator':
      return <FullWidth><hr /></FullWidth>

    case 'textfield':
      return (
        <Field c={c} required={required}>
          <input
            id={c.id}
            type="text"
            value={value ?? ''}
            disabled={is_disabled}
            required={required}
            maxLength={c.validate?.maxLength}
            minLength={c.validate?.minLength}
            pattern={c.validate?.pattern}
            placeholder={c.appearance?.prefixAdorner ?? ''}
            onInput={(e) => on_change(e.currentTarget.value)}
          />
        </Field>
      )

    case 'number':
      return (
        <Field c={c} required={required}>
          <input
            id={c.id}
            type="number"
            value={value ?? ''}
            disabled={is_disabled}
            required={required}
            min={c.validate?.min}
            max={c.validate?.max}
            step={c.increment ?? 'any'}
            onInput={(e) => {
              const v = e.currentTarget.value
              on_change(v === '' ? null : Number(v))
            }}
          />
        </Field>
      )

    case 'textarea':
      return (
        <Field c={c} required={required}>
          <textarea
            id={c.id}
            value={value ?? ''}
            disabled={is_disabled}
            required={required}
            rows={c.appearance?.rows ?? 3}
            maxLength={c.validate?.maxLength}
            onInput={(e) => on_change(e.currentTarget.value)}
          />
        </Field>
      )

    case 'checkbox':
      return (
        <Field c={c} required={required}>
          <input
            id={c.id}
            type="checkbox"
            checked={!!value}
            disabled={is_disabled}
            onChange={(e) => on_change(e.currentTarget.checked)}
          />
        </Field>
      )

    case 'radio':
      return (
        <Field c={c} required={required}>
          <div class="form-radio-group">
            {(c.values ?? []).map((v) => (
              <label key={v.value} class="form-inline-label">
                <input
                  type="radio"
                  name={c.key}
                  value={v.value}
                  checked={value === v.value}
                  disabled={is_disabled}
                  required={required}
                  onChange={() => on_change(v.value)}
                />
                {v.label}
              </label>
            ))}
          </div>
        </Field>
      )

    case 'select':
      return (
        <Field c={c} required={required}>
          <select
            id={c.id}
            value={value ?? ''}
            disabled={is_disabled}
            required={required}
            onChange={(e) => on_change(e.currentTarget.value || null)}
          >
            <option value="">—</option>
            {(c.values ?? []).map((v) => (
              <option key={v.value} value={v.value}>{v.label}</option>
            ))}
          </select>
        </Field>
      )

    case 'checklist':
      return (
        <Field c={c} required={required}>
          <div class="form-checklist">
            {(c.values ?? []).map((v) => {
              const checked = Array.isArray(value) && value.includes(v.value)
              return (
                <label key={v.value} class="form-inline-label">
                  <input
                    type="checkbox"
                    name={c.key}
                    value={v.value}
                    checked={checked}
                    disabled={is_disabled}
                    onChange={(e) => {
                      const arr = new Set(Array.isArray(value) ? value : [])
                      if (e.currentTarget.checked) arr.add(v.value)
                      else arr.delete(v.value)
                      on_change([...arr])
                    }}
                  />
                  {v.label}
                </label>
              )
            })}
          </div>
        </Field>
      )

    case 'datetime': {
      const sub = c.subtype ?? 'datetime'
      const input_type =
        sub === 'date' ? 'date' :
        sub === 'time' ? 'time' :
        'datetime-local'
      return (
        <Field c={c} required={required}>
          <input
            id={c.id}
            type={input_type}
            value={value ?? ''}
            disabled={is_disabled}
            required={required}
            onInput={(e) => on_change(e.currentTarget.value)}
          />
        </Field>
      )
    }

    default:
      return (
        <FullWidth>
          <small class="form-unsupported">Unsupported field: {c.type}</small>
        </FullWidth>
      )
  }
}

// A field is a label in column 1 + an input cell in column 2 — the global
// `form { display: grid; grid-template-columns: 12em … }` rule handles flow.
const Field = ({ c, required, children }) => (
  <>
    <label for={c.id}>
      {c.label}{required && <span class="form-required-mark">*</span>}
    </label>
    <div class="form-input-cell">{children}</div>
  </>
)

// For elements that should span both columns (text, separators, etc.).
const FullWidth = ({ children }) => (
  <div class="form-fullspan">{children}</div>
)

// Tiny markdown subset: paragraphs separated by blank lines, leading
// `#`/`##`/`###` for headings. No inline formatting; that covers our
// dev-fixture forms without dragging in a markdown library.
const Markdown = ({ md }) => {
  const blocks = (md ?? '')
    .split(/\n\s*\n/)
    .map((b) => b.trim())
    .filter(Boolean)
  return (
    <>
      {blocks.map((b, i) => {
        if (b.startsWith('### ')) return <h3 key={i}>{b.slice(4)}</h3>
        if (b.startsWith('## ')) return <h2 key={i}>{b.slice(3)}</h2>
        if (b.startsWith('# ')) return <h1 key={i}>{b.slice(2)}</h1>
        return (
          <p key={i}>
            {b.split('\n').map((line, j, arr) => (
              <span key={j}>{line}{j < arr.length - 1 && <br />}</span>
            ))}
          </p>
        )
      })}
    </>
  )
}

// Required-field validation. Hidden fields and disabled fields are skipped.
const collect_errors = (schema, data) => {
  const errors = {}
  for (const c of schema.components ?? []) {
    if (!c.key) continue
    if (is_hidden(c, data)) continue
    if (feel_or_value(c.disabled, data, false)) continue
    const required = feel_or_value(c.validate?.required, data, false)
    if (required) {
      const v = data[c.key]
      const empty =
        v === undefined ||
        v === null ||
        v === '' ||
        (Array.isArray(v) && v.length === 0)
      if (empty) errors[c.key] = ['required']
    }
  }
  return errors
}

// FEEL helpers ----------------------------------------------------------------

// Evaluate a FEEL expression. In form-js, FEEL expressions are strings that
// start with `=`. Anything else is a literal value. Returns `fallback` on
// parse/eval errors so a broken expression never crashes the form.
const eval_feel = (expr, context, fallback = undefined) => {
  if (typeof expr !== 'string' || !expr.startsWith('=')) return undefined
  try {
    const { value } = feel_evaluate(expr.slice(1), context ?? {})
    return value
  } catch {
    return fallback
  }
}

// For props that may be either a literal value or a FEEL expression
// (e.g. `disabled`, `validate.required`).
const feel_or_value = (raw, context, fallback) => {
  if (typeof raw === 'string' && raw.startsWith('=')) {
    const v = eval_feel(raw, context, fallback)
    return v ?? fallback
  }
  return raw ?? fallback
}

const is_hidden = (component, data) => {
  const expr = component.conditional?.hide
  if (expr === undefined || expr === null) return false
  if (typeof expr === 'boolean') return expr
  return !!eval_feel(expr, data, false)
}
