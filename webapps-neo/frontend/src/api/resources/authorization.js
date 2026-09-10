import {
  GET,
  POST,
  DELETE,
  PUT,
  _url_engine_rest,
  set_request_headers,
} from "../helper.jsx";

const url_params = (resource_type) =>
  new URLSearchParams({
    resourceType: resource_type,
  }).toString();

const get_authorizations = (state, resource_type) =>
  GET(
    `/authorization?${url_params(resource_type)}`,
    state,
    state.api.authorization.all,
  );

const create_authorization = (state, body) =>
  POST(`/authorization/create`, body, state, state.api.authorization.create);

const update_authorization = (state, id, body) =>
  PUT(`/authorization/${id}`, body, state, state.api.authorization.update);

// The five entries of the admin navigation, each with the permission that
// governs it. The system data has no resource of its own; reading
// authorizations is the closest thing the engine offers to "is an administrator".
const ADMIN_SECTIONS = [
  {
    section: "users",
    permission: "READ",
    resource_name: "user",
    resource_type: 1,
  },
  {
    section: "groups",
    permission: "READ",
    resource_name: "group",
    resource_type: 2,
  },
  {
    section: "tenants",
    permission: "READ",
    resource_name: "tenant",
    resource_type: 11,
  },
  {
    section: "authorizations",
    permission: "READ",
    resource_name: "authorization",
    resource_type: 4,
  },
  {
    section: "system",
    permission: "READ",
    resource_name: "authorization",
    resource_type: 4,
  },
];

const build_headers = (state) => {
  const headers = new Headers();
  set_request_headers(headers, state);
  return headers;
};

const check_one = (state, { permission, resource_name, resource_type }) =>
  fetch(
    `${_url_engine_rest(state)}/authorization/check?${new URLSearchParams({
      permissionName: permission,
      resourceName: resource_name,
      resourceType: resource_type,
      resourceId: "*",
    })}`,
    { headers: build_headers(state), credentials: "include" },
  )
    .then((response) => (response.ok ? response.json() : null))
    .then((json) => json?.authorized === true)
    .catch(() => false);

/**
 * Which parts of the admin area the signed-in user may reach. A section the
 * user has no permission for is hidden rather than shown and then refused.
 */
const get_admin_sections = async (state) => {
  const allowed = await Promise.all(
    ADMIN_SECTIONS.map((entry) => check_one(state, entry)),
  );
  return (state.api.authorization.sections.value = Object.fromEntries(
    ADMIN_SECTIONS.map(({ section }, i) => [section, allowed[i]]),
  ));
};

const delete_authorization = (state, id) =>
  DELETE(`/authorization/${id}`, {}, state, state.api.authorization.delete);

const authorization = {
  all: get_authorizations,
  create: create_authorization,
  update: update_authorization,
  delete: delete_authorization,
  sections: get_admin_sections,
};

export default authorization;
