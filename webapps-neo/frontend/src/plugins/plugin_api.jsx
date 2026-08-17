/**
 * plugin_api.jsx
 *
 * The render-time context a plugin component receives. Replaces Camunda's
 * `render(container, data)` argument with an idiomatic Preact hook: entity ids
 * come from the route, engine/auth are ambient (via AppState), and the plugin's
 * own signals/API namespace are handed back scoped by id.
 */
import { useContext } from "preact/hooks";
import { useRoute } from "preact-iso";
import { AppState } from "../state.js";
import engine_rest, { RESPONSE_STATE } from "../api/engine_rest.jsx";
import {
  GET,
  POST,
  _url_engine_rest,
  get_auth_header,
} from "../api/helper.jsx";
import { plugin_descriptor } from "./registry.js";

/**
 * Fetch an absolute URL (honouring a plugin's own `apiBase`) into a signal,
 * writing the same `{ status, data }` shape helper.jsx uses so `RequestState`
 * renders it unchanged.
 */
const request_absolute = async (
  url,
  state,
  signl,
  { method = "GET", body } = {},
) => {
  signl.value = { status: RESPONSE_STATE.LOADING, data: signl.peek?.()?.data };
  const headers = new Headers();
  headers.set("Authorization", get_auth_header(state));
  if (body) headers.set("Content-Type", "application/json");

  try {
    const response = await fetch(url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    if (!response.ok)
      return (signl.value = { status: RESPONSE_STATE.ERROR, error: response });
    const data = response.status === 204 ? "No Content" : await response.json();
    return (signl.value = { status: RESPONSE_STATE.SUCCESS, data });
  } catch (error) {
    return (signl.value = { status: RESPONSE_STATE.ERROR, error });
  }
};

/* eslint-disable react-hooks/rules-of-hooks --
   `use_plugin_api` is a custom hook, but the project names functions in
   snake_case, which the rule's `useCamelCase` heuristic cannot recognise. */
export const use_plugin_api = (plugin_id) => {
  const state = useContext(AppState);
  // Default when rendered outside a matched route (e.g. in tests).
  const route = useRoute() ?? {};
  const params = route.params ?? {};
  const query = route.query ?? {};
  const api_base = plugin_descriptor(plugin_id)?.properties?.apiBase;

  // Target the plugin's own backend when `apiBase` is set, else engine-rest.
  // Plugin code is identical in standalone and embedded deployment modes.
  const resolve_url = (path) =>
    api_base ? `${api_base}${path}` : `${_url_engine_rest(state)}${path}`;

  return {
    params,
    query,
    state,
    engine: {
      base_url: _url_engine_rest(state),
      server: state.server.value,
    },
    // The plugin's own state branch and mounted API namespace.
    signals: state.api.plugins?.[plugin_id] ?? {},
    api: engine_rest.plugins?.[plugin_id] ?? {},
    resolve_url,
    // Scoped verbs: use the plugin's backend when apiBase is set, else the
    // standard engine-rest helpers (which handle auth/pagination/errors).
    get: (path, signl) =>
      api_base
        ? request_absolute(resolve_url(path), state, signl)
        : GET(path, state, signl),
    post: (path, body, signl) =>
      api_base
        ? request_absolute(resolve_url(path), state, signl, {
            method: "POST",
            body,
          })
        : POST(path, body, state, signl),
  };
};
