/**
 * api.js
 *
 * Provides endpoints to the default Operaton REST API.
 *
 * Please refer to the `docs/Coding Conventions.md` "JavaScript > api.js" to
 * learn how we organize the code in this file.
 */
import { useTranslation } from "react-i18next";
export const _url_server = (state) => `${state.server.value.url}`;
export const _url_engine_rest = (state) =>
  `${state.server.value.url}/engine-rest`;

export const get_credentials = (state) =>
  `${state.auth.credentials.value.username}:${state.auth.credentials.value.password}`;

export const get_auth_header = (state) => {
  if (state.auth.mode === "oauth") {
    // Embedded webapps-neo authenticates against the backend through the Spring
    // OAuth2 module: the browser holds a session cookie established by the
    // server-side oauth2 login, so no Authorization header is sent. A Bearer
    // header is only used in the standalone client-side (PKCE) flow, where a
    // token is present.
    return state.auth.token.value ? `Bearer ${state.auth.token.value}` : null;
  }
  return `Basic ${window.btoa(unescape(encodeURIComponent(get_credentials(state))))}`;
};

/**
 * Builds request headers, attaching the Authorization header only when an auth
 * value is available (in server-session oauth mode it is omitted and the
 * session cookie carries the identity).
 */
const auth_headers = (state, extra) => {
  const headers = new Headers(extra);
  const auth = get_auth_header(state);
  if (auth) {
    headers.set("Authorization", auth);
  }
  return headers;
};

