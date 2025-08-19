import {
  GET_SERVER_URL,
  POST,
  POST_SERVER_URL,
  GET,
  _url_engine_rest,
  RESPONSE_STATE,
} from "../helper.jsx";
import user from "./user.js";

const cookies = (state) =>
  GET_SERVER_URL("/operaton/app/cockpit/default/", state, state.auth.cookies);

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
  /** @type {boolean} */ remember_login
) => {
  const login_form_data = new FormData();
  login_form_data.append("username", username);
  login_form_data.append("password", password);
  let headers = new Headers();
  headers.set(
    "Authorization",
    `Basic ${window.btoa(unescape(encodeURIComponent(`${username}:${password}`)))}`,
  ); //TODO authentication

  fetch(`${_url_engine_rest(state)}/user`, { headers })
    .then((response) =>
      response.ok ? response.json() : Promise.reject(response),
    )
    .then((data) => {
      state.auth.logged_in.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: "authenticated",
      };
      state.auth.credentials = { username, password };
      if (remember_login) {
        document.cookie = `credentials={"username": "${username}", "password": "${password}"};path=/`
      }
    })
    .catch(
      (error) =>
        (state.auth.logged_in.value = {
          status: RESPONSE_STATE.ERROR,
          data: "wrong_login",
        }),
    );
};
/**
 * Logout current user
 * @param {Object} state - Application state
 */
const logout = (state) =>
  POST(
    "/operaton/api/admin/auth/user/default/logout",
    null,
    state,
    state.auth.logout_response,
  );

const is_authenticated = async (state) => {
  // GET('/authorization', state, state.auth.logged_in)
  const signal = state.auth.logged_in;
  signal.value = { status: RESPONSE_STATE.LOADING };

  try {
    const response = await fetch(`${_url_engine_rest(state)}/authorization`, {
      headers: { "Content-Type": "application/json" },
      credentials: "include",
    });
    await (response.ok ? response.json() : Promise.reject(response));
    return (signal.value = {
      status: RESPONSE_STATE.SUCCESS,
      data: "authenticated",
    });
  } catch (error) {
    return (signal.value = {
      status: RESPONSE_STATE.ERROR,
      data: "unauthenticated",
    });
  }
};

const auth = {
  logout,
  login,
  cookies,
  is_authenticated,
};

export default auth;
