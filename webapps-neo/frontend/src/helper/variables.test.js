import { describe, it, expect } from "vitest";
import {
  VARIABLE_TYPES,
  coerce_variable_value,
  format_variable_value,
  object_type_label,
  variable_edit_value,
  variable_input_type,
} from "./variables.js";

describe("variable types", () => {
  it("offers the types the old cockpit offered", () => {
    expect(VARIABLE_TYPES).toContain("Date");
    expect(VARIABLE_TYPES).toContain("Null");
    expect(VARIABLE_TYPES).toContain("Object");
  });

  it("gives a null variable no value at all", () => {
    expect(coerce_variable_value("Null", "anything")).toBeNull();
    expect(variable_input_type("Null")).toBe("none");
  });

  it("gives a boolean variable a choice, not a text field", () => {
    expect(variable_input_type("Boolean")).toBe("select");
    expect(coerce_variable_value("Boolean", "false")).toBe(false);
  });

  it("counts a number as a number", () => {
    expect(variable_input_type("Integer")).toBe("number");
    expect(coerce_variable_value("Short", "7")).toBe(7);
    expect(coerce_variable_value("Double", "1.5")).toBe(1.5);
  });

  it("sends a date as the wall-clock time it was typed as", () => {
    const sent = coerce_variable_value("Date", "2026-07-01T14:30");
    expect(sent).toMatch(/^2026-07-01T14:30:00\.000[+-]\d{4}$/);
  });

  it("leaves an empty date empty rather than sending an invalid one", () => {
    expect(coerce_variable_value("Date", "")).toBeNull();
  });

  it("puts a stored date back into the input as local time", () => {
    const stored = new Date(2026, 6, 1, 14, 30).toISOString();
    expect(variable_edit_value("Date", stored)).toBe("2026-07-01T14:30");
  });

  it("edits an object as the text it is stored as", () => {
    expect(variable_edit_value("Object", { a: 1 })).toBe('{"a":1}');
  });

  it("names how an object was serialized", () => {
    expect(
      object_type_label({
        objectTypeName: "com.example.Order",
        serializationDataFormat: "application/json",
      }),
    ).toBe("com.example.Order · application/json");
  });

  it("shows a dash where there is no value", () => {
    expect(format_variable_value(null)).toBe("—");
  });
});
