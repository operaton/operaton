import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";
import { Dialog } from "./Dialog.jsx";
import { fromLocalParts, toLocalParts } from "../helper/date_formatter.js";

/**
 * The options the engine offers when a suspension state is changed: whether
 * what belongs to the definition goes with it, and whether the change happens
 * now or at a set time. Without them the only possible answer is "now, and
 * take everything along" — which is what the previous Cockpit asked about.
 *
 * @param open      {import("@preact/signals").Signal<boolean>}
 * @param suspend   {boolean} true = suspend, false = activate; wording only
 * @param scope     "definition" | "jobs" — what "include" refers to
 * @param on_confirm ({ include, execution_date }) => void
 */
export const SuspensionDialog = ({ open, suspend, scope, on_confirm }) => {
  const [t] = useTranslation(),
    include = useSignal(true),
    delayed = useSignal(false),
    parts = useSignal(toLocalParts(new Date()));

  const confirm = () => {
    on_confirm({
      include: include.value,
      execution_date: delayed.value
        ? fromLocalParts(parts.value.date, parts.value.time)
        : undefined,
    });
    open.value = false;
  };

  return (
    <Dialog
      open={open}
      title={t(
        suspend
          ? "processes.suspension.suspend"
          : "processes.suspension.activate",
      )}
    >
      <p>
        {t(
          scope === "jobs"
            ? "processes.suspension.jobs-hint"
            : "processes.suspension.instances-hint",
        )}
      </p>

      <label class="checkbox">
        <input
          type="checkbox"
          checked={include.value}
          onChange={(e) => (include.value = e.currentTarget.checked)}
        />
        {t(
          scope === "jobs"
            ? "processes.suspension.include-jobs"
            : "processes.suspension.include-instances",
        )}
      </label>

      <fieldset>
        <legend>{t("processes.suspension.when")}</legend>
        <label class="checkbox">
          <input
            type="radio"
            name="suspension-when"
            checked={!delayed.value}
            onChange={() => (delayed.value = false)}
          />
          {t("processes.suspension.immediately")}
        </label>
        <label class="checkbox">
          <input
            type="radio"
            name="suspension-when"
            checked={delayed.value}
            onChange={() => (delayed.value = true)}
          />
          {t("processes.suspension.delayed")}
        </label>
        {delayed.value && (
          <div class="date-time">
            <input
              type="date"
              aria-label={t("common.date")}
              value={parts.value.date}
              onInput={(e) =>
                (parts.value = { ...parts.peek(), date: e.currentTarget.value })
              }
            />
            <input
              type="time"
              aria-label={t("common.time")}
              value={parts.value.time}
              onInput={(e) =>
                (parts.value = { ...parts.peek(), time: e.currentTarget.value })
              }
            />
          </div>
        )}
      </fieldset>

      <div class="button-group">
        <button type="button" onClick={confirm}>
          {t("common.ok")}
        </button>
        <button type="button" onClick={() => (open.value = false)}>
          {t("common.cancel")}
        </button>
      </div>
    </Dialog>
  );
};
