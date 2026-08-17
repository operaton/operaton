import { describe, it, expect } from "vitest";
import {
  parse_list_query,
  write_list_query,
  criteria_to_params,
  build_share_link,
} from "./list_query.js";

describe("parse_list_query", () => {
  it("returns null fields and empty criteria for an empty query", () => {
    expect(parse_list_query({})).toEqual({
      saved_filter_id: null,
      sortBy: null,
      sortOrder: null,
      criteria: {},
    });
  });

  it("extracts reserved keys and namespaced criteria", () => {
    const parsed = parse_list_query({
      filter: "abc",
      sortBy: "name",
      sortOrder: "desc",
      "q.nameLike": "order",
      "q.active": "true",
      unrelated: "ignored",
    });
    expect(parsed.saved_filter_id).toBe("abc");
    expect(parsed.sortBy).toBe("name");
    expect(parsed.sortOrder).toBe("desc");
    expect(parsed.criteria).toEqual({ nameLike: "order", active: "true" });
  });

  it("drops empty criterion values", () => {
    expect(
      parse_list_query({ "q.nameLike": "", "q.key": "foo" }).criteria,
    ).toEqual({ key: "foo" });
  });
});

describe("write_list_query", () => {
  const base = "http://test.local/list?other=keep";

  it("sets saved_filter_id and deletes for 'all'", () => {
    expect(write_list_query(base, { saved_filter_id: "x" })).toBe(
      "/list?other=keep&filter=x",
    );
    expect(
      write_list_query(`${base}&filter=x`, { saved_filter_id: "all" }),
    ).toBe("/list?other=keep");
  });

  it("sets sortBy / sortOrder and clears when empty", () => {
    expect(
      write_list_query(base, { sortBy: "name", sortOrder: "asc" }),
    ).toBe("/list?other=keep&sortBy=name&sortOrder=asc");
    expect(
      write_list_query(`${base}&sortBy=name`, { sortBy: null }),
    ).toBe("/list?other=keep");
  });

  it("replaces criteria wholesale, leaving other params alone", () => {
    expect(
      write_list_query(`${base}&q.old=1&q.also=2`, {
        criteria: { fresh: "v", active: true },
      }),
    ).toBe("/list?other=keep&q.fresh=v&q.active=true");
  });

  it("round-trips with parse_list_query", () => {
    const out = write_list_query("http://_/p", {
      saved_filter_id: "f1",
      sortBy: "key",
      sortOrder: "desc",
      criteria: { nameLike: "x", active: "true" },
    });
    const parsed = parse_list_query(Object.fromEntries(new URL(out, "http://_").searchParams));
    expect(parsed).toEqual({
      saved_filter_id: "f1",
      sortBy: "key",
      sortOrder: "desc",
      criteria: { nameLike: "x", active: "true" },
    });
  });
});

describe("criteria_to_params", () => {
  it("skips empty values and stringifies the rest", () => {
    const p = criteria_to_params({
      nameLike: "foo",
      active: true,
      versionTag: "",
      tenant: null,
    });
    expect(p.toString()).toBe("nameLike=foo&active=true");
  });
});

describe("build_share_link", () => {
  it("returns the URL unchanged", () => {
    expect(build_share_link("http://test.local/p?filter=abc")).toBe(
      "http://test.local/p?filter=abc",
    );
  });
});
