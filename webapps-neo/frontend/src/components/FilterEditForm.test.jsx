import { describe, it, expect } from "vitest";
import { screen } from "@testing-library/preact";
import { signal } from "@preact/signals";
import { render_with_state } from "../test/render.jsx";
import {
  FilterEditForm,
  filter_form_from_saved,
  filter_from_form,
} from "./FilterEditForm.jsx";

const FILTER_KEYS = [
  { key: "name", nameKey: "name", type: "string" },
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
