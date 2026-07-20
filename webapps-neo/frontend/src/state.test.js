import { describe, it, expect, beforeEach } from "vitest";
import { createAppState } from "./state.js";

// VITE_BACKEND is defined in src/test/setup.js as:
//   [{ name: "Test", url: "http://localhost:8080" }, { name: "Other", url: "http://localhost:9090", c7_mode: true }]

describe("state", () => {
  beforeEach(() => localStorage.clear());

  describe("get_stored_server (via createAppState)", () => {
    it("defaults to the first configured backend and persists it", () => {
      const state = createAppState();
      expect(state.server.value).toEqual({
        name: "Test",
        url: "http://localhost:8080",
      });
      expect(JSON.parse(localStorage.getItem("server")).url).toBe(
        "http://localhost:8080",
      );
    });

    it("restores a stored server that still exists in the configured list", () => {
      localStorage.setItem(
        "server",
        JSON.stringify({ name: "Other", url: "http://localhost:9090" }),
      );
      const state = createAppState();
      expect(state.server.value.url).toBe("http://localhost:9090");
    });

    it("falls back to the first backend when the stored server is unknown", () => {
      localStorage.setItem(
        "server",
        JSON.stringify({ name: "Gone", url: "http://gone.example" }),
      );
      const state = createAppState();
      expect(state.server.value.url).toBe("http://localhost:8080");
    });
  });

  describe("state tree shape", () => {
    it("exposes auth signals with sensible defaults", () => {
      const state = createAppState();
      expect(state.auth.mode).toBe("basic");
      expect(state.auth.logged_in.value).toEqual({ data: "unknown" });
      expect(state.auth.credentials.value).toEqual({
        username: null,
        password: null,
      });
    });

    it("mirrors the REST resource structure under api.*", () => {
      const { api } = createAppState();
      expect(api.process.definition.list.value).toBeNull();
      expect(api.process.instance.one.value).toBeNull();
      expect(api.task.comment.list.value).toBeNull();
      expect(api.authorization.all.value).toBeNull();
      expect(api.batch.list.value).toBeNull();
      expect(api.batch.one.value).toBeNull();
    });
  });
});
