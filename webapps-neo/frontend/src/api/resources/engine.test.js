import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
}));

import { GET } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import engine from "./engine.js";

describe("api/resources/engine", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("telemetry() GETs the default engine telemetry data", () => {
    engine.telemetry(state);
    expect_api_call(GET, {
      url: "/engine/default/telemetry/data",
      state,
      signal: state.api.engine.telemetry,
    });
  });
});
