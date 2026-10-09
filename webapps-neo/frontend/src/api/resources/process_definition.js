import { GET, GET_TEXT, POST, PUT, DELETE } from '../helper.jsx'

export const get_process_definitions = (state, params = {}) => {
  const qs = new URLSearchParams(params).toString()
  return GET(`/process-definition/statistics${qs ? `?${qs}` : ''}`, state, state.api.process.definition.list)
}

export const get_process_definition_statistics_with_incidents = (state, id) =>
  GET(`/process-definition/${id}/statistics?incidents=true`, state, state.api.process.definition.statistics)

export const get_process_definition = (state, id) =>
  GET(`/process-definition/${id}`, state, state.api.process.definition.one)

export const get_called_process_definitions = (state, definition_id) =>
  GET(`/process-definition/${definition_id}/static-called-process-definitions`, state, state.api.process.definition.called)

/**
 * BPMN 2.0 XML for a process definition
 * @param {Object} state - Application state
 * @param {string} process_definition_id - Process definition ID
 * @sideeffects Updates state.bpmn_xml
 */
export const get_diagram = (state, process_definition_id, signal = state.api.process.definition.diagram) =>
  GET(`/process-definition/${process_definition_id}/xml`, state, signal)

/**
 * Fetches the process definition for a deployment resource. The engine returns
 * an *array* (a filtered list), so this lands in its own signal rather than
 * process.definition.one (which holds a single object) — see #94 fallout.
 * @param {Object} state - Application state
 * @param {string} deployment_id - Deployment ID
 * @param {string} resource_name - Resource name
 */
export const get_process_definition_by_deployment_id = (state, deployment_id, resource_name) =>
  GET(`/process-definition?deploymentId=${deployment_id}&resourceName=${encodeURIComponent(resource_name)}`, state, state.api.deployment.process_definition)

const url_params = () =>
  new URLSearchParams({
    latest: true,
    active: true,
    startableInTasklist: true,
    startablePermissionCheck: true,
    firstResult: 0,
    maxResults: 15
  }).toString()

const get_startable_process_definitions = (state) =>
  GET(`/process-definition?${url_params()}`, state, state.api.process.definition.list_startable)

const get_deployed_start_form = (state, processId) =>
  GET(`/process-definition/${processId}/deployed-start-form`, state, state.api.process.definition.deployed_start_form)

// Start form metadata ({ key, contextPath }) by definition id.
const get_start_form = (state, processId) =>
  GET(`/process-definition/${processId}/startForm`, state, state.api.process.definition.start_form)

export const get_rendered_start_form = async (state, id, signal = state.api.process.definition.rendered_form) =>
  GET_TEXT(`/process-definition/${id}/rendered-form`, state, signal)

export const start_process_submit_form = (state, id, body = {}) =>
  POST(`/process-definition/${id}/submit-form`, body, state, state.api.process.definition.submit_form)


export const get_activity_instance_statistics = (state, id) =>
  GET(`/process-definition/${id}/statistics`, state, state.api.process.definition.activity_instance_statistics)

const suspend_process_definition = (state, id) =>
  PUT(
    `/process-definition/${id}/suspended`,
    { suspended: true, includeProcessInstances: true },
    state,
    state.api.process.definition.suspend,
  )

const activate_process_definition = (state, id) =>
  PUT(
    `/process-definition/${id}/suspended`,
    { suspended: false, includeProcessInstances: true },
    state,
    state.api.process.definition.suspend,
  )

const delete_process_definition = (state, id) =>
  DELETE(
    `/process-definition/${id}?cascade=true`,
    null,
    state,
    state.api.process.definition.remove,
  )

const process_definition = {
  list: get_process_definitions,
  one: get_process_definition,
  called: get_called_process_definitions,
  by_deployment_id: get_process_definition_by_deployment_id,
  diagram: get_diagram,
  statistics: get_process_definition_statistics_with_incidents,
  list_startable: get_startable_process_definitions,
  start_form: get_start_form,
  get_deployed_start_form,
  rendered_start_form: get_rendered_start_form,
  submit_form: start_process_submit_form,
  activity_instance_statistics: get_activity_instance_statistics,
  suspend: suspend_process_definition,
  activate: activate_process_definition,
  remove: delete_process_definition,
}

export default process_definition
