import { useRef, useId } from "preact/hooks";
import { useSignalEffect } from "@preact/signals";
import { useTranslation } from "react-i18next";
import * as Icons from "../assets/icons.jsx";

/**
 * A modal dialog controlled by a boolean `open` signal. Opening/closing the
 * native <dialog> stays in sync with the signal, and closing it (Esc, backdrop,
 * close button) resets the signal to false.
 *
 * Provide an accessible name via `title` (shown in the header) or, for
 * header-less dialogs, via `aria_label`.
 *
 * @param open {import("@preact/signals").Signal<boolean>}
 */
const Dialog = ({ open, title, hide_header = false, aria_label, children }) => {
  const [t] = useTranslation(),
    ref = useRef(null),
    heading_id = useId();

  useSignalEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open.value && !dialog.open) dialog.showModal();
    else if (!open.value && dialog.open) dialog.close();
  });

  return (
    <dialog
      ref={ref}
      onClose={() => (open.value = false)}
      aria-label={aria_label}
      aria-labelledby={title && !hide_header ? heading_id : undefined}
    >
      {!hide_header && (
        <header>
          {title ? <h2 id={heading_id}>{title}</h2> : <span />}
          <button
            type="button"
            onClick={() => (open.value = false)}
            aria-label={t("common.close")}
          >
            <Icons.close />
            {t("common.close")}
          </button>
        </header>
      )}
      {children}
    </dialog>
  );
};

/**
 * A confirmation dialog with a danger action and a cancel button.
 *
 * @param open {import("@preact/signals").Signal<boolean>}
 * @param on_confirm called when the user confirms; the dialog then closes
 */
const ConfirmDialog = ({
  open,
  message,
  on_confirm,
  confirm_label,
  cancel_label,
}) => {
  const [t] = useTranslation();

  // The dismiss button and Esc already close the dialog, so the header's close
  // button would be a redundant third dismiss control — hide it here.
  return (
    <Dialog open={open} hide_header aria_label={t("common.confirmation")}>
      <p>{message}</p>
      <div class="button-group">
        <button
          type="button"
          class="danger"
          onClick={() => {
            on_confirm();
            open.value = false;
          }}
        >
          {confirm_label ?? t("common.delete")}
        </button>
        <button type="button" onClick={() => (open.value = false)}>
          {cancel_label ?? t("common.cancel")}
        </button>
      </div>
    </Dialog>
  );
};

export { Dialog, ConfirmDialog };
