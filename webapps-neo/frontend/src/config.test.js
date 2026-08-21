import { describe, it, expect, beforeEach, vi, afterEach } from "vitest";
import { load_config, get_config, set_config } from "./config.js";

describe("config", () => {
  beforeEach(() => set_config(null));
  afterEach(() => vi.unstubAllGlobals());

  const serve = (json, ok = true) =>
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok, json: async () => json }),
    );

  describe("from the server", () => {
    it("reports basic mode", async () => {
      serve({ backends: [{ name: "Operaton", url: "" }], authMode: "basic" });
      const config = await load_config();

      expect(config.auth_mode).toBe("basic");
      expect(config.oauth).toBeUndefined();
    });

    it("reports oauth2 with the login target the server chose", async () => {
      serve({
        authMode: "oauth2",
        oauth: {
          flow: "session",
          login: "/oauth2/authorization/operaton",
          logout: "/logout",
        },
      });
      const config = await load_config();

      expect(config.auth_mode).toBe("oauth2");
      expect(config.oauth.flow).toBe("session");
      expect(config.oauth.login).toBe("/oauth2/authorization/operaton");
    });

    it("passes through an already authenticated user", async () => {
      serve({ authMode: "oauth2", oauth: { flow: "session", login: "/x" }, user: { id: "demo" } });
      expect((await load_config()).user).toEqual({ id: "demo" });
    });

    it("defaults to a single same-origin backend", async () => {
      serve({ authMode: "basic" });
      expect((await load_config()).backends).toEqual([
        { name: "Operaton", url: "" },
      ]);
    });
  });

  describe("degrading safely", () => {
    it("falls back to the build-time environment when the request fails", async () => {
      vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("offline")));
      expect((await load_config()).auth_mode).toBe("basic");
    });

    it("falls back when no config.json is served", async () => {
      serve({}, false);
      expect((await load_config()).auth_mode).toBe("basic");
    });

    // The Docker entrypoint only substitutes placeholders the operator set, so a
    // leftover DOCKER_RUN_PLACEHOLDER_* name must not be taken for a real value.
    it("ignores unsubstituted placeholders", async () => {
      serve({
        authMode: "DOCKER_RUN_PLACEHOLDER_AUTH_MODE",
        pluginsUrl: "DOCKER_RUN_PLACEHOLDER_PLUGINS_URL",
      });
      const config = await load_config();

      expect(config.auth_mode).toBe("basic");
      expect(config.plugins_url).toBeUndefined();
    });

    // Otherwise the SPA shows a login button that navigates nowhere - which is
    // exactly the bug this configuration mechanism was built to fix.
    it("falls back to basic when a pkce flow has no authority to talk to", async () => {
      serve({ authMode: "oauth", oauth: { flow: "pkce", authority: "" } });
      const config = await load_config();

      expect(config.auth_mode).toBe("basic");
      expect(config.oauth).toBeUndefined();
    });

    it("accepts the legacy 'oauth' spelling and string booleans", async () => {
      serve({
        authMode: "oauth",
        oauth: { flow: "pkce", authority: "https://idp.example.com" },
        hideReleaseWarning: "true",
      });
      const config = await load_config();

      expect(config.auth_mode).toBe("oauth2");
      expect(config.hide_release_warning).toBe(true);
    });
  });

  it("get_config falls back to the environment when read before loading", () => {
    expect(get_config().auth_mode).toBe("basic");
  });
});
