import { GET, DELETE, PUT, POST, PAGINATED_GET } from "../helper.jsx";

/**
 * Engine batches (async operations such as process-instance modification,
 * migration, deletion). The `statistics` endpoint is used for list/detail
 * because it carries progress (totalJobs, remainingJobs, completedJobs,
 * failedJobs, suspended) on top of the batch configuration.
 *
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Batch
 */

const get_batches = (
  state,
  params = { sortBy: "batchId", sortOrder: "desc" },
  firstResult = 0,
) => {
  const qs = new URLSearchParams(params).toString();
  return PAGINATED_GET(
    `/batch/statistics${qs ? `?${qs}` : ""}`,
    state,
    state.api.batch.list,
    firstResult,
    20,
  );
};

const get_batch = (state, id) =>
  GET(`/batch/statistics?batchId=${id}`, state, state.api.batch.one);

const delete_batch = (state, id) =>
  DELETE(`/batch/${id}?cascade=true`, null, state, state.api.batch.delete);

const set_batch_suspended = (state, id, suspended) =>
  PUT(`/batch/${id}/suspended`, { suspended }, state, state.api.batch.update);

/**
 * Retry the failed execution jobs of a batch by resetting their retries. Scoped
 * to the batch's execution job definition (`batchJobDefinitionId`); returns a
 * new "set job retries" batch that the user can monitor on this page.
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Job/operation/setJobRetriesAsyncOperation
 */
const retry_batch_jobs = (state, batch_job_definition_id) =>
  POST(
    `/job/retries`,
    {
      retries: 1,
      jobQuery: {
        jobDefinitionId: batch_job_definition_id,
        withException: true,
      },
    },
    state,
    state.api.batch.retry,
  );

const batch = {
  all: get_batches,
  one: get_batch,
  delete: delete_batch,
  set_suspended: set_batch_suspended,
  retry: retry_batch_jobs,
};

export default batch;
