import { GET, DELETE, GET_TEXT } from '../helper.jsx'

/**
 * Fetches deployments sorted by deployment time, sets the first as selected
 * @sideeffects Updates `state.deployments`, triggers `get_deployment_resources`
 */
const get_deployments = (state) =>
  GET("/deployment?sortBy=deploymentTime&sortOrder=desc", state, state.api.deployment.all)

/**
 * Fetches resources for a deployment and triggers BPMN diagram fetch
 * @sideeffects Updates `state.deployment_resources`, `state.selected_resource`
 */
const get_deployment_resources = (state, deployment_id) =>
  GET(`/deployment/${deployment_id}/resources`, state, state.api.deployment.resources)

/**
 * Fetches resources for a deployment and triggers BPMN diagram fetch
 * @sideeffects Updates `state.deployment_resources`, `state.selected_resource`
 */
const get_deployment_resource = (state, deployment_id, resource_id) =>
  GET_TEXT(`/deployment/${deployment_id}/resources/${resource_id}/data`, state, state.api.deployment.resource)

/**
 * Deletes a deployment and cleans up related state
 * @param {Object} state - Application state
 * @param {string} deployment_id - Deployment ID to delete
 * @param {Object} params - Optional query parameters
 * @sideeffects Resets deployment-related state values
 */
const delete_deployment = (state, deployment_id, params = {}) =>
   DELETE(`/deployment/${deployment_id}?${new URLSearchParams(params).toString()}`, null, state, state.api.deployment.delete)


const deployment = {
  all: get_deployments,
  resources: get_deployment_resources,
  resource: get_deployment_resource,
  delete: delete_deployment
}

export default deployment
