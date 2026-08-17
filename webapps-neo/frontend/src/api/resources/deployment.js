import { GET, DELETE, GET_TEXT, POST_FORM } from "../helper.jsx";

/**
 * Fetches deployments sorted by deployment time, sets the first as selected
 * @sideeffects Updates `state.deployments`, triggers `get_deployment_resources`
 */
const get_deployments = (
  state,
  params = { sortBy: "deploymentTime", sortOrder: "desc" },
) => {
  const qs = new URLSearchParams(params).toString();
  return GET(
    `/deployment${qs ? `?${qs}` : ""}`,
    state,
    state.api.deployment.all,
  );
};

/**
 * Fetches resources for a deployment and triggers BPMN diagram fetch
 * @sideeffects Updates `state.deployment_resources`, `state.selected_resource`
 */
const get_deployment_resources = (state, deployment_id) =>
  GET(
    `/deployment/${deployment_id}/resources`,
    state,
    state.api.deployment.resources,
  );

/**
 * Fetches resources for a deployment and triggers BPMN diagram fetch
 * @sideeffects Updates `state.deployment_resources`, `state.selected_resource`
 */
const get_deployment_resource = (state, deployment_id, resource_id) =>
  GET_TEXT(
    `/deployment/${deployment_id}/resources/${resource_id}/data`,
    state,
    state.api.deployment.resource,
  );

/**
 * Deletes a deployment and cleans up related state
 * @param {Object} state - Application state
 * @param {string} deployment_id - Deployment ID to delete
 * @param {Object} params - Optional query parameters
 * @sideeffects Resets deployment-related state values
 */
const delete_deployment = (state, deployment_id, params = {}) =>
  DELETE(
    `/deployment/${deployment_id}?${new URLSearchParams(params).toString()}`,
    null,
    state,
    state.api.deployment.delete,
  );

/**
 * Uploads a new deployment. `form_data` carries `deployment-name`, the dedup
 * flags, and one or more resource files.
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Deployment
 */
const create_deployment = (state, form_data) =>
  POST_FORM(
    "/deployment/create",
    form_data,
    state,
    state.api.deployment.create,
  );

const deployment = {
  all: get_deployments,
  resources: get_deployment_resources,
  resource: get_deployment_resource,
  delete: delete_deployment,
  create: create_deployment,
};

export default deployment;
