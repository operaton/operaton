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

  const update = (key, value) =>
    (form.value = { ...form.peek(), [key]: value });
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
      form.peek().criteria.map((c, i) =>
        i === index ? { ...c, [field]: value } : c,
      ),
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
                      <CriterionValueInput
                        meta={meta}
                        value={criterion.value}
                        on_change={(v) => update_criterion(i, "value", v)}
                        label={t("common.value")}
                      />
                    </td>
                    <td>
                      <button
                        type="button"
                        onClick={() => remove_criterion(i)}
                      >
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
        <button type="submit">{t("common.save")}</button>
        <button type="button" onClick={on_cancel}>
          {t("common.cancel")}
        </button>
      </div>
    </form>
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
});

export const filter_form_from_saved = (filter) => ({
  id: filter.id ?? null,
  name: filter.name ?? "",
  sortBy: filter.sort?.sortBy ?? "",
  sortOrder: filter.sort?.sortOrder ?? "asc",
  criteria: Object.entries(filter.query ?? {}).map(([key, value]) => ({
    key,
    value: String(value),
  })),
});

export const filter_from_form = (f, filter_keys) => {
  const query = {};
  for (const { key, value } of f.criteria) {
    const meta = filter_keys.find((k) => k.key === key);
    if (!meta || value === "" || value === undefined || value === null)
      continue;
    query[key] = coerce_value(meta.type, value);
  }
  return {
    name: f.name.trim(),
    query,
    ...(f.sortBy ? { sort: { sortBy: f.sortBy, sortOrder: f.sortOrder } } : {}),
  };
};

const coerce_value = (type, value) => {
  if (type === "boolean") return value === true || value === "true";
  if (type === "number") return Number(value);
  return value;
};
