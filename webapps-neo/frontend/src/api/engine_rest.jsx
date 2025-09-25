import { RequestState as request_state, RESPONSE_STATE as response_state } from './helper.jsx'
import auth from './resources/auth.js'
import engine from './resources/engine.js'
import user from './resources/user.js'
import group from './resources/group.js'
import tenant from './resources/tenant.js'
import process_definition from './resources/process_definition.js'
import process_instance from './resources/process_instance.js'
import deployment from './resources/deployment.js'
import history from './resources/history.js'
import job_definition from './resources/job_definition.js'
import task from './resources/task.js'
import authorization from './resources/authorization.js'
import decision from './resources/decision.js'

const engine_rest = {
  auth,
  authorization,
  decision,
  deployment,
  engine,
  group,
  history,
  job_definition,
  process_definition,
  process_instance,
  task,
  tenant,
  user,
}

export default engine_rest

export const RequestState = request_state

export const RESPONSE_STATE = response_state