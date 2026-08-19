import { useTranslation } from "react-i18next";

/**
 * Slim list-page toolbar: a saved-filter dropdown + an Edit button that
 * opens the manage page, then a sort selector aligned to the right. CRUD
 * for saved filters lives in <ManageFilters>, not here.
 *
 * @param sort_options       {Array<{ key, nameKey }>}
 * @param saved_filters_signal {Signal<{ status, data: SavedFilter[] } | null>}
 * @param current            {{ saved_filter_id, sortBy, sortOrder, criteria }}
 * @param defaults           {{ sortBy, sortOrder }}
 * @param include_my_filter  {boolean} — adds the "My" preset (Tasks only)
 * @param on_change          {(patch) => void}
 * @param on_manage          {() => void}    — Edit button click
 * @param filter_predicate   {(SavedFilter) => boolean} optional gate for which
 *                           saved filters to display
 */
export const ListFilter = ({
  sort_options,
  saved_filters_signal,
  current,
  defaults = { sortBy: "", sortOrder: "asc" },
  include_my_filter = false,
  on_change,
  on_manage,
  filter_predicate = (f) => f && f.id,
}) => {
  const [t] = useTranslation();
  const saved_filters = (saved_filters_signal?.value?.data ?? []).filter(
    filter_predicate,
  );

  return (
    <div id="list-filter">
      <div class="list-filter-group">
        <label for="list-filter-saved">{t("list_filter.saved_filter")}</label>
        <select
          id="list-filter-saved"
          value={current.saved_filter_id ?? "all"}
          onChange={(e) =>
            on_change?.({ saved_filter_id: e.currentTarget.value })
          }
        >
          <option value="all">{t("list_filter.all")}</option>
          {include_my_filter && (
            <option value="my">{t("list_filter.my")}</option>
          )}
          {saved_filters.length > 0 && <option disabled>──────────</option>}
          {saved_filters.map((f) => (
            <option key={f.id} value={f.id}>
              {f.name}
            </option>
          ))}
        </select>
        <button type="button" onClick={on_manage}>
          {t("list_filter.edit")}
        </button>
      </div>
      <div class="list-filter-group">
        <label for="list-filter-sort-by">{t("list_filter.sort_by")}</label>
        <select
          id="list-filter-sort-by"
          value={current.sortBy ?? defaults.sortBy}
          onChange={(e) => on_change?.({ sortBy: e.currentTarget.value })}
        >
          {sort_options.map((o) => (
            <option key={o.key} value={o.key}>
              {t(o.nameKey)}
            </option>
          ))}
        </select>
        <select
          aria-label={t("list_filter.sort_order")}
          value={current.sortOrder ?? defaults.sortOrder}
          onChange={(e) => on_change?.({ sortOrder: e.currentTarget.value })}
        >
          <option value="asc">{t("list_filter.asc")}</option>
          <option value="desc">{t("list_filter.desc")}</option>
        </select>
      </div>
    </div>
  );
};
