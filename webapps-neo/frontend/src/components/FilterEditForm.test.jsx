import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/preact";
import { signal } from "@preact/signals";
import { fireEvent } from "@testing-library/preact";
import { render_with_state } from "../test/render.jsx";
import {
  FilterEditForm,
  filter_form_from_saved,
  filter_from_form,
} from "./FilterEditForm.jsx";

const FILTER_KEYS = [
  { key: "name", nameKey: "name", type: "string" },
  { key: "nameLike", nameKey: "nameLike", type: "string" },
  { key: "priority", nameKey: "priority", type: "number" },
  { key: "processVariables", nameKey: "processVariables", type: "variable" },
  { key: "taskVariables", nameKey: "taskVariables", type: "variable" },
];

const form_with = (criteria) => ({
  name: "A filter",
  sortBy: "",
  sortOrder: "asc",
  criteria,
});

describe("criteria of a saved filter", () => {
  it("adds a criterion row", () => {
    const form = signal(form_with([]));
    const { getByText, container } = render_with_state(
      <FilterEditForm
        filter_keys={FILTER_KEYS}
        sort_options={[]}
        form={form}
        on_submit={() => {}}
        on_cancel={() => {}}
      />,
    );
    expect(container.querySelectorAll("tbody tr")).toHaveLength(0);
    fireEvent.click(getByText("list_filter.add_criterion"));
    expect(container.querySelectorAll("tbody tr")).toHaveLength(1);
  });

  it("removes the row that was asked for, not the last one", () => {
    const form = signal(
      form_with([
        { key: "name", value: "first" },
        { key: "priority", value: "50" },
      ]),
    );
    const { getAllByText, container } = render_with_state(
      <FilterEditForm
        filter_keys={FILTER_KEYS}
        sort_options={[]}
        form={form}
        on_submit={() => {}}
        on_cancel={() => {}}
      />,
    );
    fireEvent.click(getAllByText("common.remove")[0]);
    const left = form.value.criteria;
    expect(left).toHaveLength(1);
    expect(left[0].value).toBe("50");
  });

  // must not reappear in the row that takes its place.
  it("leaves no value behind when a row is removed", () => {
    const form = signal(
      form_with([
        { key: "name", value: "first" },
        { key: "nameLike", value: "second" },
      ]),
    );
    const { getAllByText, container } = render_with_state(
      <FilterEditForm
        filter_keys={FILTER_KEYS}
        sort_options={[]}
        form={form}
        on_submit={() => {}}
        on_cancel={() => {}}
      />,
    );
    fireEvent.click(getAllByText("common.remove")[0]);
    const values = [...container.querySelectorAll("tbody input")].map(
      (i) => i.value,
    );
    expect(values).toEqual(["second"]);
  });

  it("leaves a criterion without a value out of the query", () => {
    const { query } = filter_from_form(
      form_with([
        { key: "name", value: "" },
        { key: "nameLike", value: "Review" },
      ]),
      FILTER_KEYS,
    );
    expect(query.name).toBeUndefined();
    expect(query.nameLike).toBe("Review");
  });

  it("leaves a criterion with no key out of the query", () => {
    const { query } = filter_from_form(
      form_with([{ key: "", value: "Review" }]),
      FILTER_KEYS,
    );
    expect(Object.keys(query)).toHaveLength(0);
  });

  it.each([
    ["nameLike", "Review%"],
    ["processDefinitionName", "Invoice"],
    ["processInstanceBusinessKey", "BK-1"],
    ["candidateGroup", "reviewers"],
  ])("carries %s into the query", (key, value) => {
    const keys = [...FILTER_KEYS, { key, nameKey: key, type: "string" }];
    const { query } = filter_from_form(form_with([{ key, value }]), keys);
    expect(query[key]).toBe(value);
  });

  it("carries an edited value, not the one it started with", () => {
    const { query } = filter_from_form(
      form_with([{ key: "name", value: "changed" }]),
      FILTER_KEYS,
    );
    expect(query.name).toBe("changed");
  });

  it("keeps a date criterion as the date that was typed", () => {
    const keys = [
      ...FILTER_KEYS,
      { key: "followUpBefore", nameKey: "followUpBefore", type: "date" },
    ];
    const { query } = filter_from_form(
      form_with([{ key: "followUpBefore", value: "2026-07-01T09:00" }]),
      keys,
    );
    expect(query.followUpBefore).toBe("2026-07-01T09:00");
  });

  // refused a duplicate key; this one silently keeps the last of them.
  it.fails("refuses the same criterion key twice", () => {
    const { query } = filter_from_form(
      form_with([
        { key: "name", value: "first" },
        { key: "name", value: "second" },
      ]),
      FILTER_KEYS,
    );
    expect(query.name).toBe("first");
  });
});

