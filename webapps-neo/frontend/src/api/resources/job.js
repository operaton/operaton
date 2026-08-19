import { GET, PUT, GET_TEXT } from "../helper.jsx";

/**
 * Runtime jobs (timers, async continuations, failed jobs behind incidents).
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Job
 */
const get_jobs_by_instance = (state, instance_id) =>
  GET(
    `/job?processInstanceId=${instance_id}`,
    state,
    state.api.job.by_process_instance,
  );

const set_job_retries = (state, job_id, retries) =>
  PUT(`/job/${job_id}/retries`, { retries }, state, state.api.job.update);

const set_job_suspended = (state, job_id, suspended) =>
  PUT(`/job/${job_id}/suspended`, { suspended }, state, state.api.job.update);

const set_job_duedate = (state, job_id, duedate) =>
  PUT(`/job/${job_id}/duedate`, { duedate }, state, state.api.job.update);

const get_job_stacktrace = (state, job_id) =>
  GET_TEXT(`/job/${job_id}/stacktrace`, state, state.api.job.stacktrace);

const job = {
  by_process_instance: get_jobs_by_instance,
  set_retries: set_job_retries,
  set_suspended: set_job_suspended,
  set_duedate: set_job_duedate,
  stacktrace: get_job_stacktrace,
};

export default job;
