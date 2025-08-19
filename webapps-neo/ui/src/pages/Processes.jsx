import { signal, useSignalEffect } from '@preact/signals'
import { useContext, useEffect } from 'preact/hooks'
import { useLocation, useRoute } from 'preact-iso'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import * as Icons from '../assets/icons.jsx'
import { AppState } from '../state.js'
import { Accordion } from '../components/Accordion.jsx'
import { BPMNViewer } from '../components/BPMNViewer.jsx'

/**
 * Save custom split view width to localstorage
 */
const store_details_width = () => {
  const width = window.getComputedStyle(
    document.getElementById('selection'), null).getPropertyValue('width')
  localStorage.setItem(
    'details_width',
    width
  )
  document.getElementById('canvas').style.maxWidth = `calc(100vw - ${width})`
}

/**
 * Keep the `?history=true` query params of the URL alive as long as the history
 * mode is active.
 *
 * @param query Provide the result of `useRoute().query`
 * @returns {string} Either returns `?history=true` when history mode is active or an empty string when not.
 */
const keep_history_query = (query) => {
  if (query.history) {
    return '?history=true'
  }
  return ''
}

const ProcessesPage = () => {
  const
    state = useContext(AppState),
    { params, query, path } = useRoute(),
    { route } = useLocation(),
    details_width = signal(localStorage.getItem('details_width') ?? 400),
    enable_history_mode = () => {
      route(`${path}?history=true`)
      state.history_mode.value = true
    },
    disable_history_mode = () => {
      route(`${path}`)
      state.history_mode.value = false
    },
    // condition naming for deciding on fetching data from backend
    definition_selected = params.definition_id,
    history_mode_disabled = !state.history_mode.value,
    no_definition_loaded = state.api.process.definition.one.value === null,

    /** @namespace state.api.process.definition.one.value.data **/
    loaded_definition_not_matching_url_param =
      state.api.process.definition.one.value?.data !== undefined &&
      state.api.process.definition.one.value?.data.id !== params.definition_id

  if (query.history) {
    enable_history_mode()
  }

  /** @namespace details_width.value.data **/
  useEffect(() => {
    document.getElementById('selection').style.width = details_width.value.data
  }, [details_width.value.data])

  if (definition_selected) {
    if (history_mode_disabled) {
      if (no_definition_loaded) {
        void engine_rest.process_definition.one(state, params.definition_id)
        void engine_rest.process_definition.diagram(state, params.definition_id)
        void engine_rest.process_definition.statistics(state, params.definition_id)
      } else if (loaded_definition_not_matching_url_param) {
        void engine_rest.process_definition.one(state, params.definition_id)
        void engine_rest.process_definition.diagram(state, params.definition_id)
      }
    } else {
      void engine_rest.history.process_instance.all(state, params.definition_id)

      if (no_definition_loaded) {
        void engine_rest.process_definition.one(state, params.definition_id)
        void engine_rest.process_definition.diagram(state, params.definition_id)
      } else if (loaded_definition_not_matching_url_param) {
        void engine_rest.process_definition.one(state, params.definition_id)
        void engine_rest.process_definition.diagram(state, params.definition_id)
      }
    }
  } else {
    // reset state
    state.api.process.definition.one.value = null
    state.api.process.definition.diagram.value = null
    state.api.process.instance.list.value = null

    void engine_rest.process_definition.list(state)
  }

  return (
    <main id="processes"
          class="split-layout">
      <div id="left-side">
        <div id="selection" onMouseUp={store_details_width}>
          {!params?.definition_id
            ? <ProcessDefinitionSelection />
            : <ProcessDefinitionDetails />}
        </div>

        <div id="history-mode-indicator" class={state.history_mode.value ? 'on' : 'off'}>
          {state.history_mode.value ?
            <button onClick={disable_history_mode}>
              History Mode Active
            </button>
            :
            <button onClick={enable_history_mode}>
              Enable History Mode
            </button>}
        </div>
      </div>
      <div id="canvas" />
      <ProcessDiagram />
    </main>
  )
}

