// @vitest-environment jsdom
// rendered_form_to_schema parses engine HTML via DOMParser and custom-attribute
// selectors (cam-variable-name); jsdom parses these reliably where happy-dom is
// flaky, so this file opts into jsdom.
import { describe, it, expect } from "vitest";
import {
  vars_to_form_data,
  form_data_to_vars,
  infer_type,
  rendered_form_to_schema,
} from "./TaskForm_helpers.js";

describe("TaskForm helpers", () => {
  describe("infer_type", () => {
    it("maps JS values to engine variable types", () => {
      expect(infer_type(true)).toBe("Boolean");
      expect(infer_type(42)).toBe("Long");
      expect(infer_type(3.14)).toBe("Double");
      expect(infer_type([1, 2])).toBe("Json");
      expect(infer_type({ a: 1 })).toBe("Json");
      expect(infer_type("hi")).toBe("String");
    });
  });

  describe("vars_to_form_data", () => {
    it("flattens engine form-variables to their values", () => {
      expect(
        vars_to_form_data({
          amount: { value: 10, type: "Long" },
          note: { value: "hi", type: "String" },
        }),
      ).toEqual({ amount: 10, note: "hi" });
    });

    it("returns an empty object for nullish input", () => {
      expect(vars_to_form_data(undefined)).toEqual({});
      expect(vars_to_form_data(null)).toEqual({});
    });
  });

  describe("form_data_to_vars", () => {
    it("preserves the original type when known", () => {
      expect(
        form_data_to_vars({ amount: 10 }, { amount: { type: "Integer" } }),
      ).toEqual({ amount: { value: 10, type: "Integer" } });
    });

    it("infers the type when the variable is new", () => {
      expect(form_data_to_vars({ agree: true }, {})).toEqual({
        agree: { value: true, type: "Boolean" },
      });
    });
  });

  describe("rendered_form_to_schema (engine HTML -> form-js schema)", () => {
    it("maps a required string field to a textfield with its label", () => {
      const schema = rendered_form_to_schema(
        `<form>
          <label>Full name</label>
          <input type="text" cam-variable-name="fullName" cam-variable-type="String" required />
        </form>`,
      );
      expect(schema.type).toBe("default");
      expect(schema.components).toEqual([
        {
          id: "fullName",
          key: "fullName",
          label: "Full name",
          validate: { required: true },
          type: "textfield",
        },
      ]);
    });

    it("maps number, boolean and date fields by variable/input type", () => {
      const schema = rendered_form_to_schema(
        `<form>
          <label>Age</label>
          <input type="number" cam-variable-name="age" cam-variable-type="Long" />
          <label>Agree</label>
          <input type="checkbox" cam-variable-name="agree" cam-variable-type="Boolean" />
          <label>DOB</label>
          <input type="text" cam-variable-name="dob" cam-variable-type="Date" uib-datepicker-popup />
        </form>`,
      );
      expect(
        schema.components.map((c) => [c.key, c.type, c.subtype]),
      ).toEqual([
        ["age", "number", undefined],
        ["agree", "checkbox", undefined],
        ["dob", "datetime", "date"],
      ]);
    });

    it("maps a select to values, dropping the empty placeholder option", () => {
      const schema = rendered_form_to_schema(
        `<form>
          <label>Colour</label>
          <select cam-variable-name="colour" cam-variable-type="String">
            <option value="">—</option>
            <option value="r">Red</option>
            <option value="g">Green</option>
          </select>
        </form>`,
      );
      expect(schema.components[0]).toMatchObject({
        key: "colour",
        type: "select",
        values: [
          { value: "r", label: "Red" },
          { value: "g", label: "Green" },
        ],
      });
    });

    it("marks disabled/readonly fields and drops fields without a variable name", () => {
      const schema = rendered_form_to_schema(
        `<form>
          <label>Read only</label>
          <input type="text" cam-variable-name="ro" cam-variable-type="String" readonly />
          <input type="text" name="ignored" />
        </form>`,
      );
      expect(schema.components).toHaveLength(1);
      expect(schema.components[0]).toMatchObject({ key: "ro", disabled: true });
    });

    it("falls back to the variable name and strips a trailing required marker", () => {
      const schema = rendered_form_to_schema(
        `<form>
          <input type="text" cam-variable-name="noLabel" cam-variable-type="String" />
          <label>Response *</label>
          <input type="text" cam-variable-name="withStar" cam-variable-type="String" required />
        </form>`,
      );
      expect(schema.components[0].label).toBe("noLabel");
      expect(schema.components[1].label).toBe("Response");
    });

    it("returns no components when there are no form fields", () => {
      expect(rendered_form_to_schema("<div>nothing</div>").components).toEqual(
        [],
      );
      expect(rendered_form_to_schema("").components).toEqual([]);
    });
  });
});
