import { describe, it, expect, vi, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";
import { signal } from "@preact/signals";
import { ListFilter } from "./ListFilter.jsx";

const SORT_OPTIONS = [
  { key: "name", nameKey: "list_filter.sort.name" },
  { key: "key", nameKey: "list_filter.sort.key" },
];

const make_saved_filters = (filters) =>
  signal({ status: "SUCCESS", data: filters });

const empty_state = {
  saved_filter_id: null,
  sortBy: "name",
  sortOrder: "asc",
  criteria: {},
};

const base_props = (overrides = {}) => ({
  sort_options: SORT_OPTIONS,
  saved_filters_signal: make_saved_filters([]),
  current: empty_state,
  defaults: { sortBy: "name", sortOrder: "asc" },
  on_change: vi.fn(),
  on_manage: vi.fn(),
  ...overrides,
});

describe("ListFilter", () => {
  afterEach(cleanup);

  it("renders sort select with provided options", () => {
    const { getByText } = render(<ListFilter {...base_props()} />);
    expect(getByText("list_filter.sort.name")).toBeTruthy();
    expect(getByText("list_filter.sort.key")).toBeTruthy();
  });

  it("emits saved_filter_id on saved-filter change", () => {
    const on_change = vi.fn();
    const props = base_props({
      saved_filters_signal: make_saved_filters([
        { id: "abc", name: "Mine", query: { nameLike: "x" } },
      ]),
      on_change,
    });
    const { getByLabelText } = render(<ListFilter {...props} />);
    fireEvent.change(getByLabelText("list_filter.saved_filter"), {
      target: { value: "abc" },
    });
    expect(on_change).toHaveBeenCalledWith({ saved_filter_id: "abc" });
  });

  it("emits sortBy and sortOrder on change", () => {
    const on_change = vi.fn();
    const { getByLabelText } = render(
      <ListFilter {...base_props({ on_change })} />,
    );
    fireEvent.change(getByLabelText("list_filter.sort_by"), {
      target: { value: "key" },
    });
    fireEvent.change(getByLabelText("list_filter.sort_order"), {
      target: { value: "desc" },
    });
    expect(on_change).toHaveBeenNthCalledWith(1, { sortBy: "key" });
    expect(on_change).toHaveBeenNthCalledWith(2, { sortOrder: "desc" });
  });

  it("omits the 'my' preset unless include_my_filter is set", () => {
    const without = render(<ListFilter {...base_props()} />);
    expect(without.queryByText("list_filter.my")).toBeNull();
    cleanup();
    const withMy = render(
      <ListFilter {...base_props({ include_my_filter: true })} />,
    );
    expect(withMy.getByText("list_filter.my")).toBeTruthy();
  });

  it("invokes on_manage when the Edit button is clicked", () => {
    const on_manage = vi.fn();
    const { getByText } = render(
      <ListFilter {...base_props({ on_manage })} />,
    );
    fireEvent.click(getByText("list_filter.edit"));
    expect(on_manage).toHaveBeenCalledTimes(1);
  });
});
