import { describe, it, expect, afterEach } from "vitest";
import { plugin_apis, mount_plugin_api } from "./plugins.js";
import engine_rest from "./engine_rest.jsx";

afterEach(() => {
  for (const key of Object.keys(plugin_apis)) delete plugin_apis[key];
});

describe("api/plugins", () => {
  it("mounts a plugin's functions under plugin_apis.<id>", () => {
    const fns = { go: () => {} };
    mount_plugin_api("demo", fns);
    // Same object reference — the container holds what it is handed, verbatim.
    expect(plugin_apis.demo).toBe(fns);
    expect(plugin_apis.demo.go).toBe(fns.go);
  });

  it("is the same object exposed as engine_rest.plugins", () => {
    // engine_rest.plugins IS the plugin_apis container, so anything mounted is
    // immediately reachable as engine_rest.plugins.<id>.
    expect(engine_rest.plugins).toBe(plugin_apis);
    mount_plugin_api("demo", { go: () => {} });
    expect(engine_rest.plugins.demo).toBe(plugin_apis.demo);
  });

  it("overwrites a previously mounted namespace of the same id", () => {
    const first = { a: () => {} };
    const second = { b: () => {} };
    mount_plugin_api("demo", first);
    mount_plugin_api("demo", second);
    expect(plugin_apis.demo).toBe(second);
  });
});
