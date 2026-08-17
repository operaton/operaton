import { GET, POST, PUT, DELETE } from "../helper.jsx";

const get_process_instance = (state, instance_id) =>
  GET(
    `/process-instance/${instance_id}`,
    state,
    state.api.process.instance.one,
  );

const get_all_process_instances = (state, definition_id) =>
  GET(
    `/process-instance?processDefinitionId=${definition_id}`,
    state,
    state.api.process.instance.list,
  );

const get_process_instances_by_activity_ids = (
  state,
  process_definition_id,
  activity_id,
) =>
  POST(
    `/process-instance`,
    { activityIdIn: activity_id, processDefinitionId: process_definition_id },
    state,
    state.api.process.instance.list,
  );

// deserializeValues=false so Object/Json/Xml come back as their serialized
// string form (displayable, and avoids server-side deserialization) (#91).
const get_process_instance_variables = (state, instance_id) =>
  GET(
    `/process-instance/${instance_id}/variables?deserializeValues=false`,
    state,
    state.api.process.instance.variables,
  );

const get_called_process_instances = (state, instance_id) =>
  GET(
    `/process-instance?superProcessInstance=${instance_id}`,
    state,
    state.api.process.instance.called,
  );

const get_process_instance_by_defintion_id = (state, definition_id) =>
  GET(
    `/process-instance?processDefinitionId=${definition_id}`,
    state,
    state.api.process.instance.by_defintion_id,
  );

/**
 * Counts running process instances for a deployment
 * @param {Object} state - Application state
 * @param {string} deployment_id - Deployment ID
 * @returns {Promise<>} Instance count or null on error
 */
const get_process_instance_count = (state, deployment_id) =>
  POST(
    "/process-instance/count",
    { deploymentId: deployment_id },
    state,
    state.api.process.instance.count,
  );

const get_activity_instances = (state, instance_id) =>
  GET(
    `/process-instance/${instance_id}/activity-instances`,
    state,
    state.api.process.instance.activity_instances,
  );

const modify_process_instance = (state, instance_id, body) =>
  POST(
    `/process-instance/${instance_id}/modification`,
    body,
    state,
    state.api.process.instance.modification,
  );

/**
 * Asynchronous (batch) modification across many instances of a definition.
 * Returns a Batch the user can monitor on the Batches page.
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Modification
 */
const modify_process_instance_async = (
  state,
  definition_id,
  instructions,
  options = {},
) =>
  POST(
    `/modification/executeAsync`,
    {
      processDefinitionId: definition_id,
      instructions,
      processInstanceQuery: options.query ?? null,
      processInstanceIds: options.instanceIds ?? null,
      skipCustomListeners: options.skipCustomListeners ?? false,
      skipIoMappings: options.skipIoMappings ?? false,
      annotation: options.annotation,
    },
    state,
    state.api.process.instance.modification,
  );

/**
 * Add/update and/or delete variables on a running instance in one request.
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Process-Instance/operation/modifyProcessInstanceVariables
 */
const modify_process_instance_variables = (
  state,
  instance_id,
  { modifications = {}, deletions = [] } = {},
) =>
  POST(
    `/process-instance/${instance_id}/variables`,
    { modifications, deletions },
    state,
    state.api.process.instance.variables_update,
  );

const set_process_instance_variable = (state, instance_id, name, body) =>
  PUT(
    `/process-instance/${instance_id}/variables/${name}`,
    body,
    state,
    state.api.process.instance.variables_update,
  );

const delete_process_instance_variable = (state, instance_id, name) =>
  DELETE(
    `/process-instance/${instance_id}/variables/${name}`,
    null,
    state,
    state.api.process.instance.variables_update,
  );

const set_process_instance_suspended = (state, instance_id, suspended) =>
  PUT(
    `/process-instance/${instance_id}/suspended`,
    { suspended },
    state,
    state.api.process.instance.suspend,
  );

const delete_process_instance = (
  state,
  instance_id,
  skipCustomListeners = false,
) =>
  DELETE(
    `/process-instance/${instance_id}?skipCustomListeners=${skipCustomListeners}`,
    null,
    state,
    state.api.process.instance.delete,
  );

const process_instance = {
  one: get_process_instance,
  variables: get_process_instance_variables,
  called: get_called_process_instances,
  count: get_process_instance_count,
  all: get_all_process_instances,
  by_activity_ids: get_process_instances_by_activity_ids,
  by_defintion_id: get_process_instance_by_defintion_id,
  activity_instances: get_activity_instances,
  modify: modify_process_instance,
  modify_async: modify_process_instance_async,
  modify_variables: modify_process_instance_variables,
  set_variable: set_process_instance_variable,
  delete_variable: delete_process_instance_variable,
  set_suspended: set_process_instance_suspended,
  delete: delete_process_instance,
};

export default process_instance;
