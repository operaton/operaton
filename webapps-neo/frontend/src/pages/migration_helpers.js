/**
 * Pure / signal-level helpers for the migration page.
 *
 * Kept free of rendering and heavy BPMN dependencies so they can be unit-tested
 * in isolation (see Migrations.test.js) and reused from Migrations.jsx.
 */

export const VARIABLE_TYPES = [
  "String",
  "Integer",
  "Long",
  "Double",
  "Boolean",
  "Date",
];

export const QUERY_FIELDS_KEYS = [
  {
    name: "processInstanceIds",
    labelKey: "migrations.query-fields.id",
    type: "array",
  },
  {
    name: "businessKey",
    labelKey: "migrations.query-fields.business-key",
    type: "text",
  },
  {
    name: "superProcessInstance",
    labelKey: "migrations.query-fields.parent-id",
    type: "text",
  },
  {
    name: "subProcessInstance",
    labelKey: "migrations.query-fields.sub-id",
    type: "text",
  },
  {
    name: "active",
    labelKey: "migrations.query-fields.active",
    type: "boolean",
  },
  {
    name: "suspended",
    labelKey: "migrations.query-fields.suspended",
    type: "boolean",
  },
  {
    name: "rootProcessInstances",
    labelKey: "migrations.query-fields.root-instances-only",
    type: "boolean",
  },
  {
    name: "leafProcessInstances",
    labelKey: "migrations.query-fields.leaf-instances-only",
    type: "boolean",
  },
  {
    name: "withIncident",
    labelKey: "migrations.query-fields.with-incidents-only",
    type: "boolean",
  },
  {
    name: "incidentId",
    labelKey: "migrations.query-fields.incident-id",
    type: "text",
  },
  {
    name: "incidentType",
    labelKey: "migrations.query-fields.incident-type",
    type: "text",
  },
  {
    name: "incidentMessage",
    labelKey: "migrations.query-fields.incident-message",
    type: "text",
  },
  {
    name: "tenantIdIn",
    labelKey: "migrations.query-fields.tenant-id",
    type: "array",
  },
  {
    name: "activityIdIn",
    labelKey: "migrations.query-fields.activity-id",
    type: "array",
  },
  {
    name: "withoutTenantId",
    labelKey: "migrations.query-fields.without-tenant-id",
    type: "boolean",
  },
];

/**
 * Build a migration plan from the generated plan and the current activity mappings.
 */
export const build_migration_plan = (generate_data, mappings) => ({
  ...generate_data,
  instructions: Object.keys(mappings).map((key) => ({
    sourceActivityIds: [key],
    targetActivityIds: [mappings[key]],
    updateEventTrigger: false,
  })),
});

/**
 * Build a migration plan including a variables map (skips entries with blank names).
 */
export const build_migration_plan_with_variables = (
  generate_data,
  mappings,
  variables,
) => {
  const variables_map = {};
  for (const v of variables) {
    if (v.name.trim() !== "")
      variables_map[v.name] = { type: v.type, value: v.value };
  }
  return {
    ...build_migration_plan(generate_data, mappings),
    variables: variables_map,
  };
};

/**
 * Return a new mappings object with the source activity mapped to target_value,
 * or with the mapping removed when target_value is empty. Pure — never mutates.
 */
export const update_mapping = (target_value, source_activity_id, mappings) => {
  if (target_value !== "") {
    return { ...mappings, [source_activity_id]: target_value };
  }
  const { [source_activity_id]: _, ...rest } = mappings;
  return rest;
};

export const add_variable = (migration_state) =>
  (migration_state.variables.value = [
    ...migration_state.variables.value,
    { name: "", type: "String", value: "" },
  ]);

export const remove_variable = (migration_state, index) =>
  (migration_state.variables.value = migration_state.variables.value.filter(
    (_, i) => i !== index,
  ));

export const update_variable = (migration_state, index, field, value) => {
  const updated = [...migration_state.variables.value];
  updated[index] = { ...updated[index], [field]: value };
  migration_state.variables.value = updated;
};

export const add_query_row = (migration_state) =>
  (migration_state.process_instance_query.value = [
    ...migration_state.process_instance_query.value,
    { field: QUERY_FIELDS_KEYS[0].name, value: "" },
  ]);

export const remove_query_row = (migration_state, index) =>
  (migration_state.process_instance_query.value =
    migration_state.process_instance_query.value.filter((_, i) => i !== index));

export const update_query_row = (migration_state, index, key, value) => {
  const updated = [...migration_state.process_instance_query.value];
  updated[index] = { ...updated[index], [key]: value };
  if (key === "field") {
    const field_def = QUERY_FIELDS_KEYS.find((f) => f.name === value);
    updated[index].value = field_def?.type === "boolean" ? "true" : "";
  }
  migration_state.process_instance_query.value = updated;
};

/**
 * Turn the query-builder rows into a process instance query object, coercing
 * booleans and splitting comma-separated array fields. Blank values are dropped.
 */
export const build_query = (rows) => {
  const query = {};
  for (const { field, value } of rows) {
    if (value === "") continue;
    const field_def = QUERY_FIELDS_KEYS.find((f) => f.name === field);
    if (!field_def) continue;
    if (field_def.type === "boolean") query[field] = value === "true";
    else if (field_def.type === "array")
      query[field] = value
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean);
    else query[field] = value;
  }
  return query;
};

export const add_query_params_abstract = (
  query,
  url,
  route,
  path,
  name,
  value,
) => {
  if (Object.keys(query).length === 0) {
    route(`${url}?${name}=${value}`);
  } else if (query[name] !== null) {
    query[name] = value;
    const params_as_string = Object.entries(query)
      .map(([k, v]) => `&${k}=${v}`)
      .join("");
    route(`${path}?${params_as_string}`);
  } else {
    route(`${url}&${name}=${value}`);
  }
};
