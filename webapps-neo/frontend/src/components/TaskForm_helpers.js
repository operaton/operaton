/**
 * Pure / DOM helpers for TaskForm.
 *
 * Extracted from TaskForm.jsx so they can be unit-tested against the real
 * implementation (see TaskForm_helpers.test.js) and reused by the component,
 * rather than re-implemented in tests.
 */
// ---- Camunda Forms (form-js) -----------------------------------------------

/**
 * The form-js (Camunda Forms) reference of a task or a start-form DTO, or null.
 *
 * A deployed form-js form is referenced from the BPMN with `camunda:formRef` +
 * `camunda:formRefBinding` and reported by the engine as `camundaFormRef`
 * (`operatonFormRef` on Operaton) — an object of `{ key, binding, version }`.
 * `formKey` is the *other*, legacy mechanism (embedded AngularJS HTML forms)
 * and never points at a form-js form.
 */
export const form_ref_of = (entity) =>
  entity?.camundaFormRef ?? entity?.operatonFormRef ?? null;

// ---- Camunda Forms (form-js) variable mapping ------------------------------

/**
 * Collect the engine variable names a form-js schema binds, walking nested
 * component groups. Path keys (`a.b`) map to the first segment, which is the
 * engine variable (see #92).
 */
export const schema_variable_keys = (schema) => {
  const keys = new Set();
  const walk = (components) => {
    for (const c of components ?? []) {
      if (c.key) keys.add(c.key.split(".")[0]);
      if (c.components) walk(c.components);
    }
  };
  walk(schema?.components);
  return keys;
};

/**
 * Map engine form-variables ({ name: { value, type } }) to flat form data.
 * When `allowed` (a Set of variable names) is given, only those are kept so
 * untouched Json/Object variables never enter the form round-trip (see #92).
 */
export const vars_to_form_data = (vars, allowed) => {
  const out = {};
  for (const [k, entry] of Object.entries(vars ?? {})) {
    if (allowed && !allowed.has(k)) continue;
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
export const form_data_to_vars = (data, originalVars, allowed) => {
  const out = {};
  for (const [k, value] of Object.entries(data ?? {})) {
    if (allowed && !allowed.has(k)) continue;
    const original = originalVars?.[k];
    out[k] = { value, type: original?.type ?? infer_type(value) };
  }
  return out;
};

// ---- Generated (engine-rendered) task form ---------------------------------

// The label for a rendered form field: the engine emits a sibling <label>
// before the control (or wraps it, or groups it in a .form-group). Falls back
// to the variable name. Trailing "*" (an engine required marker) is stripped —
// CamundaForm draws its own required mark.
const field_label = (field, fallback) => {
  const prev = field.previousElementSibling;
  let label = prev && prev.tagName === "LABEL" ? prev : field.closest("label");
  if (!label) label = field.closest(".form-group")?.querySelector("label");
  const text = label?.textContent?.replace(/\s*\*+\s*$/, "").trim();
  return text || fallback;
};

const NUMBER_TYPES = ["Long", "Integer", "Short", "Double", "Float"];

/**
 * Adapt an engine server-rendered form (the HTML from `/task/{id}/rendered-form`,
 * generated from `camunda:formData` form fields) into a form-js JSON schema, so
 * a generated task form can be rendered by the same `CamundaForm` component —
 * and thus looks identical to a deployed form-js form.
 *
 * Reads the field descriptors the engine annotates (`cam-variable-name`,
 * `cam-variable-type`) rather than injecting the raw HTML. Returns
 * `{ type: 'default', components: [...] }`; components is empty when the task
 * has no form fields.
 */
export const rendered_form_to_schema = (html) => {
  const doc = new DOMParser().parseFromString(html ?? "", "text/html");
  const fields = doc.querySelectorAll(
    "input[cam-variable-name], select[cam-variable-name], textarea[cam-variable-name]",
  );

  const components = [];
  for (const field of fields) {
    const key = field.getAttribute("cam-variable-name");
    if (!key) continue;

    const var_type = field.getAttribute("cam-variable-type") ?? "";
    const tag = field.tagName.toLowerCase();
    const input_type = (field.getAttribute("type") ?? "").toLowerCase();

    const base = { id: key, key, label: field_label(field, key) };
    if (field.hasAttribute("required")) base.validate = { required: true };
    if (field.hasAttribute("disabled") || field.hasAttribute("readonly"))
      base.disabled = true;

    if (tag === "select") {
      base.type = "select";
      base.values = Array.from(field.querySelectorAll("option"))
        .filter((o) => o.value !== "")
        .map((o) => ({
          value: o.value,
          label: (o.textContent || o.value).trim(),
        }));
    } else if (tag === "textarea") {
      base.type = "textarea";
    } else if (input_type === "checkbox" || var_type === "Boolean") {
      base.type = "checkbox";
    } else if (
      field.hasAttribute("uib-datepicker-popup") ||
      var_type === "Date" ||
      input_type === "date"
    ) {
      base.type = "datetime";
      base.subtype = "date";
    } else if (input_type === "number" || NUMBER_TYPES.includes(var_type)) {
      base.type = "number";
    } else {
      base.type = "textfield";
    }

    components.push(base);
  }

  return { type: "default", components };
};

/**
 * Extract the values (and engine types) the engine embedded in a rendered form
 * — input `value` / `checked`, the selected `<option>` — in the same
 * `{ name: { value, type } }` shape as `/form-variables`. Used to pre-fill a
 * generated start form (which has no instance, so no form-variables endpoint)
 * from the form field defaults the engine already rendered.
 */
export const rendered_form_variables = (html) => {
  const doc = new DOMParser().parseFromString(html ?? "", "text/html");
  const out = {};
  for (const field of doc.querySelectorAll(
    "input[cam-variable-name], select[cam-variable-name], textarea[cam-variable-name]",
  )) {
    const key = field.getAttribute("cam-variable-name");
    if (!key) continue;
    const type = field.getAttribute("cam-variable-type") || "String";
    const input_type = (field.getAttribute("type") ?? "").toLowerCase();

    let value;
    if (input_type === "checkbox") {
      value = field.hasAttribute("checked");
    } else if (field.tagName.toLowerCase() === "select") {
      value = field.querySelector("option[selected]")?.value;
    } else {
      const v = field.getAttribute("value");
      value = v !== null && v !== "" ? v : undefined;
    }
    out[key] = { value, type };
  }
  return out;
};
