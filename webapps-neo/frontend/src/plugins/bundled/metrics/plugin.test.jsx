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
    vi.spyOn(engine_rest.plugins.metrics, "running").mockImplementation(
      () => {},
    );
    vi.spyOn(engine_rest.plugins.metrics, "completed").mockImplementation(
      () => {},
    );
    vi.spyOn(
      engine_rest.plugins.metrics,
      "definition_stats",
    ).mockImplementation(() => {});
    vi.spyOn(
      engine_rest.plugins.metrics,
      "activity_series",
    ).mockImplementation(() => {});
    vi.spyOn(engine_rest.plugins.metrics, "top_tasks").mockImplementation(
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

  it("renders the running-vs-completed donut with the total in its centre", () => {
    const state = create_mock_state();
    for (const fn of [
      "version",
      "process_starts",
      "flow_nodes",
      "running",
      "completed",
      "definition_stats",
      "activity_series",
      "top_tasks",
    ]) {
      vi.spyOn(engine_rest.plugins.metrics, fn).mockImplementation(() => {});
    }
    signal_response(state.api.plugins.metrics.running, { count: 3 });
    signal_response(state.api.plugins.metrics.completed, { count: 7 });

    render_with_state(<MetricsPage />, { state });

    // Legend shows both counts; donut centre shows the total (3 + 7 = 10).
    expect(screen.getByText("3")).toBeTruthy();
    expect(screen.getByText("7")).toBeTruthy();
    expect(screen.getByText("10")).toBeTruthy();
  });

  it("ranks top processes, summing running instances across versions", () => {
    const state = create_mock_state();
    for (const fn of [
      "version",
      "process_starts",
      "flow_nodes",
      "running",
      "completed",
      "definition_stats",
      "activity_series",
      "top_tasks",
    ]) {
      vi.spyOn(engine_rest.plugins.metrics, fn).mockImplementation(() => {});
    }
    // orderFulfillment spans two versions (4 + 6 = 10) and outranks invoice (2).
    signal_response(state.api.plugins.metrics.definition_stats, [
      {
        instances: 4,
        incidents: [],
        definition: { id: "of:1", key: "orderFulfillment", name: "Order Fulfillment", version: 1 },
      },
      {
        instances: 6,
        incidents: [{ incidentType: "failedJob", incidentCount: 2 }],
        definition: { id: "of:2", key: "orderFulfillment", name: "Order Fulfillment", version: 2 },
      },
      {
        instances: 2,
        incidents: [],
        definition: { id: "inv:1", key: "invoice", name: "Invoice", version: 1 },
      },
    ]);

    render_with_state(<MetricsPage />, { state });

    // Aggregated running count for orderFulfillment is 10, incidents 2.
    expect(screen.getByText("Order Fulfillment")).toBeTruthy();
    expect(screen.getByText("10")).toBeTruthy();
    expect(screen.getByText("(2)")).toBeTruthy();
    // Links to the newest version's definition (of:2).
    const link = screen.getByText("Order Fulfillment").closest("a");
    expect(link.getAttribute("href")).toBe("/processes/of:2");
  });

  it("renders a gap-free 30-day activity chart from sparse buckets", () => {
    const state = create_mock_state();
    for (const fn of [
      "version",
      "process_starts",
      "flow_nodes",
      "running",
      "completed",
      "definition_stats",
      "activity_series",
      "top_tasks",
    ]) {
      vi.spyOn(engine_rest.plugins.metrics, fn).mockImplementation(() => {});
    }
    // A single bucket for today; the other 29 days must be filled with 0.
    const today = new Date();
    today.setUTCHours(0, 0, 0, 0);
    const iso_day = today.toISOString().slice(0, 10);
    signal_response(state.api.plugins.metrics.activity_series, [
      { timestamp: `${iso_day}T00:00:00.000+0000`, value: 100 },
    ]);

    render_with_state(<MetricsPage />, { state });

    const cols = document.querySelectorAll(".activity-col");
    expect(cols.length).toBe(30);
    // Today's column carries the value; at least one column is the 0-filled gap.
    const tips = [...document.querySelectorAll(".activity-tip")].map(
      (t) => t.textContent,
    );
    expect(tips.some((x) => x.endsWith(" 100"))).toBe(true);
    expect(tips.some((x) => x.endsWith(" 0"))).toBe(true);
  });

  it("ranks top user tasks by completed count, highest first", () => {
    const state = create_mock_state();
    for (const fn of [
      "version",
      "process_starts",
      "flow_nodes",
      "running",
      "completed",
      "definition_stats",
      "activity_series",
      "top_tasks",
    ]) {
      vi.spyOn(engine_rest.plugins.metrics, fn).mockImplementation(() => {});
    }
    signal_response(state.api.plugins.metrics.top_tasks, [
      {
        count: 4,
        taskName: "Approve Invoice",
        processDefinitionName: "Invoice Receipt",
        processDefinitionId: "invoice:2",
      },
      {
        count: 122,
        taskName: "Final decision",
        processDefinitionName: "Loan Approval",
        processDefinitionId: "loanApproval:1",
      },
    ]);

    render_with_state(<MetricsPage />, { state });

    expect(screen.getByText("Final decision")).toBeTruthy();
    expect(screen.getByText("122")).toBeTruthy();
    // Row links to its process definition.
    const link = screen.getByText("Final decision").closest("a");
    expect(link.getAttribute("href")).toBe("/processes/loanApproval:1");
    // Highest count renders before the lower one.
    const names = [...document.querySelectorAll(".metrics-ranking")]
      .flatMap((r) => [...r.querySelectorAll(".ranking-name")])
      .map((n) => n.textContent);
    expect(names.indexOf("Final decision · Loan Approval")).toBeLessThan(
      names.indexOf("Approve Invoice · Invoice Receipt"),
    );
  });
});
