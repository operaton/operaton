/**
 * Pure / DOM helpers for TaskForm.
 *
 * Extracted from TaskForm.jsx so they can be unit-tested against the real
 * implementation (see TaskForm_helpers.test.js) and reused by the component,
 * rather than re-implemented in tests.
 */
import DOMPurify from "dompurify";

// ---- Camunda Forms (form-js) variable mapping ------------------------------

/** Map engine form-variables ({ name: { value, type } }) to flat form data. */
export const vars_to_form_data = (vars) => {
  const out = {};
  for (const [k, entry] of Object.entries(vars ?? {})) {
    out[k] = entry?.value;
  }
  return out;
};

/** Infer an engine variable type from a JS value. */
export const infer_type = (v) => {
  if (typeof v === "boolean") return "Boolean";
  if (typeof v === "number") return Number.isInteger(v) ? "Long" : "Double";
  if (Array.isArray(v) || (v && typeof v === "object")) return "Json";
  return "String";
};

/**
 * Map flat form data back to engine variables, preserving the original type
 * where known and inferring it otherwise.
 */
export const form_data_to_vars = (data, originalVars) => {
  const out = {};
  for (const [k, value] of Object.entries(data ?? {})) {
    const original = originalVars?.[k];
    out[k] = { value, type: original?.type ?? infer_type(value) };
  }
  return out;
};

// ---- Legacy embedded/rendered HTML form ------------------------------------

/**
 * Collect data from the rendered legacy form's `.form-control` inputs.
 * Date values are reformatted to dd/mm/yyyy unless `temporary` (a draft save).
 */
export const build_legacy_form_data = (temporary = false) => {
  const inputs = document
    .getElementById("generated-form")
    .getElementsByClassName("form-control");
  const data = {};
  for (let input of inputs) {
    const name = input.name;
    if (!name) continue;
    switch (input.type) {
      case "checkbox":
        data[name] = { value: input.checked };
        break;
      case "date": {
        if (input.value) {
          const val = temporary
            ? input.value
            : input.value.split("-").reverse().join("/");
          data[name] = { value: val };
        }
        break;
      }
      case "number":
        if (input.value) data[name] = { value: parseInt(input.value, 10) };
        break;
      default:
        if (input.value) data[name] = { value: input.value };
    }
  }
  return data;
};

/**
 * Sanitize and normalize an engine-rendered HTML form: coerce input types,
 * disable fields when the current user is not the assignee, mark required
 * fields with a `*`, and restore any locally stored draft values.
 */
export const parse_html = (state, html) => {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  const form = doc.getElementsByTagName("form")[0];

  if (!form) return '<p class="info-box">No form available for this task.</p>';

  const disable =
    state.api.user?.profile?.value?.id !== state.api.task.value?.data.assignee;
  let storedData = localStorage.getItem(
    `task_form_${state.api.task.one.value?.data.id}`,
  );
  if (storedData) storedData = JSON.parse(storedData);

  const inputs = form.getElementsByTagName("input");
  const selects = form.getElementsByTagName("select");

  for (const field of inputs) {
    if (!field.getAttribute("name")) field.name = "name";
    if (field.hasAttribute("uib-datepicker-popup")) field.type = "date";
    if (field.getAttribute("cam-variable-type") === "Long")
      field.type = "number";
    if (disable) field.setAttribute("disabled", "disabled");
    if (field.hasAttribute("required")) {
      const prevElement = field.previousElementSibling;
      const parentLabel = field.closest("label");
      if (
        prevElement &&
        prevElement.tagName === "LABEL" &&
        !prevElement.textContent.includes("*")
      ) {
        prevElement.textContent += "*";
      } else if (parentLabel && !parentLabel.textContent.includes("*")) {
        parentLabel.textContent += "*";
      }
    }
    if (storedData) {
      if (field.type === "checkbox" && storedData[field.name]?.value)
        field.checked = true;
      else if (storedData[field.name])
        field.value = storedData[field.name].value;
    }
  }
  for (const field of selects) {
    if (disable) field.setAttribute("disabled", "disabled");
    if (storedData?.[field.name]) {
      for (const option of field.children) {
        if (option.value === storedData[field.name].value)
          option.selected = true;
      }
    }
  }
  return DOMPurify.sanitize(form.innerHTML, {
    ADD_ATTR: ["cam-variable-type"],
  });
};
