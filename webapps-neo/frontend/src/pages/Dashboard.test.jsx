import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup } from "@testing-library/preact";

// Spy all engine_rest API functions but keep RequestState/RESPONSE_STATE real.
vi.mock("../api/engine_rest.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  const spyify = (o) =>
    Object.fromEntries(
      Object.entries(o).map(([k, v]) => [
        k,
        typeof v === "function"
          ? vi.fn()
          : v && typeof v === "object"
            ? spyify(v)
            : v,
      ]),
    );
  return { ...actual, default: spyify(actual.default) };
});

let mockParams = {};
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: vi.fn(), path: "/dashboard" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { DashboardPage } from "./Dashboard.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";
import { register, _reset_registry } from "../plugins/registry.js";
import { PLUGIN_POINTS } from "../plugins/points.js";
import { plugin_apis } from "../api/plugins.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(DashboardPage, {})));

describe("DashboardPage", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
  });
  afterEach(cleanup);

  it("fires the on-mount fetches for tasks, processes, deployments and decisions", () => {
    renderPage(state);
    expect(engine_rest.task.get_tasks).toHaveBeenCalled();
    expect(engine_rest.process_definition.list).toHaveBeenCalled();
    expect(engine_rest.deployment.all).toHaveBeenCalled();
    expect(engine_rest.decision.get_decision_definitions).toHaveBeenCalled();
  });

  it("does not refetch a signal that is already populated", () => {
    signal_response(state.api.task.list, []);
    renderPage(state);
    expect(engine_rest.task.get_tasks).not.toHaveBeenCalled();
  });

  it("greets the authenticated user by name", () => {
    state.auth.credentials.value = { username: "alice", password: "x" };
    const { getByRole } = renderPage(state);
    expect(getByRole("heading", { level: 2 }).textContent).toContain("alice");
  });

  it("renders the count cards from the populated signals", () => {
    signal_response(state.api.task.list, [{ id: "t1" }, { id: "t2" }]);
    state.api.task.summary.value = {
      status: "SUCCESS",
      data: { total: 2, assigned: 1, unassigned: 1, unattended: 1 },
    };
    signal_response(state.api.process.definition.list, [
      { id: "p1", definition: { key: "p", name: "P" } },
    ]);
    signal_response(state.api.decision.definitions, [
      { id: "d1" },
      { id: "d2" },
      { id: "d3" },
    ]);
    signal_response(state.api.deployment.all, [{ id: "dep1" }]);

    const { getByText } = renderPage(state);
    // Task card
    const tasksCard = getByText("dashboard.open-tasks").closest("a");
    expect(tasksCard.getAttribute("href")).toBe("/tasks");
    expect(tasksCard.querySelector("strong").textContent).toBe("2");
    // Decisions card
    const decisionsCard = getByText("dashboard.decision-definitions").closest(
      "a",
    );
    expect(decisionsCard.querySelector("strong").textContent).toBe("3");
    // Deployments card
    const deploymentsCard = getByText("dashboard.deployments").closest("a");
    expect(deploymentsCard.querySelector("strong").textContent).toBe("1");
  });

  it("renders the recent tasks table with rows linking to each task", () => {
    signal_response(state.api.task.list, [
      { id: "t1", name: "Review", assignee: "bob" },
      { id: "t2", name: "Approve", assignee: null },
    ]);
    const { getByText } = renderPage(state);
    const link = getByText("Review");
    expect(link.getAttribute("href")).toBe("/tasks/t1");
    expect(getByText("Approve").getAttribute("href")).toBe("/tasks/t2");
  });

  it("renders the process-definitions table linking to each definition", () => {
    signal_response(state.api.process.definition.list, [
      {
        id: "p1",
        definition: { key: "invoice", name: "Invoice" },
        instances: 4,
        incidents: [],
      },
    ]);
    const { getAllByText } = renderPage(state);
    // The name appears in the process card region and in the table; the table
    // cell is an anchor to the definition.
    const link = getAllByText("Invoice").find(
      (el) => el.getAttribute && el.getAttribute("href") === "/processes/p1",
    );
    expect(link).toBeTruthy();
  });

  it("renders the open-incidents table when definitions carry incidents", () => {
    signal_response(state.api.process.definition.list, [
      {
        id: "p1",
        definition: { key: "invoice", name: "Invoice" },
        incidents: [
          {
            incidentType: "failedJob",
            incidentCount: 3,
            processDefinitionId: "p1",
          },
        ],
      },
    ]);
    const { getByText, queryByText } = renderPage(state);
    expect(getByText("failedJob")).toBeTruthy();
    expect(getByText("3")).toBeTruthy();
    expect(queryByText("dashboard.no-incidents")).toBeNull();
  });

  it("shows the empty incidents message when there are none", () => {
    signal_response(state.api.process.definition.list, [
      { id: "p1", definition: { key: "p", name: "P" }, incidents: [] },
    ]);
    const { getByText } = renderPage(state);
    expect(getByText("dashboard.no-incidents")).toBeTruthy();
  });
});

