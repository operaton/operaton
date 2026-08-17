import { describe, it, expect, beforeEach } from "vitest";
import { signal } from "@preact/signals";
import {
  list_saved_filters,
  create_saved_filter,
  update_saved_filter,
  delete_saved_filter,
  hydrate_signal,
} from "./saved_filters.js";

const RESOURCE = "test_resource";
const OTHER = "other_resource";
const KEY = `operaton.saved_filters.${RESOURCE}`;

beforeEach(() => {
  localStorage.clear();
});

describe("list_saved_filters", () => {
  it("returns an empty array when nothing is stored", () => {
    expect(list_saved_filters(RESOURCE)).toEqual([]);
  });

  it("returns an empty array when storage holds malformed JSON", () => {
    localStorage.setItem(KEY, "{not json");
    expect(list_saved_filters(RESOURCE)).toEqual([]);
  });

  it("returns an empty array when storage holds a non-array value", () => {
    localStorage.setItem(KEY, JSON.stringify({ id: "x" }));
    expect(list_saved_filters(RESOURCE)).toEqual([]);
  });
});

describe("create_saved_filter", () => {
  it("assigns an id and appends to storage", () => {
    const after = create_saved_filter(RESOURCE, {
      name: "Mine",
      query: { nameLike: "x" },
    });
    expect(after).toHaveLength(1);
    expect(after[0].id).toBeTruthy();
    expect(after[0].name).toBe("Mine");
    expect(list_saved_filters(RESOURCE)).toEqual(after);
  });

  it("namespaces by resource_type", () => {
    create_saved_filter(RESOURCE, { name: "A", query: {} });
    create_saved_filter(OTHER, { name: "B", query: {} });
    expect(list_saved_filters(RESOURCE).map((f) => f.name)).toEqual(["A"]);
    expect(list_saved_filters(OTHER).map((f) => f.name)).toEqual(["B"]);
  });
});

describe("update_saved_filter", () => {
  it("merges patch into the matching filter and preserves id", () => {
    const [created] = create_saved_filter(RESOURCE, {
      name: "Old",
      query: { a: "1" },
    });
    const after = update_saved_filter(RESOURCE, created.id, {
      name: "New",
      query: { a: "2" },
    });
    expect(after[0]).toEqual({
      id: created.id,
      name: "New",
      query: { a: "2" },
    });
  });

  it("leaves other filters unchanged when id misses", () => {
    create_saved_filter(RESOURCE, { name: "Keep", query: {} });
    const after = update_saved_filter(RESOURCE, "missing", { name: "x" });
    expect(after.map((f) => f.name)).toEqual(["Keep"]);
  });
});

describe("delete_saved_filter", () => {
  it("removes the matching filter", () => {
    const [a] = create_saved_filter(RESOURCE, { name: "A", query: {} });
    create_saved_filter(RESOURCE, { name: "B", query: {} });
    const after = delete_saved_filter(RESOURCE, a.id);
    expect(after.map((f) => f.name)).toEqual(["B"]);
  });
});

describe("hydrate_signal", () => {
  it("writes a SUCCESS-shaped value into the signal", () => {
    create_saved_filter(RESOURCE, { name: "A", query: {} });
    const s = signal(null);
    hydrate_signal(RESOURCE, s);
    expect(s.value.status).toBe("SUCCESS");
    expect(s.value.data).toHaveLength(1);
    expect(s.value.data[0].name).toBe("A");
  });
});
