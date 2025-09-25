import { useState, useContext } from 'preact/hooks'
import DOMPurify from 'dompurify'
import { AppState } from '../state.js'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import * as Icons from '../assets/icons.jsx'
import { useRoute } from 'preact-iso'

const TaskForm = () => {
  const
    [generated, setGenerated] = useState(''),
    [deployed, setDeployed] = useState([]),
    [error, setError] = useState(null),
    state = useContext(AppState),
    { params } = useRoute(),
    selectedTask = state.api.task.one.value.data,
    refName = state.server.value.c7_mode ? 'camundaFormRef' : 'operatonFormRef'

  if (!selectedTask) return <p class="info-box">No task selected.</p>

  const rendered_form = state.api.task.rendered_form.value
  const deployed_form = state.api.task.deployed_form.value

  if (!selectedTask.data?.formKey && !selectedTask[refName] && !rendered_form) {
    void engine_rest.task.get_task_rendered_form(state, selectedTask.id)
    void engine_rest.task.get_task_form(state, selectedTask.formKey.substring(13))
  }

  if (!selectedTask.formKey && selectedTask[refName] && !deployed_form) {
    void engine_rest.task.get_task_deployed_form(state, selectedTask.id)
  }

  if (rendered_form?.data && generated === '') {
    setGenerated(parse_html(state, rendered_form.data))
  }

  if (deployed_form && deployed.length === 0) {
    setDeployed(prepare_form_data(deployed_form))
  }

  if (selectedTask.formKey) {

    // todo: if a link to a document exists, place this url in the generated html
    //  localhost:8088/engine-rest/task/8c317066-e488-11ef-86ef-0242ac140003/variables/invoiceDocument/data
    //  where such a link exists <a cam-file-download="invoiceDocument"></a>

    return (
      <>
        {/*<a href={`${state.server.value.url}/${formLink}`} target="_blank" rel="noreferrer">Embedded Form</a>*/}


        <RequestState signal={state.api.task.form} on_success={() =>
          // eslint-disable-next-line react/no-danger
          <div dangerouslySetInnerHTML={{ __html: state.api.task.form.value.data }} />
        } />
      </>
    )
  }

  if (selectedTask[refName]) {
    return (
      <div id="deployed-form" class="task-form">
        <form>
          {deployed.map(({ key, value }) =>
            <DeployedFormRow key={key} components={value} />)}
        </form>
      </div>
    )
  }

  return (
    <>
      <div style={'margin-bottom: 8px;'}>(*) required field</div>
      <div id="generated-form" class="task-form">
        <form onSubmit={(e) => submit_form(e, state, setError, params.task_id)}>
          <div class="form-fields" dangerouslySetInnerHTML={{ __html: generated }} />
          <div class={`error ${error ? 'show' : 'hidden'}`}>
            <span class="icon"><Icons.exclamation_triangle /></span>
            <span class="error-text">{error}</span>
          </div>
          <div class="form-buttons">
            <button type="submit">Complete Task</button>
            <button type="button" class="secondary" onClick={() => store_data(state)}>Save Form</button>
          </div>
        </form>
      </div>
    </>
  )
}

const parse_html = (state, html) => {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  const form = doc.getElementsByTagName('form')[0]

  if (!form) {
    console.warn('No <form> element found in rendered form HTML')
    return '<p class="info-box">No form available for this task.</p>'
  }
  //TODO: Muss noch gemacht werden
  const disable = state.api.user?.profile?.value?.id !== state.api.task.value?.data.assignee

  //TODO: Muss noch gemacht werden
  let storedData = localStorage.getItem(`task_form_${state.api.task.one.value?.data.id}`)
  if (storedData) storedData = JSON.parse(storedData)

  const inputs = form.getElementsByTagName('input')
  const selects = form.getElementsByTagName('select')

  for (const field of inputs) {
    if (!field.getAttribute('name')) field.name = 'name'
    if (field.hasAttribute('uib-datepicker-popup')) field.type = 'date'
    if (field.getAttribute('cam-variable-type') === 'Long') field.type = 'number'
    if (disable) field.setAttribute('disabled', 'disabled')
    if (field.hasAttribute('required')) {
      //TODO: Muss noch gemacht werden
      if (field.type !== 'date') {
        field.previousElementSibling.textContent += '*'
      }
    }

    if (storedData) {
      if (field.type === 'checkbox' && storedData[field.name]?.value) {
        field.checked = true
      } else if (storedData[field.name]) {
        field.value = storedData[field.name].value
      }
    }
  }

  for (const field of selects) {
    if (disable) field.setAttribute('disabled', 'disabled')
    if (storedData?.[field.name]) {
      for (const option of field.children) {
        if (option.value === storedData[field.name].value) {
          option.selected = true
        }
      }
    }
  }

  return DOMPurify.sanitize(form.innerHTML, { ADD_ATTR: ['cam-variable-type'] })
}

