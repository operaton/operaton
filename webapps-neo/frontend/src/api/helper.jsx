/**
 * api.js
 *
 * Provides endpoints to the default Operaton REST API.
 *
 * Please refer to the `docs/Coding Conventions.md` "JavaScript > api.js" to
 * learn how we organize the code in this file.
 */
import { useTranslation } from "react-i18next";
import { get_config } from "../config.js";

export const _url_server = (state) => `${state.server.value.url}`;

/**
 * Whether requests go to the webapp that served us, rather than to a backend
 * configured elsewhere. Only then can a session cookie authenticate them.
 */
export const _is_own_backend = (state) => !state.server?.value?.url;

/** Base of the webapp's own API, honouring a sub-path deployment. */
export const _url_api = () =>
  new URL("api", document.baseURI).href.replace(/\/$/, "");

/**
 * Where engine requests go.
 *
 * For our own backend that is the engine API the webapp serves at
 * `{app}/api/engine/engine/{engine}`, which the session cookie authenticates and
 * which passes through the CSRF and header-security filters. Resource paths below
 * it are the same ones the standalone deployment serves, so callers are unaffected.
 *
 * A backend configured to live somewhere else has no session with us, so those
 * requests still go to its standalone `/engine-rest` and still carry credentials.
 */
export const _url_engine_rest = (state) =>
  _is_own_backend(state)
    ? `${_url_api()}/engine/engine/${get_config().engine}`
    : `${state.server.value.url}/engine-rest`;

/** Base of the webapp's authentication resource. */
export const _url_auth = () =>
  `${_url_api()}/admin/auth/user/${get_config().engine}`;

/**
 * Which user a user-scoped call is about: the one named explicitly, else whoever
 * is signed in.
 *
 * These call sites used to fall back to the literal "demo", left over from before
 * there was any authentication. That silently acted on the demo account instead —
 * the account settings page passes no name, so its "change password" form was
 * PUTting to /user/demo/credentials.
 */
export const resolve_user = (state, user_name) =>
  user_name ?? state.auth.user.id.value;

export const get_credentials = (state) =>
  `${state.auth.credentials.value.username}:${state.auth.credentials.value.password}`;

/**
 * The Authorization header value, or `undefined` when the request authenticates
 * some other way. Neither an OAuth2 session nor a webapp session has a token to
 * send — the session cookie carries the identity, which is why every request sets
 * `credentials: "include"`. Only a separately configured backend still needs a
 * Basic header, because we hold no session with it.
 */
export const get_auth_header = (state) => {
  if (state.auth.mode === "oauth2") {
    return state.auth.token.value ? `Bearer ${state.auth.token.value}` : undefined;
  }
  if (_is_own_backend(state)) return undefined;
  return `Basic ${window.btoa(unescape(encodeURIComponent(get_credentials(state))))}`;
};

/** The CSRF token the server handed us, as a cookie, on an earlier request. */
export const get_xsrf_token = () =>
  document.cookie
    .split(";")
    .map((entry) => entry.trim())
    .find((entry) => entry.startsWith("XSRF-TOKEN="))
    ?.slice("XSRF-TOKEN=".length);

/**
 * Apply the headers every request needs: the Authorization header when this
 * request authenticates with one, and the CSRF token the webapp's filter demands
 * on anything that is not a plain fetch. Sending the token on reads too is
 * harmless and keeps every call site identical.
 */
export const set_request_headers = (headers, state) => {
  const value = get_auth_header(state);
  if (value) {
    headers.set("Authorization", value);
  } else {
    headers.delete("Authorization");
  }

  const token = get_xsrf_token();
  if (token) headers.set("X-XSRF-TOKEN", token);

  return headers;
};

/** @deprecated use {@link set_request_headers}; kept so plugins keep working. */
export const set_auth_header = set_request_headers;

const form_urlencoded_headers = (state) => {
  const headers = new Headers();
  set_request_headers(headers, state);
  headers.set("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
  return headers;
};

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
    return on_load ? (
      on_load
    ) : (
      <p class="fade-in-delayed">{t("common.loading")}</p>
    );
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

  let headers = new Headers();
  set_request_headers(headers, state);

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

  let headers = new Headers();
  set_request_headers(headers, state);

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

  const headers = form_urlencoded_headers(state);

  return fetch(`${_url_server(state)}${url}`, {
    headers,
    credentials: "include",
  })
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

  const headers = form_urlencoded_headers(state);

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

  let headers = new Headers();
  set_request_headers(headers, state);

  return fetch(`${_url_engine_rest(state)}${url}`, {
    headers,
    credentials: "include",
  })
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

  let headers = new Headers();
  set_request_headers(headers, state);
  headers.set("Content-Type", "application/json");

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
      const json = await error
        .json()
        .catch(() => ({ message: error.statusText }));
      return (signl.value = { status: RESPONSE_STATE.ERROR, error: json });
    }
    return (signl.value = { status: RESPONSE_STATE.ERROR, error });
  }
};

/**
 * Multipart POST for file uploads (deployments, attachments). `body` is a
 * FormData; deliberately does NOT set Content-Type so the browser adds the
 * multipart boundary itself.
 */
export const POST_FORM = async (url, body, state, signl) => {
  signl.value = { status: RESPONSE_STATE.LOADING };

  let headers = new Headers();
  set_request_headers(headers, state);

  try {
    const response = await fetch(`${_url_engine_rest(state)}${url}`, {
      headers,
      method: "POST",
      body,
      credentials: "include",
    });
    const json = await response_data(response);
    return (signl.value = { status: RESPONSE_STATE.SUCCESS, data: json });
  } catch (error) {
    if (error instanceof Response) {
      const json = await error
        .json()
        .catch(() => ({ message: error.statusText }));
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
