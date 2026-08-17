import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { signal } from "@preact/signals";

// The hook reads entity ids from the route — make it addressable, keeping the
// real LocationProvider (render_with_state wraps in it) via importOriginal.
let mock_params = {};
let mock_query = {};
vi.mock("preact-iso", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useRoute: () => ({ params: mock_params, query: mock_query }),
  };
});

// Spy the HTTP verbs but keep _url_engine_rest / get_auth_header / RequestState
// real, so both the delegating and the apiBase paths behave authentically.
vi.mock("../api/helper.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, GET: vi.fn(), POST: vi.fn() };
});

import { GET, POST, _url_engine_rest } from "../api/helper.jsx";
import engine_rest from "../api/engine_rest.jsx";
import { plugin_apis } from "../api/plugins.js";
import { use_plugin_api } from "./plugin_api.jsx";
import { register, _reset_registry } from "./registry.js";
import { PLUGIN_POINTS } from "./points.js";
import { render_with_state } from "../test/render.jsx";
import { create_mock_state, RESPONSE_STATE } from "../test/helpers.js";

// Render a probe that calls the hook and hands the returned context back.
let captured;
const Probe = ({ id }) => {
  captured = use_plugin_api(id);
  return null;
};
const use_api = (id, state) => {
  render_with_state(<Probe id={id} />, { state });
  return captured;
};

const descriptor = (over = {}) => ({
  id: "demo",
  point: PLUGIN_POINTS.API,
  properties: {},
  ...over,
});

beforeEach(() => {
  _reset_registry();
  for (const key of Object.keys(plugin_apis)) delete plugin_apis[key];
  mock_params = {};
  mock_query = {};
  captured = undefined;
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("plugins/plugin_api — context", () => {
  it("hands back the route params and query", () => {
    mock_params = { definition_id: "def:1" };
    mock_query = { tab: "heat" };
    register(descriptor());
    const api = use_api("demo", create_mock_state());
    expect(api.params).toBe(mock_params);
    expect(api.query).toBe(mock_query);
  });

  it("scopes signals to the plugin's own state branch (by reference)", () => {
    // The signal factory must be registered before the state tree is built,
    // since createAppState() snapshots the registry into state.api.plugins.
    register(descriptor({ signals: () => ({ hits: signal(7) }) }));
    const state = create_mock_state();
    const api = use_api("demo", state);
    expect(api.signals).toBe(state.api.plugins.demo);
    expect(api.signals.hits.value).toBe(7);
  });

  it("scopes api to the plugin's mounted engine_rest namespace", () => {
    const load = vi.fn();
    register(descriptor({ api: { load } }));
    const api = use_api("demo", create_mock_state());
    expect(api.api).toBe(engine_rest.plugins.demo);
    expect(api.api.load).toBe(load);
  });

  it("falls back to empty objects for a plugin with no signals/api", () => {
    register(descriptor());
    const api = use_api("demo", create_mock_state());
    expect(api.signals).toEqual({});
    expect(api.api).toEqual({});
  });
});

describe("plugins/plugin_api — url + verbs without apiBase", () => {
  beforeEach(() => register(descriptor()));

  it("resolves paths against the engine-rest base", () => {
    const state = create_mock_state();
    const api = use_api("demo", state);
    expect(api.resolve_url("/version")).toBe(
      `${_url_engine_rest(state)}/version`,
    );
  });

  it("delegates get/post to the standard engine-rest helpers", () => {
    const state = create_mock_state();
    const api = use_api("demo", state);
    const sig = signal(null);

    api.get("/version", sig);
    expect(GET).toHaveBeenCalledTimes(1);
    // state/signal compared by reference (structural match walks the signal tree).
    expect(GET.mock.lastCall[0]).toBe("/version");
    expect(GET.mock.lastCall[1]).toBe(state);
    expect(GET.mock.lastCall[2]).toBe(sig);

    api.post("/thing", { a: 1 }, sig);
    expect(POST.mock.lastCall[0]).toBe("/thing");
    expect(POST.mock.lastCall[1]).toEqual({ a: 1 });
    expect(POST.mock.lastCall[2]).toBe(state);
    expect(POST.mock.lastCall[3]).toBe(sig);
  });
});

describe("plugins/plugin_api — verbs with apiBase (own backend)", () => {
  beforeEach(() =>
    register(descriptor({ properties: { apiBase: "https://plugin.test" } })),
  );

  it("resolves paths against the plugin's apiBase", () => {
    const api = use_api("demo", create_mock_state());
    expect(api.resolve_url("/data")).toBe("https://plugin.test/data");
  });

  it("fetches the absolute url and writes a SUCCESS response into the signal", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({
        ok: true,
        status: 200,
        json: async () => ({ n: 5 }),
      })),
    );
    const api = use_api("demo", create_mock_state());
    const sig = signal(null);

    await api.get("/data", sig);

    expect(globalThis.fetch.mock.calls[0][0]).toBe("https://plugin.test/data");
    // Never touches the engine-rest helpers on the apiBase path.
    expect(GET).not.toHaveBeenCalled();
    expect(sig.value).toEqual({
      status: RESPONSE_STATE.SUCCESS,
      data: { n: 5 },
    });
  });

  it("writes an ERROR response into the signal on a non-ok fetch", async () => {
    const response = { ok: false, status: 500 };
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => response),
    );
    const api = use_api("demo", create_mock_state());
    const sig = signal(null);

    await api.post("/data", { go: true }, sig);

    expect(globalThis.fetch.mock.calls[0][1].method).toBe("POST");
    expect(sig.value).toEqual({
      status: RESPONSE_STATE.ERROR,
      error: response,
    });
  });
});
