import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  PUT: vi.fn(),
  DELETE: vi.fn(),
}));

import { GET, PUT, DELETE } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import incident from "./incident.js";

describe("api/resources/incident", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("by_process_instance() GETs /incident filtered by processInstanceId", () => {
    incident.by_process_instance(state, "inst-1");
    expect_api_call(GET, {
      url: "/incident?processInstanceId=inst-1",
      state,
      signal: state.api.incident.by_process_instance,
    });
  });

  it("by_process_definition() GETs /incident filtered by processDefinitionId", () => {
    incident.by_process_definition(state, "def-1");
    expect_api_call(GET, {
      url: "/incident?processDefinitionId=def-1",
      state,
      signal: state.api.incident.by_process_definition,
    });
  });

  it("set_annotation() PUTs {annotation} to /incident/:id/annotation", () => {
    incident.set_annotation(state, "inc-1", "looked into it");
    expect_api_call(PUT, {
      url: "/incident/inc-1/annotation",
      body: { annotation: "looked into it" },
      state,
      signal: state.api.incident.annotation,
    });
  });

  it("clear_annotation() DELETEs /incident/:id/annotation", () => {
    incident.clear_annotation(state, "inc-1");
    expect_api_call(DELETE, {
      url: "/incident/inc-1/annotation",
      body: null,
      state,
      signal: state.api.incident.annotation,
    });
  });
});
