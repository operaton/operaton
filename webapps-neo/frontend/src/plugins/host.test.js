import { describe, it, expect, afterEach } from "vitest";
import { install_plugin_host } from "./host.js";
import { register } from "./registry.js";
import { use_plugin_api } from "./plugin_api.jsx";
import { PLUGIN_POINTS } from "./points.js";

afterEach(() => {
  delete window.__OPERATON_PLUGIN_HOST__;
});

describe("plugins/host", () => {
  it("exposes the host bridge on window with every documented primitive", () => {
    install_plugin_host();
    const host = window.__OPERATON_PLUGIN_HOST__;
    expect(host).toBeTruthy();
    // The full contract remote no-build plugins rely on.
    for (const key of [
      "h",
      "Fragment",
      "html",
      "hooks",
      "signals",
      "AppState",
      "engine_rest",
      "RequestState",
      "RESPONSE_STATE",
      "has_data",
      "GET",
      "POST",
      "PUT",
      "DELETE",
      "register",
      "use_plugin_api",
      "PLUGIN_POINTS",
    ])
      expect(host).toHaveProperty(key);
  });

  it("shares the host's own registry/hook/points instances (not copies)", () => {
    install_plugin_host();
    const host = window.__OPERATON_PLUGIN_HOST__;
    // Remote plugins must register into the SAME registry the app reads, and
    // call the SAME hook, or they'd get an isolated (broken) instance.
    expect(host.register).toBe(register);
    expect(host.use_plugin_api).toBe(use_plugin_api);
    expect(host.PLUGIN_POINTS).toBe(PLUGIN_POINTS);
    expect(typeof host.html).toBe("function");
  });
});
