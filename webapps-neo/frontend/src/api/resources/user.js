import { GET, POST, DELETE, PUT, resolve_user } from '../helper.jsx'

/**
 * Get all users
 *
 * @param state
 * @returns {Promise<{status: string, data: *} | {status: string, error: *}>}
 */
const get_users = (state) =>
  GET('/user', state, state.api.user.list)

/**
 * Look a user up by exact id. Answers an empty list when there is no such user —
 * and also when the caller may not read them, because identity queries are
 * filtered by READ on User.
 */
const find_user = (state, user_name) =>
  GET(`/user?id=${encodeURIComponent(user_name)}&maxResults=1`, state, state.api.user.lookup)

const create_user = (state, user) =>
  POST('/user/create', user, state, state.api.user.create)

const delete_user = (state, user_name) =>
  DELETE(`/user/${user_name}`, {}, state, state.api.user.delete)

const get_user_count = (state) =>
  GET('/user', state, state.api.user.count)

const get_user_profile = (state, user_name) =>
  GET(`/user/${resolve_user(state, user_name)}/profile`, state, state.api.user.profile)

const update_user_profile = (state, user_name, profile) =>
  PUT(`/user/${resolve_user(state, user_name)}/profile`, profile, state, state.api.user.update)

const update_credentials = (state, user_name, credentials_body) =>
  PUT(`/user/${resolve_user(state, user_name)}/credentials`, credentials_body, state, state.api.user.credentials)

const unlock_user = (state, user_name) =>
  POST(`/user/${resolve_user(state, user_name)}/unlock`, {}, state, state.api.user.unlock)

const user =
  {
    all: get_users,
    find: find_user,
    create: create_user,
    delete: delete_user,
    count: get_user_count,
    profile: {
      get: get_user_profile,
      update: update_user_profile,
    },
    credentials_update: update_credentials,
    unlock: unlock_user,
  }

export default user