const ProcessDiagram = () => {
  const
    { api: { process: { definition: { diagram, statistics } } } } = useContext(AppState),
    { params } = useRoute(),
    show_diagram =
      diagram.value !== null &&
      params.definition_id !== undefined

  /** @namespace diagram.value.data.bpmn20Xml **/
  return <>
    {show_diagram && diagram.value.data?.bpmn20Xml !== null && diagram.value.data?.bpmn20Xml !== undefined
    && statistics.value !== null && statistics.value?.data !== undefined
      ? <BPMNViewer xml={diagram.value.data?.bpmn20Xml} container="canvas" tokens={statistics.value.data} />
      : <> </>
    }
  </>
}


const ProcessDefinitionSelection = () => {
  const
    { api: { process: { definition } } } = useContext(AppState)

  return <div class="fade-in">
    <h1>
      Process Definitions
    </h1>
    <table class="tile p-1">
      <thead>
      <tr>
        <th>Name</th>
        <th>Version</th>
        <th>Key</th>
        <th>Instances</th>
        <th>Incidents</th>
        <th>State</th>
      </tr>
      </thead>
      <tbody>
      <RequestState
        signal={definition.list}
        on_success={() =>
          definition.list.value?.data?.map(process =>
            <ProcessDefinition key={process.id} {...process} />)
        } />
      </tbody>
    </table>
  </div>
}

const ProcessDefinitionDetails = () => {
  const
    { api: { process: { definition: { one: process_definition } } } } =
      useContext(AppState),
    { params } = useRoute()

  /** @namespace process_definition.value.data.tenantId **/
  return (
    <div class="fade-in">
      <div class="row gap-2">
        <a className="tabs-back"
           href={`/processes${keep_history_query(useRoute().query)}`}
           title="Change Definition">
          <Icons.arrow_left />
          <Icons.list />
        </a>
        <RequestState
          signal={process_definition}
          on_success={() => <div>
            <h1>{process_definition.value?.data.name ?? ' '}</h1>
            <dl>
              <dt>Definition ID</dt>
              <dd className="font-mono copy-on-click" onClick={copyToClipboard}
              title="Click to copy">
                {process_definition.value?.data.id ?? '-/-'}
              </dd>
              {process_definition.value?.data.tenantId ?
                <>
                  <dt>Tenant ID</dt>
                  <dd>{process_definition?.value.data.tenantId ?? '-/-'}</dd>
                </> : <></>
              }
            </dl>
          </div>} />
      </div>

      <Accordion
        accordion_name="process_definition_details"
        sections={process_definition_tabs}
        base_path={`/processes/${params.definition_id}${keep_history_query(useRoute().query)}`} />
    </div>
  )
}

const ProcessDefinition =
  ({ definition: { id, name, key, version }, instances, incidents }) =>
    <tr>
      <td><a href={`/processes/${id}/instances${keep_history_query(useRoute().query)}`}>{name}</a></td>
      <td>{version}</td>
      <td>{key}</td>
      <td>{instances}</td>
      <td>{incidents.length}</td>
      <td>?</td>
    </tr>

const Instances = () => {
  const
    state = useContext(AppState),
    { params } = useRoute()

  if (!params.selection_id) {
    if (!state.history_mode.value) {
      void engine_rest.history.process_instance.all_unfinished(state, params.definition_id)
    } else {
      void engine_rest.process_instance.all(state, params.definition_id)
    }
  }

  return !params?.selection_id
    ? (<table class="fade-in">
      <thead>
      <tr>
        <th>ID</th>
        <th>Start Time</th>
        <th>State</th>
        <th>Business Key</th>
      </tr>
      </thead>
      <tbody>
      <InstanceTableRows />
      </tbody>
    </table>)
    : (<InstanceDetails />)
}

const InstanceTableRows = () =>
  useContext(AppState).api.process.instance.list.value.data?.map((instance) => (
    <ProcessInstance key={instance.id} {...instance} />
  )) ?? <p>...</p>

