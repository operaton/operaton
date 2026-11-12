// noinspection HtmlUnknownAnchorTarget,JSValidateTypes

import { useLocation } from "preact-iso";
import * as Icons from "../assets/icons.jsx";
import { useHotkeys } from "react-hotkeys-hook";
import { useContext, useEffect, useState } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { plugins_for } from "../plugins/registry.js";
import { PLUGIN_POINTS } from "../plugins/points.js";
import { app_path, get_config } from "../config.js";
import { app_name } from "../branding.js";
import { Logo } from "./Logo.jsx";
import { ThemeToggle } from "./ThemeToggle.jsx";
import { LanguageSelect } from "./LanguageToggle.jsx";
import { LANGUAGES, language_for } from "../helper/languages.js";
import { THEME_PREFS, apply_theme, theme_pref } from "../theme.js";
import { page_allowed } from "../helper/authorized_apps.js";

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

// The name shown for the backend in use. The pill in the header row has room for
// one short token, so the stored server's own name is used as-is; the menu's
// <select> keeps the fuller "name (C7)" form.
const server_label = (state) => state.server.value?.name || "-";

// The person's name, in the order this header is written for: given name first,
// family name second. The login id is only the fallback — it is all we have
// until the profile answers, and for a profile the engine will not let us read.
const user_label = (state) => {
  const profile = state.api.user.profile.value?.data;
  return (
    [profile?.firstName, profile?.lastName].filter(Boolean).join(" ") ||
    state.auth.user.id.value ||
    ""
  );
};

