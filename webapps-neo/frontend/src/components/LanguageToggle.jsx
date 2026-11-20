/**
 * LanguageToggle.jsx
 *
 * The header's language switch: a button carrying the current language tag, and
 * a menu listing the offered ones. Switching is in place — `changeLanguage()`
 * re-renders every `useTranslation()` consumer, and LanguageDetector caches the
 * choice in `localStorage`, so the language survives the next visit without a
 * reload or a re-login.
 *
 * The menu is a `<dialog>` rather than a popover so it gets the focus trap,
 * Escape handling and backdrop the mobile menu already relies on.
 */
import { useTranslation } from "react-i18next";
import * as Icons from "../assets/icons.jsx";
import { LANGUAGES, language_for } from "../helper/languages.js";

// jsdom has no HTMLDialogElement.showModal; a plain `open` attribute gets the
// menu on screen there and in any browser old enough to miss showModal.
const open_dialog = (dialog) => {
  if (!dialog) return;
  if (typeof dialog.showModal === "function") dialog.showModal();
  else dialog.setAttribute("open", "");
};

const close_dialog = (dialog) => {
  if (!dialog) return;
  if (typeof dialog.close === "function") dialog.close();
  else dialog.removeAttribute("open");
};

export const LanguageToggle = ({ id = "language-toggle", menu_id = "language-menu" }) => {
  const [t, i18n] = useTranslation(),
    current = language_for(i18n.resolvedLanguage ?? i18n.language),
    menu = () => document.getElementById(menu_id),
    // `nav.language` reads "Language: English" here — the tag alone would be
    // announced as two letters, meaningless out of context.
    title = `${t("nav.language")}: ${current.label}`;

  const choose = (code) => {
    close_dialog(menu());
    // Closing first keeps the page behind the menu in place while it relabels.
    i18n.changeLanguage(code);
  };

  return (
    <>
      {/* The menu precedes the trigger so the trigger stays the element right
          before the theme switch in the header's top-right group. A closed
          <dialog> is display:none but is still a sibling element, so rendering
          it between the two buttons would separate them in the DOM. */}
      <dialog id={menu_id} class="language-menu" aria-label={t("nav.language")}>
        <header>
          <h2>{t("nav.language")}</h2>
          <button
            type="button"
            onClick={() => close_dialog(menu())}
            aria-label={t("nav.close-menu")}
          >
            <Icons.close />
          </button>
        </header>
        <menu>
          {LANGUAGES.map((lng) => (
            <li key={lng.code}>
              <button
                type="button"
                lang={lng.code}
                aria-current={lng.code === current.code ? "true" : undefined}
                onClick={() => choose(lng.code)}
              >
                <span lang={lng.code}>{lng.label}</span>
                <span class="language-check" aria-hidden="true">
                  ✓
                </span>
              </button>
            </li>
          ))}
        </menu>
      </dialog>

      <button
        type="button"
        id={id}
        class="language-toggle"
        title={title}
        aria-label={title}
        aria-haspopup="dialog"
        onClick={() => open_dialog(menu())}
      >
        <span aria-hidden="true">{current.short}</span>
      </button>
    </>
  );
};

/**
 * The same choice as a plain `<select>`, for the mobile menu: it stacks inside
 * the dialog with the server selector, where a second modal would be awkward.
 */
export const LanguageSelect = ({ id = "mobile-language-select" }) => {
  const [t, i18n] = useTranslation(),
    current = language_for(i18n.resolvedLanguage ?? i18n.language);

  return (
    <label id={id}>
      <span>{t("nav.language")}</span>
      <select
        aria-label={t("nav.language")}
        value={current.code}
        onChange={(e) => i18n.changeLanguage(e.currentTarget.value)}
      >
        {LANGUAGES.map((lng) => (
          <option key={lng.code} value={lng.code} lang={lng.code}>
            {lng.label}
          </option>
        ))}
      </select>
    </label>
  );
};
