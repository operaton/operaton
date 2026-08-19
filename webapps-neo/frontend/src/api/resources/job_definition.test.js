import { describe, it, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  PUT: vi.fn(),
}));

import { GET, PUT } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import job_definition from "./job_definition.js";

describe("api/resources/job_definition", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all.by_process_definition() GETs job definitions for a process definition", () => {
    job_definition.all.by_process_definition(state, "def-1");
    expect_api_call(GET, {
      url: "/job-definition?processDefinitionId=def-1",
      state,
      signal: state.api.job_definition.all.by_process_definition,
    });
  });

  it("set_suspended() PUTs {suspended, includeJobs} to /job-definition/:id/suspended", () => {
    job_definition.set_suspended(state, "jd-1", true);
    expect_api_call(PUT, {
      url: "/job-definition/jd-1/suspended",
      body: { suspended: true, includeJobs: true },
      state,
      signal: state.api.job_definition.update,
    });
  });

  it("set_priority() PUTs {jobPriority, includeJobs} to /job-definition/:id/jobPriority", () => {
    job_definition.set_priority(state, "jd-1", 10);
    expect_api_call(PUT, {
      url: "/job-definition/jd-1/jobPriority",
      body: { jobPriority: 10, includeJobs: true },
      state,
      signal: state.api.job_definition.update,
    });
  });

  it("set_retries() PUTs {retries} to /job-definition/:id/retries", () => {
    job_definition.set_retries(state, "jd-1", 5);
    expect_api_call(PUT, {
      url: "/job-definition/jd-1/retries",
      body: { retries: 5 },
      state,
      signal: state.api.job_definition.update,
    });
  });
});
