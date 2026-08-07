import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("../helper.jsx", () => ({
  GET: vi.fn(),
  DELETE: vi.fn(),
  PUT: vi.fn(),
  PAGINATED_GET: vi.fn(),
}));

import { GET, DELETE, PUT, PAGINATED_GET } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import batch from "./batch.js";

describe("api/resources/batch", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("all() paginates batch statistics into the list signal", () => {
    batch.all(state, 40);
    expect_api_call(PAGINATED_GET, {
      url: "/batch/statistics?sortBy=batchId&sortOrder=desc",
      state,
      signal: state.api.batch.list,
    });
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(40);
    expect(PAGINATED_GET.mock.lastCall[4]).toBe(20);
  });

  it("all() defaults firstResult to 0", () => {
    batch.all(state);
    expect(PAGINATED_GET.mock.lastCall[3]).toBe(0);
  });

  it("one() GETs statistics filtered by batchId", () => {
    batch.one(state, "b1");
    expect_api_call(GET, {
      url: "/batch/statistics?batchId=b1",
      state,
      signal: state.api.batch.one,
    });
  });

  it("delete() DELETEs the batch with cascade", () => {
    batch.delete(state, "b1");
    expect_api_call(DELETE, {
      url: "/batch/b1?cascade=true",
      body: null,
      state,
      signal: state.api.batch.delete,
    });
  });

  it("set_suspended() PUTs the suspended flag", () => {
    batch.set_suspended(state, "b1", true);
    expect_api_call(PUT, {
      url: "/batch/b1/suspended",
      body: { suspended: true },
      state,
      signal: state.api.batch.update,
    });
  });

  it("set_suspended(false) resumes the batch", () => {
    batch.set_suspended(state, "b1", false);
    expect(PUT.mock.lastCall[1]).toEqual({ suspended: false });
  });
});
