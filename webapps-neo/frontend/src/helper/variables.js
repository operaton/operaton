// Operaton stores typed variables. These three are shared by every page that
// shows or edits them — the process instance panel and the task detail.

export const VARIABLE_TYPES = [
  "String",
  "Boolean",
  "Integer",
  "Long",
  "Double",
  "Short",
  "Json",
];

/** Coerce raw text from an input to the JS type the REST API expects. */
export const coerce_variable_value = (type, raw) => {
  switch (type) {
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
