import { GET } from '../helper.jsx'

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

const get_process_instances = (state, definition_id) =>
  GET(`/history/process-instance?${url_params(definition_id)}`, state, state.api.process.instance.list)

const get_process_instances_unfinished = (state, definition_id) =>
  GET(`/history/process-instance?${url_params_unfinished(definition_id)}`, state, state.api.process.instance.list)

const get_process_instance = (state, definition_id) =>
  GET(`/history/process-instance/${definition_id}`, state, state.api.process.instance.one)

const get_process_incidents = (state, definition_id) =>
  GET(`/history/incident?processDefinitionId=${definition_id}`, state, state.api.history.incident.by_process_definition)

const get_process_instance_incidents = (state, instance_id) =>
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
    by_process_definition: get_process_instance_incidents,
    by_process_instance: get_process_incidents
  },
  variable_instance: {
    by_process_instance: get_process_instance_variable,
  },
  get_user_operation
}

export default history