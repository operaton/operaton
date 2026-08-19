import { _url_api, set_request_headers, RESPONSE_STATE } from "../helper.jsx";
import { get_config } from "../../config.js";

/**
 * First-run setup.
 *
 * These two calls are the only ones in the app that are made with nobody signed in — by
 * definition, since their whole purpose is to create the first account. The server guards
 * them: once any user belongs to the `operaton-admin` group, both stop answering.
 *
 * They talk to the webapp's own admin namespace, not to the engine API, so they go through
 * `_url_api()` the way `_url_auth()` does rather than through `_url_engine_rest`.
 */

/** Base of the webapp's setup resource. */
const _url_setup = () => `${_url_api()}/admin/setup/${get_config().engine}`;

/**
 * Whether an initial user can still be created.
 *
 * Answers `false` on any failure rather than rejecting: a deployment that has already been
 * set up, an engine that is not reachable, or an older backend without the endpoint should
 * all land the user on the normal login screen, never on a setup form that cannot work.
 *
 * @returns {Promise<boolean>}
 */
const is_setup_available = (timeout_ms = 5000) => {
  const headers = new Headers();
  headers.set("Accept", "application/json");

  const probe = fetch(_url_setup(), { headers, credentials: "include" })
    .then((response) => (response.ok ? response.json() : null))
    .then((json) => json?.setupAvailable === true)
    .catch(() => false);

  // The login screen waits on this answer, so a backend that never responds must not leave
  // the user staring at nothing. Losing the race means "no setup" and the login mask.
  return Promise.race([
    probe,
    new Promise((resolve) => setTimeout(() => resolve(false), timeout_ms)),
  ]);
};

/**
 * Create the first administrator and make it a member of `operaton-admin`.
 *
 * @param {Object} state - Application state
 * @param {Object} user - `{profile: {id, firstName, lastName, email}, credentials: {password}}`
 */
const create_initial_user = (state, user) => {
  const signal = state.api.setup.create_initial_user;
  signal.value = { status: RESPONSE_STATE.LOADING };

  const headers = new Headers();
  set_request_headers(headers, state);
  headers.set("Content-Type", "application/json");

  return fetch(`${_url_setup()}/user/create`, {
    method: "POST",
    headers,
    credentials: "include",
    body: JSON.stringify(user),
  })
    .then((response) => {
      if (!response.ok) return Promise.reject(response);
      return (signal.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: user.profile.id,
      });
    })
    .catch((error) => (signal.value = { status: RESPONSE_STATE.ERROR, error }));
};

export default { is_setup_available, create_initial_user };
