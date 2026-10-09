import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  GET_TEXT: vi.fn(),
  PAGINATED_GET: vi.fn(),
}));

import { GET, PAGINATED_GET } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import decision from "./decision.js";

describe("api/resources/decision", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("get_decision_definition() GETs /decision-definition/:id", () => {
    decision.get_decision_definition(state, "dec-1");
    expect_api_call(GET, {
      url: "/decision-definition/dec-1",
      state,
      signal: state.api.decision.definition,
    });
  });

  it("get_decision_definitions() GETs /decision-definition", () => {
    decision.get_decision_definitions(state);
    expect_api_call(GET, {
      url: "/decision-definition",
      state,
      signal: state.api.decision.definitions,
    });
  });

  it("get_dmn_xml() GETs /decision-definition/:id/xml", () => {
    decision.get_dmn_xml(state, "dec-1");
    expect_api_call(GET, {
      url: "/decision-definition/dec-1/xml",
      state,
      signal: state.api.decision.dmn,
    });
  });

  it("get_decision_instances() paginates /history/decision-instance", () => {
    decision.get_decision_instances(state, "dec-1", 20);
    expect_api_call(PAGINATED_GET, {
      url: "/history/decision-instance?decisionDefinitionId=dec-1&sortBy=evaluationTime&sortOrder=desc",
      state,
      signal: state.api.decision.instances,
    });
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(20);
  });

  it("get_decision_instance() GETs a single instance with inputs and outputs", () => {
    decision.get_decision_instance(state, "di-1");
    expect_api_call(GET, {
      url: "/history/decision-instance/di-1?includeInputs=true&includeOutputs=true",
      state,
      signal: state.api.decision.instance,
    });
  });
});
