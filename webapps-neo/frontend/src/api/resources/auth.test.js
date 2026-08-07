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

import { POST_SERVER_URL, RESPONSE_STATE } from "../helper.jsx";
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

  describe("login", () => {
    it("stores credentials, marks authenticated and persists the session on success", async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => ({ id: "bob" }),
      });
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
      // Sends a Basic auth header derived from the supplied credentials.
      expect(fetchMock.mock.calls[0][1].headers.get("Authorization")).toMatch(
        /^Basic /,
      );
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

      expect(POST_SERVER_URL).toHaveBeenCalled();
      expect(POST_SERVER_URL.mock.lastCall[0]).toBe(
        "/api/admin/auth/user/default/logout",
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

    it("verifies stored credentials against /authorization and authenticates", async () => {
      fetchMock.mockResolvedValue({ ok: true, json: async () => [] });
      await auth.is_authenticated(state);

      expect(fetchMock.mock.calls[0][0]).toBe(
        "http://localhost:8080/engine-rest/authorization",
      );
      expect(state.auth.logged_in.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: "authenticated",
      });
    });

    it("is unauthenticated when the verification request fails", async () => {
      fetchMock.mockResolvedValue({ ok: false, status: 401 });
      await auth.is_authenticated(state);
      expect(state.auth.logged_in.value.data).toBe("unauthenticated");
    });

    it("restores a persisted basic-auth session before checking", async () => {
      state.auth.credentials.value = { username: null, password: null };
      sessionStorage.setItem(
        BASIC_AUTH_KEY,
        JSON.stringify({ username: "carol", password: "pw" }),
      );
      fetchMock.mockResolvedValue({ ok: true, json: async () => [] });
      await auth.is_authenticated(state);

      expect(state.auth.credentials.value).toEqual({
        username: "carol",
        password: "pw",
      });
      expect(state.auth.logged_in.value.data).toBe("authenticated");
    });
  });
});
