import { GET, PUT, POST, GET_TEXT } from "../helper.jsx";

/**
 * External tasks — work items fetched-and-locked by external workers.
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/External-Task
 */
const get_by_instance = (state, instance_id) =>
  GET(
    `/external-task?processInstanceId=${instance_id}`,
    state,
    state.api.external_task.by_process_instance,
  );

const set_retries = (state, id, retries) =>
  PUT(
    `/external-task/${id}/retries`,
    { retries },
    state,
    state.api.external_task.update,
  );

const unlock = (state, id) =>
  POST(
    `/external-task/${id}/unlock`,
    null,
    state,
    state.api.external_task.update,
  );

const get_error_details = (state, id) =>
  GET_TEXT(
    `/external-task/${id}/errorDetails`,
    state,
    state.api.external_task.error_details,
  );

const external_task = {
  by_process_instance: get_by_instance,
  set_retries,
  unlock,
  error_details: get_error_details,
};

export default external_task;
