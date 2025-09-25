import { GET, GET_TEXT, POST } from '../helper.jsx'

export const get_process_definitions = (state) =>
  GET('/process-definition/statistics', state, state.api.process.definition.list)

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
export const get_diagram = (state, process_definition_id) =>
  GET(`/process-definition/${process_definition_id}/xml`, state, state.api.process.definition.diagram)

/**
 * Fetches statistics and all details for a single process definition
 * @param {Object} state - Application state
 * @param {string} process_definition_id - Process definition ID
 * @sideeffects Updates state.selected_process_statistics
 */
// export const get_process_definition_statistics = (state, process_definition_id) =>
//   get(`/process-definition/${id}`, state, state.api.process.definition.single)
//
// {
//   return fetch(`${_url(state)}/process-definition/statistics`)
//     .then((res) => res.json())
//     .then((data) => {
//       const filteredData = data.filter(
//         (item) => item.definition.id === process_definition_id
//       )
//       state.selected_process_statistics.value = filteredData[0] || null
//     })
//     .catch((error) => {
//       console.error('Error fetching statistics:', error)
//       state.selected_process_statistics.value = null
//     })
// }

/**
 * Fetches process definition by deployment ID and resource name
 * @param {Object} state - Application state
 * @param {string} deployment_id - Deployment ID
 * @param {string} resource_name - Resource name
 * @sideeffects Triggers statistics fetch
 */
export const get_process_definition_by_deployment_id = (state, deployment_id, resource_name) =>
  GET(`/process-definition?deploymentId=${deployment_id}&resourceName=${encodeURIComponent(resource_name)}`, state, state.api.process.definition.one)

// .then(() => state.api.process.definition.one = state.api.process.definition.one[0])

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
  GET(`/process-definition?${url_params()}`, state, state.api.process.definition.list)

const get_deployed_start_form = (state, processId) =>
  GET(`/process-definition/${processId}/deployed-start-form`, state, state.api.process.definition.deployed_start_form)

const get_start_form = (state, processId) =>
  // GET(`/process-definition/${processId}/deployed-start-form`, state, state.api.process.definition.start_form)
  GET(`/process-definition/key/${processId}/startForm`, state, state.api.process.definition.start_form)
  // GET(`/process-definition/${processId}/rendered-form`, state, state.api.process.definition.start_form)

export const get_rendered_start_form = async (state, id) =>
  GET_TEXT(`/process-definition/${id}/rendered-form`, state, state.api.process.definition.rendered_form)

export const start_process_submit_form = (state, id, body = {}) =>
  POST(`/process-definition/${id}/submit-form`, body, state, state.api.process.definition.submit_form)

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
  submit_form: start_process_submit_form
}

export default process_definition