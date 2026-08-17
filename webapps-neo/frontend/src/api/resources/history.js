import { GET, PAGINATED_GET, PUT } from "../helper.jsx";

const INSTANCE_PAGE_SIZE = 20;

const instance_url = (
  definition_id,
  params = {},
  { unfinished = false } = {},
) => {
  const merged = {
    sortBy: "startTime",
    sortOrder: "asc",
    ...(unfinished ? { unfinished: true } : {}),
    processDefinitionId: definition_id,
    ...params,
  };
  return new URLSearchParams(merged).toString();
};

const get_process_instances = (
  state,
  definition_id,
  params = {},
  firstResult = 0,
) =>
  PAGINATED_GET(
    `/history/process-instance?${instance_url(definition_id, params)}`,
    state,
    state.api.history.process_instance.list,
    firstResult,
    INSTANCE_PAGE_SIZE,
  );

const get_process_instances_unfinished = (
  state,
  definition_id,
  params = {},
  firstResult = 0,
) =>
  PAGINATED_GET(
    `/history/process-instance?${instance_url(definition_id, params, { unfinished: true })}`,
    state,
    state.api.history.process_instance.list,
    firstResult,
    INSTANCE_PAGE_SIZE,
  );

const get_process_instance = (state, definition_id) =>
  GET(
    `/history/process-instance/${definition_id}`,
    state,
    state.api.history.process_instance.one,
  );

const get_incidents_by_process_definition = (state, definition_id) =>
  GET(
    `/history/incident?processDefinitionId=${definition_id}`,
    state,
    state.api.history.incident.by_process_definition,
  );

const get_incidents_by_process_instance = (state, instance_id) =>
  GET(
    `/history/incident?processInstanceId=${instance_id}`,
    state,
    state.api.history.incident.by_process_instance,
  );

const get_process_instance_variable = (state, instance_id) =>
  GET(
    `/history/variable-instance?processInstanceId=${instance_id}&deserializeValues=false`,
    state,
    state.api.history.variable_instance.by_process_instance,
  );

const get_historic_tasks_by_instance = (state, instance_id) =>
  GET(
    `/history/task?processInstanceId=${instance_id}`,
    state,
    state.api.history.task.by_process_instance,
  );

const get_activity_instances_by_process_instance = (state, instance_id) =>
  GET(
    `/history/activity-instance?processInstanceId=${instance_id}&sortBy=startTime&sortOrder=asc`,
    state,
    state.api.history.activity_instance.by_process_instance,
  );

const get_historic_called_instances = (state, instance_id) =>
  GET(
    `/history/process-instance?superProcessInstanceId=${instance_id}`,
    state,
    state.api.history.process_instance.called,
  );

/**
 * Task History
 */
const get_user_operation = (state, execution_id) =>
  GET(
    `/history/user-operation?processInstanceId=${execution_id}&sortBy=timestamp&sortOrder=desc`,
    state,
    state.api.history.user_operation,
  );

// An operation id groups all log entries of one user action; the annotation
// applies to the whole operation, so these are keyed by operationId.
const set_user_operation_annotation = (state, operation_id, annotation) =>
  PUT(
    `/history/user-operation/${operation_id}/set-annotation`,
    { annotation },
    state,
    state.api.history.user_operation_annotation,
  );

const clear_user_operation_annotation = (state, operation_id) =>
  PUT(
    `/history/user-operation/${operation_id}/clear-annotation`,
    {},
    state,
    state.api.history.user_operation_annotation,
  );

/**
 * Finished/historic batches (carry `endTime` once complete).
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Historic-Batch
 */
const get_historic_batches = (
  state,
  params = { sortBy: "batchId", sortOrder: "desc" },
  firstResult = 0,
) => {
  const qs = new URLSearchParams(params).toString();
  return PAGINATED_GET(
    `/history/batch${qs ? `?${qs}` : ""}`,
    state,
    state.api.history.batch.list,
    firstResult,
    20,
  );
};

const get_historic_batch = (state, id) =>
  GET(`/history/batch/${id}`, state, state.api.history.batch.one);

const history = {
  process_instance: {
    all: get_process_instances,
    one: get_process_instance,
    all_unfinished: get_process_instances_unfinished,
    called: get_historic_called_instances,
  },
  incident: {
    by_process_definition: get_incidents_by_process_definition,
    by_process_instance: get_incidents_by_process_instance,
  },
  variable_instance: {
    by_process_instance: get_process_instance_variable,
  },
  task: {
    by_process_instance: get_historic_tasks_by_instance,
  },
  activity_instance: {
    by_process_instance: get_activity_instances_by_process_instance,
  },
  batch: {
    all: get_historic_batches,
    one: get_historic_batch,
  },
  get_user_operation,
  set_user_operation_annotation,
  clear_user_operation_annotation,
};

export default history;
