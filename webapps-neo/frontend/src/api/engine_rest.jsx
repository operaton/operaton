import {
  RequestState as request_state,
  RESPONSE_STATE as response_state,
} from "./helper.jsx";
import { plugin_apis } from "./plugins.js";
import auth from "./resources/auth.js";
import batch from "./resources/batch.js";
import engine from "./resources/engine.js";
import user from "./resources/user.js";
import group from "./resources/group.js";
import tenant from "./resources/tenant.js";
import process_definition from "./resources/process_definition.js";
import process_instance from "./resources/process_instance.js";
import deployment from "./resources/deployment.js";
import external_task from "./resources/external_task.js";
import history from "./resources/history.js";
import incident from "./resources/incident.js";
import job from "./resources/job.js";
import job_definition from "./resources/job_definition.js";
import migration from "./resources/migration.js";
import task from "./resources/task.js";
import authorization from "./resources/authorization.js";
import decision from "./resources/decision.js";
import filter from "./resources/filter.js";

const engine_rest = {
  auth,
  authorization,
  batch,
  decision,
  deployment,
  engine,
  external_task,
  filter,
  group,
  history,
  incident,
  job,
  job_definition,
  migration,
  process_definition,
  process_instance,
  task,
  tenant,
  user,
  // Plugin-contributed API namespaces are mounted here at boot by the plugin
  // registry, as engine_rest.plugins.<plugin-id>.<fn> (see api/plugins.js).
  // `mock_engine_rest()` deep-clones whatever is mounted, so plugin APIs are
  // covered in tests too.
  plugins: plugin_apis,
};

export default engine_rest;

export const RequestState = request_state;

export const RESPONSE_STATE = response_state;
