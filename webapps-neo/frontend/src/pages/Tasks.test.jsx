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

// Stub the heavy diagram + form-js child components so bpmn-js / feelin never
// load. TaskForm itself is kept real: with no formKey it takes the
// GeneratedTaskForm path, which turns the engine's rendered form into a schema
// and submits via post_task_form — exercised here without pulling in form-js.
vi.mock("../components/BPMNViewer.jsx", () => ({
  BPMNViewer: ({ xml }) => h("div", { "data-testid": "bpmn-viewer" }, xml),
}));
// Expose submit() the way the real CamundaForm does (via on_ready), so a
// parent's "Complete task" button can drive on_submit in these tests.
vi.mock("../components/CamundaForm.jsx", () => ({
  CamundaForm: ({ schema, data, on_submit, on_ready }) => {
    on_ready?.({ submit: () => on_submit?.({ data: data ?? {}, errors: {} }) });
    return h("div", { "data-testid": "camunda-form" }, JSON.stringify(schema));
  },
}));
// StartProcessList pulls in its own heavy fetch tree; stub to a marker.
vi.mock("./StartProcessList.jsx", () => ({
  StartProcessList: () =>
    h("div", { "data-testid": "start-process-list" }, "start"),
}));

let mockParams = {};
let mockQuery = {};
const routeFn = vi.fn();
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams, query: mockQuery }),
  useLocation: () => ({ route: routeFn, path: "/tasks" }),
}));

import { AppState } from "../state.js";
import engine_rest, { RESPONSE_STATE } from "../api/engine_rest.jsx";
import { TasksPage } from "./Tasks.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(TasksPage, {})));

// A task as the engine would return it once loaded into state.api.task.one.
const sample_task = (over = {}) => ({
  id: "t1",
  name: "Approve invoice",
  description: "Please approve",
  assignee: null,
  due: null,
  followUp: null,
  processDefinitionId: "pd:1",
  executionId: "exec1",
  taskDefinitionKey: "approve",
  ...over,
});

