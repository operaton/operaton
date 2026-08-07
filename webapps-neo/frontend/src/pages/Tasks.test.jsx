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
// RenderedFallbackForm path which only touches DOMPurify, exercising the real
// post_task_form wiring without pulling in form-js.
vi.mock("../components/BPMNViewer.jsx", () => ({
  BPMNViewer: ({ xml }) => h("div", { "data-testid": "bpmn-viewer" }, xml),
}));
vi.mock("../components/CamundaForm.jsx", () => ({
  CamundaForm: ({ schema }) =>
    h("div", { "data-testid": "camunda-form" }, JSON.stringify(schema)),
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
      const select = container.querySelector("#filter-list");
      fireEvent.change(select, { target: { value: "f1" } });
      expect(engine_rest.filter.execute_filter).toHaveBeenCalled();
      expect(engine_rest.filter.execute_filter.mock.lastCall[0]).toBe(state);
      expect(engine_rest.filter.execute_filter.mock.lastCall[1]).toBe("f1");
    });

    it("re-fetches via get_tasks when switching to 'my tasks'", () => {
      const { container } = renderPage(state);
      engine_rest.task.get_tasks.mockClear();
      const select = container.querySelector("#filter-list");
      fireEvent.change(select, { target: { value: "my" } });
      expect(engine_rest.task.get_tasks).toHaveBeenCalled();
    });

    it("re-fetches when the sort key changes", () => {
      const { container } = renderPage(state);
      engine_rest.task.get_tasks.mockClear();
      const select = container.querySelector("#sort-by");
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

    it("claims the task via claim_task", () => {
      signal_response(state.api.task.one, sample_task({ assignee: null }));
      const { getByText, container } = renderDetail();
      open_assignee_dialog(container);
      fireEvent.click(getByText("tasks.claim"));
      expect(engine_rest.task.claim_task).toHaveBeenCalled();
      expect(engine_rest.task.claim_task.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.claim_task.mock.lastCall[1]).toBe("t1");
    });

    it("resets a foreign assignee via assign_task", () => {
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

    it("submits the (auto-generated) task form via post_task_form", () => {
      // No formKey on the task => real TaskForm renders the RenderedFallbackForm
      // path, which posts via post_task_form on submit.
      signal_response(state.api.task.one, sample_task());
      signal_response(
        state.api.task.rendered_form,
        '<form><input class="form-control" name="amount" value="42"/></form>',
      );
      engine_rest.task.post_task_form.mockResolvedValue(undefined);
      const { getByText } = renderDetail();
      fireEvent.submit(getByText("tasks.form.complete-task").closest("form"));
      expect(engine_rest.task.post_task_form).toHaveBeenCalled();
      expect(engine_rest.task.post_task_form.mock.lastCall[0]).toBe(state);
      expect(engine_rest.task.post_task_form.mock.lastCall[1]).toBe("t1");
    });
  });
});
