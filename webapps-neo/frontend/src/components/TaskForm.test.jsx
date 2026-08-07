import { describe, it, expect, beforeEach, afterEach } from "vitest";
import {
  vars_to_form_data,
  form_data_to_vars,
  infer_type,
  build_legacy_form_data,
  parse_html,
} from "./TaskForm_helpers.js";
import { create_mock_state } from "../test/helpers.js";

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

  describe("build_legacy_form_data (reads the #generated-form DOM)", () => {
    afterEach(() => {
      document.body.innerHTML = "";
    });

    const mount = (innerHTML) => {
      document.body.innerHTML = `<div id="generated-form">${innerHTML}</div>`;
    };

    it("collects text, checkbox, number and date fields", () => {
      mount(`
        <input class="form-control" name="username" type="text" value="john" />
        <input class="form-control" name="agree" type="checkbox" checked />
        <input class="form-control" name="age" type="number" value="25" />
        <input class="form-control" name="dob" type="date" value="2000-12-25" />
      `);
      expect(build_legacy_form_data()).toEqual({
        username: { value: "john" },
        agree: { value: true },
        age: { value: 25 },
        dob: { value: "25/12/2000" },
      });
    });

    it("keeps the raw date format for a temporary (draft) save", () => {
      mount(
        `<input class="form-control" name="dob" type="date" value="2000-12-25" />`,
      );
      expect(build_legacy_form_data(true)).toEqual({
        dob: { value: "2000-12-25" },
      });
    });

    it("skips fields without a name", () => {
      mount(`<input class="form-control" type="text" value="x" />`);
      expect(build_legacy_form_data()).toEqual({});
    });
  });

  describe("parse_html", () => {
    let state;
    beforeEach(() => {
      state = create_mock_state();
      // current user is the assignee -> fields stay enabled
      state.api.user.profile.value = { id: "demo" };
      state.api.task.one.value = { data: { id: "t1" } };
      state.api.task.value = { data: { assignee: "demo" } };
      localStorage.clear();
    });

    it("returns an info box when there is no <form>", () => {
      expect(parse_html(state, "<div>nope</div>")).toContain("info-box");
    });

    it("marks required fields with an asterisk", () => {
      const out = parse_html(
        state,
        `<form><label>Name</label><input type="text" name="n" required /></form>`,
      );
      expect(out).toContain("Name*");
    });

    it("disables fields when the user is not the assignee", () => {
      state.api.task.value = { data: { assignee: "someone-else" } };
      const out = parse_html(
        state,
        `<form><input type="text" name="n" /></form>`,
      );
      expect(out).toContain("disabled");
    });
  });
});
