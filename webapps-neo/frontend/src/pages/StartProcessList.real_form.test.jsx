import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// Deliberately NOT stubbing CamundaForm: what is measured here is how the real
// one hands its submit function to the page around it.
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
  useLocation: () => ({ route: vi.fn(), path: "/tasks/start" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { StartProcessList } from "./StartProcessList.jsx";
import {
  create_mock_state,
  signal_response,
  signal_error,
} from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(StartProcessList, {})));

describe("starting a process that has no start form", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = { tab: "p1" };
    signal_response(state.api.process.definition.one, { id: "p1" });
    signal_response(state.api.process.definition.start_form, { key: null });
    engine_rest.process_definition.start_form.mockResolvedValue(undefined);
    // No start form at all: the engine answers rendered-form with a 404.
    engine_rest.process_definition.rendered_start_form.mockImplementation(() =>
      signal_error(state.api.process.definition.rendered_form),
    );
    engine_rest.process_definition.submit_form.mockResolvedValue({
      status: "SUCCESS",
    });
  });
  afterEach(cleanup);

  it("carries the business key typed before the click", async () => {
    const { container, getByText, findByText } = renderPage(state);
    await findByText("tasks.start-process.start");

    fireEvent.input(container.querySelector('input[name="business_key"]'), {
      target: { value: "BK-42" },
    });
    fireEvent.click(getByText("tasks.start-process.start"));

    await vi.waitFor(() =>
      expect(engine_rest.process_definition.submit_form).toHaveBeenCalled(),
    );
    const payload = engine_rest.process_definition.submit_form.mock.lastCall[2];
    expect(payload.businessKey).toBe("BK-42");
  });

  it("carries a variable typed before the click", async () => {
    const { container, getByText, findByText } = renderPage(state);
    await findByText("tasks.start-process.start");

    fireEvent.click(getByText("tasks.form.add-variable"));
    const name_input = container.querySelector("tbody input");
    fireEvent.input(name_input, { target: { value: "land" } });
    fireEvent.click(getByText("tasks.start-process.start"));

    await vi.waitFor(() =>
      expect(engine_rest.process_definition.submit_form).toHaveBeenCalled(),
    );
    const payload = engine_rest.process_definition.submit_form.mock.lastCall[2];
    expect(Object.keys(payload.variables)).toContain("land");
  });
});
