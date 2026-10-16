import { useTranslation } from "react-i18next";
import { useSignal } from "@preact/signals";

/**
 * Search and paging for the admin's user, group and tenant lists.
 *
 * @param criteria  {Array<{ key, nameKey }>} the query parameters to offer
 * @param page_size {number}
 * @param on_search {(query: object) => void} run a fresh query
 * @param on_more   {(query: object, first_result: number) => void} append the next page
 * @param loaded    {number} how many rows are on screen
 */
export const IdentitySearch = ({
  criteria,
  page_size = 50,
  on_search,
  on_more,
  loaded,
}) => {
  const [t] = useTranslation(),
    query = useSignal({});

  const set = (key, value) => (query.value = { ...query.peek(), [key]: value }),
    // Empty fields must not reach the engine: an empty nameLike matches nothing.
    filled = () =>
      Object.fromEntries(
        Object.entries(query.value).filter(([, value]) => value !== ""),
      ),
    submit = (e) => {
      e.preventDefault();
      on_search(filled());
    };

  return (
    <>
      <form class="identity-search" onSubmit={submit}>
        {criteria.map(({ key, nameKey }) => (
          <label key={key}>
            {t(nameKey)}
            <input
              type="search"
              value={query.value[key] ?? ""}
              onInput={(e) => set(key, e.currentTarget.value)}
            />
          </label>
        ))}
        <button type="submit">{t("common.search")}</button>
      </form>

      {loaded >= page_size && loaded % page_size === 0 ? (
        <div class="button-group">
          <button type="button" onClick={() => on_more(filled(), loaded)}>
            {t("common.load-more")}
          </button>
        </div>
      ) : null}
    </>
  );
};