describe("DashboardPage — plugin widgets", () => {
  let state;
  beforeEach(() => {
    _reset_registry();
    for (const key of Object.keys(plugin_apis)) delete plugin_apis[key];
    state = create_mock_state();
    mockParams = {};
  });
  afterEach(() => {
    cleanup();
    _reset_registry();
  });

  it("renders a registered DASHBOARD_WIDGET after the built-in sections", () => {
    register({
      id: "widget-demo",
      point: PLUGIN_POINTS.DASHBOARD_WIDGET,
      properties: {},
      Component: () =>
        h("section", { "data-testid": "demo-widget" }, "hello widget"),
    });

    const { getByTestId, getByText } = renderPage(state);
    expect(getByTestId("demo-widget").textContent).toBe("hello widget");
    // The built-in dashboard content still renders alongside the widget.
    expect(getByText("nav.tasks")).toBeTruthy();
  });

  it("renders no widget markup when no widget plugin is registered", () => {
    const { queryByTestId } = renderPage(state);
    expect(queryByTestId("demo-widget")).toBeNull();
  });

  describe("open tasks", () => {
    it("separates assigned, unassigned and nobody responsible", () => {
      state.api.task.summary.value = {
        status: "SUCCESS",
        data: { total: 9, assigned: 4, unassigned: 5, unattended: 2 },
      };
      const { getByText } = renderPage(state);
      const card = getByText("dashboard.open-tasks").closest("a");
      expect(card.querySelector("strong").textContent).toBe("9");
      expect(card.textContent).toContain("4");
      expect(card.textContent).toContain("5");
      expect(card.textContent).toContain("2");
    });

    it("counts the open tasks of each group", () => {
      signal_response(state.api.task.by_group, [
        { groupName: "reviewers", taskCount: 3 },
        { groupName: null, taskCount: 1 },
      ]);
      const { getByText } = renderPage(state);
      expect(getByText("reviewers")).toBeTruthy();
      expect(getByText("dashboard.no-group")).toBeTruthy();
      expect(getByText("dashboard.multiple-groups-hint")).toBeTruthy();
    });

    it("asks the engine for both breakdowns", () => {
      renderPage(state);
      expect(engine_rest.task.summary).toHaveBeenCalled();
      expect(engine_rest.task.by_group).toHaveBeenCalled();
    });
  });

  describe("folding a section away", () => {
    it("remembers what was folded", () => {
      localStorage.clear();
      const { container } = renderPage(state);
      const section = container.querySelector(
        "details.dashboard-section[open]",
      );
      section.open = false;
      section.dispatchEvent(new Event("toggle"));
      expect(localStorage.getItem("dashboard.hidden-sections")).toContain(
        "tasks-by-group",
      );
    });

    it("starts folded when it was folded before", () => {
      localStorage.setItem(
        "dashboard.hidden-sections",
        JSON.stringify(["incidents"]),
      );
      const { container } = renderPage(state);
      const sections = [
        ...container.querySelectorAll("details.dashboard-section"),
      ];
      const incidents = sections[1];
      expect(incidents.open).toBe(false);
      localStorage.clear();
    });
  });
});
