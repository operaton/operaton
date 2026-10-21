import { describe, it, expect, vi, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";
import { signal } from "@preact/signals";
import { ManageFilters } from "./ManageFilters.jsx";

const SORT_OPTIONS = [{ key: "name", nameKey: "list_filter.sort.name" }];
const FILTER_KEYS = [
  { key: "nameLike", nameKey: "list_filter.keys.nameLike", type: "string" },
];

const make_saved_filters = (filters) =>
  signal({ status: "SUCCESS", data: filters });

const base_props = (overrides = {}) => ({
  title: "Manage filters",
  saved_filters_signal: make_saved_filters([]),
  filter_keys: FILTER_KEYS,
  sort_options: SORT_OPTIONS,
  on_save: vi.fn(),
  on_update: vi.fn(),
  on_delete: vi.fn(),
  on_close: vi.fn(),
  build_share_link: vi.fn(() => "http://test/share"),
  ...overrides,
});

describe("ManageFilters", () => {
  afterEach(cleanup);

  it("renders the title and an empty-state hint when no filters exist", () => {
    const { getByText } = render(<ManageFilters {...base_props()} />);
    expect(getByText("Manage filters")).toBeTruthy();
    expect(getByText("list_filter.empty")).toBeTruthy();
  });

  it("invokes on_close when Back is clicked", () => {
    const on_close = vi.fn();
    const { getByText } = render(
      <ManageFilters {...base_props({ on_close })} />,
    );
    fireEvent.click(getByText("list_filter.back"));
    expect(on_close).toHaveBeenCalledTimes(1);
  });

  it("renders one row per saved filter with per-row actions", () => {
    const props = base_props({
      saved_filters_signal: make_saved_filters([
        { id: "abc", name: "Active orders", query: { nameLike: "order" } },
      ]),
    });
    const { getByText, getAllByText } = render(<ManageFilters {...props} />);
    // The name itself is the edit trigger (a button), not a separate "edit".
    expect(getByText("Active orders").closest("button")).toBeTruthy();
    expect(getAllByText("list_filter.duplicate").length).toBeGreaterThan(0);
    expect(getAllByText("list_filter.share_link").length).toBeGreaterThan(0);
    expect(getAllByText("common.delete").length).toBeGreaterThan(0);
  });

  it("on_delete fires with the filter id after confirm", () => {
    const on_delete = vi.fn();
    const props = base_props({
      saved_filters_signal: make_saved_filters([
        { id: "abc", name: "Mine", query: {} },
      ]),
      on_delete,
    });
    const { getByText } = render(<ManageFilters {...props} />);
    fireEvent.click(getByText("common.delete"));
    fireEvent.click(getByText("list_filter.confirm_delete"));
    expect(on_delete).toHaveBeenCalledWith("abc");
  });

  it("Duplicate opens the editor pre-populated with a copy", () => {
    const on_save = vi.fn();
    const props = base_props({
      saved_filters_signal: make_saved_filters([
        { id: "abc", name: "Mine", query: { nameLike: "x" } },
      ]),
      on_save,
    });
    const { getByText, getByLabelText } = render(<ManageFilters {...props} />);
    fireEvent.click(getByText("list_filter.duplicate"));
    const name_input = getByLabelText("list_filter.name");
    expect(name_input.value).toContain("Mine");
    expect(name_input.value).toContain("list_filter.duplicate_suffix");
    fireEvent.click(getByText("common.save"));
    expect(on_save).toHaveBeenCalledTimes(1);
    expect(on_save.mock.calls[0][0].query).toEqual({ nameLike: "x" });
  });

  it("opens the editor inline when a filter name is clicked (no dialog)", () => {
    const props = base_props({
      saved_filters_signal: make_saved_filters([
        { id: "abc", name: "Active orders", query: { nameLike: "order" } },
      ]),
    });
    const { getByText, getByLabelText } = render(<ManageFilters {...props} />);
    // Nothing selected yet: the hint shows on the right.
    expect(getByText("list_filter.select_hint")).toBeTruthy();
    fireEvent.click(getByText("Active orders"));
    expect(getByLabelText("list_filter.name").value).toBe("Active orders");
  });

  it("passes the sharing grants to on_save when sharing is enabled", () => {
    const on_save = vi.fn();
    const props = base_props({ on_save, sharing: { can_share: true } });
    const { getByText, getByLabelText } = render(<ManageFilters {...props} />);
    fireEvent.click(getByText("list_filter.new_filter"));
    fireEvent.input(getByLabelText("list_filter.name"), {
      target: { value: "Team filter" },
    });
    fireEvent.click(getByText("list_filter.share_all"));
    fireEvent.click(getByText("common.save"));

    expect(on_save).toHaveBeenCalledTimes(1);
    expect(on_save.mock.calls[0][1]).toEqual({
      readable_by_all: true,
      grants: [],
    });
  });

  it("omits sharing from on_save when sharing is not enabled", () => {
    const on_save = vi.fn();
    const props = base_props({ on_save });
    const { getByText, getByLabelText, queryByText } = render(
      <ManageFilters {...props} />,
    );
    fireEvent.click(getByText("list_filter.new_filter"));
    expect(queryByText("list_filter.share_section")).toBeNull();
    fireEvent.input(getByLabelText("list_filter.name"), {
      target: { value: "Private" },
    });
    fireEvent.click(getByText("common.save"));
    expect(on_save.mock.calls[0][1]).toBeUndefined();
  });
});
