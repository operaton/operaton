import { useTranslation } from "react-i18next";
import { VARIABLE_TYPES, variable_input_type } from "../helper/variables.js";

/** Whether any name is used more than once, ignoring the unnamed rows. */
export const repeated_names = (rows) => {
  const named = rows.map((r) => r.name.trim()).filter(Boolean);
  return named.length !== new Set(named).size;
};

/**
 * Variables entered by hand: a name, a type and a value per row.
 *
 * Used where there is no form to fill in — a task that belongs to no process,
 * and a process definition without a start form. Both need the same thing, and
 * a variable typed in one place should behave as it does in the other.
 */
export const VariableRows = ({ rows, on_change, disabled = false }) => {
  const [t] = useTranslation();

  const update = (index, field, value) =>
      on_change(
        rows.map((row, i) => (i === index ? { ...row, [field]: value } : row)),
      ),
    add = () => on_change([...rows, { name: "", type: "String", value: "" }]),
    remove = (index) => on_change(rows.filter((_, i) => i !== index));

  return (
    <>
      {rows.length > 0 && (
        <table>
          <thead>
            <tr>
              <th scope="col">{t("common.name")}</th>
              <th scope="col">{t("common.type")}</th>
              <th scope="col">{t("common.value")}</th>
              <th scope="col">{t("common.action")}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i}>
                <td>
                  <input
                    aria-label={t("common.name")}
                    value={row.name}
                    disabled={disabled}
                    onInput={(e) => update(i, "name", e.currentTarget.value)}
                  />
                </td>
                <td>
                  <select
                    aria-label={t("common.type")}
                    value={row.type}
                    disabled={disabled}
                    onChange={(e) => update(i, "type", e.currentTarget.value)}
                  >
                    {VARIABLE_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  {variable_input_type(row.type) === "none" ? null : (
                    <input
                      type={variable_input_type(row.type)}
                      aria-label={t("common.value")}
                      value={row.value}
                      disabled={disabled}
                      onInput={(e) => update(i, "value", e.currentTarget.value)}
                    />
                  )}
                </td>
                <td>
                  <button
                    type="button"
                    class="danger"
                    disabled={disabled}
                    onClick={() => remove(i)}
                  >
                    {t("common.remove")}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div class="button-group">
        <button type="button" disabled={disabled} onClick={add}>
          {t("tasks.form.add-variable")}
        </button>
      </div>

      {repeated_names(rows) && (
        <p class="error" role="alert">
          {t("tasks.form.duplicate-variable")}
        </p>
      )}
    </>
  );
};
