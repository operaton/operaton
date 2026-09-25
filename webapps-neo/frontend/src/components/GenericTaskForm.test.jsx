import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

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
const routeFn = vi.fn();
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: routeFn, path: "/tasks" }),
}));

import { RESPONSE_STATE } from "../api/helper.jsx";
import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { TaskForm } from "./TaskForm.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

// A task created by hand: no process, so no task definition and no form.
const STANDALONE = {
  id: "t1",
  name: "Nacharbeit",
  assignee: "alice",
  taskDefinitionKey: null,
  processDefinitionId: null,
};

const renderForm = (state) =>
  render(h(AppState.Provider, { value: state }, h(TaskForm, {})));

describe("a task that belongs to no process", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    state.auth.user.id.value = "alice";
    mockParams = { task_id: "t1" };
    routeFn.mockClear();
    signal_response(state.api.task.one, STANDALONE);
  });
  afterEach(cleanup);

  it("does not ask the engine to render a form it cannot have", () => {
    signal_response(state.api.task.form_variables, {});
    renderForm(state);
    expect(engine_rest.task.get_task_rendered_form).not.toHaveBeenCalled();
    expect(engine_rest.task.get_task_form_variables).toHaveBeenCalled();
  });

  it("offers a variable editor instead of a load error", () => {
    signal_response(state.api.task.form_variables, {});
    const { getByText, queryByText } = renderForm(state);
    expect(getByText("tasks.form.add-variable")).toBeTruthy();
    expect(queryByText(/tasks\.form\.fetch-failed/)).toBeNull();
  });

  it("can be completed even with no variables at all", () => {
    signal_response(state.api.task.form_variables, {});
    engine_rest.task.post_task_form.mockResolvedValue({});
    const { getByText } = renderForm(state);
    fireEvent.click(getByText("tasks.form.complete-directly"));
    expect(engine_rest.task.post_task_form).toHaveBeenCalled();
    expect(engine_rest.task.post_task_form.mock.lastCall[2]).toEqual({});
  });

  it("stays put and says so when the engine refuses the completion", async () => {
    signal_response(state.api.task.form_variables, {});
    engine_rest.task.post_task_form.mockResolvedValue({
      status: RESPONSE_STATE.ERROR,
      error: { message: "task is null" },
    });
    const { getByText } = renderForm(state);
    fireEvent.click(getByText("tasks.form.complete-directly"));

    await vi.waitFor(() => expect(getByText("task is null")).toBeTruthy());
    expect(routeFn).not.toHaveBeenCalled();
  });

  it("carries the typed variables along when it is completed", () => {
    signal_response(state.api.task.form_variables, {});
    engine_rest.task.post_task_form.mockResolvedValue({});
    const { getByText, getByLabelText } = renderForm(state);

    fireEvent.click(getByText("tasks.form.add-variable"));
    fireEvent.input(getByLabelText("common.name"), {
      target: { value: "amount" },
    });
    fireEvent.change(getByLabelText("common.type"), {
      target: { value: "Integer" },
    });
    fireEvent.input(getByLabelText("common.value"), {
      target: { value: "42" },
    });
    fireEvent.click(getByText("tasks.form.complete-task"));

    expect(engine_rest.task.post_task_form.mock.lastCall[2]).toEqual({
      amount: { value: 42, type: "Integer" },
    });
  });

  it("leaves out a row that was never given a name", () => {
    signal_response(state.api.task.form_variables, {});
    engine_rest.task.post_task_form.mockResolvedValue({});
    const { getByText } = renderForm(state);
    fireEvent.click(getByText("tasks.form.add-variable"));
    fireEvent.click(getByText("tasks.form.complete-task"));
    expect(engine_rest.task.post_task_form.mock.lastCall[2]).toEqual({});
  });

  it("refuses to complete while a name is used twice", () => {
    signal_response(state.api.task.form_variables, {});
    const { getByText, getAllByLabelText } = renderForm(state);

    fireEvent.click(getByText("tasks.form.add-variable"));
    fireEvent.click(getByText("tasks.form.add-variable"));
    const names = getAllByLabelText("common.name");
    fireEvent.input(names[0], { target: { value: "amount" } });
    fireEvent.input(names[1], { target: { value: "amount" } });

    expect(getByText("tasks.form.duplicate-variable")).toBeTruthy();
    fireEvent.click(getByText("tasks.form.complete-task"));
    expect(engine_rest.task.post_task_form).not.toHaveBeenCalled();
  });

  it("stays read-only until the task is held", () => {
    signal_response(state.api.task.one, { ...STANDALONE, assignee: null });
    signal_response(state.api.task.form_variables, {});
    const { getByText } = renderForm(state);
    expect(getByText("tasks.form.claim-first")).toBeTruthy();
    expect(getByText("tasks.form.add-variable").disabled).toBe(true);
  });

  it("still renders the engine's form for a task that has one", () => {
    signal_response(state.api.task.one, {
      ...STANDALONE,
      taskDefinitionKey: "Task_Review",
      processDefinitionId: "p:1:abc",
    });
    renderForm(state);
    expect(engine_rest.task.get_task_rendered_form).toHaveBeenCalled();
  });
});
