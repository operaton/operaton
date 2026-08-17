import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  PUT: vi.fn(),
  GET_TEXT: vi.fn(),
}));

import { GET, PUT, GET_TEXT } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import job from "./job.js";

describe("api/resources/job", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("by_process_instance() GETs /job filtered by processInstanceId", () => {
    job.by_process_instance(state, "inst-1");
    expect_api_call(GET, {
      url: "/job?processInstanceId=inst-1",
      state,
      signal: state.api.job.by_process_instance,
    });
  });

  it("set_retries() PUTs {retries} to /job/:id/retries", () => {
    job.set_retries(state, "job-1", 3);
    expect_api_call(PUT, {
      url: "/job/job-1/retries",
      body: { retries: 3 },
      state,
      signal: state.api.job.update,
    });
  });

  it("set_suspended() PUTs {suspended} to /job/:id/suspended", () => {
    job.set_suspended(state, "job-1", true);
    expect_api_call(PUT, {
      url: "/job/job-1/suspended",
      body: { suspended: true },
      state,
      signal: state.api.job.update,
    });
  });

  it("set_duedate() PUTs {duedate} to /job/:id/duedate", () => {
    job.set_duedate(state, "job-1", "2026-07-01T00:00:00.000+0000");
    expect_api_call(PUT, {
      url: "/job/job-1/duedate",
      body: { duedate: "2026-07-01T00:00:00.000+0000" },
      state,
      signal: state.api.job.update,
    });
  });

  it("stacktrace() GET_TEXTs /job/:id/stacktrace", () => {
    job.stacktrace(state, "job-1");
    expect_api_call(GET_TEXT, {
      url: "/job/job-1/stacktrace",
      state,
      signal: state.api.job.stacktrace,
    });
  });
});
