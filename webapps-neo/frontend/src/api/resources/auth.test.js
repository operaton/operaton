import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

// Keep url/auth-header/RESPONSE_STATE real; stub the request wrappers.
vi.mock("../helper.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    GET: vi.fn(),
    POST: vi.fn(),
    GET_SERVER_URL: vi.fn(),
    POST_SERVER_URL: vi.fn(),
  };
});

import { RESPONSE_STATE } from "../helper.jsx";
import { create_mock_state } from "../../test/helpers.js";
import auth from "./auth.js";

const BASIC_AUTH_KEY = "basic_auth";

describe("api/resources/auth (basic mode)", () => {
  let state, fetchMock;

  beforeEach(() => {
    state = create_mock_state();
    sessionStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });
  afterEach(() => vi.unstubAllGlobals());

  const verified = (user = "bob") => ({
    ok: true,
    json: async () => ({ authenticated: true, authenticatedUser: user }),
  });

  describe("login", () => {
    it("keeps credentials in memory for a remote backend, and never on disk", async () => {
      fetchMock.mockResolvedValue(verified());
      auth.login(state, "bob", "secret");

      await vi.waitFor(() =>
        expect(state.auth.logged_in.value.data).toBe("authenticated"),
      );
      // A backend configured elsewhere has no session with us, so its requests
      // still carry a Basic header and the credentials have to stay reachable.
      expect(state.auth.credentials.value).toEqual({
        username: "bob",
        password: "secret",
      });
      // ...but only for this tab. They used to be mirrored into sessionStorage
      // in cleartext, where any script in the page could read them.
      expect(sessionStorage.getItem(BASIC_AUTH_KEY)).toBeNull();
    });

    it("checks the credentials against the identity service", async () => {
      fetchMock.mockResolvedValue(verified());
      auth.login(state, "bob", "secret");

      await vi.waitFor(() =>
        expect(state.auth.logged_in.value.data).toBe("authenticated"),
      );
      const [url, options] = fetchMock.mock.calls[0];
      expect(url).toBe("http://localhost:8080/engine-rest/identity/verify");
      expect(options.method).toBe("POST");
      expect(JSON.parse(options.body)).toEqual({
        username: "bob",
        password: "secret",
      });
    });

    it("records the user the identity service resolved", async () => {
      // The web apps need to know who is looking in order to load anything
      // user-specific, whether or not the REST API demanded credentials.
      fetchMock.mockResolvedValue(verified("bob.smith"));
      auth.login(state, "bob", "secret");

      await vi.waitFor(() =>
        expect(state.auth.logged_in.value.data).toBe("authenticated"),
      );
      expect(state.auth.user.id.value).toBe("bob.smith");
    });

    // The endpoint answers 200 with authenticated:false, so an ok response is
    // not by itself a successful login. Probing an arbitrary endpoint with a
    // Basic header used to accept ANY password against an unauthenticated API.
    it("rejects a wrong password even when the API needs no authentication", async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({ authenticated: false, authenticatedUser: "bob" }),
      });
      auth.login(state, "bob", "wrong");

      await vi.waitFor(() =>
        expect(state.auth.logged_in.value.data).toBe("wrong_login"),
      );
      // nothing is persisted, so a reload lands back on the login screen
      expect(sessionStorage.getItem(BASIC_AUTH_KEY)).toBeNull();
    });

    it("marks wrong_login on a failed response", async () => {
      fetchMock.mockResolvedValue({ ok: false, status: 401 });
      auth.login(state, "bob", "wrong");
      await vi.waitFor(() =>
        expect(state.auth.logged_in.value.data).toBe("wrong_login"),
      );
      expect(state.auth.logged_in.value.status).toBe(RESPONSE_STATE.ERROR);
    });
  });

  describe("logout", () => {
    it("resets auth state for a remote backend, which has no session to end", async () => {
      state.auth.credentials.value = { username: "bob", password: "secret" };
      await auth.logout(state);

      expect(fetchMock).not.toHaveBeenCalled();
      expect(state.auth.credentials.value).toEqual({
        username: null,
        password: null,
      });
      expect(state.auth.logged_in.value).toEqual({
        status: RESPONSE_STATE.ERROR,
        data: "unauthenticated",
      });
    });
  });

  describe("is_authenticated", () => {
    it("is unauthenticated when there are no credentials", async () => {
      state.auth.credentials.value = { username: null, password: null };
      await auth.is_authenticated(state);
      expect(state.auth.logged_in.value).toEqual({
        status: RESPONSE_STATE.ERROR,
        data: "unauthenticated",
      });
    });

    it("re-checks stored credentials against the identity service", async () => {
      fetchMock.mockResolvedValue(verified());
      await auth.is_authenticated(state);

      expect(fetchMock.mock.calls[0][0]).toBe(
        "http://localhost:8080/engine-rest/identity/verify",
      );
      expect(state.auth.logged_in.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: "authenticated",
      });
      expect(state.auth.user.id.value).toBe("bob");
    });

    it("is unauthenticated when the verification request fails", async () => {
      fetchMock.mockResolvedValue({ ok: false, status: 401 });
      await auth.is_authenticated(state);
      expect(state.auth.logged_in.value.data).toBe("unauthenticated");
    });

    it("drops credentials the identity service rejects", async () => {
      state.auth.credentials.value = { username: "bob", password: "stale" };
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({ authenticated: false }),
      });
      await auth.is_authenticated(state);

      expect(state.auth.logged_in.value.data).toBe("unauthenticated");
      expect(state.auth.credentials.value).toEqual({
        username: null,
        password: null,
      });
    });
  });
});

