import { GET, POST, DELETE, PUT } from '../helper.jsx'


const url_params = (resource_type) =>
  new URLSearchParams({
    resourceType: resource_type,
  }).toString()

const get_authorizations = (state, resource_type) =>
  GET(`/authorization?${url_params(resource_type)}`, state, state.api.authorization.all)

const create_authorization = (state, body) =>
  POST(`/authorization/create`, body, state, state.api.authorization.create)

const update_authorization = (state, id, body) =>
  PUT(`/authorization/${id}`, body, state, state.api.authorization.update)

const delete_authorization = (state, id) =>
  DELETE(`/authorization/${id}`, {}, state, state.api.authorization.delete)

const authorization = {
  all: get_authorizations,
  create: create_authorization,
  update: update_authorization,
  delete: delete_authorization
}

export default authorization
