import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import {
  is_oauth,
  is_session_flow,
  start_oauth_login,
  oauth_logout,
  restore_oauth_session,
  handle_oauth_callback,
} from "./oauth.js";
import { create_mock_state } from "../test/helpers.js";
import { set_config } from "../config.js";

/**
 * The server-side flow: Spring Security performs the identity provider
 * handshake and hands us a session cookie. The SPA holds no token — it only
 * navigates the browser in and out of the flow.
 */
describe("api/oauth (server-side session flow)", () => {
  let state, location;

  const session_config = (extra = {}) =>
    set_config({
      authMode: "oauth2",
      oauth: {
        flow: "session",
        login: "/oauth2/authorization/operaton",
        logout: "/logout",
      },
      ...extra,
    });

  beforeEach(() => {
    state = create_mock_state();
    sessionStorage.clear();
    // window.location.href is a navigation in a real browser; capture it instead
    location = { href: "" };
    vi.stubGlobal("location", location);
    session_config();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    set_config(null);
  });

  it("is recognised as oauth using the session flow", () => {
    expect(is_oauth()).toBe(true);
    expect(is_session_flow()).toBe(true);
  });

  it("starts login by navigating to the server, not to the provider", async () => {
    await start_oauth_login();
    expect(location.href).toBe("/oauth2/authorization/operaton");
  });

  it("logs out through the server so the session cookie is cleared", () => {
    oauth_logout(state);
    expect(location.href).toBe("/logout");
  });

  it("restores the session from the user the server reported", async () => {
    session_config({ user: { id: "demo" } });

    expect(await restore_oauth_session(state)).toBe(true);
    expect(state.auth.user.id.value).toBe("demo");
    expect(state.auth.logged_in.value.data).toBe("authenticated");
  });

  it("reports no session when the server reported no user", async () => {
    expect(await restore_oauth_session(state)).toBe(false);
  });

  // Spring Security consumes the authorization code at its own redirect
  // endpoint, long before the SPA is loaded.
  it("does not try to exchange an authorization code itself", async () => {
    expect(await handle_oauth_callback(state)).toBe(false);
  });
});

describe("api/oauth (browser pkce flow)", () => {
  afterEach(() => set_config(null));

  it("still talks to the identity provider directly", () => {
    set_config({
      authMode: "oauth2",
      oauth: {
        flow: "pkce",
        authority: "https://idp.example.com/realms/operaton",
        clientId: "operaton-web-apps",
      },
    });

    expect(is_oauth()).toBe(true);
    expect(is_session_flow()).toBe(false);
  });
});