const InstanceDetails = () => {
  const
    state = useContext(AppState),
    { params: { selection_id, definition_id, panel } } = useRoute()

  if (selection_id) {
    if (state.api.process.instance.one === undefined || state.api.process.instance.one.value === null) {
      if (!state.history_mode.value) {
        void engine_rest.process_instance.one(state, selection_id)
      } else {
        void engine_rest.history.process_instance.one(state, selection_id)
      }
    }
  }

  return (
    <div class="fade-in">
      <div class="row gap-2">
        <BackToListBtn
          url={`/processes/${definition_id}/instances${keep_history_query(useRoute().query)}`}
          title="Change Instance"
          className="bg-1" />
        <InstanceDetailsDescription />
      </div>

      <Accordion
        sections={process_instance_tabs}
        accordion_name="instance_details_accordion"
        param_name="sub_panel"
        base_path={`/processes/${definition_id}/${panel}/${selection_id}${keep_history_query(useRoute().query)}`} />
    </div>
  )
}

const InstanceDetailsDescription = () =>

  <dl>
    <dt>Instance ID</dt>
    <dd>{useContext(AppState).api.process.instance.one.value.data?.id ?? '-/-'}</dd>
    <dt>Business Key</dt>
    <dd>{useContext(AppState).api.process.instance.one.value.data?.businessKey ?? '-/-'}</dd>
  </dl>

const ProcessInstance = ({ id, startTime, state, businessKey }) => (
  <tr>
    <td class="font-mono"><a
      href={`./instances/${id}/vars${keep_history_query(useRoute().query)}`}> {id.substring(0, 8)}</a></td>
    <td>{new Date(Date.parse(startTime)).toLocaleString()}</td>
    <td>{state}</td>
    <td>{businessKey}</td>
  </tr>
)

const InstanceVariables = () => {
  const
    state = useContext(AppState),
    { params } = useRoute(),
    selection_exists =
      state.api.process.instance.variables.value !== null
      && state.api.process.instance.variables.value.data !== null
      && state.api.process.instance.variables.value.data !== undefined

  // fixme: rm useSignalEffect
  useSignalEffect(() => {
    if (!state.history_mode.value) {
      void engine_rest.process_instance.variables(state, params.selection_id)
    } else {
      void
        engine_rest.history.variable_instance.by_process_instance(state, params.selection_id)
    }
  })

  return (
    <table>
      <thead>
      <tr>
        <th>Name</th>
        <th>Type</th>
        <th>Value</th>
        <th>Actions</th>
      </tr>
      </thead>
      <tbody>
      {selection_exists
        ? !state.history_mode.value
          ? Object.entries(state.api.process.instance.variables.value.data).map(
            // eslint-disable-next-line react/jsx-key
            ([name, { type, value }]) => (<tr>
              <td>{name}</td>
              <td>{type}</td>
              <td>{value}</td>
            </tr>))
          : state.api.process.instance.variables.value.data.map(
            // eslint-disable-next-line react/jsx-key
            ({ name, type, value }) => (<tr>
              <td>{name}</td>
              <td>{type}</td>
              <td>{value}</td>
            </tr>))
        : 'Loading ...'}
      </tbody>
    </table>
  )
}

const InstanceIncidents = () => {
  const
    state = useContext(AppState),
    { params } = useRoute()

  // fixme: rm useSignalEffect
  useSignalEffect(() => {
    void engine_rest.history.incident.by_process_instance(state, params.selection_id)
  })

  /** @namespace state.api.history.incident.by_process_definition.value.data **/
  return (
    <table>
      <thead>
      <tr>
        <th>Message</th>
        <th>Process Instance</th>
        <th>Timestamp</th>
        <th>Activity</th>
        <th>Failing Activity</th>
        <th>Cause Process Instance ID</th>
        <th>Root Cause Process Instance ID</th>
        <th>Type</th>
        <th>Annotation</th>
        <th>Action</th>
      </tr>
      </thead>
      <tbody>
      {state.api.history.incident.by_process_definition.value?.data?.map(
        // eslint-disable-next-line react/jsx-key
        ({
          id,
          incidentMessage,
          processInstanceId,
          createTime,
          activityId,
          failedActivityId,
          causeIncidentId,
          rootCauseIncidentId,
          incidentType,
          annotation,
        }) => (
          <tr key={id}>
            <td>{incidentMessage}</td>
            <td><UUIDLink path={'/processes'} uuid={processInstanceId} /></td>
            <td>
              <time
                datetime={createTime}>{createTime ? createTime.substring(0, 19) : '-/-'}</time>
            </td>
            <td>{activityId}</td>
            <td>{failedActivityId}</td>
            <td><UUIDLink path={''} uuid={causeIncidentId} /></td>
            <td><UUIDLink path={''} uuid={rootCauseIncidentId} /></td>
            <td>{incidentType}</td>
            <td>{annotation}</td>
          </tr>))}
      </tbody>
    </table>
  )
}