const submit_form = (e, state, setError, taskId) => {
  e.preventDefault()
  setError(null)

  const data = build_form_data()

  engine_rest.task.post_task_form(state, taskId, data)
    .then(() => {
      localStorage.removeItem(`task_form_${taskId}`)

      window.location.href = '/tasks'
    })
    .catch(error => {
      console.error('Submit failed:', error)
      setError(error?.message || 'An unknown error occurred.')
    })
}

/* with "Save TaskForm" we store the form data in the local storage, so the task can be completed in the future,
   no matter when, we reuse the JSON structure from the REST API POST call */
const store_data = (state) => {
  localStorage.setItem(`task_form_${state.api.task.one.value?.data?.id}`, JSON.stringify(build_form_data(true)))
}

const build_form_data = (temporary = false) => {
  const inputs = document.getElementById('generated-form').getElementsByClassName('form-control')
  const data = {}

  for (let input of inputs) {
    const name = input.name
    if (!name) continue

    switch (input.type) {
      case 'checkbox':
        data[name] = { value: input.checked }
        break
      case 'date': {
        if (input.value) {
          const val = temporary ? input.value : input.value.split('-').reverse().join('/')
          data[name] = { value: val }
        }
        break
      }
      case 'number':
        if (input.value) data[name] = { value: parseInt(input.value, 10) }
        break
      default:
        if (input.value) data[name] = { value: input.value }
    }
  }

  return data
}

const prepare_form_data = (form) => {
  const components = []
  let rowName = ''
  let row = []

  form.components.forEach((component, index) => {
    if (rowName !== component.layout.row) {
      if (rowName !== '') components.push({ key: rowName, value: row })
      row = []
      rowName = component.layout.row
    }

    row.push(component)

    if (index === form.components.length - 1) {
      components.push({ key: rowName, value: row })
    }
  })

  return components
}

const DeployedFormRow = (props) =>
  <div class="form-fields">
    {props.components?.map(component =>
      <DeployedFormComponent key={component.id} component={component} />)}
  </div>

const DeployedFormComponent = (props) =>
  <div class={`col col-${props.component.layout.columns ? props.component.layout.columns : '16'}`}>


    {(() => {
      switch (props.component.type) {
        case 'spacer':
          return <span>&nbsp;</span>
        case 'separator':
          return <hr />
        case 'text':
          return <div class="task-text">{props.component.text}</div>
        case 'checklist':
          return <MultiInput component={props.component} />
        case 'radio':
          return <MultiInput component={props.component} />
        case 'select':
          return <Select component={props.component} />
        default:
          return <Input type={props.component.type} component={props.component} />
      }
    })()}

  </div>

const Input = (props) => {
  let type = props.type
  const label = props.component.dateLabel ? props.component.dateLabel :
    (props.component.timeLabel ? props.component.timeLabel : props.component.label)

  if (type === 'textfield') {
    type = 'text'
  }

  if (type === 'datetime') {
    type = props.component.subtype === 'datetime' ? 'datetime-local' : props.component.subtype
  }

  return (
    <div class="row">
      <label>{label}<br />
        <input type={type} name={props.component.key}
               required={props.component.validate && props.component.validate.required}
               min={props.component.validate ? props.component.validate.min : ''}
               max={props.component.validate ? props.component.validate.max : ''}
               maxlength={props.component.validate ? props.component.validate.maxlength : ''}
               pattern={props.component.validate ? props.component.validate.pattern : ''}
               step={props.component.validate ? props.component.validate.step : ''}
        />
      </label>
    </div>)
}

const Select = (props) => {
  const options = props.component.values.map((data) => <option key={data.value} value={data.value}>{data.label}</option>)

  return (
    <div class="row">
      <label>{props.component.label}</label>
      <select name={props.component.key}>
        {options}
      </select>
    </div>
  )
}

const MultiInput = (props) => {
  let type = props.component.type

  if (type === 'checklist') {
    type = 'checkbox'
  }

  const options = props.component.values.map((data) => {
    return (
      <div class="input-list" key={`list_${data.value}`}>
        <input type={type} name={data.value} value={data.value} key={`field_${data.value}`} />
        <label key={data.value}>{data.label}</label>
      </div>
    )
  })

  return (
    <>
      <label>{props.component.label}</label>
      {options}
    </>)
}

export { TaskForm }
