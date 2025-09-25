import { POST, DELETE, PUT } from '../helper.jsx'

/* groups */

const get_groups = (state) =>
  POST('/group', {
    firstResult: 0,
    maxResults: 50,
    sortBy: 'id',
    sortOrder: 'asc'
  }, state, state.api.group.list)

const add_user_to_group = (state, group_id, user_name) =>
  PUT(`/group/${group_id}/members/${user_name ?? 'demo'}`, {
    id: group_id,
    // TODO remove `?? 'demo'` when we have working authentication
    userId: user_name ?? 'demo',
  }, state, state.api.group.add_user)

const create_group = (state, group) =>
  POST(`/group/create`, group, state, state.api.group.create)

const remove_group = (state, group_id, user_name) =>
  DELETE(`/group/${group_id}/members/${user_name ?? 'demo'}`, {
    id: group_id,
    // TODO remove `?? 'demo'` when we have working authentication
    userId: user_name ?? 'demo',
  }, state, state.remove_group_response)

const get_user_groups = (state) =>
  POST('/group', {
    member: state.auth.user.id.value,
    firstResult: 0,
    maxResults: 50
  }, state, state.api.user.group.list)


const group = {
  all: get_groups,
  create: create_group,
  delete: remove_group,
  by_member: get_user_groups,
  add_user: add_user_to_group,
}

export default group