import { GET, DELETE, PUT, POST } from '../helper.jsx'


const get_user_tenants = (state, user_name) =>
  // TODO remove `?? 'demo'` when we have working authentication
  GET(`/tenant?userMember=${user_name ?? 'demo'}&maxResult=50&firstResult=0`, state, state.api.tenant.by_member)

const get_tenants = (state) =>
  GET(`/tenant?firstResult=0&maxResults=20&sortBy=id&sortOrder=asc`, state, state.api.tenant.list)

const create_tenant = (state, body) =>
  POST(`/tenant/create`, body, state, state.api.tenant.create)

const add_user_to_tenant = (state, tenant_id, user_name) =>
  PUT(`/tenant/${tenant_id}/user-members/${user_name ?? 'demo'}`, {
    id: tenant_id,
    // TODO remove `?? 'demo'` when we have working authentication
    userId: user_name ?? 'demo',
  }, state, state.api.tenant.add_user)

const remove_tenant = (state, tenant_id, user_name) =>
  DELETE(`/tenant/${tenant_id}/user-members/${user_name ?? 'demo'}`, {
    id: tenant_id,
    // TODO remove `?? 'demo'` when we have working authentication
    userId: user_name ?? 'demo',
  }, state, state.remove_tenant_response)


const tenant = {
  all: get_tenants,
  create: create_tenant,
  delete: remove_tenant,
  by_member: get_user_tenants,
  add_user: add_user_to_tenant
}

export default tenant