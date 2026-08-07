import { describe, it, expect } from "vitest";
import { signal } from "@preact/signals";
import {
  _url_server,
  _url_engine_rest,
  get_credentials,
  get_auth_header,
  has_data,
  RESPONSE_STATE,
} from "./helper.jsx";

// Test helper functions
const createMockState = (
  serverUrl,
  credentials = { username: "demo", password: "demo" },
) => ({
  server: {
    value: {
      url: serverUrl,
    },
  },
  auth: {
    credentials: { value: credentials },
  },
});

describe("api/helper", () => {
  describe("URL builders", () => {
    it("should build server URL from state", () => {
      const state = createMockState("http://localhost:8080");
      const result = _url_server(state);
      expect(result).toBe("http://localhost:8080");
    });

    it("should build engine REST URL from state", () => {
      const state = createMockState("http://localhost:8080");
      const result = _url_engine_rest(state);
      expect(result).toBe("http://localhost:8080/engine-rest");
    });

    it("should handle different server URLs", () => {
      const state = createMockState("https://example.com:9090");
      expect(_url_server(state)).toBe("https://example.com:9090");
      expect(_url_engine_rest(state)).toBe(
        "https://example.com:9090/engine-rest",
      );
    });

    it("should handle URLs without trailing slash", () => {
      const state = createMockState("http://127.0.0.1:5173");
      expect(_url_engine_rest(state)).toBe("http://127.0.0.1:5173/engine-rest");
    });
  });

  describe("get_credentials", () => {
    it("should format credentials as username:password", () => {
      const state = createMockState("http://localhost:8080", {
        username: "demo",
        password: "demo",
      });
      const result = get_credentials(state);
      expect(result).toBe("demo:demo");
    });

    it("should handle different credentials", () => {
      const state = createMockState("http://localhost:8080", {
        username: "admin",
        password: "secret123",
      });
      const result = get_credentials(state);
      expect(result).toBe("admin:secret123");
    });

    it("should handle special characters in credentials", () => {
      const state = createMockState("http://localhost:8080", {
        username: "user@example.com",
        password: "p@ssw0rd!",
      });
      const result = get_credentials(state);
      expect(result).toBe("user@example.com:p@ssw0rd!");
    });

    it("should handle empty credentials", () => {
      const state = createMockState("http://localhost:8080", {
        username: "",
        password: "",
      });
      const result = get_credentials(state);
      expect(result).toBe(":");
    });
  });

  describe("get_auth_header", () => {
    it("builds a Basic header from credentials in basic mode", () => {
      const state = {
        ...createMockState("http://x", { username: "demo", password: "demo" }),
        auth: {
          mode: "basic",
          credentials: { value: { username: "demo", password: "demo" } },
        },
      };
      expect(get_auth_header(state)).toBe(`Basic ${btoa("demo:demo")}`);
    });

    it("builds a Bearer header from the token in oauth mode", () => {
      const state = {
        auth: { mode: "oauth", token: { value: "abc.def.ghi" } },
      };
      expect(get_auth_header(state)).toBe("Bearer abc.def.ghi");
    });

    it("encodes non-ASCII credentials safely", () => {
      const state = {
        auth: {
          mode: "basic",
          credentials: { value: { username: "übör", password: "pä" } },
        },
      };
      // Should not throw and should be valid base64 of the UTF-8 bytes.
      const header = get_auth_header(state);
      expect(header.startsWith("Basic ")).toBe(true);
    });
  });

  describe("has_data", () => {
    it("is false for a null signal value", () => {
      expect(has_data(signal(null))).toBe(false);
    });

    it("is false while loading", () => {
      expect(has_data(signal({ status: RESPONSE_STATE.LOADING }))).toBe(false);
    });

    it("is false on SUCCESS with null data", () => {
      expect(
        has_data(signal({ status: RESPONSE_STATE.SUCCESS, data: null })),
      ).toBe(false);
    });

    it("is true on SUCCESS with data", () => {
      expect(
        has_data(signal({ status: RESPONSE_STATE.SUCCESS, data: [1] })),
      ).toBe(true);
    });
  });

  describe("RESPONSE_STATE constants", () => {
    it("should have NOT_INITIALIZED state", () => {
      expect(RESPONSE_STATE.NOT_INITIALIZED).toBe("NOT_INITIALIZED");
    });

    it("should have LOADING state", () => {
      expect(RESPONSE_STATE.LOADING).toBe("LOADING");
    });

    it("should have SUCCESS state", () => {
      expect(RESPONSE_STATE.SUCCESS).toBe("SUCCESS");
    });

    it("should have ERROR state", () => {
      expect(RESPONSE_STATE.ERROR).toBe("ERROR");
    });

    it("should have exactly 4 states", () => {
      const states = Object.keys(RESPONSE_STATE);
      expect(states).toHaveLength(4);
    });

    it("should have unique state values", () => {
      const values = Object.values(RESPONSE_STATE);
      const uniqueValues = new Set(values);
      expect(uniqueValues.size).toBe(values.length);
    });
  });
});