const InstanceUserTasks = () => {
  const
    state = useContext(AppState),
    { params } = useRoute()

  // fixme: rm useSignalEffect
  useSignalEffect(() => {
    // void engine_rest.task.by_process_instance(state, params.selection_id)
    void engine_rest.task.get_process_instance_tasks(state, params.selection_id)
  })

  /** @namespace state.api.task.by_process_instance.value.data **/
  return (
    <table>
      <thead>
      <tr>
        <th>Activity</th>
        <th>Assignee</th>
        <th>Owner</th>
        <th>Created</th>
        <th>Due</th>
        <th>Follow Up</th>
        <th>Priority</th>
        <th>Delegation State</th>
        <th>Task ID</th>
        <th>Action</th>
      </tr>
      </thead>
      <tbody>
      {state.api.task.by_process_instance.value?.data?.map(
        // eslint-disable-next-line react/jsx-key
        ({
          id,
          assignee,
          name,
          owner,
          created,
          due,
          followUp,
          priority,
          delegationState,
        }) => (
          <tr key={id}>
            <td>{name}</td>
            <td>{assignee}</td>
            <td>{owner}</td>
            <td>{created}</td>
            <td>{due}</td>
            <td>{followUp}</td>
            <td>{priority}</td>
            <td>{priority}</td>
            <td>{delegationState}</td>
            <td><UUIDLink path="/" uuid={id} /></td>
            <td>
              <button>Groups</button>
              <button>Users</button>
            </td>
          </tr>))}
      </tbody>
    </table>
  )
}

const CalledProcessInstances = () => {
  const
    state = useContext(AppState),
    { selection_id, query } = useRoute()

  // fixme: rm useSignalEffect
  useSignalEffect(() =>
    void engine_rest.process_instance.called(state, selection_id)
  )

  /** @namespace state.api.process.instance.called.value.data **/
  /** @namespace instance.definitionId **/
  return (
    <table>
      <thead>
      <tr>
        <th>State</th>
        <th>Called Process Instance</th>
        <th>Process Definition</th>
        <th>Activity</th>
      </tr>
      </thead>
      <tbody>
      {state.api.process.instance.called.value?.data?.map(instance =>
        <tr key={instance.id}>
          <td>{instance.suspended ? 'Suspended' : 'Running'}</td>
          <td><a href={`/processes/${instance.id}${keep_history_query(query)}`}>{instance.id}</a></td>
          <td>{instance.definitionId}</td>
          <td>{instance.definitionId}</td>
        </tr>
      )}
      </tbody>
    </table>
  )
}

const Incidents = () => {
  const
    state = useContext(AppState),
    { definition_id } = useRoute()

  // fixme: rm useSignalEffect
  useSignalEffect(() =>
    void engine_rest.history.incident.by_process_definition(state, definition_id)
  )

  /** @namespace instance.incidentMessage **/
  /** @namespace instance.incidentType **/
  return (
    <table>
      <thead>
      <tr>
        <th>Message</th>
        <th>Type</th>
        <th>Configuration</th>
      </tr>
      </thead>
      <tbody>
      {state.api.history.incident.by_process_definition.value?.data?.map(incident =>
        <tr key={incident.id}>
          <td>{incident.incidentMessage}</td>
          <td>{incident.incidentType}</td>
          <td>{incident.configuration}</td>
        </tr>
      )}
      </tbody>
    </table>
  )
}

