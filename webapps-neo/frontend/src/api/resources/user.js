import {
  GET,
  GET_LIST,
  PAGE_SIZE,
  POST,
  DELETE,
  PUT,
  resolve_user,
  encode_id,
} from "../helper.jsx";

/**
 * Get all users
 *
 * @param state
 * @returns {Promise<{status: string, data: *} | {status: string, error: *}>}
 */
/**
 * @param query {object} engine query parameters, e.g. { firstNameLike: "Al" }
 * @param append {boolean} keep the rows already loaded and add the next page
 */
const get_users = (state, query = {}, append = false) =>
  GET_LIST(
    `/user?${new URLSearchParams({ maxResults: PAGE_SIZE, firstResult: 0, sortBy: "userId", sortOrder: "asc", ...query })}`,
    state,
    state.api.user.list,
    append,
  );

/**
 * Look a user up by exact id. Answers an empty list when there is no such user —
 * and also when the caller may not read them, because identity queries are
 * filtered by READ on User.
 */
const find_user = (state, user_name) =>
  GET(
    `/user?id=${encodeURIComponent(user_name)}&maxResults=1`,
    state,
    state.api.user.lookup,
  );

const create_user = (state, user) =>
  POST("/user/create", user, state, state.api.user.create);

const delete_user = (state, user_name) =>
  DELETE(`/user/${encode_id(user_name)}`, {}, state, state.api.user.delete);

const get_user_count = (state) => GET("/user", state, state.api.user.count);

const get_user_profile = (state, user_name) =>
  GET(
    `/user/${encode_id(resolve_user(state, user_name))}/profile`,
    state,
    state.api.user.profile,
  );

const update_user_profile = (state, user_name, profile) =>
  PUT(
    `/user/${encode_id(resolve_user(state, user_name))}/profile`,
    profile,
    state,
    state.api.user.update,
  );

const update_credentials = (state, user_name, credentials_body) =>
  PUT(
    `/user/${encode_id(resolve_user(state, user_name))}/credentials`,
    credentials_body,
    state,
    state.api.user.credentials,
  );

/**
 * The rules a password has to satisfy. The engine answers 404 when no policy
 * is configured, which is not an error — it means there are no rules.
 * @see https://docs.operaton.org/reference/latest/rest-api/#tag/Identity
 */
const get_password_policy = (state) =>
  GET("/identity/password-policy", state, state.api.user.password_policy);

/** Ask the engine whether a password satisfies the policy. */
const check_password = (state, password, user_id) =>
  POST(
    "/identity/password-policy",
    { password, ...(user_id ? { profile: { id: user_id } } : {}) },
    state,
    state.api.user.password_check,
  );

const unlock_user = (state, user_name) =>
  POST(
    `/user/${encode_id(resolve_user(state, user_name))}/unlock`,
    {},
    state,
    state.api.user.unlock,
  );

const user = {
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
  password_policy: get_password_policy,
  check_password: check_password,
};

export default user;
