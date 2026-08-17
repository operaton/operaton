import { useContext, useEffect, useRef, useState } from 'preact/hooks'
import { useTranslation } from 'react-i18next'
import { AppState } from '../state.js'
import { useSignal } from '@preact/signals'
import engine_rest, { RequestState, RESPONSE_STATE } from '../api/engine_rest.jsx'
import { useRoute, useLocation } from 'preact-iso'
import { CamundaForm } from '../components/CamundaForm.jsx'
import {
  form_data_to_vars,
  schema_variable_keys,
  vars_to_form_data,
  rendered_form_to_schema,
  rendered_form_variables,
  form_ref_of,
} from '../components/TaskForm_helpers.js'

const EMBEDDED_APP_PREFIX = 'embedded:app:'
const EMBEDDED_DEPLOYMENT_PREFIX = 'embedded:deployment:'

// A generated start form has no author-set form key: the engine reports it as
// `embedded:engine://.../rendered-form` (or, rarely, no key). Only the app/
// deployment embedded keys are the unsupported legacy AngularJS forms.
const is_legacy_start_key = (key) =>
  key != null &&
  (key.startsWith(EMBEDDED_APP_PREFIX) ||
    key.startsWith(EMBEDDED_DEPLOYMENT_PREFIX))
const FORM_JS_MIGRATION_DOCS =
  'https://docs.operaton.org/documentation/user-guide/task-forms/'