// Server / theme / language / account / help in one place, so the header row
// keeps only the primary nav plus this single trigger. A <dialog> gives the
// menu the focus trap, Escape handling and backdrop the mobile menu already
// relies on.
const OptionsMenu = ({ id = "options-menu" }) => {
  const [t, i18n] = useTranslation(),
    { route } = useLocation(),
    state = useContext(AppState),
    // Which settings row has its choice list open. One at a time, like a set of
    // radio cards — opening one closes the other. `null` is the resting state
    // where only the three rows show.
    open_row = useState(null),
    show_row = open_row[0],
    set_row = open_row[1],
    menu = () => document.getElementById(id),
    pref = theme_pref.value,
    lang = language_for(i18n.resolvedLanguage ?? i18n.language),
    close = () => close_dialog(menu()),
    sign_out = async () => {
      close();
      await engine_rest.auth.logout(state);
      if (!state.auth.user.id.peek()) route(app_path("/"));
    },
    // The tick marking the choice in use.
    Check = ({ on }) => (
      <span class="options-check" aria-hidden="true">
        {on ? "✓" : ""}
      </span>
    ),
    // A settings row: its name on the left, the value in use and a caret on the
    // right. The choice list follows in the panel, under the row it belongs to.
    // The row head is the button that toggles the list.
    Row = ({ name, for_id, children }) => (
      <li class="options-row">
        <span class="options-row-head">
          <span class="options-label" id={for_id + "-label"}>
            {name}
          </span>
          {children}
        </span>
      </li>
    ),
    // The head of a click-to-expand row. `on` is whether this row's list is
    // open; clicking it toggles, so the caret points the way the list will go.
    Head = ({ label, on, on_toggle, id }) => (
      <button
        type="button"
        id={id}
        class="options-row-toggle"
        aria-expanded={on ? "true" : "false"}
        onClick={on_toggle}
      >
        <span class="options-value">{label}</span>
        <span class="options-caret" aria-hidden="true">
          <Icons.chevron_down />
        </span>
      </button>
    ),
    // A hairline between the settings rows. The panel is three settings blocks
    // followed by the links; without a rule between them the three read as one
    // list and the caret looks like it belongs to the whole panel.
    Rule = () => <li class="options-rule" role="separator" />,
    // One choice in an expanded list, ticking the one in use.
    Choice = ({ on, lang, on_pick, children }) => (
      <li>
        <button
          type="button"
          class="options-item"
          lang={lang}
          aria-current={on ? "true" : undefined}
          onClick={on_pick}
        >
          {children}
          <Check on={on} />
        </button>
      </li>
    );

  return (
    <>
      <button
        type="button"
        id="options-toggle"
        aria-haspopup="dialog"
        aria-label={server_label(state) + " - " + (user_label(state) || t("nav.account"))}
        title={t("nav.options")}
        onClick={(event) => {
          // The panel is as wide as the pill it hangs from, so the width is
          // taken from the pill and not from the panel's own content: the
          // settings rows are short while the server and user names in the
          // pill are not, and the two must not disagree. The right edge is
          // aligned to the pill's too, so the panel cannot overhang it.
          const panel = menu(),
            pill = event.currentTarget.getBoundingClientRect();
          if (panel) {
            panel.style.width = pill.width + "px";
            panel.style.insetInlineEnd =
              Math.max(0, window.innerWidth - pill.right) + "px";
          }
          open_dialog(panel);
        }}
      >
        <span class="options-toggle-part" id="options-toggle-server">
          <Icons.database />
          {server_label(state)}
        </span>
        <span class="options-toggle-sep" aria-hidden="true">
          -
        </span>
        <span class="options-toggle-part" id="options-toggle-user">
          <Icons.user />
          {user_label(state) || t("nav.account")}
        </span>
        <span class="options-toggle-sep" aria-hidden="true">
          -
        </span>
        <span class="options-toggle-caret" aria-hidden="true">
          <Icons.chevron_down />
        </span>
      </button>

      <dialog id={id} class="options-menu" aria-label={t("nav.options")}>
        <header>
          <h2>{user_label(state) || t("nav.options")}</h2>
          <button type="button" onClick={close} aria-label={t("nav.close-menu")}>
            <Icons.close />
          </button>
        </header>
        <menu>
          <Row name={t("nav.server")} for_id="options-server">
            <Head
              id="options-server-toggle"
              label={server_label(state)}
              on={show_row === "server"}
              on_toggle={() => set_row(show_row === "server" ? null : "server")}
            />
          </Row>
          {show_row === "server" && (
            <li class="options-submenu">
              <ul class="options-submenu-list" id="options-server-list">
                {get_config().backends.map((server) => (
                  <Choice
                    key={server.url}
                    on={state.server.value?.url === server.url}
                    on_pick={() => pick_server(server, state)}
                  >
                    {server.name} {server.c7_mode ? "(C7)" : ""}
                  </Choice>
                ))}
              </ul>
            </li>
          )}

          <Rule />

          <Row name={t("theme.label")} for_id="options-theme">
            <Head
              id="options-theme-toggle"
              label={t("theme." + pref)}
              on={show_row === "theme"}
              on_toggle={() => set_row(show_row === "theme" ? null : "theme")}
            />
          </Row>
          {show_row === "theme" && (
            <li class="options-submenu">
              <ul class="options-submenu-list" id="options-theme-list">
                {THEME_PREFS.map((value) => (
                  <Choice
                    key={value}
                    on={pref === value}
                    lang={value}
                    on_pick={() => apply_theme(value)}
                  >
                    {t("theme." + value)}
                  </Choice>
                ))}
              </ul>
            </li>
          )}

          <Rule />

          <Row name={t("nav.language")} for_id="options-language">
            <Head
              id="options-language-toggle"
              label={lang.label}
              on={show_row === "language"}
              on_toggle={() => set_row(show_row === "language" ? null : "language")}
            />
          </Row>
          {show_row === "language" && (
            <li class="options-submenu">
              <ul class="options-submenu-list" id="options-language-list">
                {LANGUAGES.map((lng) => (
                  <Choice
                    key={lng.code}
                    on={lang.code === lng.code}
                    lang={lng.code}
                    on_pick={() => i18n.changeLanguage(lng.code)}
                  >
                    <span lang={lng.code}>{lng.label}</span>
                  </Choice>
                ))}
              </ul>
            </li>
          )}

          <Rule />

          <li>
            <a class="options-link" href={app_path("/account")}>
              {t("nav.account")}
            </a>
          </li>
          <li>
            <a class="options-link" href={app_path("/help")}>
              {t("nav.help")}
            </a>
          </li>
          <li>
            <button type="button" id="logout" class="options-link" onClick={sign_out}>
              {t("nav.logout")}
            </button>
          </li>
        </menu>
      </dialog>
    </>
  );
};const swap_server = (e, state) => {
  const server = get_config().backends.find((s) => s.url === e.target.value);
  state.server.value = server;
  localStorage.setItem("server", JSON.stringify(server));
};

