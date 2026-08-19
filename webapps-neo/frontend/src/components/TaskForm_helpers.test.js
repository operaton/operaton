import { describe, it, expect } from "vitest";
import {
  schema_variable_keys,
  vars_to_form_data,
  form_data_to_vars,
  infer_type,
  form_ref_of,
} from "./TaskForm_helpers.js";

describe("TaskForm_helpers", () => {
  describe("form_ref_of", () => {
    const ref = { key: "loan-reviewer", binding: "deployment" };

    it("reads camundaFormRef (C7) and operatonFormRef (Operaton)", () => {
      expect(form_ref_of({ camundaFormRef: ref })).toEqual(ref);
      expect(form_ref_of({ operatonFormRef: ref })).toEqual(ref);
    });

    it("is null without a formRef — a formKey is not one", () => {
      expect(form_ref_of({ formKey: "embedded:app:forms/x.html" })).toBe(null);
      expect(form_ref_of({})).toBe(null);
      expect(form_ref_of(undefined)).toBe(null);
    });
  });

  describe("schema_variable_keys", () => {
    it("collects top-level component keys", () => {
      const schema = {
        components: [
          { type: "textfield", key: "amount" },
          { type: "textfield", key: "credit" },
          { type: "text" }, // no key
        ],
      };
      expect(schema_variable_keys(schema)).toEqual(new Set(["amount", "credit"]));
    });

    it("walks nested groups", () => {
      const schema = {
        components: [
          { type: "group", components: [{ key: "nested" }] },
          { key: "top" },
        ],
      };
      expect(schema_variable_keys(schema)).toEqual(new Set(["nested", "top"]));
    });

    it("maps path keys to their first segment (the engine variable)", () => {
      const schema = { components: [{ key: "person.name" }] };
      expect(schema_variable_keys(schema)).toEqual(new Set(["person"]));
    });

    it("returns an empty set for a missing schema", () => {
      expect(schema_variable_keys(undefined)).toEqual(new Set());
    });
  });

  describe("vars_to_form_data", () => {
    const vars = {
      amount: { value: 100, type: "Long" },
      payload: { value: { a: 1 }, type: "Json" },
    };

    it("flattens all variables when no allow-set is given", () => {
      expect(vars_to_form_data(vars)).toEqual({ amount: 100, payload: { a: 1 } });
    });

    it("keeps only allowed keys, dropping untouched Json/Object vars (#92)", () => {
      expect(vars_to_form_data(vars, new Set(["amount"]))).toEqual({
        amount: 100,
      });
    });
  });

  describe("form_data_to_vars", () => {
    it("preserves the original type where known and infers otherwise", () => {
      const out = form_data_to_vars(
        { amount: 100, note: "hi" },
        { amount: { value: 0, type: "Long" } },
      );
      expect(out).toEqual({
        amount: { value: 100, type: "Long" },
        note: { value: "hi", type: "String" },
      });
    });

    it("drops keys outside the allow-set (#92)", () => {
      const out = form_data_to_vars(
        { amount: 100, payload: { a: 1 } },
        { payload: { value: {}, type: "Json" } },
        new Set(["amount"]),
      );
      expect(out).toEqual({ amount: { value: 100, type: "Long" } });
    });
  });

  describe("infer_type", () => {
    it("infers engine types from JS values", () => {
      expect(infer_type(true)).toBe("Boolean");
      expect(infer_type(3)).toBe("Long");
      expect(infer_type(3.5)).toBe("Double");
      expect(infer_type({ a: 1 })).toBe("Json");
      expect(infer_type([1, 2])).toBe("Json");
      expect(infer_type("x")).toBe("String");
    });
  });
});
