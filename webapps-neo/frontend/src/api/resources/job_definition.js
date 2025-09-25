import { GET } from '../helper.jsx'

const get_job_definitions = (state, definition_id) =>
  GET(`/job-definition?processDefinitionId=${definition_id}`, state, state.api.job_definition.all)

const job_definition = {
  all: {
    by_process_definition: get_job_definitions
  }
}

export default job_definition