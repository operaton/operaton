import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { screen } from "@testing-library/preact";
import descriptors from "./plugin.jsx";
import { register, _reset_registry } from "../../registry.js";
import { PLUGIN_POINTS } from "../../points.js";
import engine_rest from "../../../api/engine_rest.jsx";
import { plugin_apis } from "../../../api/plugins.js";
import { render_with_state } from "../../../test/render.jsx";
import { create_mock_state, signal_response } from "../../../test/helpers.js";

const [page_descriptor] = descriptors;
const MetricsPage = page_descriptor.Component;

beforeEach(() => {
  _reset_registry();
  for (const key of Object.keys(plugin_apis)) delete plugin_apis[key];
  register(page_descriptor);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("Engine Metrics plugin — descriptors", () => {
  it("declares a single PAGE descriptor", () => {
    expect(descriptors).toHaveLength(1);
    expect(page_descriptor.point).toBe(PLUGIN_POINTS.PAGE);
    expect(page_descriptor.properties.href).toBe("/plugin/metrics");
  });
});

describe("Engine Metrics plugin — page", () => {
  it("requests metrics on mount and renders the engine version", () => {
    const state = create_mock_state();
    // Stub the mounted API so mounting makes no network calls.
    vi.spyOn(engine_rest.plugins.metrics, "version").mockImplementation(
      () => {},
    );
    vi.spyOn(engine_rest.plugins.metrics, "process_starts").mockImplementation(
      () => {},
    );
    vi.spyOn(engine_rest.plugins.metrics, "flow_nodes").mockImplementation(
      () => {},
    );
    signal_response(state.api.plugins.metrics.version, { version: "7.99.0" });

    render_with_state(<MetricsPage />, { state });

    // Compare by reference — structurally matching `state` would recurse into
    // the signal tree and throw (see helpers.js expect_api_call).
    expect(engine_rest.plugins.metrics.version).toHaveBeenCalled();
    expect(engine_rest.plugins.metrics.version.mock.lastCall[0]).toBe(state);
    expect(screen.getByText("7.99.0")).toBeTruthy();
  });
});
