import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

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

vi.mock("../components/Breadcrumbs.jsx", () => ({
  Breadcrumbs: () => h("nav", { "data-testid": "breadcrumbs" }),
}));

let mockParams = {};
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: vi.fn(), path: "/tasks/start" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { StartProcessList } from "./StartProcessList.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(StartProcessList, {})));

describe("StartProcessList", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = { tab: null };
  });
  afterEach(cleanup);

  it("fetches the startable process definitions on render", () => {
    renderPage(state);
    expect(engine_rest.process_definition.list_startable).toHaveBeenCalled();
    expect(engine_rest.process_definition.list_startable.mock.lastCall[0]).toBe(
      state,
    );
  });

  it("renders the startable process list with links to start each one", () => {
    signal_response(state.api.process.definition.list, [
      { id: "p1", name: "Invoice", version: 1, description: "d", key: "inv" },
      {
        id: "p2",
        name: "Onboarding",
        version: 2,
        description: "x",
        key: "onb",
      },
    ]);
    const { getByText } = renderPage(state);
    expect(getByText("Invoice").getAttribute("href")).toBe("/tasks/start/p1");
    expect(getByText("Onboarding").getAttribute("href")).toBe(
      "/tasks/start/p2",
    );
  });

  it("filters the list by the search term", () => {
    signal_response(state.api.process.definition.list, [
      { id: "p1", name: "Invoice", version: 1, description: "d", key: "inv" },
      {
        id: "p2",
        name: "Onboarding",
        version: 2,
        description: "x",
        key: "onb",
      },
    ]);
    const { getByText, queryByText, container } = renderPage(state);
    const input = container.querySelector("#process-popup-search-input");
    fireEvent.change(input, { target: { value: "invo" } });
    expect(getByText("Invoice")).toBeTruthy();
    expect(queryByText("Onboarding")).toBeNull();
  });

  it("prompts to select a definition when no tab is in the route", () => {
    mockParams = {};
    const { getByText } = renderPage(state);
    expect(getByText("tasks.start-process.select-definition")).toBeTruthy();
  });

  it("fetches the selected process definition when a tab is in the route", () => {
    mockParams = { tab: "p1" };
    renderPage(state);
    expect(engine_rest.process_definition.one).toHaveBeenCalled();
    expect(engine_rest.process_definition.one.mock.lastCall[1]).toBe("p1");
  });

  it("loads the start form once the selected definition resolves", () => {
    mockParams = { tab: "p1" };
    signal_response(state.api.process.definition.one, { id: "p1", key: "inv" });
    // The page chains start_form().then(() => get_task_form(... start_form.value.data.key ...)),
    // so populate the start_form signal and resolve the promise.
    signal_response(state.api.process.definition.start_form, {
      key: "embedded:app:inv",
    });
    engine_rest.process_definition.start_form.mockResolvedValue(undefined);
    renderPage(state);
    expect(engine_rest.process_definition.start_form).toHaveBeenCalled();
    expect(engine_rest.process_definition.start_form.mock.lastCall[1]).toBe(
      "inv",
    );
  });

  it("renders the start form when the task form html is available", () => {
    mockParams = { tab: "p1" };
    signal_response(state.api.process.definition.one, { id: "p1", key: "inv" });
    signal_response(state.api.process.definition.start_form, {
      key: "embedded:app:inv",
    });
    engine_rest.process_definition.start_form.mockResolvedValue(undefined);
    signal_response(
      state.api.task.form,
      '<form><input cam-variable-name="amount" cam-variable-type="String" type="text" /></form>',
    );
    const { getByText } = renderPage(state);
    expect(getByText("tasks.form.form-title")).toBeTruthy();
  });
});
