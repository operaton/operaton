import { describe, it, expect, beforeEach } from "vitest";
import {
  is_oauth,
  handle_oauth_callback,
  refresh_oauth_token,
  restore_oauth_session,
} from "./oauth.js";
import { create_mock_state } from "../test/helpers.js";

// No VITE_AUTH_MODE is set in tests, so the module is in "basic" mode.
describe("api/oauth (basic mode defaults)", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    sessionStorage.clear();
  });

  it("reports is_oauth === false when auth mode is not 'oauth'", () => {
    expect(is_oauth).toBe(false);
  });

  it("handle_oauth_callback returns false when there is no code/verifier", async () => {
    expect(await handle_oauth_callback(state)).toBe(false);
  });

  it("refresh_oauth_token returns false without a stored refresh token", async () => {
    expect(await refresh_oauth_token(state)).toBe(false);
  });

  it("restore_oauth_session returns false without a stored access token", async () => {
    expect(await restore_oauth_session(state)).toBe(false);
  });

  it("restore_oauth_session refreshes (and fails) when the access token is expired", async () => {
    // exp far in the past -> is_token_expired -> refresh_oauth_token -> no refresh token -> false
    const expired_payload = btoa(JSON.stringify({ exp: 1 }));
    sessionStorage.setItem(
      "oauth_access_token",
      `header.${expired_payload}.sig`,
    );
    expect(await restore_oauth_session(state)).toBe(false);
  });
});
