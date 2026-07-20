/**
 * Shared state helpers for tests.
 *
 * Intentionally free of imports from `api/helper.jsx` / `api/engine_rest.jsx` so
 * that test files which `vi.mock("../helper.jsx")` (the API-resource tests) can
 * import these utilities without the mock bleeding into the helpers.
 *
 * `createAppState` only depends on preact + signals, so building the real state
 * tree here is safe everywhere.
 */
import { expect } from "vitest";
import { createAppState } from "../state.js";

export const RESPONSE_STATE = {
  NOT_INITIALIZED: "NOT_INITIALIZED",
  LOADING: "LOADING",
  SUCCESS: "SUCCESS",
  ERROR: "ERROR",
};

/**
 * A fresh global state tree (all signals), as the app uses at runtime.
 * Sets default basic-auth credentials so `get_auth_header` works in API tests.
 */
export const create_mock_state = () => {
  const state = createAppState();
  state.auth.credentials.value = { username: "demo", password: "demo" };
  return state;
};

/** Populate a request signal with a SUCCESS response carrying `data`. */
export const signal_response = (
  signl,
  data,
  status = RESPONSE_STATE.SUCCESS,
) => {
  signl.value = { status, data };
  return signl;
};

/** Set a request signal to the ERROR state. */
export const signal_error = (signl, error = { message: "boom" }) => {
  signl.value = { status: RESPONSE_STATE.ERROR, error };
  return signl;
};

/**
 * Assert that a mocked API wrapper (GET/POST/PUT/DELETE/...) was called with the
 * expected arguments. The URL/body are compared structurally; `state` and the
 * target `signal` are compared **by reference**.
 *
 * This deliberately avoids `toHaveBeenCalledWith(..., state, signal)`: when any
 * arg needs structural comparison, the matcher walks every arg structurally,
 * and recursing into the Preact signal tree throws.
 *
 * Positional, matching the wrappers' signatures — `GET(url, state, signal)` and
 * `POST(url, body, state, signal)`. Omit `body` for the GET-style arity.
 */
export const expect_api_call = (fn, { url, body, state, signal } = {}) => {
  expect(fn).toHaveBeenCalled();
  const call = fn.mock.lastCall ?? [];
  let i = 0;
  if (url !== undefined) expect(call[i++]).toBe(url);
  if (body !== undefined) expect(call[i++]).toEqual(body);
  if (state !== undefined) expect(call[i++]).toBe(state);
  if (signal !== undefined) expect(call[i++]).toBe(signal);
};
