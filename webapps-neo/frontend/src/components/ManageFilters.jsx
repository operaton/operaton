import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";
import { ConfirmDialog } from "./Dialog.jsx";
import {
  FilterEditForm,
  empty_filter_form,
  filter_form_from_saved,
  filter_from_form,
} from "./FilterEditForm.jsx";

/**
 * The "manage filters" view rendered in place of the list table when the URL
 * carries ?filters=manage. A master-detail page: the saved filters on the
 * left, the editor for the selected one on the right. There is no dialog —
 * selecting a filter, or "New filter", shows its form inline.
 *
 * @param title                 page heading (already translated)
 * @param saved_filters_signal  {Signal<{ status, data: SavedFilter[] }>}
 * @param filter_keys           {Array<{ key, nameKey, type? }>}
 * @param sort_options          {Array<{ key, nameKey }>}
 * @param on_save               {(filter, sharing?) => void}
 * @param on_update             {(id, filter, sharing?) => void}
 * @param on_delete             {(id) => void}
 * @param on_close              {() => void}        — Back to list
 * @param build_share_link      {(filter) => string} — per-row share URL
 * @param sharing               {{ can_share: boolean }} optional — enables the
 *                              sharing section in the editor and passes the
 *                              grants back through on_save/on_update
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
  sharing,
}) => {
  const [t] = useTranslation();
  // `edit_form` holds the form being edited; null means nothing is open on the
  // right. `selected_id` drives the left list's current-item marker.
  const edit_form = useSignal(null);
  const selected_id = useSignal(null);
  const delete_open = useSignal(false);
  const pending_delete = useSignal(null);
  const copied = useSignal(null);

  const saved_filters = (saved_filters_signal?.value?.data ?? []).filter(
    (f) => f && f.id,
  );

  const open_create = () => {
    edit_form.value = empty_filter_form();
    selected_id.value = null;
  };
  const open_edit = (filter) => {
    edit_form.value = filter_form_from_saved(filter);
    selected_id.value = filter.id;
  };
  const open_duplicate = (filter) => {
    edit_form.value = {
      ...filter_form_from_saved(filter),
      id: null,
      name: `${filter.name} ${t("list_filter.duplicate_suffix")}`.trim(),
    };
    selected_id.value = null;
  };
  const close_editor = () => {
    edit_form.value = null;
    selected_id.value = null;
  };
  const ask_delete = (filter) => {
    pending_delete.value = filter;
    delete_open.value = true;
  };
  const confirm_delete = () => {
    const victim = pending_delete.value;
    if (victim) {
      on_delete?.(victim.id);
      if (selected_id.peek() === victim.id) close_editor();
    }
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
    const f = edit_form.value;
    const built = filter_from_form(f, filter_keys);
    const grants = sharing
      ? { readable_by_all: f.readable_by_all === true, grants: f.grants ?? [] }
      : undefined;
    if (f.id) on_update?.(f.id, built, grants);
    else on_save?.(built, grants);
    close_editor();
  };

  return (
    <div id="manage-filters" class="fade-in">
      <header>
        <h1>{title}</h1>
        <button type="button" onClick={on_close}>
          {t("list_filter.back")}
        </button>
      </header>

      <div class="filter-manager">
        <div class="filter-list">
          <button type="button" class="primary" onClick={open_create}>
            {t("list_filter.new_filter")}
          </button>
          {saved_filters.length === 0 ? (
            <p class="info-box">{t("list_filter.empty")}</p>
          ) : (
            <menu>
              {saved_filters.map((filter) => (
                <li
                  key={filter.id}
                  aria-current={
                    selected_id.value === filter.id ? "true" : undefined
                  }
                >
                  <button
                    type="button"
                    class="filter-name"
                    onClick={() => open_edit(filter)}
                  >
                    {filter.name}
                  </button>
                  <span class="row-actions">
                    <button
                      type="button"
                      onClick={() => open_duplicate(filter)}
                      title={t("list_filter.duplicate")}
                    >
                      {t("list_filter.duplicate")}
                    </button>
                    <button
                      type="button"
                      onClick={() => share(filter)}
                      aria-live="polite"
                    >
                      {copied.value === filter.id
                        ? t("list_filter.share_link_copied")
                        : t("list_filter.share_link")}
                    </button>
                    <button
                      type="button"
                      class="danger"
                      onClick={() => ask_delete(filter)}
                    >
                      {t("common.delete")}
                    </button>
                  </span>
                </li>
              ))}
            </menu>
          )}
        </div>

        <div class="filter-detail">
          {edit_form.value ? (
            <FilterEditForm
              form={edit_form}
              filter_keys={filter_keys}
              sort_options={sort_options}
              sharing={sharing}
              on_submit={submit_form}
              on_cancel={close_editor}
            />
          ) : (
            <p class="info-box">{t("list_filter.select_hint")}</p>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={delete_open}
        message={t("list_filter.delete_confirm", {
          name: pending_delete.value?.name ?? "",
        })}
        confirm_label={t("list_filter.confirm_delete")}
        on_confirm={confirm_delete}
      />
    </div>
  );
};
