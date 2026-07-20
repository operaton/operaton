import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  POST: vi.fn(),
}));

import { GET, POST } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import process_instance from "./process_instance.js";

describe("api/resources/process_instance", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("one() GETs /process-instance/:id", () => {
    process_instance.one(state, "inst-1");
    expect_api_call(GET, {
      url: "/process-instance/inst-1",
      state,
      signal: state.api.process.instance.one,
    });
  });

  it("variables() GETs /process-instance/:id/variables", () => {
    process_instance.variables(state, "inst-1");
    expect_api_call(GET, {
      url: "/process-instance/inst-1/variables",
      state,
      signal: state.api.process.instance.variables,
    });
  });

  it("called() GETs sub process instances by superProcessInstance", () => {
    process_instance.called(state, "inst-1");
    expect_api_call(GET, {
      url: "/process-instance?superProcessInstance=inst-1",
      state,
      signal: state.api.process.instance.called,
    });
  });

  it("count() POSTs deploymentId to /process-instance/count", () => {
    process_instance.count(state, "dep-1");
    expect_api_call(POST, {
      url: "/process-instance/count",
      body: { deploymentId: "dep-1" },
      state,
      signal: state.api.process.instance.count,
    });
  });

  it("all() GETs /process-instance filtered by processDefinitionId", () => {
    process_instance.all(state, "def-1");
    expect_api_call(GET, {
      url: "/process-instance?processDefinitionId=def-1",
      state,
      signal: state.api.process.instance.list,
    });
  });

  it("by_activity_ids() POSTs activityIdIn and processDefinitionId to /process-instance", () => {
    process_instance.by_activity_ids(state, "def-1", ["act-1", "act-2"]);
    expect_api_call(POST, {
      url: "/process-instance",
      body: { activityIdIn: ["act-1", "act-2"], processDefinitionId: "def-1" },
      state,
      signal: state.api.process.instance.list,
    });
  });

  it("by_defintion_id() GETs /process-instance filtered by processDefinitionId", () => {
    process_instance.by_defintion_id(state, "def-1");
    expect_api_call(GET, {
      url: "/process-instance?processDefinitionId=def-1",
      state,
      signal: state.api.process.instance.by_defintion_id,
    });
  });

  it("activity_instances() GETs /process-instance/:id/activity-instances", () => {
    process_instance.activity_instances(state, "inst-1");
    expect_api_call(GET, {
      url: "/process-instance/inst-1/activity-instances",
      state,
      signal: state.api.process.instance.activity_instances,
    });
  });

  it("modify() POSTs the body to /process-instance/:id/modification", () => {
    const body = { instructions: [{ type: "cancel" }] };
    process_instance.modify(state, "inst-1", body);
    expect_api_call(POST, {
      url: "/process-instance/inst-1/modification",
      body,
      state,
      signal: state.api.process.instance.modification,
    });
  });

  it("modify_async() POSTs a batch modification to /modification/executeAsync", () => {
    const instructions = [
      { type: "cancel", activityId: "a" },
      { type: "startBeforeActivity", activityId: "b" },
    ];
    const query = { processDefinitionId: "def:1:x", activityIdIn: ["a"] };
    process_instance.modify_async(state, "def:1:x", instructions, { query });
    expect_api_call(POST, {
      url: "/modification/executeAsync",
      body: {
        processDefinitionId: "def:1:x",
        instructions,
        processInstanceQuery: query,
        processInstanceIds: null,
        skipCustomListeners: false,
        skipIoMappings: false,
        annotation: undefined,
      },
      state,
      signal: state.api.process.instance.modification,
    });
  });
});
