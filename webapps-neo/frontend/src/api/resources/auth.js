import {
  GET_SERVER_URL,
  POST,
  _url_engine_rest,
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

const BASIC_AUTH_KEY = "basic_auth";

/**
 * Restore Basic Auth credentials persisted in sessionStorage into the signal.
 * Survives page reloads within the tab; cleared when the tab is closed.
 * @param {Object} state - Application state
 * @returns {boolean} whether credentials were restored
 */
const restore_basic_session = (state) => {
  const stored = sessionStorage.getItem(BASIC_AUTH_KEY);
  if (!stored) return false;
  state.auth.credentials.value = JSON.parse(stored);
  return true;
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
) =>
  verify_credentials(state, username, password)
    .then((user_id) => {
      state.auth.credentials.value = { username, password };
      state.auth.user.id.value = user_id;
      state.auth.logged_in.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: "authenticated",
      };
      sessionStorage.setItem(
        BASIC_AUTH_KEY,
        JSON.stringify({ username, password }),
      );
    })
    .catch(
      () =>
        (state.auth.logged_in.value = {
          status: RESPONSE_STATE.ERROR,
          data: "wrong_login",
        }),
    );
/**
 * Logout current user
 * @param {Object} state - Application state
 */
const logout = (state) => {
  // Dispatch the server logout while credentials are still valid, then clear
  // the client session and drop back to the login screen.
  const response = POST(
    "/operaton/api/admin/auth/user/default/logout",
    null,
    state,
    state.auth.logout_response,
  );
  sessionStorage.removeItem(BASIC_AUTH_KEY);
  state.auth.credentials.value = { username: null, password: null };
  state.auth.logged_in.value = {
    status: RESPONSE_STATE.ERROR,
    data: "unauthenticated",
  };
  return response;
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

  // Basic auth: restore a persisted session, then require credentials
  restore_basic_session(state);
  const signal = state.auth.logged_in;
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
    sessionStorage.removeItem(BASIC_AUTH_KEY);
    state.auth.credentials.value = { username: null, password: null };
    return (signal.value = {
      status: RESPONSE_STATE.ERROR,
      data: "unauthenticated",
    });
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
