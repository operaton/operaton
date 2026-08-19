import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";
import { Dialog, ConfirmDialog } from "./Dialog.jsx";
import {
  FilterEditForm,
  empty_filter_form,
  filter_form_from_saved,
  filter_from_form,
} from "./FilterEditForm.jsx";

/**
 * The "manage filters" page rendered in place of the list table when the
 * URL carries ?filters=manage. Surfaces every saved filter for the
 * resource with row-level actions (edit, duplicate, share-link, delete)
 * and a primary "New filter" button. The create/edit dialog lives inside
 * this component and reuses <FilterEditForm>.
 *
 * @param title                 page heading (already translated)
 * @param saved_filters_signal  {Signal<{ status, data: SavedFilter[] }>}
 * @param filter_keys           {Array<{ key, nameKey, type? }>}
 * @param sort_options          {Array<{ key, nameKey }>}
 * @param on_save               {(filter) => void}
 * @param on_update             {(id, filter) => void}
 * @param on_delete             {(id) => void}
 * @param on_close              {() => void}        — Back to list
 * @param build_share_link      {(filter) => string} — per-row share URL
 * @param advanced_editor_href  {string} optional — Tasks links to /tasks/filter
 */
export const ManageFilters = ({
  title,
  saved_filters_signal,
  filter_keys,
  sort_options,
  on_save,
  on_update,
  on_delete,
  on_close,
  build_share_link,
  advanced_editor_href,
}) => {
  const [t] = useTranslation();
  const edit_open = useSignal(false);
  const delete_open = useSignal(false);
  const edit_form = useSignal(empty_filter_form());
  const pending_delete = useSignal(null);
  const copied = useSignal(null);

  const saved_filters = (saved_filters_signal?.value?.data ?? []).filter(
    (f) => f && f.id,
  );

  const open_create = () => {
    edit_form.value = empty_filter_form();
    edit_open.value = true;
  };
  const open_edit = (filter) => {
    edit_form.value = filter_form_from_saved(filter);
    edit_open.value = true;
  };
  const open_duplicate = (filter) => {
    edit_form.value = {
      ...filter_form_from_saved(filter),
      id: null,
      name: `${filter.name} ${t("list_filter.duplicate_suffix")}`.trim(),
    };
    edit_open.value = true;
  };
  const ask_delete = (filter) => {
    pending_delete.value = filter;
    delete_open.value = true;
  };
  const confirm_delete = () => {
    if (pending_delete.value) on_delete?.(pending_delete.value.id);
    pending_delete.value = null;
  };
  const share = (filter) => {
    const link = build_share_link(filter);
    if (navigator.clipboard?.writeText) {
      void navigator.clipboard.writeText(link);
      copied.value = filter.id;
      setTimeout(() => {
        if (copied.peek() === filter.id) copied.value = null;
      }, 2000);
    }
  };

  const submit_form = (event) => {
    event.preventDefault();
    const built = filter_from_form(edit_form.value, filter_keys);
    if (edit_form.value.id) on_update?.(edit_form.value.id, built);
    else on_save?.(built);
    edit_open.value = false;
  };

  return (
    <div id="manage-filters" class="fade-in">
      <header>
        <h1>{title}</h1>
        <button type="button" onClick={on_close}>
          {t("list_filter.back")}
        </button>
      </header>

      <div class="manage-filters-actions">
        <button type="button" class="primary" onClick={open_create}>
          {t("list_filter.new_filter")}
        </button>
        {advanced_editor_href && (
          <a href={advanced_editor_href} class="button secondary">
            {t("list_filter.advanced")}
          </a>
        )}
      </div>

      {saved_filters.length === 0 ? (
        <p class="info-box">{t("list_filter.empty")}</p>
      ) : (
        <table>
          <caption class="screen-hidden">{title}</caption>
          <thead>
            <tr>
              <th scope="col">{t("common.name")}</th>
              <th scope="col">{t("list_filter.criteria")}</th>
              <th scope="col">{t("list_filter.sort_override")}</th>
              <th scope="col">{t("common.action")}</th>
            </tr>
          </thead>
          <tbody>
            {saved_filters.map((filter) => (
              <tr key={filter.id}>
                <th scope="row">{filter.name}</th>
                <td>
                  <CriteriaSummary query={filter.query} />
                </td>
                <td>
                  {filter.sort?.sortBy
                    ? `${filter.sort.sortBy} ${filter.sort.sortOrder ?? "asc"}`
                    : "—"}
                </td>
                <td class="actions">
                  <button type="button" onClick={() => open_edit(filter)}>
                    {t("common.edit")}
                  </button>
                  <button type="button" onClick={() => open_duplicate(filter)}>
                    {t("list_filter.duplicate")}
                  </button>
                  <button type="button" onClick={() => share(filter)}>
                    <span aria-live="polite">
                      {copied.value === filter.id
                        ? t("list_filter.share_link_copied")
                        : t("list_filter.share_link")}
                    </span>
                  </button>
                  <button
                    type="button"
                    class="danger"
                    onClick={() => ask_delete(filter)}
                  >
                    {t("common.delete")}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Dialog
        open={edit_open}
        title={
          edit_form.value.id
            ? t("list_filter.edit_filter_title")
            : t("list_filter.new_filter_title")
        }
      >
        <FilterEditForm
          form={edit_form}
          filter_keys={filter_keys}
          sort_options={sort_options}
          on_submit={submit_form}
          on_cancel={() => (edit_open.value = false)}
        />
      </Dialog>

      <ConfirmDialog
        open={delete_open}
        message={t("list_filter.delete_confirm", {
          name: pending_delete.value?.name ?? "",
        })}
        confirm_label="list_filter.confirm_delete"
        on_confirm={confirm_delete}
      />
    </div>
  );
};

const CriteriaSummary = ({ query }) => {
  const entries = Object.entries(query ?? {});
  if (entries.length === 0) return <em>—</em>;
  return entries.map(([k, v]) => `${k}=${v}`).join(", ");
};
