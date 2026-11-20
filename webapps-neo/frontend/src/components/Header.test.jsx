import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// Control the current location and capture programmatic navigation.
const route = vi.fn();
let mockUrl = "/";
vi.mock("preact-iso", () => ({
  useLocation: () => ({ url: mockUrl, route }),
}));

// useHotkeys pulls in React, which is null under the Preact test alias; stub it.
vi.mock("react-hotkeys-hook", () => ({ useHotkeys: vi.fn() }));

// Spy all engine_rest API functions but keep RESPONSE_STATE etc. real.
vi.mock("../api/engine_rest.jsx", async (importOriginal) => {
  const actual = await importOriginal();
  const spyify = (o) =>
    Object.fromEntries(
      Object.entries(o).map(([k, v]) => [
        k,
        typeof v === "function"
          ? vi.fn()
          : v && typeof v === "object"
            ? spyify(v)
            : v,
      ]),
    );
  return { ...actual, default: spyify(actual.default) };
});

// Icons pull no weight in these assertions; render lightweight stubs.
vi.mock("../assets/icons.jsx", () => ({
  close: () => h("span", { "data-testid": "icon-close" }),
  search: () => h("span", { "data-testid": "icon-search" }),
  server: () => h("span", { "data-testid": "icon-server" }),
  database: () => h("span", { "data-testid": "icon-database" }),
  user: () => h("span", { "data-testid": "icon-user" }),
  chevron_down: () => h("span", { "data-testid": "icon-chevron-down" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { Header } from "./Header.jsx";
import {
  create_mock_state,
  signal_response,
  with_base_uri,
} from "../test/helpers.js";
import { register, _reset_registry } from "../plugins/registry.js";
import { PLUGIN_POINTS } from "../plugins/points.js";

const renderHeader = (state) =>
  render(h(AppState.Provider, { value: state }, h(Header, {})));

describe("Header", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockUrl = "/";
    route.mockClear();
    localStorage.clear();
  });
  afterEach(cleanup);

  it("renders the main navigation links with the right hrefs", () => {
    const { container } = renderHeader(state);
    const nav = container.querySelector("#primary-navigation");
    const hrefs = Array.from(nav.querySelectorAll("li > a")).map((a) =>
      a.getAttribute("href"),
    );
    // The logo link lives outside #primary-navigation now; the menu holds just
    // the page entries.
    expect(hrefs).toEqual([
      "/tasks",
      "/processes",
      "/decisions",
      "/deployments",
      "/batches",
      "/migrations",
      "/admin",
    ]);
  });

  it("prefixes every navigation link under a sub-path deployment", async () => {
    await with_base_uri("http://localhost:3000/app-neo/", () => {
      mockUrl = "/app-neo/processes";
      const { container } = renderHeader(state);
      const nav = container.querySelector("#primary-navigation");
      const hrefs = Array.from(nav.querySelectorAll("li > a")).map((a) =>
        a.getAttribute("href"),
      );

      expect(hrefs).toEqual([
        "/app-neo/tasks",
        "/app-neo/processes",
        "/app-neo/decisions",
        "/app-neo/deployments",
        "/app-neo/batches",
        "/app-neo/migrations",
        "/app-neo/admin",
      ]);
      // The active check compares the prefixed href against the prefixed URL.
      expect(
        nav
          .querySelector('a[href="/app-neo/processes"]')
          .getAttribute("aria-current"),
      ).toBe("page");
      expect(container.querySelector("#logo").getAttribute("href")).toBe(
        "/app-neo/",
      );
    });
  });

  it("shows only the pages the signed-in user is authorized for", () => {
    state.auth.authorized_apps.value = ["neo", "welcome", "tasklist"];
    const { container } = renderHeader(state);
    const hrefs = Array.from(
      container.querySelectorAll("#primary-navigation li > a"),
    ).map((a) => a.getAttribute("href"));
    expect(hrefs).toEqual(["/tasks"]);
  });

  it("shows the cockpit pages when the user has cockpit", () => {
    state.auth.authorized_apps.value = ["neo", "welcome", "cockpit"];
    const { container } = renderHeader(state);
    const hrefs = Array.from(
      container.querySelectorAll("#primary-navigation li > a"),
    ).map((a) => a.getAttribute("href"));
    expect(hrefs).not.toContain("/tasks");
    expect(hrefs).not.toContain("/admin");
    expect(hrefs).toContain("/processes");
    expect(hrefs).toContain("/migrations");
  });

  it("marks the link for the current route as the current page", () => {
    mockUrl = "/processes/p1";
    const { container } = renderHeader(state);
    const nav = container.querySelector("#primary-navigation");
    const processes = nav.querySelector('a[href="/processes"]');
    const tasks = nav.querySelector('a[href="/tasks"]');
    // The active link exposes its state via aria-current="page" (semantic,
    // announced by screen readers) rather than a CSS class.
    expect(processes.getAttribute("aria-current")).toBe("page");
    expect(tasks.getAttribute("aria-current")).not.toBe("page");
  });

  it("renders an option per VITE_BACKEND server in the menu", () => {
    const { container } = renderHeader(state);
    // The row opens on click, like the theme and language rows.
    const toggle = container.querySelector("#options-server-toggle");
    expect(toggle.getAttribute("aria-expanded")).toBe("false");
    expect(container.querySelector("#options-server-list")).toBeNull();

    fireEvent.click(toggle);
    const labels = Array.from(
      container.querySelectorAll("#options-server-list .options-item"),
    ).map((i) => i.textContent.trim());
    expect(labels.some((l) => l.startsWith("Test"))).toBe(true);
    expect(labels.some((l) => l.startsWith("Other"))).toBe(true);
    // c7_mode server is annotated with "(C7)".
    expect(labels.some((l) => l.includes("(C7)"))).toBe(true);
  });

  it("updates state.server and persists to localStorage when a server is picked", () => {
    const { container } = renderHeader(state);
    fireEvent.click(container.querySelector("#options-server-toggle"));
    const other = Array.from(
      container.querySelectorAll("#options-server-list .options-item"),
    ).find((i) => i.textContent.trim().startsWith("Other"));
    fireEvent.click(other);
    expect(state.server.value).toMatchObject({
      name: "Other",
      url: "http://localhost:9090",
    });
    expect(JSON.parse(localStorage.getItem("server"))).toMatchObject({
      url: "http://localhost:9090",
    });
  });

  it("offers the theme choices in the options menu, opened on click", () => {
    const { container } = renderHeader(state);
    // The top row no longer carries a language switch or a theme switch.
    expect(container.querySelector("#language-toggle")).toBeNull();
    expect(container.querySelector("#theme-toggle")).toBeNull();

    // The choice list is closed until its row is clicked; aria-expanded
    // announces the collapsed state and no theme choice is rendered yet.
    const toggle = container.querySelector(
      '#options-theme-toggle',
    );
    expect(toggle.getAttribute("aria-expanded")).toBe("false");
    expect(container.querySelector("#options-theme-list")).toBeNull();
    expect(container.querySelector("#options-menu button[lang]")).toBeNull();

    // Clicking the row opens the list, one choice per theme, the one in use
    // marked as such.
    fireEvent.click(toggle);
    expect(
      container
        .querySelector('#options-theme-toggle')
        .getAttribute("aria-expanded"),
    ).toBe("true");
    expect(container.querySelector("#options-theme-list")).not.toBeNull();
    expect(
      container.querySelectorAll("#options-menu button[lang]").length,
    ).toBeGreaterThan(1);
  });

  it("offers the language choices in the options menu, opened on click", () => {
    const { container } = renderHeader(state);
    // The top row no longer carries a language switch or a theme switch.
    expect(container.querySelector("#language-toggle")).toBeNull();
    expect(container.querySelector("#theme-toggle")).toBeNull();

    // The choice list is closed until its row is clicked; aria-expanded
    // announces the collapsed state and no language choice is rendered yet.
    const toggle = container.querySelector(
      "#options-language-toggle",
    );
    expect(toggle.getAttribute("aria-expanded")).toBe("false");
    expect(container.querySelector("#options-language-list")).toBeNull();
    expect(container.querySelector("#options-menu button[lang]")).toBeNull();

    // Clicking the row opens the list, one choice per language, the one in use
    // marked as such.
    fireEvent.click(toggle);
    expect(
      container
        .querySelector("#options-language-toggle")
        .getAttribute("aria-expanded"),
    ).toBe("true");
    const current = container.querySelector(
      '#options-menu [lang="en-US"][aria-current="true"]',
    );
    expect(current).not.toBeNull();
    expect(
      container.querySelectorAll("#options-menu button[lang]").length,
    ).toBeGreaterThan(1);
  });
  it("goes back to the start once the session has ended", async () => {
    mockUrl = "/tasks/t1/form";
    state.auth.user.id.value = "alice";
    engine_rest.auth.logout.mockImplementation(async () => {
      state.auth.user.id.value = null;
    });
    const { container } = renderHeader(state);

    fireEvent.click(container.querySelector("#logout"));
    await vi.waitFor(() => expect(route).toHaveBeenCalledWith("/"));
  });

  it("stays put when the server refuses the sign-out", async () => {
    mockUrl = "/tasks/t1/form";
    state.auth.user.id.value = "alice";
    // A refused logout leaves the session alone; moving the user would say
    // otherwise.
    engine_rest.auth.logout.mockResolvedValue(undefined);
    const { container } = renderHeader(state);

    fireEvent.click(container.querySelector("#logout"));
    await vi.waitFor(() => expect(engine_rest.auth.logout).toHaveBeenCalled());
    expect(route).not.toHaveBeenCalledWith("/");
  });

  it("names the signed-in user where the account link sits", () => {
    state.auth.user.id.value = "alice";
    // Only the mobile menu: on the desktop the signed-in user names the
    // options button, and the account entry inside that menu is a plain
    // item without a title.
    const { getAllByTitle } = renderHeader(state);
    const links = getAllByTitle("nav.account");
    expect(links).toHaveLength(1);
    links.forEach((a) => expect(a.textContent.trim()).toBe("alice"));
  });

  it("names the signed-in user by profile name, given name first", () => {
    state.auth.user.id.value = "demo";
    signal_response(state.api.user.profile, {
      id: "demo",
      firstName: "Tom",
      lastName: "Hanks",
    });
    const { container } = renderHeader(state);
    expect(
      container.querySelector("#options-toggle-user").textContent.trim(),
    ).toBe("Tom Hanks");
  });

  it("separates server and account, and account and caret, by a dash", () => {
    const { container } = renderHeader(state);
    expect(
      container.querySelectorAll("#options-toggle .options-toggle-sep"),
    ).toHaveLength(2);
  });

  it("falls back to the account label when nobody is signed in", () => {
    state.auth.user.id.value = null;
    const { getAllByTitle } = renderHeader(state);
    expect(getAllByTitle("nav.account")[0].textContent.trim()).toBe(
      "nav.account",
    );
  });

  it("calls engine_rest.auth.logout when the logout button is clicked", () => {
    const { container } = renderHeader(state);
    fireEvent.click(container.querySelector("#logout"));
    expect(engine_rest.auth.logout).toHaveBeenCalled();
    // state is passed by reference; never compare structurally.
    expect(engine_rest.auth.logout.mock.lastCall[0]).toBe(state);
  });
});

describe("Header — plugin nav", () => {
  let state;
  beforeEach(() => {
    _reset_registry();
    register({
      id: "test-nav",
      point: PLUGIN_POINTS.PAGE,
      properties: { href: "/plugin/test-nav", nameKey: "plugins.test.nav" },
      Component: () => null,
    });
    state = create_mock_state();
    mockUrl = "/";
  });
  afterEach(() => {
    cleanup();
    _reset_registry();
  });

  it("renders a PAGE plugin entry in both the desktop and mobile menus", () => {
    const { container } = renderHeader(state);
    const desktop = container.querySelectorAll(
      '#primary-navigation a[href="/plugin/test-nav"]',
    );
    const mobile = container.querySelectorAll(
      '#mobile-menu a[href="/plugin/test-nav"]',
    );
    expect(desktop).toHaveLength(1);
    expect(mobile).toHaveLength(1);
  });

  it("marks the plugin entry aria-current when its route is active", () => {
    mockUrl = "/plugin/test-nav";
    const { container } = renderHeader(state);
    const link = container.querySelector(
      '#primary-navigation a[href="/plugin/test-nav"]',
    );
    expect(link.getAttribute("aria-current")).toBe("page");
  });
});