const StartProcessList = () => {
  const
    state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation()

  // Fetch in effects, not the component body — preact-iso params are
  // `undefined` when absent (never null), so guard with `!= null` (see #90).
  useEffect(() => {
    void engine_rest.process_definition.list_startable(state)
  }, [])

  useEffect(() => {
    if (params.tab != null) {
      void engine_rest.process_definition.one(state, params.tab)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.tab])

  return <div id="start-task">
    <div class="definitions">
      <StartableProcessesList />
    </div>
    <div class="start-form">
      {params.tab != null
        ? <StartProcessForm />
        : <p class="empty-state">{t("tasks.start-process.select-definition")}</p>}
    </div>
  </div>
}

const StartableProcessesList = () => {
  const
    state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    search_term = useSignal('')

  return <div>

    <div class="row space-between p-1">
      <h2>{t("tasks.start-process.title")}</h2>

      <input
        type="text"
        class="search-input"
        id="process-popup-search-input"
        aria-label={t("tasks.start-process.search-placeholder")}
        placeholder={t("tasks.start-process.search-placeholder")}
        value={search_term.value}
        onChange={(e) => (search_term.value = e.target.value)} />
    </div>
    <table>
      <thead>
      <tr>
        <th>{t("tasks.start-process.definition-name")}</th>
        <th title={t("tasks.start-process.version")}>
          {t("tasks.start-process.version-abbr")}
        </th>
        <th title={t("tasks.start-process.description")}>
          {t("tasks.start-process.description-abbr")}
        </th>
        <th>{t("common.key")}</th>
      </tr>
      </thead>
      <tbody>
      <RequestState
        signal={state.api.process.definition.list_startable}
        on_success={() =>
          <>
            {state.api.process.definition.list_startable.value.data
              .filter((process) => {
                if (search_term.value.length === 0) {
                  return true
                }
                return process.name
                  .toLowerCase()
                  .includes(search_term.value.toLowerCase())

              })
              .map((process) => (
                <tr key={process.id}
                    aria-selected={process.id === params.tab}>
                  <td><a href={`/tasks/start/${process.id}`}>{process.name}</a></td>
                  <td>{process.version}</td>
                  <td>{process.description}</td>
                  <td>{process.key}</td>
                </tr>
              ))}
          </>} />
      </tbody>
    </table>
  </div>
}

const StartProcessForm = () => {
  const
    state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    // 'loading' | 'legacy' | 'form-js' | 'generated'
    form_mode = useSignal('loading')

  // When the selected definition changes, fetch its start-form metadata and
  // dispatch on it:
  //   - a formRef (form-js, i.e. camunda:formRef on the start event) → CamundaForm
  //   - embedded:app: / embedded:deployment: key (legacy AngularJS HTML) → notice
  //   - otherwise (no key, or embedded:engine://…/rendered-form) → the engine's
  //     generated form, also rendered through CamundaForm
  useEffect(() => {
    state.api.process.definition.rendered_form.value = null
    state.api.process.definition.deployed_start_form.value = null
    state.api.task.form.value = null
    form_mode.value = 'loading'
    if (params.tab == null) return

    void engine_rest.process_definition.start_form(state, params.tab).then(() => {
      const start_form = state.api.process.definition.start_form.value?.data,
        form_key = start_form?.key

      if (form_ref_of(start_form)) {
        form_mode.value = 'form-js'
        void engine_rest.process_definition.get_deployed_start_form(
          state,
          params.tab
        )
      } else if (is_legacy_start_key(form_key)) {
        form_mode.value = 'legacy'
        // embedded:app: forms are app-served — fetch the source per #96.
        if (form_key.startsWith(EMBEDDED_APP_PREFIX)) {
          const path = form_key.substring(EMBEDDED_APP_PREFIX.length),
            context_path = start_form.contextPath ?? ''
          void engine_rest.task.get_task_form(
            state,
            `${context_path}/${path}`.replace(/^\/+/, '')
          )
        }
      } else {
        form_mode.value = 'generated'
        void engine_rest.process_definition.rendered_start_form(state, params.tab)
      }
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.tab])

  // The selected definition is fetched by the page above; fall back to the key
  // and then the id while it is still in flight.
  const definition = state.api.process.definition.one.value?.data

  return <div>
    <h2>{definition?.name ?? definition?.key ?? t("tasks.form.form-title")}</h2>
    {form_mode.value === 'legacy'
      ? <p class="info-box">
          {t("tasks.form.legacy-html-unsupported")}{" "}
          <a href={FORM_JS_MIGRATION_DOCS} target="_blank" rel="noreferrer">
            {t("tasks.form.legacy-html-migrate-link")}
          </a>
        </p>
      : form_mode.value === 'form-js'
        ? <StartCamundaForm definition_id={params.tab} />
        : form_mode.value === 'generated'
          ? <StartGeneratedForm definition_id={params.tab} />
          : <p class="fade-in-delayed">{t("common.loading")}</p>}
  </div>
}

// The business key is a property of the process instance, not a form variable,
// so no start form (generated or form-js) carries it. Every start mode renders
// it above the form and folds it into the submit-form payload when set.
const BusinessKeyField = ({ value, on_change }) => {
  const [t] = useTranslation()
  return <label class="business-key">
    {t("tasks.start-process.business-key")}
    <input
      type="text"
      name="business_key"
      value={value}
      onInput={(e) => on_change(e.target.value)} />
  </label>
}

// Add `businessKey` to a submit-form payload only when the user typed one —
// the engine rejects an empty string as a business key.
const with_business_key = (payload, business_key) =>
  business_key === '' ? payload : { ...payload, businessKey: business_key }

// Generated (engine-rendered) start form — the start-side twin of the task
// GeneratedTaskForm. The engine renders it from the start event's
// camunda:formData; we turn that HTML into a form-js schema (+ its default
// values) and render it with CamundaForm, so it looks like a form-js form.
const StartGeneratedForm = ({ definition_id }) => {
  const
    state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation(),
    [error, setError] = useState(null),
    [business_key, setBusinessKey] = useState(''),
    submit_ref = useRef(null)

  const rendered = state.api.process.definition.rendered_form.value
  // A 404 from rendered-form means the definition has no start form at all.
  const no_form = rendered?.status === RESPONSE_STATE.ERROR
  if (!no_form && (rendered?.status !== RESPONSE_STATE.SUCCESS || !rendered?.data)) {
    return <p class="fade-in-delayed">{t("common.loading")}</p>
  }

  const schema = no_form
    ? { type: 'default', components: [] }
    : rendered_form_to_schema(rendered.data)
  const vars = no_form ? {} : rendered_form_variables(rendered.data)
  const allowed = schema_variable_keys(schema)
  const initial_data = vars_to_form_data(vars, allowed)

  const on_submit = ({ data, errors }) => {
    if (errors && Object.keys(errors).length > 0) {
      setError(t("tasks.form.validation-error"))
      return
    }
    setError(null)
    const variables = form_data_to_vars(data, vars, allowed)
    engine_rest.process_definition
      .submit_form(state, definition_id, with_business_key({ variables }, business_key))
      .then((result) => {
        if (result?.status === RESPONSE_STATE.SUCCESS) route('/tasks')
        else setError(result?.error?.message ?? t("tasks.form.unknown-error"))
      })
      .catch((e) => setError(e?.message ?? t("tasks.form.unknown-error")))
  }

  return <div class="task-form camunda-task-form">
    {schema.components.length === 0 &&
      <p class="info-box">{t("tasks.start-process.no-form")}</p>}
    <BusinessKeyField value={business_key} on_change={setBusinessKey} />
    <CamundaForm
      schema={schema}
      data={initial_data}
      on_submit={on_submit}
      on_ready={(c) => (submit_ref.current = c.submit)}
    />
    {error && <p class="error" role="alert">{error}</p>}
    <div class="form-buttons">
      <button type="button" onClick={() => submit_ref.current?.()}>
        {t("tasks.start-process.start")}
      </button>
    </div>
  </div>
}

// form-js (deployed) start form — mirrors the task-side CamundaTaskForm, fed by
// /process-definition/{id}/deployed-start-form and submitted via submit-form
// (see #95). Start forms have no pre-existing scope, so initial data is empty
// and only schema-bound fields are submitted.
const StartCamundaForm = ({ definition_id }) => {
  const
    state = useContext(AppState),
    { route } = useLocation(),
    [t] = useTranslation(),
    [error, setError] = useState(null),
    [business_key, setBusinessKey] = useState(''),
    submit_ref = useRef(null)

  const deployed = state.api.process.definition.deployed_start_form.value,
    schema = deployed?.data

  if (!schema || typeof schema !== 'object' || !Array.isArray(schema.components)) {
    if (deployed?.status === RESPONSE_STATE.ERROR) {
      return <p class="error" role="alert">{t("tasks.form.fetch-failed")}</p>
    }
    return <p class="fade-in-delayed">{t("common.loading")}</p>
  }

  const on_submit = ({ data, errors }) => {
    if (errors && Object.keys(errors).length > 0) {
      setError(t("tasks.form.validation-error"))
      return
    }
    setError(null)
    const variables = form_data_to_vars(data, {}, schema_variable_keys(schema))
    engine_rest.process_definition
      .submit_form(state, definition_id, with_business_key({ variables }, business_key))
      .then((result) => {
        if (result?.status === RESPONSE_STATE.SUCCESS) route('/tasks')
        else setError(result?.error?.message ?? t("tasks.form.unknown-error"))
      })
      .catch((e) => setError(e?.message ?? t("tasks.form.unknown-error")))
  }

  return <div class="task-form camunda-task-form">
    <BusinessKeyField value={business_key} on_change={setBusinessKey} />
    <CamundaForm
      schema={schema}
      data={{}}
      on_submit={on_submit}
      on_ready={(c) => (submit_ref.current = c.submit)}
    />
    {error && <p class="error" role="alert">{error}</p>}
    <div class="form-buttons">
      <button type="button" onClick={() => submit_ref.current?.()}>
        {t("tasks.start-process.start")}
      </button>
    </div>
  </div>
}

export { StartProcessList }
