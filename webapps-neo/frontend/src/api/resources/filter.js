import { GET, POST, PUT, DELETE, RESPONSE_STATE, _url_engine_rest, get_auth_header } from "../helper.jsx";

const get_filters = (state) =>
  GET("/filter?resourceType=Task", state, state.api.filter.list);

const get_filter = (state, filter_id) =>
  GET(`/filter/${filter_id}`, state, state.api.filter.one);

const create_filter = (state, body) =>
  POST("/filter/create", body, state, state.api.filter.create);

const update_filter = (state, filter_id, body) =>
  PUT(`/filter/${filter_id}`, body, state, state.api.filter.update);

const delete_filter = (state, filter_id) =>
  DELETE(`/filter/${filter_id}`, null, state, state.api.filter.delete);

const execute_filter = async (state, filter_id, firstResult = 0, maxResults = 15, sorting = { sortBy: "created", sortOrder: "desc" }) => {
  const prev = state.api.task.list.value;
  if (firstResult === 0) state.api.task.list.value = { status: RESPONSE_STATE.LOADING };

  const headers = new Headers();
  headers.set("Authorization", get_auth_header(state));
  headers.set("Content-Type", "application/json");

  const body = {
    sorting: [sorting],
  };

  try {
    const response = await fetch(
      `${_url_engine_rest(state)}/filter/${filter_id}/list?firstResult=${firstResult}&maxResults=${maxResults}`,
      { headers, method: "POST", body: JSON.stringify(body) },
    );
    const json = await (response.ok ? response.json() : Promise.reject(response));
    const existing = firstResult > 0 ? (prev?.data ?? []) : [];
    const existingIds = new Set(existing.map((t) => t.id));
    const newTasks = json.filter((t) => !existingIds.has(t.id));
    state.api.task.list.value = {
      status: RESPONSE_STATE.SUCCESS,
      data: [...existing, ...newTasks],
      hasMore: json.length === maxResults,
    };
  } catch (error) {
    state.api.task.list.value = { status: RESPONSE_STATE.ERROR, error };
  }
};

const filter = {
  get_filters,
  get_filter,
  create_filter,
  update_filter,
  delete_filter,
  execute_filter,
};

export default filter;
