import { GET, DELETE, PUT, POST } from '../helper.jsx'

// Tenants the given user is a member of (used on the user details page).
const get_user_tenants = (state, user_name) =>
  // TODO remove `?? 'demo'` when we have working authentication
  GET(`/tenant?userMember=${user_name ?? 'demo'}&maxResults=50&firstResult=0`, state, state.api.tenant.by_member)

const get_tenants = (state) =>
  GET(`/tenant?firstResult=0&maxResults=50&sortBy=id&sortOrder=asc`, state, state.api.tenant.list)

const create_tenant = (state, body) =>
  POST(`/tenant/create`, body, state, state.api.tenant.create)

const update_tenant = (state, tenant_id, body) =>
  PUT(`/tenant/${tenant_id}`, body, state, state.api.tenant.update)

const delete_tenant = (state, tenant_id) =>
  DELETE(`/tenant/${tenant_id}`, {}, state, state.api.tenant.delete)

// Members of a tenant (used on the tenant details page).
const get_tenant_users = (state, tenant_id) =>
  GET(`/user?memberOfTenant=${tenant_id}&firstResult=0&maxResults=50`, state, state.api.tenant.user_members)

const get_tenant_groups = (state, tenant_id) =>
  GET(`/group?memberOfTenant=${tenant_id}&firstResult=0&maxResults=50`, state, state.api.tenant.group_members)

const add_user_to_tenant = (state, tenant_id, user_name) =>
  PUT(`/tenant/${tenant_id}/user-members/${user_name}`, {
    id: tenant_id,
    userId: user_name,
  }, state, state.api.tenant.add_user)

const remove_user_from_tenant = (state, tenant_id, user_name) =>
  DELETE(`/tenant/${tenant_id}/user-members/${user_name}`, {
    id: tenant_id,
    userId: user_name,
  }, state, state.api.tenant.remove_user)

const add_group_to_tenant = (state, tenant_id, group_id) =>
  PUT(`/tenant/${tenant_id}/group-members/${group_id}`, {
    id: tenant_id,
    groupId: group_id,
  }, state, state.api.tenant.add_group)

const remove_group_from_tenant = (state, tenant_id, group_id) =>
  DELETE(`/tenant/${tenant_id}/group-members/${group_id}`, {
    id: tenant_id,
    groupId: group_id,
  }, state, state.api.tenant.remove_group)


const tenant = {
  all: get_tenants,
  create: create_tenant,
  update: update_tenant,
  delete: delete_tenant,
  by_member: get_user_tenants,
  user_members: get_tenant_users,
  group_members: get_tenant_groups,
  add_user: add_user_to_tenant,
  remove_user: remove_user_from_tenant,
  add_group: add_group_to_tenant,
  remove_group: remove_group_from_tenant,
}

export default tenant