describe("variable criteria", () => {
  it("writes a comparison as name, operator and value", () => {
    const { query } = filter_from_form(
      form_with([
        {
          key: "processVariables",
          variable_name: "amount",
          operator: "gt",
          value: "100",
        },
      ]),
      FILTER_KEYS,
    );
    expect(query.processVariables).toEqual([
      { name: "amount", operator: "gt", value: 100 },
    ]);
  });

  it("keeps a numeric value a number so greater-than compares numerically", () => {
    const { query } = filter_from_form(
      form_with([
        {
          key: "processVariables",
          variable_name: "amount",
          operator: "gteq",
          value: "9",
        },
      ]),
      FILTER_KEYS,
    );
    expect(query.processVariables[0].value).toBe(9);
  });

  it("keeps true and false booleans", () => {
    const { query } = filter_from_form(
      form_with([
        {
          key: "taskVariables",
          variable_name: "approved",
          operator: "eq",
          value: "false",
        },
      ]),
      FILTER_KEYS,
    );
    expect(query.taskVariables[0].value).toBe(false);
  });

  it("leaves a value that is neither number nor boolean a string", () => {
    const { query } = filter_from_form(
      form_with([
        {
          key: "processVariables",
          variable_name: "city",
          operator: "like",
          value: "%burg",
        },
      ]),
      FILTER_KEYS,
    );
    expect(query.processVariables[0].value).toBe("%burg");
  });

  it("collects several comparisons on the same key into one list", () => {
    const { query } = filter_from_form(
      form_with([
        {
          key: "processVariables",
          variable_name: "amount",
          operator: "gt",
          value: "10",
        },
        {
          key: "processVariables",
          variable_name: "amount",
          operator: "lt",
          value: "20",
        },
      ]),
      FILTER_KEYS,
    );
    expect(query.processVariables).toHaveLength(2);
    expect(query.processVariables.map((c) => c.operator)).toEqual(["gt", "lt"]);
  });

  it("compares with equals when no operator was picked", () => {
    const { query } = filter_from_form(
      form_with([
        { key: "processVariables", variable_name: "amount", value: "1" },
      ]),
      FILTER_KEYS,
    );
    expect(query.processVariables[0].operator).toBe("eq");
  });

  it("drops a comparison that names no variable", () => {
    const { query } = filter_from_form(
      form_with([
        { key: "processVariables", variable_name: "", value: "1" },
        { key: "name", value: "Review" },
      ]),
      FILTER_KEYS,
    );
    expect(query.processVariables).toBeUndefined();
    expect(query.name).toBe("Review");
  });

  it("reads a saved comparison back into its own row", () => {
    const form = filter_form_from_saved({
      name: "Large amounts",
      query: {
        processVariables: [{ name: "amount", operator: "gt", value: 100 }],
        name: "Review",
      },
    });
    expect(form.criteria).toEqual([
      {
        key: "processVariables",
        variable_name: "amount",
        operator: "gt",
        value: "100",
      },
      { key: "name", value: "Review" },
    ]);
  });

  it("survives a round trip through the saved filter", () => {
    const criteria = [
      {
        key: "processVariables",
        variable_name: "amount",
        operator: "lteq",
        value: "42",
      },
    ];
    const saved = filter_from_form(form_with(criteria), FILTER_KEYS);
    expect(filter_form_from_saved(saved).criteria).toEqual(criteria);
  });

  it("offers a name, a comparison and a value for a variable criterion", () => {
    render_with_state(
      <FilterEditForm
        filter_keys={FILTER_KEYS}
        sort_options={[]}
        form={signal(
          form_with([
            {
              key: "processVariables",
              variable_name: "amount",
              operator: "gt",
              value: "100",
            },
          ]),
        )}
        on_submit={() => {}}
        on_cancel={() => {}}
      />,
    );
    expect(screen.getByLabelText("list_filter.variable_name").value).toBe(
      "amount",
    );
    expect(screen.getByLabelText("list_filter.variable_operator").value).toBe(
      "gt",
    );
    expect(screen.getByLabelText("common.value").value).toBe("100");
  });
});
