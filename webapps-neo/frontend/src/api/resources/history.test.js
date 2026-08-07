import { describe, it, vi, beforeEach, expect } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  PAGINATED_GET: vi.fn(),
}));

import { GET, PAGINATED_GET } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import history from "./history.js";

describe("api/resources/history", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("process_instance.all() PAGINATED_GETs the instance list", () => {
    history.process_instance.all(state, "def-1", 40);
    expect_api_call(PAGINATED_GET, {
      url: "/history/process-instance?sortBy=startTime&sortOrder=asc&processDefinitionId=def-1",
      state,
      signal: state.api.process.instance.list,
    });
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(40);
    expect(PAGINATED_GET.mock.lastCall[4]).toBe(20);
  });

  it("process_instance.all() defaults firstResult to 0", () => {
    history.process_instance.all(state, "def-1");
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(0);
    expect(PAGINATED_GET.mock.lastCall[4]).toBe(20);
  });

  it("process_instance.one() GETs a single instance", () => {
    history.process_instance.one(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/process-instance/inst-1",
      state,
      signal: state.api.process.instance.one,
    });
  });

  it("process_instance.all_unfinished() PAGINATED_GETs with unfinished=true", () => {
    history.process_instance.all_unfinished(state, "def-1", 60);
    expect_api_call(PAGINATED_GET, {
      url: "/history/process-instance?unfinished=true&sortBy=startTime&sortOrder=asc&processDefinitionId=def-1",
      state,
      signal: state.api.process.instance.list,
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
      url: "/history/variable-instance?processInstanceId=inst-1",
      state,
      signal: state.api.process.instance.variables,
    });
  });

  it("get_user_operation() GETs user operations filtered by instance", () => {
    history.get_user_operation(state, "inst-1");
    expect_api_call(GET, {
      url: "/history/user-operation?processInstanceId=inst-1",
      state,
      signal: state.api.history.user_operation,
    });
  });
});