const CalledProcessDefinitions = () => {
  const
    state = useContext(AppState),
    { definition_id, query } = useRoute()

  // fixme: rm useSignalEffect
  useSignalEffect(() =>
    void engine_rest.process_definition.called(state, definition_id)
  )

  /** @namespace definition.calledFromActivityIds **/
  return (
    <table>
      <thead>
      <tr>
        <th>Called Process Definition</th>
        <th>State</th>
        <th>Activity</th>
      </tr>
      </thead>
      <tbody>
      {state.api.process.definition.called.value?.data?.map(definition =>
        <tr key={definition.id}>
          <td><a href={`/processes/${definition.id}${keep_history_query(query)}`}>{definition.name}</a></td>
          <td>{definition.suspended ? 'Suspended' : 'Running'}</td>
          <td>{definition.calledFromActivityIds.map(a => `${a}, `)}</td>
        </tr>
      )}
      </tbody>
    </table>
  )
}

const JobDefinitions = () => {
  const
    state = useContext(AppState),
    { definition_id } = useRoute()

  // fixme: rm useSignalEffect
  useSignalEffect(() =>
    void engine_rest.job_definition.all.by_process_definition(state, definition_id)
  )

  /** @namespace state.api.job_definition.all.by_process_definition.value.data **/
  /** @namespace definition.jobType **/
  /** @namespace definition.jobConfiguration **/
  /** @namespace definition.overridingJobPriority **/
  return (
    <div class="relative">
      <table>
        <thead>
        <tr>
          <th>State</th>
          <th>Activity</th>
          <th>Type</th>
          <th>Configuration</th>
          <th>Overriding Job Priority</th>
          <th>Action</th>
        </tr>
        </thead>
        <tbody>
        {state.api.job_definition.all.by_process_definition.value?.data?.map(definition =>
          <tr key={definition.id}>
            <td>{definition.suspended ? 'Suspended' : 'Active'}</td>
            <td>?</td>
            {/*<td>{definition.calledFromActivityIds.map(a => `${a}, `)}</td>*/}
            <td>{definition.jobType}</td>
            <td>{definition.jobConfiguration}</td>
            <td>{definition.overridingJobPriority ?? '-'}</td>
            <td>
              <button>Suspend</button>
              <button>Change Overriding Job Priority</button>
            </td>
          </tr>
        )}
        </tbody>
      </table>
    </div>
  )
}

const BackToListBtn = ({ url, title, className }) =>
  <a className={`tabs-back ${className || ''}`}
     href={url}
     title={title}>
    <Icons.arrow_left />
    <Icons.list />
  </a>

const process_definition_tabs = [
  {
    name: 'Instances',
    id: 'instances',
    pos: 0,
    target: <Instances />
  },
  {
    name: 'Incidents',
    id: 'incidents',
    pos: 1,
    target: <Incidents />
  },
  {
    name: 'Called Definitions',
    id: 'called_definitions',
    pos: 2,
    target: <CalledProcessDefinitions />
  },
  {
    name: 'Jobs',
    id: 'jobs',
    pos: 3,
    target: <JobDefinitions />
  }]

const UUIDLink = ({ uuid = '?', path }) =>
  <a href={`${path}${keep_history_query(useRoute().query)}`}>{uuid.substring(0, 8)}</a>

const process_instance_tabs = [
  {
    name: 'Variables',
    id: 'vars',
    pos: 0,
    target: <InstanceVariables />
  },
  {
    name: 'Instance Incidents',
    id: 'instance_incidents',
    pos: 1,
    target: <InstanceIncidents />
  },
  {
    name: 'Called Instances',
    id: 'called_instances',
    pos: 2,
    target: <CalledProcessInstances />
  },
  {
    name: 'User Tasks',
    id: 'user_tasks',
    pos: 3,
    target: <InstanceUserTasks />
  },
  {
    name: 'Jobs',
    id: 'jobs',
    pos: 4,
    // TODO: create Jobs example for old Camunda apps
    target: <p>Jobs</p>
  },
  {
    name: 'External Tasks',
    id: 'external_tasks',
    pos: 5,
    // TODO: create External Apps example for old Camunda apps
    target: <p>External Tasks</p>
  }]

// fixme : extract to util file
const copyToClipboard = (event) =>
  navigator.clipboard.writeText(event.target.innerText)

export { ProcessesPage }
