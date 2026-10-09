import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

// Keep RESPONSE_STATE / url / auth-header builders real; stub the wrappers.
vi.mock("../helper.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    GET: vi.fn(),
    POST: vi.fn(),
    PUT: vi.fn(),
    DELETE: vi.fn(),
  };
});

import { GET, POST, PUT, DELETE, RESPONSE_STATE } from "../helper.jsx";
import { create_mock_state, expect_api_call } from "../../test/helpers.js";
import filter from "./filter.js";

describe("api/resources/filter", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });

  it("get_filters GETs task filters", () => {
    filter.get_filters(state);
    expect_api_call(GET, {
      url: "/filter?resourceType=Task",
      state,
      signal: state.api.filter.list,
    });
  });

  it("get_filter GETs a single filter", () => {
    filter.get_filter(state, "f1");
    expect_api_call(GET, {
      url: "/filter/f1",
      state,
      signal: state.api.filter.one,
    });
  });

  it("create_filter POSTs the body", () => {
    const body = { name: "My tasks" };
    filter.create_filter(state, body);
    expect_api_call(POST, {
      url: "/filter/create",
      body,
      state,
      signal: state.api.filter.create,
    });
  });

  it("update_filter PUTs the body", () => {
    const body = { name: "Renamed" };
    filter.update_filter(state, "f1", body);
    expect_api_call(PUT, {
      url: "/filter/f1",
      body,
      state,
      signal: state.api.filter.update,
    });
  });

  it("delete_filter DELETEs the filter", () => {
    filter.delete_filter(state, "f1");
    expect_api_call(DELETE, {
      url: "/filter/f1",
      body: null,
      state,
      signal: state.api.filter.delete,
    });
  });

  describe("execute_filter (raw fetch into task.list)", () => {
    let fetchMock;
    beforeEach(() => {
      fetchMock = vi.fn();
      vi.stubGlobal("fetch", fetchMock);
    });
    afterEach(() => vi.unstubAllGlobals());

    it("posts the default sorting and replaces task.list on first page", async () => {
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => [{ id: "t1" }, { id: "t2" }],
      });
      await filter.execute_filter(state, "f1", 0, 15);

      const [url, opts] = fetchMock.mock.calls[0];
      expect(url).toContain("/filter/f1/list?firstResult=0&maxResults=15");
      expect(opts.method).toBe("POST");
      expect(JSON.parse(opts.body)).toEqual({
        sorting: [{ sortBy: "created", sortOrder: "desc" }],
      });
      expect(state.api.task.list.value).toEqual({
        status: RESPONSE_STATE.SUCCESS,
        data: [{ id: "t1" }, { id: "t2" }],
        hasMore: false,
      });
    });

    it("appends and de-dupes on subsequent pages and flags hasMore", async () => {
      state.api.task.list.value = {
        status: RESPONSE_STATE.SUCCESS,
        data: [{ id: "t1" }],
      };
      fetchMock.mockResolvedValue({
        ok: true,
        json: async () => [{ id: "t1" }, { id: "t2" }],
      });
      await filter.execute_filter(state, "f1", 15, 2);

      expect(state.api.task.list.value.data).toEqual([
        { id: "t1" },
        { id: "t2" },
      ]);
      expect(state.api.task.list.value.hasMore).toBe(true);
    });

    it("stores ERROR when the request fails", async () => {
      fetchMock.mockResolvedValue({ ok: false, status: 500 });
      await filter.execute_filter(state, "f1", 0, 15);
      expect(state.api.task.list.value.status).toBe(RESPONSE_STATE.ERROR);
    });
  });
});
