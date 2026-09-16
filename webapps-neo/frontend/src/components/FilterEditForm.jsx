import { useEffect } from "preact/hooks";
import { useTranslation } from "react-i18next";

/**
 * The create/edit dialog body for a saved filter. Used by <ManageFilters>;
 * lives in its own file so the toolbar (<ListFilter>) can stay slim and
 * doesn't pull in the form code on every list page render.
 *
 * @param form         {Signal<{ id, name, sortBy, sortOrder, criteria }>}
 * @param filter_keys  {Array<{ key, nameKey, type? }>}
 * @param sort_options {Array<{ key, nameKey }>}
 * @param on_submit    {(SubmitEvent) => void}
 * @param on_cancel    {() => void}
 */
export const FilterEditForm = ({
  form,
  filter_keys,
  sort_options,
  on_submit,
  on_cancel,
}) => {
  const [t] = useTranslation();

  // If the resource's allowed criteria change while a form is open (e.g. the
  // user navigates between resources without unmounting), strip rows whose
  // key is no longer legal.
  useEffect(() => {
    const allowed = new Set(filter_keys.map((k) => k.key));
    const cleaned = form.value.criteria.filter((c) => allowed.has(c.key));
    if (cleaned.length !== form.value.criteria.length)
      form.value = { ...form.peek(), criteria: cleaned };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter_keys]);

  // A key may appear once. Variable criteria are the exception: several
  // comparisons on the same key are how a range is expressed.
  const repeated_keys = (() => {
    const seen = new Set(),
      twice = new Set();
    for (const { key } of form.value.criteria) {
      const meta = filter_keys.find((k) => k.key === key);
      if (!key || meta?.type === "variable") continue;
      if (seen.has(key)) twice.add(key);
      seen.add(key);
    }
    return twice;
  })();

  const update = (key, value) =>
    (form.value = { ...form.peek(), [key]: value });
  const add_column = () =>
      update("columns", [
        ...(form.peek().columns ?? []),
        { name: "", label: "" },
      ]),
    remove_column = (index) =>
      update(
        "columns",
        (form.peek().columns ?? []).filter((_, i) => i !== index),
      ),
    update_column = (index, field, value) =>
      update(
        "columns",
        (form.peek().columns ?? []).map((c, i) =>
          i === index ? { ...c, [field]: value } : c,
        ),
      );

  const add_criterion = () =>
    update("criteria", [
      ...form.peek().criteria,
      { key: filter_keys[0]?.key ?? "", value: "" },
    ]);
  const remove_criterion = (index) =>
    update(
      "criteria",
      form.peek().criteria.filter((_, i) => i !== index),
    );
  const update_criterion = (index, field, value) =>
    update(
      "criteria",
      form
        .peek()
        .criteria.map((c, i) => (i === index ? { ...c, [field]: value } : c)),
    );

  return (
    <form onSubmit={on_submit} class="filter-edit-form">
      <label for="list-filter-name">{t("list_filter.name")}</label>
      <input
        id="list-filter-name"
        required
        value={form.value.name}
        onInput={(e) => update("name", e.currentTarget.value)}
      />

      <fieldset>
        <legend>{t("list_filter.criteria")}</legend>
        {form.value.criteria.length > 0 && (
          <table>
            <thead>
              <tr>
                <th scope="col">{t("common.key")}</th>
                <th scope="col">{t("common.value")}</th>
                <th scope="col">{t("common.action")}</th>
              </tr>
            </thead>
            <tbody>
              {form.value.criteria.map((criterion, i) => {
                const meta = filter_keys.find((k) => k.key === criterion.key);
                return (
                  <tr key={i}>
                    <td>
                      <select
                        aria-label={t("common.key")}
                        aria-invalid={
                          repeated_keys.has(criterion.key) || undefined
                        }
                        value={criterion.key}
                        onChange={(e) =>
                          update_criterion(i, "key", e.currentTarget.value)
                        }
                      >
                        {filter_keys.map((k) => (
                          <option key={k.key} value={k.key}>
                            {t(k.nameKey)}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      {meta?.type === "variable" ? (
                        <VariableCriterionInput
                          criterion={criterion}
                          operators={meta.operators}
                          on_change={(field, v) =>
                            update_criterion(i, field, v)
                          }
                        />
                      ) : (
                        <CriterionValueInput
                          meta={meta}
                          value={criterion.value}
                          on_change={(v) => update_criterion(i, "value", v)}
                          label={t("common.value")}
                        />
                      )}
                    </td>
                    <td>
                      <button type="button" onClick={() => remove_criterion(i)}>
                        {t("common.remove")}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
        <button type="button" onClick={add_criterion}>
          {t("list_filter.add_criterion")}
        </button>
        {repeated_keys.size > 0 && (
          <p class="error" role="alert">
            {t("list_filter.duplicate_criterion", {
              keys: [...repeated_keys].join(", "),
            })}
          </p>
        )}
      </fieldset>

      <fieldset>
        <legend>{t("list_filter.columns")}</legend>
        <p class="hint">{t("list_filter.columns_hint")}</p>
        {form.value.columns?.length > 0 && (
          <table>
            <thead>
              <tr>
                <th scope="col">{t("list_filter.variable_name")}</th>
                <th scope="col">{t("list_filter.column_label")}</th>
                <th scope="col">{t("common.action")}</th>
              </tr>
            </thead>
            <tbody>
              {form.value.columns.map((column, i) => (
                <tr key={i}>
                  <td>
                    <input
                      aria-label={t("list_filter.variable_name")}
                      value={column.name}
                      onInput={(e) =>
                        update_column(i, "name", e.currentTarget.value)
                      }
                    />
                  </td>
                  <td>
                    <input
                      aria-label={t("list_filter.column_label")}
                      value={column.label}
                      onInput={(e) =>
                        update_column(i, "label", e.currentTarget.value)
                      }
                    />
                  </td>
                  <td>
                    <button type="button" onClick={() => remove_column(i)}>
                      {t("common.remove")}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <button type="button" onClick={add_column}>
          {t("list_filter.add_column")}
        </button>
        <label>
          <input
            type="checkbox"
            checked={form.value.show_undefined === true}
            onInput={(e) => update("show_undefined", e.currentTarget.checked)}
          />
          {t("list_filter.show_undefined")}
        </label>
      </fieldset>

      <fieldset>
        <legend>{t("list_filter.sort_override")}</legend>
        <label for="list-filter-form-sort-by">
          {t("list_filter.sort_override_by")}
        </label>
        <select
          id="list-filter-form-sort-by"
          value={form.value.sortBy}
          onChange={(e) => update("sortBy", e.currentTarget.value)}
        >
          <option value="">{t("list_filter.no_override")}</option>
          {sort_options.map((o) => (
            <option key={o.key} value={o.key}>
              {t(o.nameKey)}
            </option>
          ))}
        </select>
        <select
          aria-label={t("list_filter.sort_override_order")}
          value={form.value.sortOrder}
          onChange={(e) => update("sortOrder", e.currentTarget.value)}
        >
          <option value="asc">{t("list_filter.asc")}</option>
          <option value="desc">{t("list_filter.desc")}</option>
        </select>
      </fieldset>

      <div class="button-group">
        <button type="submit" disabled={repeated_keys.size > 0}>
          {t("common.save")}
        </button>
        <button type="button" onClick={on_cancel}>
          {t("common.cancel")}
        </button>
      </div>
    </form>
  );
};

export const VARIABLE_OPERATORS = [
  "eq",
  "neq",
  "gt",
  "gteq",
  "lt",
  "lteq",
  "like",
  "notLike",
];

const VariableCriterionInput = ({ criterion, operators, on_change }) => {
  const [t] = useTranslation();
  return (
    <span class="variable-criterion">
      <input
        aria-label={t("list_filter.variable_name")}
        placeholder={t("list_filter.variable_name")}
        value={criterion.variable_name ?? ""}
        onInput={(e) => on_change("variable_name", e.currentTarget.value)}
      />
      <select
        aria-label={t("list_filter.variable_operator")}
        value={criterion.operator ?? "eq"}
        onChange={(e) => on_change("operator", e.currentTarget.value)}
      >
        {(operators ?? VARIABLE_OPERATORS).map((operator) => (
          <option key={operator} value={operator}>
            {t(`list_filter.variable_operators.${operator}`)}
          </option>
        ))}
      </select>
      <input
        aria-label={t("common.value")}
        value={criterion.value}
        onInput={(e) => on_change("value", e.currentTarget.value)}
      />
    </span>
  );
};

const CriterionValueInput = ({ meta, value, on_change, label }) => {
  if (!meta)
    return (
      <input
        aria-label={label}
        value={value}
        onInput={(e) => on_change(e.currentTarget.value)}
      />
    );
  if (meta.type === "boolean")
    return (
      <select
        aria-label={label}
        value={value}
        onChange={(e) => on_change(e.currentTarget.value)}
      >
        <option value="">—</option>
        <option value="true">true</option>
        <option value="false">false</option>
      </select>
    );
  if (meta.type === "enum" && Array.isArray(meta.options))
    return (
      <select
        aria-label={label}
        value={value}
        onChange={(e) => on_change(e.currentTarget.value)}
      >
        <option value="">—</option>
        {meta.options.map((o) => (
          <option key={o.value ?? o} value={o.value ?? o}>
            {o.label ?? o.value ?? o}
          </option>
        ))}
      </select>
    );
  if (meta.type === "list")
    return (
      <input
        aria-label={label}
        value={value}
        placeholder={meta.placeholderKey ? undefined : "a, b, c"}
        onInput={(e) => on_change(e.currentTarget.value)}
      />
    );
  if (meta.type === "date")
    return (
      <input
        type="datetime-local"
        aria-label={label}
        value={value}
        onInput={(e) => on_change(e.currentTarget.value)}
      />
    );
  return (
    <input
      aria-label={label}
      value={value}
      onInput={(e) => on_change(e.currentTarget.value)}
    />
  );
};

export const empty_filter_form = () => ({
  id: null,
  name: "",
  sortBy: "",
  sortOrder: "asc",
  criteria: [],
  columns: [],
  show_undefined: false,
});

export const filter_form_from_saved = (filter) => ({
  id: filter.id ?? null,
  name: filter.name ?? "",
  columns: (filter.properties?.variables ?? []).map((v) => ({
    name: v.name ?? "",
    label: v.label ?? "",
  })),
  show_undefined: filter.properties?.showUndefinedVariable === true,
  sortBy: filter.sort?.sortBy ?? "",
  sortOrder: filter.sort?.sortOrder ?? "asc",
  criteria: Object.entries(filter.query ?? {}).flatMap(([key, value]) =>
    is_variable_query(value)
      ? value.map((comparison) => ({
          key,
          variable_name: comparison.name ?? "",
          operator: comparison.operator ?? "eq",
          value: String(comparison.value ?? ""),
        }))
      : [
          {
            key,
            value: Array.isArray(value) ? value.join(", ") : String(value),
          },
        ],
  ),
});

export const filter_from_form = (f, filter_keys) => {
  const query = {};
  for (const criterion of f.criteria) {
    const { key, value } = criterion;
    const meta = filter_keys.find((k) => k.key === key);
    if (!meta || value === "" || value === undefined || value === null)
      continue;
    if (meta.type === "variable") {
      if (!criterion.variable_name) continue;
      query[key] = [
        ...(query[key] ?? []),
        {
          name: criterion.variable_name,
          operator: criterion.operator ?? "eq",
          value: variable_value(value),
        },
      ];
      continue;
    }
    query[key] = coerce_value(meta.type, value);
  }
  return {
    name: f.name.trim(),
    query,
    // Variables the list shows as columns, in the shape the engine stores.
    properties: {
      variables: (f.columns ?? [])
        .filter((c) => c.name.trim() !== "")
        .map((c) => ({
          name: c.name.trim(),
          label: c.label.trim() || c.name.trim(),
        })),
      showUndefinedVariable: f.show_undefined === true,
    },
    ...(f.sortBy ? { sort: { sortBy: f.sortBy, sortOrder: f.sortOrder } } : {}),
  };
};

const is_variable_query = (value) =>
  Array.isArray(value) &&
  value.every((entry) => entry && typeof entry === "object");

// A typed value keeps gt/lt comparable; the engine compares strings lexically.
const variable_value = (value) => {
  if (value === "true" || value === "false") return value === "true";
  return value.trim() !== "" && !Number.isNaN(Number(value))
    ? Number(value)
    : value;
};

const coerce_value = (type, value) => {
  if (type === "boolean") return value === true || value === "true";
  if (type === "number") return Number(value);
  // A list criterion is typed as "a, b, c" and sent as the array the engine
  // expects.
  if (type === "list")
    return String(value)
      .split(",")
      .map((part) => part.trim())
      .filter(Boolean);
  return value;
};
