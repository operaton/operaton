import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

import setup from "./setup.js";
import { create_mock_state } from "../../test/helpers.js";
import { RESPONSE_STATE } from "../helper.jsx";

describe("api/resources/setup", () => {
  let fetch_mock;

  beforeEach(() => {
    fetch_mock = vi.fn();
    vi.stubGlobal("fetch", fetch_mock);
  });

  afterEach(() => vi.unstubAllGlobals());

  describe("is_setup_available", () => {
    it("reports true only when the server says so", async () => {
      fetch_mock.mockResolvedValue({
        ok: true,
        json: async () => ({ setupAvailable: true }),
      });

      await expect(setup.is_setup_available()).resolves.toBe(true);

      const [url] = fetch_mock.mock.calls[0];
      expect(url).toContain("/api/admin/setup/");
    });

    it("reports false when setup has already been done", async () => {
      fetch_mock.mockResolvedValue({
        ok: true,
        json: async () => ({ setupAvailable: false }),
      });

      await expect(setup.is_setup_available()).resolves.toBe(false);
    });

    /**
     * An older backend without the endpoint, or an engine that is down, must land the user
     * on the login screen rather than on a setup form that cannot work.
     */
    it("reports false when the endpoint is missing or the request fails", async () => {
      fetch_mock.mockResolvedValue({ ok: false, status: 404 });
      await expect(setup.is_setup_available()).resolves.toBe(false);

      fetch_mock.mockRejectedValue(new Error("connection refused"));
      await expect(setup.is_setup_available()).resolves.toBe(false);
    });

    /** The login screen waits on this, so a hung backend must not blank the page forever. */
    it("gives up and reports false rather than hanging", async () => {
      vi.useFakeTimers();
      fetch_mock.mockReturnValue(new Promise(() => {}));

      const pending = setup.is_setup_available(50);
      await vi.advanceTimersByTimeAsync(60);

      await expect(pending).resolves.toBe(false);
      vi.useRealTimers();
    });
  });

  describe("create_initial_user", () => {
    it("posts the user and records success", async () => {
      const state = create_mock_state();
      fetch_mock.mockResolvedValue({ ok: true });

      await setup.create_initial_user(state, {
        profile: { id: "admin" },
        credentials: { password: "s3cret" },
      });

      const [url, options] = fetch_mock.mock.calls[0];
      expect(url).toContain("/api/admin/setup/");
      expect(url.endsWith("/user/create")).toBe(true);
      expect(options.method).toBe("POST");
      expect(JSON.parse(options.body).profile.id).toBe("admin");
      expect(state.api.setup.create_initial_user.value.status).toBe(
        RESPONSE_STATE.SUCCESS,
      );
    });

    it("records an error when the server refuses", async () => {
      const state = create_mock_state();
      fetch_mock.mockResolvedValue({ ok: false, status: 403 });

      await setup.create_initial_user(state, {
        profile: { id: "admin" },
        credentials: { password: "s3cret" },
      });

      expect(state.api.setup.create_initial_user.value.status).toBe(
        RESPONSE_STATE.ERROR,
      );
    });
  });
});