describe("api/resources/auth (own backend, session)", () => {
  let state, fetchMock;

  beforeEach(() => {
    state = create_mock_state();
    // No configured backend URL: requests go to the webapp that served us, so
    // the session cookie authenticates them.
    state.server.value = { name: "Operaton", url: "" };
    sessionStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });
  afterEach(() => vi.unstubAllGlobals());

  it("logs in against the webapp's own auth resource and stores no password", async () => {
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({ userId: "bob" }) });

    await auth.login(state, "bob", "secret");

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toContain("/api/admin/auth/user/default/login/");
    expect(options.method).toBe("POST");
    expect(options.credentials).toBe("include");
    expect(state.auth.user.id.value).toBe("bob");
    expect(state.auth.logged_in.value.data).toBe("authenticated");
    // The whole point: nothing on the client can re-send the password.
    expect(state.auth.credentials.value.password).toBeNull();
    expect(sessionStorage.getItem(BASIC_AUTH_KEY)).toBeNull();
  });

  it("marks wrong_login when the auth resource refuses", async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 401 });

    await auth.login(state, "bob", "wrong");

    expect(state.auth.logged_in.value.data).toBe("wrong_login");
  });

  it("restores a session from the server rather than from stored credentials", async () => {
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({ userId: "carol" }) });

    await auth.is_authenticated(state);

    expect(fetchMock.mock.calls[0][0]).toContain("/api/admin/auth/user/default");
    expect(state.auth.user.id.value).toBe("carol");
    expect(state.auth.logged_in.value.data).toBe("authenticated");
  });

  it("is unauthenticated when the server reports no session", async () => {
    fetchMock.mockResolvedValue({ ok: true, json: async () => ({}) });

    await auth.is_authenticated(state);

    expect(state.auth.logged_in.value.data).toBe("unauthenticated");
  });

  it("keeps the user signed in when logout fails", async () => {
    // Previously the local session was torn down regardless, so a rejected
    // logout showed the user as signed out while the server session lived on.
    state.auth.logged_in.value = {
      status: RESPONSE_STATE.SUCCESS,
      data: "authenticated",
    };
    fetchMock.mockResolvedValue({ ok: false, status: 403 });

    await auth.logout(state);

    expect(state.auth.logged_in.value.data).toBe("authenticated");
    expect(state.auth.logout_response.value.data).toBe("logout_failed");
  });

  it("clears the session once the server confirms the logout", async () => {
    fetchMock.mockResolvedValue({ ok: true, status: 204 });

    await auth.logout(state);

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toContain("/api/admin/auth/user/default/logout");
    expect(options.method).toBe("POST");
    expect(state.auth.logged_in.value.data).toBe("unauthenticated");
  });
});
