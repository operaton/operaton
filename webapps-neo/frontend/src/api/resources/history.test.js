import { describe, it, vi, beforeEach, expect } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  PAGINATED_GET: vi.fn(),
  PUT: vi.fn(),
}));

import { GET, PAGINATED_GET, PUT } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import history from "./history.js";

describe("api/resources/history", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("process_instance.all() PAGINATED_GETs the instance list", () => {
    history.process_instance.all(state, "def-1", {}, 40);
    expect_api_call(PAGINATED_GET, {
      url: "/history/process-instance?sortBy=startTime&sortOrder=asc&processDefinitionId=def-1",
      state,
      signal: state.api.history.process_instance.list,
    });
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(40);
    expect(PAGINATED_GET.mock.lastCall[4]).toBe(20);
  });

  it("process_instance.all() defaults firstResult to 0", () => {
    history.process_instance.all(state, "def-1");
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(0);
    expect(PAGINATED_GET.mock.lastCall[4]).toBe(20);
  });

  it("process_instance.all() forwards extra filter params and lets them override defaults", () => {
    history.process_instance.all(state, "def-1", {
      businessKeyLike: "%foo%",
      sortBy: "businessKey",
      sortOrder: "desc",
    });
    expect_api_call(PAGINATED_GET, {
      url: "/history/process-instance?sortBy=businessKey&sortOrder=desc&processDefinitionId=def-1&businessKeyLike=%25foo%25",
      state,
      signal: state.api.history.process_instance.list,
    });
  });

  it("process_instance.one() GETs a single instance", () => {
    history.process_instance.one(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/process-instance/inst-1",
      state,
      signal: state.api.history.process_instance.one,
    });
  });

  it("process_instance.all_unfinished() PAGINATED_GETs with unfinished=true", () => {
    history.process_instance.all_unfinished(state, "def-1", {}, 60);
    expect_api_call(PAGINATED_GET, {
      url: "/history/process-instance?sortBy=startTime&sortOrder=asc&unfinished=true&processDefinitionId=def-1",
      state,
      signal: state.api.history.process_instance.list,
    });
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(60);
    expect(PAGINATED_GET.mock.lastCall[4]).toBe(20);
  });

  it("incident.by_process_definition() GETs incidents filtered by definition", () => {
    history.incident.by_process_definition(state, "def-1");
    expect_api_call(GET, {
      url: "/history/incident?processDefinitionId=def-1",
      state,
      signal: state.api.history.incident.by_process_definition,
    });
  });

  it("incident.by_process_instance() GETs incidents filtered by instance", () => {
    history.incident.by_process_instance(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/incident?processInstanceId=inst-1",
      state,
      signal: state.api.history.incident.by_process_instance,
    });
  });

  it("variable_instance.by_process_instance() GETs variables filtered by instance", () => {
    history.variable_instance.by_process_instance(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/variable-instance?processInstanceId=inst-1&deserializeValues=false",
      state,
      signal: state.api.history.variable_instance.by_process_instance,
    });
  });

  it("get_user_operation() GETs user operations filtered by instance", () => {
    history.get_user_operation(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/user-operation?processInstanceId=inst-1&sortBy=timestamp&sortOrder=desc",
      state,
      signal: state.api.history.user_operation,
    });
  });

  it("set_user_operation_annotation() PUTs the annotation by operationId", () => {
    history.set_user_operation_annotation(state, "op-1", "note");
    expect_api_call(PUT, {
      url: "/history/user-operation/op-1/set-annotation",
      body: { annotation: "note" },
      state,
      signal: state.api.history.user_operation_annotation,
    });
  });

  it("clear_user_operation_annotation() PUTs clear by operationId", () => {
    history.clear_user_operation_annotation(state, "op-1");
    expect_api_call(PUT, {
      url: "/history/user-operation/op-1/clear-annotation",
      body: {},
      state,
      signal: state.api.history.user_operation_annotation,
    });
  });

  it("task.by_process_instance() GETs historic tasks filtered by instance", () => {
    history.task.by_process_instance(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/task?processInstanceId=inst-1",
      state,
      signal: state.api.history.task.by_process_instance,
    });
  });

  it("process_instance.called() GETs historic child instances of an instance", () => {
    history.process_instance.called(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/process-instance?superProcessInstanceId=inst-1",
      state,
      signal: state.api.history.process_instance.called,
    });
  });

  it("batch.all() PAGINATED_GETs /history/batch", () => {
    history.batch.all(state, { sortBy: "batchId", sortOrder: "desc" }, 20);
    expect_api_call(PAGINATED_GET, {
      url: "/history/batch?sortBy=batchId&sortOrder=desc",
      state,
      signal: state.api.history.batch.list,
    });
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(20);
  });

  it("batch.one() GETs /history/batch/:id", () => {
    history.batch.one(state, "b1");
    expect_api_call(GET, {
      url: "/history/batch/b1",
      state,
      signal: state.api.history.batch.one,
    });
  });
});
