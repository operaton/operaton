import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { load_plugins } from "./loader.js";
import { plugins_for, _reset_registry } from "./registry.js";
import { PLUGIN_POINTS } from "./points.js";
import { plugin_apis } from "../api/plugins.js";

const remote_good = {
  default: {
    id: "remote-good",
    point: PLUGIN_POINTS.PAGE,
    properties: { href: "/plugin/g", nameKey: "g" },
    Component: () => null,
  },
};

const manifest = (entries) =>
  vi.fn(async () => ({ ok: true, json: async () => entries }));

beforeEach(() => {
  _reset_registry();
  for (const key of Object.keys(plugin_apis)) delete plugin_apis[key];
  document.head.innerHTML = "";
  vi.spyOn(console, "error").mockImplementation(() => {});
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("plugins/loader", () => {
  it("registers a manifest plugin and skips a broken one", async () => {
    vi.stubGlobal(
      "fetch",
      manifest([
        { name: "good", location: "/plugins/good" },
        { name: "bad", location: "/plugins/bad" },
      ]),
    );
    const importer = vi.fn(async (url) => {
      if (url.includes("/plugins/good/")) return remote_good;
      throw new Error("boom");
    });

    await load_plugins({ importer });

    expect(plugins_for(PLUGIN_POINTS.PAGE).map((p) => p.id)).toContain(
      "remote-good",
    );
    expect(importer).toHaveBeenCalledTimes(2);
    // the broken plugin is logged, not thrown
    expect(console.error).toHaveBeenCalled();
  });

  it("injects a stylesheet link when the package declares css", async () => {
    vi.stubGlobal(
      "fetch",
      manifest([{ name: "g", location: "/plugins/g", css: "plugin.css" }]),
    );
    await load_plugins({ importer: vi.fn(async () => remote_good) });

    const link = document.head.querySelector('link[rel="stylesheet"]');
    expect(link?.getAttribute("href")).toContain("/plugins/g/plugin.css");
  });

  it("tolerates a missing manifest without importing anything", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({ ok: false })),
    );
    const importer = vi.fn();
    await expect(load_plugins({ importer })).resolves.toBeUndefined();
    expect(importer).not.toHaveBeenCalled();
  });

  it("always registers bundled plugins (the metrics example)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({ ok: false })),
    );
    await load_plugins({ importer: vi.fn() });
    expect(plugins_for(PLUGIN_POINTS.PAGE).map((p) => p.id)).toContain(
      "metrics",
    );
  });
});
