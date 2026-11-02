import { describe, it, expect, vi, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";
import { IdentitySearch } from "./IdentitySearch.jsx";

const CRITERIA = [
  { key: "id", nameKey: "admin.search-id" },
  { key: "nameLike", nameKey: "admin.search-name" },
];

const setup = (overrides = {}) => {
  const on_search = vi.fn();
  const utils = render(
    <IdentitySearch
      criteria={CRITERIA}
      loaded={0}
      on_search={on_search}
      on_more={vi.fn()}
      {...overrides}
    />,
  );
  return { on_search, ...utils };
};

describe("IdentitySearch", () => {
  afterEach(cleanup);

  it("renders a search landmark with a heading", () => {
    const { container, getByText } = setup();
    expect(container.querySelector("search")).toBeTruthy();
    expect(getByText("admin.search-title")).toBeTruthy();
  });

  it("wraps *Like fields in wildcards so a substring matches", () => {
    const { on_search, getByLabelText, getByText } = setup();
    fireEvent.input(getByLabelText("admin.search-name"), {
      target: { value: "de" },
    });
    fireEvent.click(getByText("common.search"));
    expect(on_search).toHaveBeenCalledWith({ nameLike: "%de%" });
  });

  it("leaves exact (non-Like) fields and user wildcards untouched", () => {
    const { on_search, getByLabelText, getByText } = setup();
    fireEvent.input(getByLabelText("admin.search-id"), {
      target: { value: "demo" },
    });
    fireEvent.input(getByLabelText("admin.search-name"), {
      target: { value: "de%" },
    });
    fireEvent.click(getByText("common.search"));
    expect(on_search).toHaveBeenCalledWith({ id: "demo", nameLike: "de%" });
  });
});
