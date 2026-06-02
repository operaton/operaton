import { useContext } from 'preact/hooks'
import { useTranslation } from 'react-i18next'
import { AppState } from '../state.js'
import { useSignal } from '@preact/signals'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import { useRoute } from 'preact-iso'
import { Breadcrumbs } from '../components/Breadcrumbs.jsx'

const StartProcessList = () => {
  const
    state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation()

  void engine_rest.process_definition.list_startable(state)

  if (params.tab !== null) {
    void engine_rest.process_definition.one(state, params.tab)
  }

  return <div>
    <StartableProcessesList />
    {params.tab !== undefined
      ? <StartProcessForm />
      : <p>{t("tasks.start-process.select-definition")}</p>}
  </div>
}

const StartableProcessesList = () => {
  const
    state = useContext(AppState),
    { params } = useRoute(),
    [t] = useTranslation(),
    search_term = useSignal('')

  if (params.tab !== null && state.api.process.definition.one.value !== null
    && state.api.process.definition.one.value?.data !== undefined) {
    void engine_rest.process_definition.start_form(state, state.api.process.definition.one.value.data.key)
      .then(() => void engine_rest.task.get_task_form(state, state.api.process.definition.start_form.value.data.key.substring(13)))
  }

  return <div>

    <div className="row space-between p-1">
      <h2>{t("tasks.start-process.title")}</h2>

      <input
        type="text"
        className="search-input"
        id="process-popup-search-input"
        placeholder={t("tasks.start-process.search-placeholder")}
        value={search_term.value}
        onChange={(e) => (search_term.value = e.target.value)} />
    </div>
    <table>
      <thead>
      <tr>
        <th>{t("tasks.start-process.definition-name")}</th>
        <th>{t("tasks.start-process.version")}</th>
        <th>{t("tasks.start-process.description")}</th>
        <th>{t("common.key")}</th>
      </tr>
      </thead>
      <tbody>
      <RequestState
        signal={state.api.process.definition.list}
        on_success={() =>
          <>
            {state.api.process.definition.list.value.data
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
                    class={(process.id === params.tab) ? 'selected' : ''}>
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
    form_fields = useSignal([]),
    fr = new FileReader(),

    handleSubmit = (event) => {
      event.preventDefault()

      const
        form = event.target,
        form_data = new FormData(form),
        response = {},
        to_base_64 = (file) => new Promise((resolve, reject) => {
          fr.onload = () => resolve(fr)
          fr.onerror = (err) => reject(err)
          fr.readAsDataURL(file)
        })

      form_fields.value.map(async ({ variable_name, type, input_type }) => {
        response[variable_name] = {
          type,
          value:
            input_type === 'file'
              ? await to_base_64(form_data.get(variable_name)).result.split('base,')[1]
              : form_data.get(variable_name)
        }

        if (input_type === 'file') {
          response[variable_name]['valueInfo'] = {
            fileName: form_data.get(variable_name).name,
            mimeType: form_data.get(variable_name).type
          }
        }
      })

      if (form_data.get('business_key') !== null) {
        response['business_key'] = form_data.get('business_key').toString()
      }


      // void engine_rest.process_definition.submit_form(state, params.id, response)
    }

  const parse_form = (form_html) => {
    const parser = new DOMParser(),
      parsed = parser.parseFromString(form_html, 'text/html'),
      form = parsed.querySelector('form'),
      inputs = form.querySelectorAll('input'),
      selects = form.querySelectorAll('select'),
      form_groups = form.querySelectorAll('.form-group'),
      button_group = document.createElement('div'),
      submit_button = document.createElement('button')

    const fields = [
      ...Array.from(inputs, input => ({
        variable_name: input.getAttribute('cam-variable-name'),
        type: input.getAttribute('cam-variable-type'),
        input_type: input.getAttribute('type')
      })),
      ...Array.from(selects, input => ({
        variable_name: input.getAttribute('cam-variable-name'),
        type: input.getAttribute('cam-variable-type'),
        input_type: 'select'
      })),
    ]
    form_fields.value = [...form_fields.peek(), ...fields]

    submit_button.innerText = t("common.submit")
    submit_button.setAttribute('type', 'submit')
    button_group.classList.add('button-group')
    button_group.appendChild(submit_button)

    form_groups.forEach(form_group =>
      form.innerHTML += form_group.innerHTML
    )

    form.querySelectorAll('.form-group').forEach(el => el.remove())

    form.querySelectorAll('[cam-variable-name]').forEach(form_element =>
      form_element.setAttribute('name', form_element.getAttribute('cam-variable-name'))
    )
    form.querySelectorAll('select').forEach(select =>
      select.querySelectorAll('option').forEach(option =>
        option.value = option.innerText
      )
    )

    form.appendChild(button_group)

    return form.innerHTML
  }

  return <div>
    <h2>{t("tasks.form.form-title")}</h2>
    <RequestState signal={state.api.task.form} on_success={() => <>
      {/*eslint-disable-next-line react/no-danger*/}
      <form onSubmit={handleSubmit} dangerouslySetInnerHTML={{ __html: parse_form(state.api.task.form.value.data) }}>
        <div class="button-group">
          <button type="submit">{t("tasks.start-process.start")}</button>
        </div>
      </form>
    </>
    } />
  </div>
}

export { StartProcessList }
