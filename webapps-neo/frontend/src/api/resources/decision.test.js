import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  GET_TEXT: vi.fn(),
}));

import { GET } from "../helper.jsx";
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
});
