import { describe, it, expect, vi, afterEach } from "vitest";
import { render, cleanup, fireEvent } from "@testing-library/preact";
import { VariableRows, repeated_names } from "./VariableRows.jsx";

const rows_of = (...names) =>
  names.map((name) => ({ name, type: "String", value: "" }));

describe("variables entered by hand", () => {
  afterEach(cleanup);

  it("starts with nothing but a way to add", () => {
    const { getByText, container } = render(
      <VariableRows rows={[]} on_change={() => {}} />,
    );
    expect(container.querySelector("table")).toBeNull();
    expect(getByText("tasks.form.add-variable")).toBeTruthy();
  });

  it("adds a row with a sensible default type", () => {
    const on_change = vi.fn();
    const { getByText } = render(
      <VariableRows rows={[]} on_change={on_change} />,
    );
    fireEvent.click(getByText("tasks.form.add-variable"));
    expect(on_change).toHaveBeenCalledWith([
      { name: "", type: "String", value: "" },
    ]);
  });

  it("removes the row that was asked for", () => {
    const on_change = vi.fn();
    const { getAllByText } = render(
      <VariableRows rows={rows_of("a", "b")} on_change={on_change} />,
    );
    fireEvent.click(getAllByText("common.remove")[0]);
    expect(on_change.mock.lastCall[0].map((r) => r.name)).toEqual(["b"]);
  });

  it("offers no value field for a variable that has no value", () => {
    const { queryByLabelText } = render(
      <VariableRows
        rows={[{ name: "n", type: "Null", value: "" }]}
        on_change={() => {}}
      />,
    );
    expect(queryByLabelText("common.value")).toBeNull();
  });

  it("says so when a name is used twice", () => {
    const { getByText } = render(
      <VariableRows rows={rows_of("amount", "amount")} on_change={() => {}} />,
    );
    expect(getByText("tasks.form.duplicate-variable")).toBeTruthy();
  });

  it("ignores the unnamed rows when looking for repeats", () => {
    expect(repeated_names(rows_of("", "", "a"))).toBe(false);
    expect(repeated_names(rows_of("a", "a"))).toBe(true);
  });

  it("locks every field while the form is read-only", () => {
    const { container } = render(
      <VariableRows rows={rows_of("a")} on_change={() => {}} disabled />,
    );
    const controls = [...container.querySelectorAll("input, select, button")];
    expect(controls.every((c) => c.disabled)).toBe(true);
  });
});
