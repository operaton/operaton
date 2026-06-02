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
