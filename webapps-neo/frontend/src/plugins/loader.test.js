import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { load_plugins, is_allowed_location } from "./loader.js";
import { plugins_for, _reset_registry } from "./registry.js";
import { PLUGIN_POINTS } from "./points.js";
import { plugin_apis } from "../api/plugins.js";
import { set_config } from "../config.js";

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

/** Remote plugins are opt-in; most cases here need them switched on. */
const enable_remote = (allow_origins) =>
  set_config({
    remotePluginsEnabled: true,
    ...(allow_origins ? { remotePluginsAllowOrigins: allow_origins } : {}),
  });

beforeEach(() => {
  _reset_registry();
  for (const key of Object.keys(plugin_apis)) delete plugin_apis[key];
  document.head.innerHTML = "";
  vi.spyOn(console, "error").mockImplementation(() => {});
  set_config(null);
});

afterEach(() => {
  vi.unstubAllGlobals();
  set_config(null);
});

describe("plugins/loader", () => {
  it("registers a manifest plugin and skips a broken one", async () => {
    enable_remote();
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
    enable_remote();
    vi.stubGlobal(
      "fetch",
      manifest([{ name: "g", location: "/plugins/g", css: "plugin.css" }]),
    );
    await load_plugins({ importer: vi.fn(async () => remote_good) });

    const link = document.head.querySelector('link[rel="stylesheet"]');
    expect(link?.getAttribute("href")).toContain("/plugins/g/plugin.css");
  });

  it("tolerates a missing manifest without importing anything", async () => {
    enable_remote();
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

  it("does not fetch the manifest at all unless remote plugins are enabled", async () => {
    const fetch_spy = manifest([{ name: "g", location: "/plugins/g" }]);
    vi.stubGlobal("fetch", fetch_spy);
    const importer = vi.fn();

    await load_plugins({ importer });

    expect(fetch_spy).not.toHaveBeenCalled();
    expect(importer).not.toHaveBeenCalled();
    // bundled plugins are unaffected by the gate
    expect(plugins_for(PLUGIN_POINTS.PAGE).map((p) => p.id)).toContain(
      "metrics",
    );
  });

  it("refuses a cross-origin plugin that is not in the allow list", async () => {
    enable_remote();
    vi.stubGlobal(
      "fetch",
      manifest([{ name: "evil", location: "https://evil.example/p" }]),
    );
    const importer = vi.fn();

    await load_plugins({ importer });

    expect(importer).not.toHaveBeenCalled();
    expect(document.head.querySelector('link[rel="stylesheet"]')).toBeNull();
  });

  it("loads a cross-origin plugin once its origin is allowed", async () => {
    enable_remote(["https://trusted.example"]);
    vi.stubGlobal(
      "fetch",
      manifest([{ name: "t", location: "https://trusted.example/p" }]),
    );
    const importer = vi.fn(async () => remote_good);

    await load_plugins({ importer });

    expect(importer).toHaveBeenCalledTimes(1);
    expect(plugins_for(PLUGIN_POINTS.PAGE).map((p) => p.id)).toContain(
      "remote-good",
    );
  });
});

describe("plugins/loader is_allowed_location", () => {
  it("accepts same-origin locations, absolute or relative", () => {
    expect(is_allowed_location("/plugins/good")).toBe(true);
    expect(is_allowed_location(`${window.location.origin}/plugins/good`)).toBe(
      true,
    );
  });

  it("rejects other origins unless listed", () => {
    expect(is_allowed_location("https://evil.example/p")).toBe(false);
    expect(
      is_allowed_location("https://evil.example/p", ["https://trusted.example"]),
    ).toBe(false);
    expect(
      is_allowed_location("https://trusted.example/p", [
        "https://trusted.example",
      ]),
    ).toBe(true);
  });

  it("rejects a location it cannot parse, and ignores unparseable allow entries", () => {
    expect(is_allowed_location(undefined)).toBe(false);
    expect(is_allowed_location("http://[bad", ["https://trusted.example"])).toBe(
      false,
    );
    expect(is_allowed_location("https://evil.example/p", ["not a url"])).toBe(
      false,
    );
  });
});
