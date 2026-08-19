import { GET, PUT } from "../helper.jsx";

const get_job_definitions = (state, definition_id) =>
  GET(
    `/job-definition?processDefinitionId=${definition_id}`,
    state,
    state.api.job_definition.all.by_process_definition,
  );

/**
 * Suspend/activate a job definition (and, by default, its jobs).
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Job-Definition
 */
const set_job_definition_suspended = (
  state,
  id,
  suspended,
  includeJobs = true,
) =>
  PUT(
    `/job-definition/${id}/suspended`,
    { suspended, includeJobs },
    state,
    state.api.job_definition.update,
  );

const set_job_definition_priority = (
  state,
  id,
  jobPriority,
  includeJobs = true,
) =>
  PUT(
    `/job-definition/${id}/jobPriority`,
    { jobPriority, includeJobs },
    state,
    state.api.job_definition.update,
  );

const set_job_definition_retries = (state, id, retries) =>
  PUT(
    `/job-definition/${id}/retries`,
    { retries },
    state,
    state.api.job_definition.update,
  );

const job_definition = {
  all: {
    by_process_definition: get_job_definitions,
  },
  set_suspended: set_job_definition_suspended,
  set_priority: set_job_definition_priority,
  set_retries: set_job_definition_retries,
};

export default job_definition;
