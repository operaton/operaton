import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  PUT: vi.fn(),
  POST: vi.fn(),
  GET_TEXT: vi.fn(),
}));

import { GET, PUT, POST, GET_TEXT } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import external_task from "./external_task.js";

describe("api/resources/external_task", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("by_process_instance() GETs /external-task filtered by processInstanceId", () => {
    external_task.by_process_instance(state, "inst-1");
    expect_api_call(GET, {
      url: "/external-task?processInstanceId=inst-1",
      state,
      signal: state.api.external_task.by_process_instance,
    });
  });

  it("set_retries() PUTs {retries} to /external-task/:id/retries", () => {
    external_task.set_retries(state, "et-1", 2);
    expect_api_call(PUT, {
      url: "/external-task/et-1/retries",
      body: { retries: 2 },
      state,
      signal: state.api.external_task.update,
    });
  });

  it("unlock() POSTs to /external-task/:id/unlock", () => {
    external_task.unlock(state, "et-1");
    expect_api_call(POST, {
      url: "/external-task/et-1/unlock",
      body: null,
      state,
      signal: state.api.external_task.update,
    });
  });

  it("error_details() GET_TEXTs /external-task/:id/errorDetails", () => {
    external_task.error_details(state, "et-1");
    expect_api_call(GET_TEXT, {
      url: "/external-task/et-1/errorDetails",
      state,
      signal: state.api.external_task.error_details,
    });
  });
});