// The server row's pick: same effect as the <select> it replaced, so the row
// reads, ticks and applies like the theme and language rows do.
const pick_server = (server, state) => {
  state.server.value = server;
  localStorage.setItem("server", JSON.stringify(server));
};

// The built-in primary-nav entries. Hotkeys live here too (alt+shift+0 → "/"
// is handled separately as the logo link).
const builtin_nav = [
  { href: "/tasks", nameKey: "nav.tasks", hotkey: "alt+shift+1" },
  { href: "/processes", nameKey: "nav.processes", hotkey: "alt+shift+2" },
  { href: "/decisions", nameKey: "nav.decisions", hotkey: "alt+shift+3" },
  { href: "/deployments", nameKey: "nav.deployments", hotkey: "alt+shift+4" },
  { href: "/batches", nameKey: "nav.batches", hotkey: "alt+shift+5" },
  { href: "/migrations", nameKey: "nav.migrations", hotkey: "alt+shift+6" },
  { href: "/admin", nameKey: "nav.admin", hotkey: "alt+shift+7" },
];

// Built-ins plus every PAGE plugin's nav entry — the single source of truth for
// both the desktop menu and the mobile dialog. Entries are declared root-relative
// (a plugin cannot know the deployment's prefix) and prefixed here, once, so both
// the rendered href and the `url.startsWith` active check see the same shape.
const nav_entries = (authorized_apps) =>
  [
    ...builtin_nav.filter((entry) => page_allowed(authorized_apps, entry.href)),
    ...plugins_for(PLUGIN_POINTS.PAGE)
      .filter((plugin) => plugin.properties?.href && plugin.properties?.nameKey)
      .map((plugin) => ({
        href: plugin.properties.href,
        nameKey: plugin.properties.nameKey,
        hotkey: plugin.properties.hotkey,
      })),
  ].map((entry) => ({ ...entry, href: app_path(entry.href) }));

// Rendered in both the desktop <menu> and the mobile <dialog> so nav entries
// (built-in and plugin) are declared exactly once.
const MainNavEntries = ({ url, on_navigate }) => {
  const [t] = useTranslation(),
    state = useContext(AppState);
  return nav_entries(state.auth.authorized_apps.value).map((entry) => (
    <li key={entry.href}>
      <a
        href={entry.href}
        aria-current={url.startsWith(entry.href) ? "page" : undefined}
        onClick={on_navigate}
      >
        {t(entry.nameKey)}
      </a>
    </li>
  ));
};

