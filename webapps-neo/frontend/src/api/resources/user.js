import { GET, POST, DELETE, PUT } from '../helper.jsx'

/**
 * Get all users
 *
 * @param state
 * @returns {Promise<{status: string, data: *} | {status: string, error: *}>}
 */
const get_users = (state) =>
  GET('/user', state, state.api.user.list)

const create_user = (state, user) =>
  POST('/user/create', user, state, state.api.user.create)

const delete_user = (state, user_name) =>
  DELETE(`/user/${user_name}`, {}, state, state.api.user.delete)

const get_user_count = (state) =>
  GET('/user', state, state.api.user.count)

const get_user_profile = (state, user_name) =>
  // TODO remove `?? 'demo'` when we have working authentication
  GET(`/user/${user_name ?? 'demo'}/profile`, state, state.api.user.profile)

const update_user_profile = (state, user_name) =>
  // TODO remove `?? 'demo'` when we have working authentication
  PUT(`/user/${user_name ?? 'demo'}/profile`, state.api.user.profile, state, state.api.user.profile)

const update_credentials = (state, user_name) =>
  // TODO remove `?? 'demo'` when we have working authentication
  PUT(`/user/${user_name ?? 'demo'}/credentials`, state.api.user.credentials.value.data, state, state.api.user.credentials)

const unlock_user = (state, user_name) =>
  // TODO remove `?? 'demo'` when we have working authentication
  POST(`/user/${user_name ?? 'demo'}/unlock`, {}, state, state.api.user.unlock)

const user =
  {
    all: get_users,
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