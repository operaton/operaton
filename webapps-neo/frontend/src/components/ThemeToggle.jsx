/**
 * ThemeToggle.jsx
 *
 * One button for the three-state preference (`auto` → `light` → `dark` →
 * `auto`). Used by the header, the mobile menu and the login page, so all three
 * stay in step: they read the same `theme_pref` signal and call the same
 * `cycle_theme()`.
 *
 * Reading `.value` inside render subscribes this component, so the icon and the
 * accessible label follow the preference without extra state.
 */
import { useTranslation } from "react-i18next";
import { cycle_theme, theme_pref } from "../theme.js";

export const theme_icon = (pref) =>
  pref === "light" ? "☀" : pref === "dark" ? "☾" : "◐";

export const ThemeToggle = ({ id, class: class_name, show_label = false }) => {
  const [t] = useTranslation();

  const pref = theme_pref.value,
    label = t(`theme.${pref}`),
    title = `${t("theme.label")}: ${label}`;

  return (
    <button
      type="button"
      id={id}
      class={class_name}
      title={title}
      aria-label={title}
      onClick={() => cycle_theme()}
    >
      <span aria-hidden="true">{theme_icon(pref)}</span>
      {show_label ? `${t("theme.label")}: ${label}` : null}
    </button>
  );
};