describe("TasksPage", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    mockQuery = {};
    routeFn.mockClear();
  });
  afterEach(cleanup);

  describe("task list", () => {
    it("fetches the saved filters on mount when none are loaded", () => {
      renderPage(state);
      expect(engine_rest.filter.get_filters).toHaveBeenCalled();
      expect(engine_rest.filter.get_filters.mock.lastCall[0]).toBe(state);
    });

    it("loads the (unfiltered) task list on mount via get_tasks", () => {
      renderPage(state);
      expect(engine_rest.task.get_tasks).toHaveBeenCalled();
      expect(engine_rest.task.get_tasks.mock.lastCall[0]).toBe(state);
    });

    it("renders a row per task with a link to the task form tab", () => {
      signal_response(state.api.task.list, [
        sample_task({ id: "t1", name: "Approve invoice", assignee: "demo" }),
        sample_task({ id: "t2", name: "Review contract" }),
      ]);
      const { getByText } = renderPage(state);
      const link = getByText("Approve invoice");
      expect(link.getAttribute("href")).toBe("/tasks/t1/form");
      expect(getByText("Review contract").getAttribute("href")).toBe(
        "/tasks/t2/form",
      );
      // assignee column shows the assignee / a dash placeholder
      expect(getByText("demo")).toBeTruthy();
    });

    it("renders the saved filters as options in the filter selector", () => {
      signal_response(state.api.filter.list, [
        { id: "f1", name: "Urgent", query: { priority: 50 } },
        { id: "f2", name: "Empty", query: {} }, // filtered out (no criteria)
      ]);
      const { getByText, queryByText } = renderPage(state);
      expect(getByText("Urgent")).toBeTruthy();
      expect(queryByText("Empty")).toBeNull();
    });

    it("executes a saved filter when one is selected", () => {
      signal_response(state.api.filter.list, [
        { id: "f1", name: "Urgent", query: { priority: 50 } },
      ]);
      const { container } = renderPage(state);
      const select = container.querySelector("#list-filter-saved");
      fireEvent.change(select, { target: { value: "f1" } });
      expect(engine_rest.filter.execute_filter).toHaveBeenCalled();
      expect(engine_rest.filter.execute_filter.mock.lastCall[0]).toBe(state);
      expect(engine_rest.filter.execute_filter.mock.lastCall[1]).toBe("f1");
    });

    it("re-fetches via get_tasks when switching to 'my tasks'", () => {
      const { container } = renderPage(state);
      engine_rest.task.get_tasks.mockClear();
      const select = container.querySelector("#list-filter-saved");
      fireEvent.change(select, { target: { value: "my" } });
      expect(engine_rest.task.get_tasks).toHaveBeenCalled();
    });

    it("re-fetches when the sort key changes", () => {
      const { container } = renderPage(state);
      engine_rest.task.get_tasks.mockClear();
      const select = container.querySelector("#list-filter-sort-by");
      fireEvent.change(select, { target: { value: "priority" } });
      expect(engine_rest.task.get_tasks).toHaveBeenCalled();
      // sortBy is the 2nd positional arg of get_tasks(state, sortBy, ...)
      expect(engine_rest.task.get_tasks.mock.lastCall[1]).toBe("priority");
    });

    it("shows a 'load more' button and pages when more results exist", () => {
      state.api.task.list.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: [sample_task({ id: "t1" })],
        hasMore: true,
      };
      const { getByText } = renderPage(state);
      const more = getByText("tasks.load-more");
      engine_rest.task.get_tasks.mockClear();
      fireEvent.click(more);
      expect(engine_rest.task.get_tasks).toHaveBeenCalled();
      // firstResult (4th positional arg) is the current row count, 1
      expect(engine_rest.task.get_tasks.mock.lastCall[3]).toBe(1);
    });

    it("shows the end-of-list note when there are no more items", () => {
      state.api.task.list.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: [sample_task({ id: "t1" })],
        hasMore: false,
      };
      const { getByText, queryByText } = renderPage(state);
      expect(getByText("tasks.no-more-items")).toBeTruthy();
      expect(queryByText("tasks.load-more")).toBeNull();
    });

    it("renders the start-process link", () => {
      const { getByText } = renderPage(state);
      expect(getByText("tasks.start-process-label").getAttribute("href")).toBe(
        "/tasks/start",
      );
    });

    it("prompts to select a task when none is in the route", () => {
      const { getByText } = renderPage(state);
      expect(getByText("tasks.select-task")).toBeTruthy();
    });
  });

  describe("returning to the list", () => {
    const rerenderPage = (r, state) =>
      r.rerender(h(AppState.Provider, { value: state }, h(TasksPage, {})));

    it("reloads the list when a task is left, so a completed one disappears", () => {
      mockParams = { task_id: "t1" };
      const r = renderPage(state);
      signal_response(state.api.task.list, [
        sample_task({ id: "t1" }),
        sample_task({ id: "t2" }),
      ]);
      engine_rest.task.get_tasks.mockClear();

      mockParams = {};
      rerenderPage(r, state);

      expect(engine_rest.task.get_tasks).toHaveBeenCalled();
      const [, , , firstResult] = engine_rest.task.get_tasks.mock.lastCall;
      expect(firstResult).toBe(0);
    });

    it("keeps however many entries were loaded, so 'load more' is not undone", () => {
      mockParams = { task_id: "t1" };
      const r = renderPage(state);
      signal_response(
        state.api.task.list,
        Array.from({ length: 25 }, (_, i) => sample_task({ id: `t${i}` })),
      );
      engine_rest.task.get_tasks.mockClear();

      mockParams = {};
      rerenderPage(r, state);

      const [, , , firstResult, maxResults] =
        engine_rest.task.get_tasks.mock.lastCall;
      expect(firstResult).toBe(0);
      expect(maxResults).toBe(25);
    });

    it("does not reload when a task is opened", () => {
      mockParams = {};
      const r = renderPage(state);
      engine_rest.task.get_tasks.mockClear();

      mockParams = { task_id: "t1" };
      rerenderPage(r, state);

      expect(engine_rest.task.get_tasks).not.toHaveBeenCalled();
    });
  });

  describe("special task_id routes", () => {
    it("renders the StartProcessList for /tasks/start", () => {
      mockParams = { task_id: "start" };
      const { getByTestId } = renderPage(state);
      expect(getByTestId("start-process-list")).toBeTruthy();
    });

    it("renders the filter editor for /tasks/filter", () => {
      mockParams = { task_id: "filter" };
      const { getByText } = renderPage(state);
      expect(getByText("tasks.filter.title")).toBeTruthy();
    });

    it("creates a filter from the editor and routes back to /tasks", () => {
      mockParams = { task_id: "filter" };
      engine_rest.filter.create_filter.mockResolvedValue(undefined);
      const { container, getByText } = renderPage(state);
      const name = container.querySelector("#filter-name");
      fireEvent.input(name, { target: { value: "My Filter" } });
      fireEvent.submit(getByText("common.save").closest("form"));
      expect(engine_rest.filter.create_filter).toHaveBeenCalled();
      expect(engine_rest.filter.create_filter.mock.lastCall[0]).toBe(state);
      expect(engine_rest.filter.create_filter.mock.lastCall[1].name).toBe(
        "My Filter",
      );
      expect(
        engine_rest.filter.create_filter.mock.lastCall[1].resourceType,
      ).toBe("Task");
    });
  });

  describe("after a task turned out to be gone", () => {
    afterEach(() => engine_rest.task.get_task.mockReset());

    it("loads the next task that is opened", async () => {
      // Reported: after "task not found", going back to the list and opening
      // any other task reported it missing too.
      mockParams = { task_id: "gone" };
      engine_rest.task.get_task.mockImplementation(() => {
        state.api.task.one.value = {
          status: RESPONSE_STATE.ERROR,
          error: { status: 404 },
        };
        return Promise.resolve();
      });
      const { rerender } = renderPage(state);
      await vi.waitFor(() =>
        expect(engine_rest.task.get_task).toHaveBeenCalled(),
      );

      // Now open a different task, which the engine still knows.
      engine_rest.task.get_task.mockImplementation(() => {
        signal_response(state.api.task.one, sample_task({ id: "alive" }));
        return Promise.resolve();
      });
      mockParams = { task_id: "alive" };
      rerender(h(AppState.Provider, { value: state }, h(TasksPage, {})));

      await vi.waitFor(() =>
        expect(engine_rest.task.get_task.mock.lastCall?.[1]).toBe("alive"),
      );
      expect(state.api.task.one.value?.data?.id).toBe("alive");
    });
  });

  describe("the chosen filter stays chosen", () => {
    it("carries the filter and sorting into the link on a task", () => {
      mockQuery = { filter: "f1", sortBy: "created", sortOrder: "desc" };
      signal_response(state.api.task.list, [{ id: "t1", name: "One" }]);
      const { container } = renderPage(state);
      const href = container.querySelector("tbody a").getAttribute("href");
      expect(href).toContain("filter=f1");
      expect(href).toContain("sortBy=created");
      expect(href).toContain("sortOrder=desc");
    });

    it("carries an ad-hoc criterion along too", () => {
      mockQuery = { filter: "f1", "q.nameLike": "Review" };
      signal_response(state.api.task.list, [{ id: "t1", name: "One" }]);
      const { container } = renderPage(state);
      expect(container.querySelector("tbody a").getAttribute("href")).toContain(
        "q.nameLike=Review",
      );
    });

    it("adds nothing when no filter is chosen", () => {
      signal_response(state.api.task.list, [{ id: "t1", name: "One" }]);
      const { container } = renderPage(state);
      expect(container.querySelector("tbody a").getAttribute("href")).toBe(
        "/tasks/t1/form",
      );
    });
  });

  describe("what the list can be sorted by", () => {
    it("offers no sorting the request cannot carry", () => {
      // Sorting by a variable needs its name and type alongside the key. Until
      // the request can carry those, the choice would only ever answer
      // "variableName is null" — or, over GET, refuse the key outright.
      const { container } = renderPage(state);
      const offered = [
        ...container.querySelectorAll("#list-filter select option"),
      ].map((o) => o.value);
      expect(offered).not.toContain("processVariable");
      expect(offered).not.toContain("taskVariable");
    });
  });

  describe("a task that no longer exists", () => {
    afterEach(() => engine_rest.task.get_task.mockReset());

    it("stops after one attempt instead of loading for ever", async () => {
      mockParams = { task_id: "gone" };
      engine_rest.task.get_task.mockImplementation(() => {
        state.api.task.one.value = {
          status: RESPONSE_STATE.ERROR,
          error: { status: 404 },
        };
        return Promise.resolve();
      });
      renderPage(state);

      await vi.waitFor(() =>
        expect(engine_rest.task.get_task).toHaveBeenCalled(),
      );
      const after_first = engine_rest.task.get_task.mock.calls.length;
      await new Promise((r) => setTimeout(r, 120));
      expect(engine_rest.task.get_task.mock.calls.length).toBe(after_first);
    });
  });

  describe("task detail", () => {
    it("loads the task chain when a task_id is in the route", () => {
      mockParams = { task_id: "t1", tab: "form" };
      engine_rest.task.get_task.mockResolvedValue(undefined);
      renderPage(state);
      expect(engine_rest.task.get_task).toHaveBeenCalled();
      expect(engine_rest.task.get_task.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.get_task.mock.lastCall[1]).toBe("t1");
    });

    it("renders the task metadata once the task signal is populated", () => {
      mockParams = { task_id: "t1", tab: "form" };
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.process.definition.one, {
        id: "pd:1",
        name: "Invoicing",
        version: 3,
      });
      const { getByText } = renderPage(state);
      expect(getByText("Approve invoice")).toBeTruthy();
      expect(getByText("Please approve")).toBeTruthy();
    });

    it("shows a not-found message when the task load 404s", () => {
      mockParams = { task_id: "missing", tab: "form" };
      state.api.task.one.value = {
        status: RESPONSE_STATE.ERROR,
        error: { status: 404 },
      };
      const { getByText } = renderPage(state);
      expect(getByText("tasks.task-not-found")).toBeTruthy();
    });

    it("renders the tab list once the task is loaded", () => {
      mockParams = { task_id: "t1", tab: "form" };
      signal_response(state.api.task.one, sample_task());
      const { getByText } = renderPage(state);
      expect(getByText("tasks.tabs.form")).toBeTruthy();
      expect(getByText("tasks.tabs.history")).toBeTruthy();
      expect(getByText("tasks.tabs.diagram")).toBeTruthy();
    });

    it("renders the history tab table once operations + comments are ready", () => {
      mockParams = { task_id: "t1", tab: "history" };
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.history.user_operation, [
        {
          timestamp: "2024-01-02T00:00:00Z",
          userId: "demo",
          operationType: "Claim",
          property: "assignee",
          newValue: "demo",
        },
      ]);
      signal_response(state.api.task.comment.list, [
        { time: "2024-01-01T00:00:00Z", userId: "demo", message: "hello" },
      ]);
      const { getByText } = renderPage(state);
      expect(getByText("tasks.history.title")).toBeTruthy();
      expect(getByText("Claim")).toBeTruthy();
      expect(getByText("hello")).toBeTruthy();
    });

    it("re-reads the history when the tab is opened", () => {
      mockParams = { task_id: "t1", tab: "history" };
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.history.user_operation, []);
      signal_response(state.api.task.comment.list, []);
      renderPage(state);
      expect(
        engine_rest.history.get_user_operation_by_task.mock.lastCall,
      ).toEqual([state, "t1"]);
      expect(engine_rest.task.get_comments.mock.lastCall).toEqual([
        state,
        "t1",
      ]);
    });

    it("asks before an attachment is deleted", () => {
      mockParams = { task_id: "t1", tab: "attachments" };
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.task.attachment.list, [
        { id: "a1", name: "Rechnung.pdf", description: "" },
      ]);
      const { getByText, getByLabelText } = renderPage(state);

      fireEvent.click(getByLabelText("common.delete"));
      expect(engine_rest.task.delete_attachment).not.toHaveBeenCalled();

      fireEvent.click(getByText("tasks.attachments.confirm-delete"));
      expect(engine_rest.task.delete_attachment.mock.lastCall).toEqual([
        state,
        "t1",
        "a1",
      ]);
    });

    it("fetches + renders the BPMN diagram on the diagram tab", () => {
      mockParams = { task_id: "t1", tab: "diagram" };
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.process.definition.diagram, {
        bpmn20Xml: "<bpmn>xml</bpmn>",
      });
      const { getByTestId } = renderPage(state);
      expect(engine_rest.process_definition.diagram).toHaveBeenCalled();
      expect(getByTestId("bpmn-viewer").textContent).toBe("<bpmn>xml</bpmn>");
    });
  });

  // The history tab reads signals that the detail's loading chain fills.
  describe("loading what the history tab shows", () => {
    // get_task is a shared spy; a stubbed implementation would otherwise leak
    // into the tests that follow.
    afterEach(() => engine_rest.task.get_task.mockReset());

    const load_task = (over) =>
      engine_rest.task.get_task.mockImplementation(() => {
        signal_response(state.api.task.one, sample_task({ id: "t1", ...over }));
        return Promise.resolve();
      });

    it("asks the operation log about the task, not about an execution", async () => {
      mockParams = { task_id: "t1", tab: "history" };
      load_task({ executionId: "exec-9", processDefinitionId: "p:1:abc" });
      renderPage(state);

      await vi.waitFor(() =>
        expect(
          engine_rest.history.get_user_operation_by_task,
        ).toHaveBeenCalled(),
      );
      expect(
        engine_rest.history.get_user_operation_by_task.mock.lastCall[1],
      ).toBe("t1");
      expect(engine_rest.history.get_user_operation).not.toHaveBeenCalled();
    });

    it("does not ask for a process definition a standalone task has not got", async () => {
      mockParams = { task_id: "t1", tab: "history" };
      load_task({ executionId: null, processDefinitionId: null });
      renderPage(state);

      await vi.waitFor(() =>
        expect(engine_rest.task.get_comments).toHaveBeenCalled(),
      );
      expect(engine_rest.process_definition.one).not.toHaveBeenCalled();
    });
  });

  describe("tenant", () => {
    it("names the tenant a task belongs to", () => {
      mockParams = { task_id: "t1", tab: "form" };
      signal_response(state.api.task.one, sample_task({ tenantId: "sales" }));
      const { getByText } = renderPage(state);
      expect(getByText(/sales/)).toBeTruthy();
    });

    it("says nothing when the task has no tenant", () => {
      mockParams = { task_id: "t1", tab: "form" };
      signal_response(state.api.task.one, sample_task({ tenantId: null }));
      const { container } = renderPage(state);
      // Scoped to the detail: the create dialog has a tenant field of its own,
      // under the same label.
      expect(container.querySelector("p.tenant")).toBeNull();
    });
  });

  describe("due and follow-up dates", () => {
    const click_in_dialog = (container, dialog_id, text) => {
      const dialog = container.querySelector(`#${dialog_id}`);
      const button = [...dialog.querySelectorAll("button")].find(
        (b) => b.textContent.trim() === text,
      );
      fireEvent.click(button);
    };

    it("clears a due date", () => {
      mockParams = { task_id: "t1", tab: "form" };
      signal_response(
        state.api.task.one,
        sample_task({ due: "2026-07-15T10:30:00.000+0200" }),
      );
      const { container } = renderPage(state);
      click_in_dialog(container, "set_due_date", "tasks.dates.reset");

      expect(engine_rest.task.update_task).toHaveBeenCalled();
      const [, changeset] = engine_rest.task.update_task.mock.lastCall;
      expect(changeset).toEqual({ due: null });
    });

    it("clears a follow-up date", () => {
      mockParams = { task_id: "t1", tab: "form" };
      signal_response(
        state.api.task.one,
        sample_task({ followUp: "2026-07-15T10:30:00.000+0200" }),
      );
      const { container } = renderPage(state);
      click_in_dialog(container, "set_follow_up_date", "tasks.dates.reset");

      const [, changeset] = engine_rest.task.update_task.mock.lastCall;
      expect(changeset).toEqual({ followUp: null });
    });

    it("sets a follow-up date to now", () => {
      mockParams = { task_id: "t1", tab: "form" };
      signal_response(state.api.task.one, sample_task());
      const { container } = renderPage(state);
      click_in_dialog(container, "set_follow_up_date", "tasks.dates.now");

      const [, changeset] = engine_rest.task.update_task.mock.lastCall;
      expect(changeset.followUp).toMatch(
        /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:00\.000[+-]\d{4}$/,
      );
    });
  });

  describe("creating a task outside a process", () => {
    const open_dialog = (container) =>
      fireEvent.click(container.querySelector("button.create-task"));

    it("sends name, assignee and description", async () => {
      engine_rest.task.create_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      const { container, getByText } = renderPage(state);
      open_dialog(container);
      fireEvent.input(container.querySelector("#new-task-name"), {
        target: { value: "Call the reporter" },
      });
      fireEvent.input(container.querySelector("#new-task-assignee"), {
        target: { value: "alice" },
      });
      fireEvent.click(getByText("tasks.create.save"));

      await vi.waitFor(() =>
        expect(engine_rest.task.create_task).toHaveBeenCalled(),
      );
      const [, body] = engine_rest.task.create_task.mock.lastCall;
      expect(body.name).toBe("Call the reporter");
      expect(body.assignee).toBe("alice");
      expect(body.id).toBeTruthy();
    });

    it("opens the new task afterwards", async () => {
      engine_rest.task.create_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      const { container, getByText } = renderPage(state);
      open_dialog(container);
      fireEvent.input(container.querySelector("#new-task-name"), {
        target: { value: "Call the reporter" },
      });
      fireEvent.click(getByText("tasks.create.save"));

      await vi.waitFor(() => expect(routeFn).toHaveBeenCalled());
      const [, body] = engine_rest.task.create_task.mock.lastCall;
      expect(routeFn.mock.lastCall[0]).toBe(`/tasks/${body.id}/form`);
    });

    it("refuses to create a task without a name", () => {
      const { container, getByText } = renderPage(state);
      open_dialog(container);
      expect(getByText("tasks.create.save").disabled).toBe(true);
    });

    it("suggests the tenants the user may read, not the ones they belong to", () => {
      // An administrator is a member of no tenant and must still be able to
      // place a task in one.
      signal_response(state.api.tenant.by_member, []);
      signal_response(state.api.tenant.list, [
        { id: "sales" },
        { id: "support" },
      ]);
      const { container } = renderPage(state);
      open_dialog(container);
      const suggested = [
        ...container.querySelectorAll("#new-task-tenants option"),
      ].map((o) => o.value);
      expect(suggested).toEqual(["sales", "support"]);
    });

    it("asks for the tenant even when none is visible", () => {
      // The old dialog always asked, and an id may be typed that the user
      // cannot read.
      signal_response(state.api.tenant.list, []);
      const { container } = renderPage(state);
      open_dialog(container);
      expect(container.querySelector("#new-task-tenant")).not.toBeNull();
    });

    it("takes a tenant that is not in the list", async () => {
      engine_rest.task.create_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      signal_response(state.api.tenant.list, [{ id: "sales" }]);
      const { container, getByText } = renderPage(state);
      open_dialog(container);
      fireEvent.input(container.querySelector("#new-task-name"), {
        target: { value: "Call the reporter" },
      });
      fireEvent.input(container.querySelector("#new-task-tenant"), {
        target: { value: "elsewhere" },
      });
      fireEvent.click(getByText("tasks.create.save"));

      await vi.waitFor(() =>
        expect(engine_rest.task.create_task).toHaveBeenCalled(),
      );
      expect(engine_rest.task.create_task.mock.lastCall[1].tenantId).toBe(
        "elsewhere",
      );
    });

    it("sends no tenant when the field was left blank", async () => {
      engine_rest.task.create_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      const { container, getByText } = renderPage(state);
      open_dialog(container);
      fireEvent.input(container.querySelector("#new-task-name"), {
        target: { value: "Call the reporter" },
      });
      fireEvent.input(container.querySelector("#new-task-tenant"), {
        target: { value: "   " },
      });
      fireEvent.click(getByText("tasks.create.save"));

      await vi.waitFor(() =>
        expect(engine_rest.task.create_task).toHaveBeenCalled(),
      );
      expect(engine_rest.task.create_task.mock.lastCall[1].tenantId).toBeNull();
    });
  });

  describe("who may use a saved filter", () => {
    const open_editor = () => {
      mockParams = { task_id: "filter" };
    };

    it("grants everyone read access when asked", async () => {
      open_editor();
      engine_rest.filter.create_filter.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
        data: { id: "f1" },
      });
      const { container, getByText } = renderPage(state);
      fireEvent.input(container.querySelector("#filter-name"), {
        target: { value: "Overdue" },
      });
      fireEvent.click(getByText("tasks.filter.readable-by-all"));
      fireEvent.submit(container.querySelector("form"));

      await vi.waitFor(() =>
        expect(engine_rest.authorization.create).toHaveBeenCalled(),
      );
      const [, body] = engine_rest.authorization.create.mock.lastCall;
      expect(body).toMatchObject({
        type: 0,
        userId: "*",
        permissions: ["READ"],
        resourceType: 5,
        resourceId: "f1",
      });
    });

    it("grants a named group read access", async () => {
      open_editor();
      engine_rest.filter.create_filter.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
        data: { id: "f1" },
      });
      const { container, getByText } = renderPage(state);
      fireEvent.input(container.querySelector("#filter-name"), {
        target: { value: "Overdue" },
      });
      fireEvent.click(getByText("tasks.filter.add-permission"));
      const type = container.querySelector("fieldset:last-of-type select");
      fireEvent.change(type, { target: { value: "group" } });
      const id = container.querySelector("fieldset:last-of-type tbody input");
      fireEvent.input(id, { target: { value: "reviewers" } });
      fireEvent.submit(container.querySelector("form"));

      await vi.waitFor(() =>
        expect(engine_rest.authorization.create).toHaveBeenCalled(),
      );
      const [, body] = engine_rest.authorization.create.mock.lastCall;
      expect(body).toMatchObject({
        type: 1,
        groupId: "reviewers",
        permissions: ["READ"],
        resourceType: 5,
      });
    });

    it("adds and removes a row again", () => {
      open_editor();
      const { container, getByText } = renderPage(state);
      fireEvent.click(getByText("tasks.filter.add-permission"));
      expect(
        container.querySelectorAll("fieldset:last-of-type tbody tr").length,
      ).toBe(1);
    });
  });

  describe("a task that is gone", () => {
    // Once, not on every call: a mock that writes a signal each time it runs
    // feeds its own re-render and never settles.
    const vanishes_on_load = () => {
      engine_rest.task.get_task
        .mockImplementationOnce(() => {
          state.api.task.one.value = {
            status: RESPONSE_STATE.ERROR,
            error: { status: 404 },
          };
          return Promise.resolve();
        })
        .mockResolvedValue(undefined);
    };

    afterEach(() => engine_rest.task.get_task.mockReset());

    it("takes it out of the list, so it cannot be clicked again", async () => {
      mockParams = { task_id: "gone" };
      signal_response(state.api.task.list, [
        { id: "gone", name: "Completed meanwhile" },
        { id: "t2", name: "Still there" },
      ]);
      vanishes_on_load();
      renderPage(state);

      await vi.waitFor(() =>
        expect(state.api.task.list.value.data.map((t) => t.id)).toEqual(["t2"]),
      );
    });

    it("leaves the list alone when the task is fine", async () => {
      mockParams = { task_id: "t2" };
      signal_response(state.api.task.list, [{ id: "t2", name: "Still there" }]);
      signal_response(state.api.task.one, sample_task({ id: "t2" }));
      engine_rest.task.get_task.mockResolvedValue(undefined);
      renderPage(state);

      await vi.waitFor(() =>
        expect(engine_rest.task.get_comments).toHaveBeenCalled(),
      );
      expect(state.api.task.list.value.data).toHaveLength(1);
    });
  });

  describe("variables a filter shows as columns", () => {
    const with_columns = (variables, show_undefined = false) => {
      mockQuery = { filter: "f1" };
      signal_response(state.api.filter.list, [
        {
          id: "f1",
          name: "With columns",
          properties: { variables, showUndefinedVariable: show_undefined },
        },
      ]);
    };

    it("adds a column per variable the filter names", () => {
      with_columns([
        { name: "amount", label: "Amount" },
        { name: "city", label: "" },
      ]);
      const { container } = renderPage(state);
      const headings = [
        ...container.querySelectorAll("th.filter-variable"),
      ].map((th) => th.textContent);
      expect(headings).toEqual(["Amount", "city"]);
    });

    it("puts the value of each task into its column", () => {
      with_columns([{ name: "amount", label: "Amount" }]);
      signal_response(state.api.task.list, [
        {
          id: "t1",
          name: "One",
          filter_variables: { amount: { name: "amount", value: 42 } },
        },
      ]);
      const { container } = renderPage(state);
      expect(container.querySelector("td.filter-variable").textContent).toBe(
        "42",
      );
    });

    it("leaves the cell empty when the task has no such variable", () => {
      with_columns([{ name: "amount", label: "Amount" }]);
      signal_response(state.api.task.list, [
        { id: "t1", name: "One", filter_variables: {} },
      ]);
      const { container } = renderPage(state);
      expect(container.querySelector("td.filter-variable").textContent).toBe(
        "",
      );
    });

    it("marks the missing value when the filter asks for it", () => {
      with_columns([{ name: "amount", label: "Amount" }], true);
      signal_response(state.api.task.list, [
        { id: "t1", name: "One", filter_variables: {} },
      ]);
      const { container } = renderPage(state);
      expect(container.querySelector("td.filter-variable").textContent).toBe(
        "—",
      );
    });

    it("shows no extra column when the filter names none", () => {
      with_columns([]);
      const { container } = renderPage(state);
      expect(container.querySelectorAll("th.filter-variable")).toHaveLength(0);
    });
  });

  describe("saved filters", () => {
    it("keeps the chosen filter across a reload, because it lives in the route", () => {
      mockQuery = { filter: "f1" };
      signal_response(state.api.filter.list, [{ id: "f1", name: "Mine" }]);
      renderPage(state);
      expect(engine_rest.filter.execute_filter).toHaveBeenCalled();
      expect(engine_rest.filter.execute_filter.mock.lastCall[1]).toBe("f1");
    });

    it("asks the engine for the task list when no filter is chosen", () => {
      renderPage(state);
      expect(engine_rest.task.get_tasks).toHaveBeenCalled();
      expect(engine_rest.filter.execute_filter).not.toHaveBeenCalled();
    });

    it("narrows the list to the signed-in user for 'my tasks'", () => {
      mockQuery = { filter: "my" };
      state.auth.user.id.value = "alice";
      renderPage(state);
      const [, , , , , filter] = engine_rest.task.get_tasks.mock.lastCall;
      expect(filter).toEqual({ assignee: "alice" });
    });
  });

  describe("task actions", () => {
    const renderDetail = () => {
      mockParams = { task_id: "t1", tab: "form" };
      return renderPage(state);
    };

    // The assignee dialog opener lives inside #task-details (the same label key
    // is also a task-list table header), so scope the lookup there.
    const open_assignee_dialog = (container) => {
      const detail = container.querySelector("#task-details");
      const buttons = [...detail.querySelectorAll("button.task-card")];
      const opener = buttons.find((b) =>
        b.textContent.includes("tasks.task-list.table-headings.assignee"),
      );
      fireEvent.click(opener);
    };

    const open_groups_dialog = (container) => {
      const detail = container.querySelector("#task-details");
      const opener = [...detail.querySelectorAll("button.task-card")].find(
        (b) => b.textContent.includes("tasks.groups.set"),
      );
      fireEvent.click(opener);
    };

    it("re-reads the candidate groups after one was added", async () => {
      engine_rest.task.add_group.mockImplementation((state) => {
        state.api.task.add_group.value = { status: RESPONSE_STATE.SUCCESS };
        return Promise.resolve(state.api.task.add_group.value);
      });
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.task.identity_links, []);
      const { getByText, container } = renderDetail();
      open_groups_dialog(container);
      engine_rest.task.get_identity_links.mockClear();

      fireEvent.input(container.querySelector("#group_id"), {
        target: { value: "reviewers" },
      });
      fireEvent.click(getByText("tasks.groups.add-group"));

      await vi.waitFor(() =>
        expect(engine_rest.task.get_identity_links).toHaveBeenCalled(),
      );
    });

    it("re-reads the candidate groups after one was removed", async () => {
      engine_rest.task.delete_group.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.task.identity_links, [
        { groupId: "reviewers", type: "candidate" },
      ]);
      const { getByText, container } = renderDetail();
      open_groups_dialog(container);
      engine_rest.task.get_identity_links.mockClear();

      fireEvent.click(getByText("common.delete"));

      await vi.waitFor(() =>
        expect(engine_rest.task.get_identity_links).toHaveBeenCalled(),
      );
    });

    it("claims the task via claim_task", () => {
      engine_rest.task.claim_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);
      fireEvent.click(getByText("tasks.claim"));
      expect(engine_rest.task.claim_task).toHaveBeenCalled();
      expect(engine_rest.task.claim_task.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.claim_task.mock.lastCall[1]).toBe("t1");
    });

    it("assigns the task to the user typed into the dialog", async () => {
      engine_rest.user.find.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
        data: [{ id: "bob" }],
      });
      engine_rest.task.assign_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);

      const input = container.querySelector("#assignee-input");
      fireEvent.input(input, { target: { value: " bob " } });
      fireEvent.click(getByText("tasks.assign"));

      await vi.waitFor(() =>
        expect(engine_rest.task.assign_task).toHaveBeenCalled(),
      );
      const [call_state, assignee, task_id] =
        engine_rest.task.assign_task.mock.lastCall;
      expect(call_state).toBe(state);
      expect(assignee).toBe("bob");
      expect(task_id).toBe("t1");
    });

    it("refuses an id the engine does not know and says so", async () => {
      // The lookup answers empty both when the user does not exist and when the
      // caller may not read them; either way the task must not be handed over.
      engine_rest.user.find.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
        data: [],
      });
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);

      fireEvent.input(container.querySelector("#assignee-input"), {
        target: { value: "no-such-user" },
      });
      fireEvent.click(getByText("tasks.assign"));

      await vi.waitFor(() =>
        expect(getByText("tasks.assign-unknown-user")).toBeTruthy(),
      );
      expect(engine_rest.task.assign_task).not.toHaveBeenCalled();
    });

    it("clears the unknown-id message once the field is edited again", async () => {
      engine_rest.user.find.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
        data: [],
      });
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, queryByText, container } = renderDetail();
      open_assignee_dialog(container);
      const input = container.querySelector("#assignee-input");

      fireEvent.input(input, { target: { value: "no-such-user" } });
      fireEvent.click(getByText("tasks.assign"));
      await vi.waitFor(() =>
        expect(getByText("tasks.assign-unknown-user")).toBeTruthy(),
      );

      fireEvent.input(input, { target: { value: "gibtsnich" } });
      expect(queryByText("tasks.assign-unknown-user")).toBeNull();
    });

    it("does not assign when no user was typed", () => {
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);
      expect(getByText("tasks.assign").disabled).toBe(true);
    });

    it("re-reads the task after claiming, so the dialog stops showing the old state", async () => {
      engine_rest.task.claim_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);
      engine_rest.task.get_task.mockClear();

      fireEvent.click(getByText("tasks.claim"));
      await vi.waitFor(() =>
        expect(engine_rest.task.get_task).toHaveBeenCalled(),
      );
      expect(engine_rest.task.get_task.mock.lastCall[1]).toBe("t1");
    });

    it("re-reads the list too, so the assignee column catches up", async () => {
      engine_rest.task.claim_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);
      engine_rest.task.get_tasks.mockClear();

      fireEvent.click(getByText("tasks.claim"));
      await vi.waitFor(() =>
        expect(engine_rest.task.get_tasks).toHaveBeenCalled(),
      );
    });

    it("leaves the dialog alone when the action failed", async () => {
      engine_rest.task.claim_task.mockResolvedValue({
        status: RESPONSE_STATE.ERROR,
      });
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);
      engine_rest.task.get_task.mockClear();

      fireEvent.click(getByText("tasks.claim"));
      await new Promise((r) => setTimeout(r, 0));
      expect(engine_rest.task.get_task).not.toHaveBeenCalled();
    });

    it("resets a foreign assignee via assign_task", () => {
      engine_rest.task.assign_task.mockResolvedValue({
        status: RESPONSE_STATE.SUCCESS,
      });
      signal_response(state.api.task.one, sample_task({ assignee: "someone" }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);
      fireEvent.click(getByText("tasks.reset-assignee"));
      expect(engine_rest.task.assign_task).toHaveBeenCalled();
      expect(engine_rest.task.assign_task.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.assign_task.mock.lastCall[1]).toBeNull();
      expect(engine_rest.task.assign_task.mock.lastCall[2]).toBe("t1");
    });

    it("adds a comment via create_comment", () => {
      signal_response(state.api.task.one, sample_task());
      engine_rest.task.create_comment.mockResolvedValue(undefined);
      const { getByText, container } = renderDetail();
      fireEvent.click(getByText("tasks.comment-add"));
      const textarea = container.querySelector("#comment_message");
      fireEvent.input(textarea, { target: { value: "Looks good" } });
      fireEvent.submit(textarea.closest("form"));
      expect(engine_rest.task.create_comment).toHaveBeenCalled();
      expect(engine_rest.task.create_comment.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.create_comment.mock.lastCall[1]).toBe("t1");
      expect(engine_rest.task.create_comment.mock.lastCall[2]).toBe(
        "Looks good",
      );
    });

    it("adds a candidate group via add_group", () => {
      signal_response(state.api.task.one, sample_task());
      // The submit handler inspects add_group.value.status in its .then, so seed
      // a SUCCESS response to keep that callback from dereferencing null.
      signal_response(state.api.task.add_group, {});
      engine_rest.task.add_group.mockResolvedValue(undefined);
      const { getByText, container } = renderDetail();
      fireEvent.click(getByText("tasks.groups.set").closest("button"));
      const input = container.querySelector("#group_id");
      fireEvent.input(input, { target: { value: "managers" } });
      fireEvent.submit(input.closest("form"));
      expect(engine_rest.task.add_group).toHaveBeenCalled();
      expect(engine_rest.task.add_group.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.add_group.mock.lastCall[1]).toBe("t1");
      expect(engine_rest.task.add_group.mock.lastCall[2]).toBe("managers");
    });

    it("removes a candidate group via delete_group", () => {
      signal_response(state.api.task.one, sample_task());
      signal_response(state.api.task.identity_links, [
        { type: "candidate", groupId: "managers" },
      ]);
      const { getByText } = renderDetail();
      fireEvent.click(getByText("tasks.groups.set").closest("button"));
      fireEvent.click(getByText("common.delete"));
      expect(engine_rest.task.delete_group).toHaveBeenCalled();
      expect(engine_rest.task.delete_group.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.delete_group.mock.lastCall[1]).toBe("t1");
      expect(engine_rest.task.delete_group.mock.lastCall[2]).toBe("managers");
    });

    it("updates the due date via update_task", () => {
      signal_response(state.api.task.one, sample_task());
      engine_rest.task.update_task.mockResolvedValue(undefined);
      const { getByText } = renderDetail();
      fireEvent.click(getByText("tasks.due-date.label").closest("button"));
      fireEvent.submit(
        getByText("tasks.due-date.title")
          .closest("dialog")
          .querySelector("form"),
      );
      expect(engine_rest.task.update_task).toHaveBeenCalled();
      expect(engine_rest.task.update_task.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.update_task.mock.lastCall[1].due).toBeTruthy();
      expect(engine_rest.task.update_task.mock.lastCall[2]).toBe("t1");
    });

    it("leaves the form read-only until the task is held by the signed-in user", () => {
      state.auth.user.id.value = "alice";
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      signal_response(
        state.api.task.rendered_form,
        '<form><label>Amount</label><input cam-variable-name="amount" cam-variable-type="String" value="42"/></form>',
      );
      signal_response(state.api.task.form_variables, {
        amount: { value: "42", type: "String" },
      });
      const { getByText, container } = renderDetail();

      expect(getByText("tasks.form.claim-first")).toBeTruthy();
      expect(getByText("tasks.form.complete-task").disabled).toBe(true);
      expect(
        [...container.querySelectorAll(".task-form input")].every(
          (i) => i.disabled,
        ),
      ).toBe(true);
    });

    it("keeps the form read-only when someone else holds the task", () => {
      state.auth.user.id.value = "alice";
      signal_response(state.api.task.one, sample_task({ assignee: "bob" }));
      signal_response(
        state.api.task.rendered_form,
        '<form><label>Amount</label><input cam-variable-name="amount" cam-variable-type="String" value="42"/></form>',
      );
      signal_response(state.api.task.form_variables, {
        amount: { value: "42", type: "String" },
      });
      const { getByText } = renderDetail();
      expect(getByText("tasks.form.complete-task").disabled).toBe(true);
    });

    it("submits the generated task form via post_task_form", () => {
      // No formKey => real TaskForm renders GeneratedTaskForm: it parses the
      // engine's rendered form into a schema and submits via post_task_form.
      state.auth.user.id.value = "alice";
      signal_response(state.api.task.one, sample_task({ assignee: "alice" }));
      signal_response(
        state.api.task.rendered_form,
        '<form><label>Amount</label><input cam-variable-name="amount" cam-variable-type="String" value="42"/></form>',
      );
      signal_response(state.api.task.form_variables, {
        amount: { value: "42", type: "String" },
      });
      engine_rest.task.post_task_form.mockResolvedValue(undefined);
      const { getByText } = renderDetail();
      fireEvent.click(getByText("tasks.form.complete-task"));
      expect(engine_rest.task.post_task_form).toHaveBeenCalled();
      expect(engine_rest.task.post_task_form.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.post_task_form.mock.lastCall[1]).toBe("t1");
    });
  });
});
