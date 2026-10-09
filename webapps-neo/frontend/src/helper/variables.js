import { fromLocalParts, toLocalParts } from "./date_formatter.js";

// Operaton stores typed variables. These helpers are shared by every page that
// shows or edits them — the process instance panel and the task detail.

export const VARIABLE_TYPES = [
  "String",
  "Boolean",
  "Integer",
  "Long",
  "Double",
  "Short",
  "Date",
  "Json",
  "Object",
  "Null",
];

/** Which input a type needs. Null carries no value at all. */
export const variable_input_type = (type) =>
  ({ Boolean: "select", Date: "datetime-local", Null: "none" })[type] ??
  (["Integer", "Long", "Short", "Double"].includes(type) ? "number" : "text");

/** Coerce raw text from an input to the JS type the REST API expects. */
export const coerce_variable_value = (type, raw) => {
  switch (type) {
    case "Null":
      return null;
    case "Date":
      // A datetime-local input holds wall-clock time; the engine wants that same
      // wall-clock time with an offset, never a UTC shift of it.
      return raw === "" ? null : fromLocalParts(...raw.split("T"));
    case "Boolean":
      return raw === "true" || raw === true;
    case "Integer":
    case "Long":
    case "Short":
      return raw === "" ? null : parseInt(raw, 10);
    case "Double":
    case "Float":
      return raw === "" ? null : parseFloat(raw);
    default:
      return raw;
  }
};

/**
 * Render any variable value as text: JSX skips boolean children, and Object/Json
 * values arrive deserialized, so format them explicitly.
 */
export const format_variable_value = (value) => {
  if (value === null || value === undefined) return "—";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
};

/** The engine's value, in the shape the type's input expects. */
export const variable_edit_value = (type, value) => {
  if (value === null || value === undefined) return "";
  if (type === "Date") {
    const at = new Date(Date.parse(value));
    if (Number.isNaN(at.getTime())) return "";
    const { date, time } = toLocalParts(at);
    return `${date}T${time}`;
  }
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
};

/**
 * How an Object variable was stored. The engine needs it back unchanged on an
 * update, and the reader wants to know what they are looking at.
 */
export const object_type_label = (value_info) =>
  [value_info?.objectTypeName, value_info?.serializationDataFormat]
    .filter(Boolean)
    .join(" · ");
