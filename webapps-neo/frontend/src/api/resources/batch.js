import { GET, DELETE, PUT, PAGINATED_GET } from "../helper.jsx";

/**
 * Engine batches (async operations such as process-instance modification,
 * migration, deletion). The `statistics` endpoint is used for list/detail
 * because it carries progress (totalJobs, remainingJobs, completedJobs,
 * failedJobs, suspended) on top of the batch configuration.
 *
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Batch
 */

const get_batches = (state, firstResult = 0) =>
  PAGINATED_GET(
    `/batch/statistics?sortBy=batchId&sortOrder=desc`,
    state,
    state.api.batch.list,
    firstResult,
    20,
  );

const get_batch = (state, id) =>
  GET(`/batch/statistics?batchId=${id}`, state, state.api.batch.one);

const delete_batch = (state, id) =>
  DELETE(`/batch/${id}?cascade=true`, null, state, state.api.batch.delete);

const set_batch_suspended = (state, id, suspended) =>
  PUT(`/batch/${id}/suspended`, { suspended }, state, state.api.batch.update);

const batch = {
  all: get_batches,
  one: get_batch,
  delete: delete_batch,
  set_suspended: set_batch_suspended,
};

export default batch;
