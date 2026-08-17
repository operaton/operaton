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

import { POST, RESPONSE_STATE } from "../helper.jsx";
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
    it("stores credentials, marks authenticated and persists the session on success", async () => {
      fetchMock.mockResolvedValue(verified());
      auth.login(state, "bob", "secret");

      await vi.waitFor(() =>
        expect(state.auth.logged_in.value.data).toBe("authenticated"),
      );
      expect(state.auth.credentials.value).toEqual({
        username: "bob",
        password: "secret",
      });
      expect(JSON.parse(sessionStorage.getItem(BASIC_AUTH_KEY))).toEqual({
        username: "bob",
        password: "secret",
      });
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
    it("clears the persisted session and resets auth state", () => {
      sessionStorage.setItem(
        BASIC_AUTH_KEY,
        JSON.stringify({ username: "bob" }),
      );
      auth.logout(state);

      expect(POST).toHaveBeenCalled();
      expect(POST.mock.lastCall[0]).toBe(
        "/operaton/api/admin/auth/user/default/logout",
      );
      expect(sessionStorage.getItem(BASIC_AUTH_KEY)).toBeNull();
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

    it("discards a stored session the identity service rejects", async () => {
      sessionStorage.setItem(
        BASIC_AUTH_KEY,
        JSON.stringify({ username: "bob", password: "stale" }),
      );
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({ authenticated: false }),
      });
      await auth.is_authenticated(state);

      expect(state.auth.logged_in.value.data).toBe("unauthenticated");
      expect(sessionStorage.getItem(BASIC_AUTH_KEY)).toBeNull();
    });

    it("restores a persisted basic-auth session before checking", async () => {
      state.auth.credentials.value = { username: null, password: null };
      sessionStorage.setItem(
        BASIC_AUTH_KEY,
        JSON.stringify({ username: "carol", password: "pw" }),
      );
      fetchMock.mockResolvedValue(verified("carol"));
      await auth.is_authenticated(state);

      expect(state.auth.credentials.value).toEqual({
        username: "carol",
        password: "pw",
      });
      expect(state.auth.logged_in.value.data).toBe("authenticated");
    });
  });
});
