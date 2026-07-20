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
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { Header } from "./Header.jsx";
import { create_mock_state } from "../test/helpers.js";

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
    // Logo (/), tasks, processes, decisions, deployments, batches,
    // migrations, admin.
    expect(hrefs).toEqual([
      "/",
      "/tasks",
      "/processes",
      "/decisions",
      "/deployments",
      "/batches",
      "/migrations",
      "/admin",
    ]);
  });

  it("marks the link for the current route as active", () => {
    mockUrl = "/processes/p1";
    const { container } = renderHeader(state);
    const nav = container.querySelector("#primary-navigation");
    const processes = nav.querySelector('a[href="/processes"]');
    const tasks = nav.querySelector('a[href="/tasks"]');
    // The source toggles an `active` class (not aria-current) for the matching
    // route prefix; assert on that observable behavior.
    expect(processes.getAttribute("class")).toBe("active");
    expect(tasks.getAttribute("class")).not.toBe("active");
  });

  it("renders an option per VITE_BACKEND server in the selector", () => {
    const { container } = renderHeader(state);
    const select = container.querySelector("#server-selector select");
    const labels = Array.from(select.querySelectorAll("option")).map((o) =>
      o.textContent.trim(),
    );
    // First option is the disabled placeholder, then the two backends.
    expect(labels).toContain("nav.choose-server");
    expect(labels.some((l) => l.startsWith("Test"))).toBe(true);
    expect(labels.some((l) => l.startsWith("Other"))).toBe(true);
    // c7_mode server is annotated with "(C7)".
    expect(labels.some((l) => l.includes("(C7)"))).toBe(true);
  });

  it("updates state.server and persists to localStorage when the server changes", () => {
    const { container } = renderHeader(state);
    const select = container.querySelector("#server-selector select");
    fireEvent.change(select, { target: { value: "http://localhost:9090" } });
    expect(state.server.value).toMatchObject({
      name: "Other",
      url: "http://localhost:9090",
    });
    expect(JSON.parse(localStorage.getItem("server"))).toMatchObject({
      url: "http://localhost:9090",
    });
  });

  it("calls engine_rest.auth.logout when the logout button is clicked", () => {
    const { container } = renderHeader(state);
    fireEvent.click(container.querySelector("#logout"));
    expect(engine_rest.auth.logout).toHaveBeenCalled();
    // state is passed by reference; never compare structurally.
    expect(engine_rest.auth.logout.mock.lastCall[0]).toBe(state);
  });
});
