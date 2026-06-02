import { GET, PAGINATED_GET } from '../helper.jsx'

const url_params = (definition_id) =>
  new URLSearchParams({
    sortBy: 'startTime',
    sortOrder: 'asc',
    processDefinitionId: definition_id,
  }).toString()

const url_params_unfinished = (definition_id) =>
  new URLSearchParams({
    unfinished: true,
    sortBy: 'startTime',
    sortOrder: 'asc',
    processDefinitionId: definition_id,
  }).toString()

const INSTANCE_PAGE_SIZE = 20

const get_process_instances = (state, definition_id, firstResult = 0) =>
  PAGINATED_GET(
    `/history/process-instance?${url_params(definition_id)}`,
    state,
    state.api.process.instance.list,
    firstResult,
    INSTANCE_PAGE_SIZE,
  )

const get_process_instances_unfinished = (state, definition_id, firstResult = 0) =>
  PAGINATED_GET(
    `/history/process-instance?${url_params_unfinished(definition_id)}`,
    state,
    state.api.process.instance.list,
    firstResult,
    INSTANCE_PAGE_SIZE,
  )

const get_process_instance = (state, definition_id) =>
  GET(`/history/process-instance/${definition_id}`, state, state.api.process.instance.one)

const get_incidents_by_process_definition = (state, definition_id) =>
  GET(`/history/incident?processDefinitionId=${definition_id}`, state, state.api.history.incident.by_process_definition)

const get_incidents_by_process_instance = (state, instance_id) =>
  GET(`/history/incident?processInstanceId=${instance_id}`, state, state.api.history.incident.by_process_instance)

const get_process_instance_variable = (state, instance_id) =>
  GET(`/history/variable-instance?processInstanceId=${instance_id}`, state, state.api.process.instance.variables)

/**
 * Task History
 */
const get_user_operation = (state, execution_id) =>
  GET(`/history/user-operation?processInstanceId=${execution_id}`, state, state.api.history.user_operation)

const history = {
  process_instance: {
    all: get_process_instances,
    one: get_process_instance,
    all_unfinished: get_process_instances_unfinished,
  },
  incident: {
    by_process_definition: get_incidents_by_process_definition,
    by_process_instance: get_incidents_by_process_instance
  },
  variable_instance: {
    by_process_instance: get_process_instance_variable,
  },
  get_user_operation
}

export default history