const FORM_URLENCODED = { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" };

/* helpers */

// fixme: hide when get_tasks is solved better
export const RESPONSE_STATE = {
  NOT_INITIALIZED: "NOT_INITIALIZED",
  LOADING: "LOADING",
  SUCCESS: "SUCCESS",
  ERROR: "ERROR",
};

/**
 * Displays the result (SUCCESS, ERROR) of an api request and all other states (LOADING, NOT_INITIALIZED, NULL)
 *
 * @param signal {preact.Signal || Array<preact.Signal>} the state signal where the result is stored
 * @param on_success {function: JSXInternal.Element} the element that is shown when the result state is SUCCESS
 * @param on_error {function: JSXInternal.Element} (optional) the element that is shown when the result state is ERROR
 * @param on_nothing (optional) the element that is shown when the state is null
 * @param on_load (optional) the element shown when the request is loading
 * @returns {JSXInternal.Element}
 */

//
//
export const RequestState = ({
  signal,
  on_success,
  on_error = null,
  on_nothing = null,
  on_load = null,
}) => {
  const [t] = useTranslation(),
    is_array = Array.isArray(signal),
    is_not_null = is_array
      ? !signal.some((sig) => sig.value === null)
      : signal.value !== null;

  if (is_not_null) {
    if (is_array) {
      if (signal.some((sig) => sig.value === null)) {
        return;
      }

      const error = signal.find(
        (sig) => sig.value.status === RESPONSE_STATE.ERROR,
      );
      if (error) {
        return resolve_signal(error, on_load, on_success, on_error, t);
      }

      const not_init = signal.find(
        (sig) => sig.value.status === RESPONSE_STATE.NOT_INITIALIZED,
      );
      if (not_init) {
        return resolve_signal(not_init, on_load, on_success, on_error, t);
      }

      const loading = signal.find(
        (sig) => sig.value.status === RESPONSE_STATE.LOADING,
      );
      if (loading) {
        return resolve_signal(loading, on_load, on_success, on_error, t);
      }

      return resolve_signal(signal[0], on_load, on_success, on_error, t);
    }
    return resolve_signal(signal, on_load, on_success, on_error, t);
  }
  if (on_nothing) {
    return on_nothing();
  }
  return <p class="fade-in-delayed">{t("common.fetching")}</p>;
};

const resolve_signal = (signal, on_load, on_success, on_error, t) => {
  if (signal.value.status === RESPONSE_STATE.NOT_INITIALIZED) {
    return <p>{t("common.no-data-requested")}</p>;
  }

  if (signal.value.status === RESPONSE_STATE.LOADING) {
    return on_load ? on_load : <p class="fade-in-delayed">{t("common.loading")}</p>;
  }

  if (signal.value.status === RESPONSE_STATE.SUCCESS) {
    return signal.value?.data ? on_success() : <p>{t("common.no-data")}</p>;
  }

  if (signal.value.status === RESPONSE_STATE.ERROR) {
    return on_error ? (
      on_error
    ) : (
      <p class="error">
        <strong>{t("common.error")}</strong>
        {signal.value.error !== undefined
          ? signal.value.error.message
          : t("common.no-error-message")}
      </p>
    );
  }
};

export const has_data = (signal) =>
  signal.value !== null &&
  signal.value.status === RESPONSE_STATE.SUCCESS &&
  signal.value.data !== null;

const response_data = (response) =>
  response.ok
    ? response.status === 204
      ? Promise.resolve("No Content")
      : response.json()
    : Promise.reject(response);

/**
 * Paginated GET. Appends results to the prior data when `firstResult > 0`,
 * otherwise replaces. Sets a `hasMore` flag based on whether the engine
 * returned a full page. Page size defaults to 20.
 *
 * The signal value shape becomes:
 *   { status, data: <array>, hasMore, ...prior fields }
 */
export const PAGINATED_GET = async (
  url,
  state,
  signl,
  firstResult = 0,
  maxResults = 20,
) => {
  const prev = signl.peek();
  signl.value = {
    status: RESPONSE_STATE.LOADING,
    data: prev?.data,
    hasMore: prev?.hasMore,
  };

  const sep = url.includes("?") ? "&" : "?";
  const paged_url = `${url}${sep}firstResult=${firstResult}&maxResults=${maxResults}`;

  const headers = auth_headers(state);

  try {
    const response = await fetch(`${_url_engine_rest(state)}${paged_url}`, {
      headers,
      credentials: "include",
    });
    const json = await (response.ok
      ? response.json()
      : Promise.reject(response));

    const existing = firstResult > 0 ? (prev?.data ?? []) : [];
    const existing_ids = new Set(
      existing.map((i) => i.id ?? JSON.stringify(i)),
    );
    const fresh = json.filter(
      (i) => !existing_ids.has(i.id ?? JSON.stringify(i)),
    );

    return (signl.value = {
      status: RESPONSE_STATE.SUCCESS,
      data: [...existing, ...fresh],
      hasMore: json.length === maxResults,
    });
  } catch (error) {
    return (signl.value = { status: RESPONSE_STATE.ERROR, error });
  }
};

export const GET = async (url, state, signl) => {
  signl.value = { status: RESPONSE_STATE.LOADING, data: signl.peek?.()?.data };

  const headers = auth_headers(state);

  try {
    const response = await fetch(`${_url_engine_rest(state)}${url}`, {
        headers,
        credentials: "include",
      }),
      json = await (response.ok ? response.json() : Promise.reject(response));
    return (signl.value = { status: RESPONSE_STATE.SUCCESS, data: json });
  } catch (error) {
    return (signl.value = { status: RESPONSE_STATE.ERROR, error });
  }
};

export const GET_SERVER_URL = (url, state, signl) => {
  signl.value = { status: RESPONSE_STATE.LOADING };

  const headers = auth_headers(state, FORM_URLENCODED);

  return fetch(`${_url_server(state)}${url}`, { headers, credentials: "include" })
    .then((response) =>
      response.ok ? response.text() : Promise.reject(response),
    )
    .then(
      (text) => (signl.value = { status: RESPONSE_STATE.SUCCESS, data: text }),
    )
    .catch((error) => (signl.value = { status: RESPONSE_STATE.ERROR, error }));
};

// todo fixme: proper name for login post
export const POST_SERVER_URL = (url, body, state, signl) => {
  signl.value = { status: RESPONSE_STATE.LOADING };

  const headers = auth_headers(state, FORM_URLENCODED);

  return fetch(`${_url_server(state)}${url}`, {
    headers,
    method: "POST",
    body,
    credentials: "include",
  })
    .then(response_data)
    .then(
      (json) => (signl.value = { status: RESPONSE_STATE.SUCCESS, data: json }),
    )
    .catch(
      (error) => console.log("error:", error),

      // error.json().then(json => signl.value = { status: RESPONSE_STATE.ERROR, data: json })
    );
};

export const GET_TEXT = (url, state, signl) => {
  signl.value = { status: RESPONSE_STATE.LOADING };

  const headers = auth_headers(state);

  return fetch(`${_url_engine_rest(state)}${url}`, { headers, credentials: "include" })
    .then((response) =>
      response.ok ? response.text() : Promise.reject(response),
    )
    .then(
      (text) => (signl.value = { status: RESPONSE_STATE.SUCCESS, data: text }),
    )
    .catch((error) => (signl.value = { status: RESPONSE_STATE.ERROR, error }));
};

const fetch_with_body = async (method, url, body, state, signl) => {
  signl.value = { status: RESPONSE_STATE.LOADING };

  const headers = auth_headers(state, { "Content-Type": "application/json" });

  try {
    const response = await fetch(`${_url_engine_rest(state)}${url}`, {
      headers,
      method,
      body: JSON.stringify(body),
      credentials: "include",
    });
    const json = await response_data(response);
    return (signl.value = { status: RESPONSE_STATE.SUCCESS, data: json });
  } catch (error) {
    if (error instanceof Response) {
      const json = await error.json().catch(() => ({ message: error.statusText }));
      return (signl.value = { status: RESPONSE_STATE.ERROR, error: json });
    }
    return (signl.value = { status: RESPONSE_STATE.ERROR, error });
  }
};

export const POST = (url, body, state, signl) =>
  fetch_with_body("POST", url, body, state, signl);

export const PUT = (url, body, state, signl) =>
  fetch_with_body("PUT", url, body, state, signl);

export const DELETE = (url, body, state, signl) =>
  fetch_with_body("DELETE", url, body, state, signl);
