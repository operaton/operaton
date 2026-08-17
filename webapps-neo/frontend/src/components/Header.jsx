// noinspection HtmlUnknownAnchorTarget,JSValidateTypes

import { useLocation } from "preact-iso";
import * as Icons from "../assets/icons.jsx";
import { useHotkeys } from "react-hotkeys-hook";
import { useContext } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { plugins_for } from "../plugins/registry.js";
import { PLUGIN_POINTS } from "../plugins/points.js";

const servers = JSON.parse(import.meta.env.VITE_BACKEND);

const swap_server = (e, state) => {
  const server = servers.find((s) => s.url === e.target.value);
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
// both the desktop menu and the mobile dialog.
const nav_entries = () => [
  ...builtin_nav,
  ...plugins_for(PLUGIN_POINTS.PAGE)
    .filter((plugin) => plugin.properties?.href && plugin.properties?.nameKey)
    .map((plugin) => ({
      href: plugin.properties.href,
      nameKey: plugin.properties.nameKey,
      hotkey: plugin.properties.hotkey,
    })),
];

// Rendered in both the desktop <menu> and the mobile <dialog> so nav entries
// (built-in and plugin) are declared exactly once.
const MainNavEntries = ({ url, on_navigate }) => {
  const [t] = useTranslation();
  return nav_entries().map((entry) => (
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
    logout = () => engine_rest.auth.logout(state);

  useHotkeys("alt+shift+0", () => route("/"));
  useHotkeys("alt+shift+1", () => route("/tasks"));
  useHotkeys("alt+shift+2", () => route("/processes"));
  useHotkeys("alt+shift+3", () => route("/decisions"));
  useHotkeys("alt+shift+4", () => route("/deployments"));
  useHotkeys("alt+shift+5", () => route("/batches"));
  useHotkeys("alt+shift+6", () => route("/migrations"));
  useHotkeys("alt+shift+7", () => route("/admin"));

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
      if (hit) route(hit.href);
    },
  );

  return (
    <>
      <header id="top">
        {/* {import.meta.env.VITE_HIDE_RELEASE_WARNING === "true"
          ? null
          : <div id="release-warning">
              {t("nav.release-warning")}{" "}
              <a href="https://github.com/operaton/web-apps/issues">{t("nav.release-warning-issue")}</a>{" "}
              {t("nav.release-warning-forum") !== t("nav.release-warning-issue") && <>
                {t("nav.release-warning-or")}{" "}
                <a href="https://forum.operaton.org/">{t("nav.release-warning-forum")}</a>
              </>}
            </div>}*/}

        <menu id="skip-links">
          <li>
            <a href="#content">{t("nav.skip-to-content")}</a>
          </li>
        </menu>

        <a href="/" id="mobile-logo">
          OPERATON
        </a>
        <a
          href="/"
          id="logo"
          aria-label="Operaton"
          aria-current={url === "/" ? "page" : undefined}
        >
          <img src="/operaton-logo.svg" alt="Operaton" />
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
            <label id="server-selector">
              {/* <Icons.server />*/}
              <span>{t("nav.server")}</span>
              <select onChange={(e) => swap_server(e, state)}>
                <option disabled>{t("nav.choose-server")}</option>
                {servers.map((server) => (
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
            <button type="button" id="go-to" onClick={showSearch}>
              {t("nav.go-to")} <kbd>Alt+K</kbd>
            </button>
            <div>
              <nav id="secondary-navigation">
                <menu>
                  <li>
                    <a
                      href="/help"
                      aria-current={
                        url.startsWith("/help") ? "page" : undefined
                      }
                    >
                      {t("nav.help")}
                    </a>
                  </li>
                  <li>
                    <a
                      href="/account"
                      aria-current={
                        url.startsWith("/account") ? "page" : undefined
                      }
                    >
                      {t("nav.account")}
                    </a>
                  </li>
                </menu>
              </nav>
              <button type="button" id="logout" onClick={logout}>
                {t("nav.logout")}
              </button>
            </div>
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
              <a href="/help">{t("nav.help")}</a>
            </li>
            <li>
              <a href="/account">{t("nav.account")}</a>
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
                {servers.map((server) => (
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