export function Header() {
  const { url, route } = useLocation(),
    state = useContext(AppState),
    [t] = useTranslation(),
    // dialogs
    showSearch = () => document.getElementById("global-search").showModal(),
    show_mobile_menu = () => document.getElementById("mobile-menu").showModal(),
    close_mobile_menu = () => document.getElementById("mobile-menu").close(),
    // Leave the page behind: after signing out the address would otherwise
    // still name a task or an instance the next person may not open at all.
    // Only once the session really ended — a logout the server refuses leaves
    // the user signed in, and moving them then would say otherwise.
    logout = async () => {
      await engine_rest.auth.logout(state);
      if (!state.auth.user.id.peek()) route(app_path("/"));
    };

  useHotkeys("alt+shift+0", () => route(app_path("/")));
  useHotkeys("alt+shift+1", () => route(app_path("/tasks")));
  useHotkeys("alt+shift+2", () => route(app_path("/processes")));
  useHotkeys("alt+shift+3", () => route(app_path("/decisions")));
  useHotkeys("alt+shift+4", () => route(app_path("/deployments")));
  useHotkeys("alt+shift+5", () => route(app_path("/batches")));
  useHotkeys("alt+shift+6", () => route(app_path("/migrations")));
  useHotkeys("alt+shift+7", () => route(app_path("/admin")));

  // Plugin page hotkeys, resolved in one handler. The list is frozen before
  // render, so the combined keys string is stable across renders.
  const plugin_hotkeys = plugins_for(PLUGIN_POINTS.PAGE)
    .filter((plugin) => plugin.properties?.hotkey && plugin.properties?.href)
    .map((plugin) => ({
      hotkey: plugin.properties.hotkey,
      href: plugin.properties.href,
    }));

  useHotkeys(
    plugin_hotkeys.map((entry) => entry.hotkey).join(",") || "f13",
    (_event, handler) => {
      // react-hotkeys-hook 5 lower-cases `handler.hotkey`, so normalise both
      // sides — a plugin may declare its hotkey in any casing.
      const normalise = (hotkey) => hotkey.replaceAll(" ", "").toLowerCase();
      const pressed = normalise(
        handler?.hotkey ?? handler?.keys?.join("+") ?? "",
      );
      const hit = plugin_hotkeys.find(
        (entry) => normalise(entry.hotkey) === pressed,
      );
      if (hit) route(app_path(hit.href));
    },
  );

  // The profile is where the given and family names live, and nothing else on
  // the way in fetches it. One request per signed-in user; until it answers the
  // login id stands in.
  useEffect(() => {
    const id = state.auth.user.id.value;
    if (!id) return;
    const profile = state.api.user.profile;
    // Someone else's profile must not stand in for this user — one left over
    // from the previous session, or the account page's own fetch.
    if (profile.value?.data?.id === id) return;
    void engine_rest.user.profile.get(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.auth.user.id.value]);

  return (
    <>
      <header id="top">
        <menu id="skip-links">
          <li>
            <a href="#content">{t("nav.skip-to-content")}</a>
          </li>
        </menu>

        <a href={app_path("/")} id="mobile-logo">
          {app_name()}
        </a>
        <a
          href={app_path("/")}
          id="logo"
          aria-label={app_name()}
          aria-current={url === app_path("/") ? "page" : undefined}
        >
          <Logo />
        </a>
        <button
          type="button"
          id="mobile-menu-toggle"
          onClick={show_mobile_menu}
          aria-label={t("nav.menu")}
        />
        <div id="nav-wrapper">
          <nav id="primary-navigation" aria-label={t("nav.main-navigation")}>
            <menu>
              <MainNavEntries url={url} />
            </menu>
          </nav>
          <div>
            <button type="button" id="go-to" onClick={showSearch}>
              {t("nav.go-to")} <kbd>Alt + K</kbd>
            </button>
            <OptionsMenu />
          </div>
        </div>
      </header>

      <dialog id="mobile-menu">
        <header>
          <h2>{t("nav.menu")}</h2>
          <button
            type="button"
            onClick={close_mobile_menu}
            aria-label={t("nav.close-menu")}
          >
            <Icons.close />
          </button>
        </header>
        <nav aria-label={t("nav.mobile-navigation")}>
          <menu>
            <MainNavEntries url={url} on_navigate={close_mobile_menu} />
          </menu>
          <menu>
            <li>
              <a href={app_path("/help")}>{t("nav.help")}</a>
            </li>
            <li>
              {/* The menu stays open: the user can watch the page change
                  while the dialog is still in front of it. */}
              <ThemeToggle id="mobile-theme-toggle" show_label />
            </li>
            <li>
              <LanguageSelect id="mobile-language-select" />
            </li>
            <li>
              <a href={app_path("/account")} title={t("nav.account")}>
                {user_label(state) || t("nav.account")}
              </a>
            </li>
            <li>
              <button
                type="button"
                id="mobile-logout"
                onClick={() => {
                  close_mobile_menu();
                  logout();
                }}
              >
                {t("nav.logout")}
              </button>
            </li>
          </menu>
        </nav>
        <menu>
          <li>
            <button
              type="button"
              onClick={() => {
                close_mobile_menu();
                showSearch();
              }}
            >
              <Icons.search />
              {t("nav.go-to")}
            </button>
          </li>
          <li>
            <label id="mobile-server-selector">
              <Icons.server />
              <select
                aria-label={t("nav.choose-server")}
                onChange={(e) => swap_server(e, state)}
              >
                <option disabled>{t("nav.choose-server")}</option>
                {get_config().backends.map((server) => (
                  <option
                    key={server.url}
                    value={server.url}
                    selected={state.server.value?.url === server.url}
                  >
                    {server.name} {server.c7_mode ? "(C7)" : ""}
                  </option>
                ))}
              </select>
            </label>
          </li>
        </menu>
      </dialog>
    </>
  );
}
