import {
  GET_SERVER_URL,
  _is_own_backend,
  _url_auth,
  _url_engine_rest,
  set_request_headers,
  RESPONSE_STATE,
} from "../helper.jsx";
import {
  is_oauth,
  start_oauth_login,
  handle_oauth_callback,
  restore_oauth_session,
  oauth_logout,
} from "../oauth.js";

const cookies = (state) =>
  GET_SERVER_URL("/operaton/app/cockpit/default/", state, state.auth.cookies);

// The webapp records which app a session was opened for. There is only one here.
const APP_NAME = "neo";

/**
 * Establish a server-side session with the webapp.
 *
 * The credentials are sent once, to the webapp's own authentication resource, and
 * the session cookie that comes back authenticates everything afterwards — so
 * nothing has to keep the password around to re-send. They used to be held in a
 * signal and mirrored into sessionStorage in cleartext, which any script running
 * in the page could read.
 *
 * @param {Object} state - Application state
 * @returns {Promise<string>} the authenticated user id
 */
const start_session = (state, username, password) => {
  const headers = new Headers();
  set_request_headers(headers, state);
  headers.set("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

  return fetch(`${_url_auth()}/login/${APP_NAME}`, {
    method: "POST",
    headers,
    credentials: "include",
    body: new URLSearchParams({ username, password }).toString(),
  })
    .then((response) =>
      response.ok ? response.json() : Promise.reject(response),
    )
    .then((result) => {
      // Once the session exists the password has no further use here, so make
      // sure nothing is left holding one.
      state.auth.credentials.value = { username: null, password: null };
      return result.userId ?? username;
    });
};

/** Whoever the current session belongs to, or `null` when there is none. */
const session_user = (state) => {
  const headers = new Headers();
  set_request_headers(headers, state);

  return fetch(_url_auth(), { headers, credentials: "include" })
    .then((response) => (response.ok ? response.json() : null))
    .then((result) => result?.userId ?? null)
    .catch(() => null);
};

/**
 * Check a username and password against the engine's identity service.
 *
 * Deliberately not a plain request to some endpoint with an Authorization
 * header: the REST API may be configured without authentication, in which case
 * every request succeeds and any password would be accepted. This endpoint
 * verifies the credentials whether or not the API itself demands them, and
 * answers with the resolved user, which the web apps need in order to load
 * anything user-specific.
 *
 * @param {Object} state - Application state
 * @returns {Promise<string>} the authenticated user id
 */
const verify_credentials = (state, username, password) =>
  fetch(`${_url_engine_rest(state)}/identity/verify`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ username, password }),
  })
    .then((response) =>
      response.ok ? response.json() : Promise.reject(response),
    )
    .then((result) =>
      result.authenticated
        ? (result.authenticatedUser ?? username)
        : Promise.reject(result),
    );

/**
 * Login new user
 * @param {Object} state - Application state
 * @param username User name
 * @param password Password
 */
const login = (
  state,
  /** @type {string} */ username,
  /** @type {string} */ password,
) => {
  // Our own backend authenticates by session; one configured elsewhere has no
  // session with us, so it keeps verifying credentials and re-sending them.
  const authenticate = _is_own_backend(state)
    ? start_session(state, username, password)
    : verify_credentials(state, username, password).then((user_id) => {
        state.auth.credentials.value = { username, password };
        return user_id;
      });

  return authenticate
    .then((user_id) => {
      state.auth.user.id.value = user_id;
      state.auth.logged_in.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: "authenticated",
      };
    })
    .catch(
      () =>
        (state.auth.logged_in.value = {
          status: RESPONSE_STATE.ERROR,
          data: "wrong_login",
        }),
    );
};

/** Forget everything this tab knows about who is signed in. */
const clear_local_session = (state) => {
  state.auth.credentials.value = { username: null, password: null };
  state.auth.user.id.value = null;
  state.auth.logged_in.value = {
    status: RESPONSE_STATE.ERROR,
    data: "unauthenticated",
  };
};

/**
 * Logout current user.
 *
 * Awaits the server before clearing anything. It used to fire the request and
 * tear down the local session regardless of the outcome, so a rejected logout
 * still showed the user as signed out while their server session lived on — and
 * the request went to the *legacy* webapp's path, which a neo-only deployment
 * does not even serve.
 *
 * @param {Object} state - Application state
 */
const logout = async (state) => {
  if (!_is_own_backend(state)) {
    // Nothing to end: those requests only ever carried a header.
    clear_local_session(state);
    return;
  }

  const headers = new Headers();
  set_request_headers(headers, state);

  const response = await fetch(`${_url_auth()}/logout`, {
    method: "POST",
    headers,
    credentials: "include",
  }).catch(() => null);

  if (!response?.ok) {
    state.auth.logout_response.value = {
      status: RESPONSE_STATE.ERROR,
      data: "logout_failed",
    };
    return;
  }

  state.auth.logout_response.value = {
    status: RESPONSE_STATE.SUCCESS,
    data: "logged_out",
  };
  clear_local_session(state);
};

const is_authenticated = async (state) => {
  if (is_oauth()) {
    // OAuth: restore an existing session — a server-side one reported by
    // config.json, or tokens held in sessionStorage for the PKCE flow
    const restored = await restore_oauth_session(state);
    if (restored) return state.auth.logged_in.value;
    // Check for OAuth callback (authorization code in URL)
    const handled = await handle_oauth_callback(state);
    if (handled) return state.auth.logged_in.value;
    // No session, no callback — unauthenticated
    return (state.auth.logged_in.value = {
      status: RESPONSE_STATE.ERROR,
      data: "unauthenticated",
    });
  }

  const signal = state.auth.logged_in;

  if (_is_own_backend(state)) {
    // Ask the server who we are. A reload keeps the user signed in because the
    // session cookie survives it — no credentials are held on the client to
    // re-verify with, which is the point.
    signal.value = { status: RESPONSE_STATE.LOADING };
    const user_id = await session_user(state);
    if (!user_id) {
      return (signal.value = {
        status: RESPONSE_STATE.ERROR,
        data: "unauthenticated",
      });
    }
    state.auth.user.id.value = user_id;
    return (signal.value = {
      status: RESPONSE_STATE.SUCCESS,
      data: "authenticated",
    });
  }

  // A backend configured elsewhere still authenticates per request, so it needs
  // credentials — held in memory for this tab only, never persisted.
  if (!state.auth.credentials.value?.username) {
    return (signal.value = {
      status: RESPONSE_STATE.ERROR,
      data: "unauthenticated",
    });
  }

  signal.value = { status: RESPONSE_STATE.LOADING };
  const { username, password } = state.auth.credentials.value;

  try {
    // Re-verify rather than probing an arbitrary endpoint, for the same reason
    // login does: against an unauthenticated REST API a probe proves nothing.
    state.auth.user.id.value = await verify_credentials(
      state,
      username,
      password,
    );
    return (signal.value = {
      status: RESPONSE_STATE.SUCCESS,
      data: "authenticated",
    });
  } catch {
    clear_local_session(state);
    return signal.value;
  }
};

const auth = {
  // Dispatch at call time: the mode is only known once config.json has loaded
  logout: (state) => (is_oauth() ? oauth_logout(state) : logout(state)),
  login,
  cookies,
  is_authenticated,
  start_oauth_login,
};

export default auth;
