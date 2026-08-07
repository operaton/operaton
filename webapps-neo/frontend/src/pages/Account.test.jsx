import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// Spy all engine_rest API functions but keep RequestState/RESPONSE_STATE real.
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

let mockParams = {};
const routeFn = vi.fn();
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams }),
  useLocation: () => ({ route: routeFn, path: "/account" }),
}));

import { AppState } from "../state.js";
import engine_rest from "../api/engine_rest.jsx";
import { AccountPage } from "./Account.jsx";
import { create_mock_state, signal_response } from "../test/helpers.js";

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(AccountPage, {})));

describe("AccountPage", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    routeFn.mockClear();
  });
  afterEach(cleanup);

  it("redirects to the profile sub-page when no page_id is set", () => {
    mockParams = {};
    renderPage(state);
    expect(routeFn).toHaveBeenCalledWith("/account/profile", true);
  });

  it("renders the account navigation links", () => {
    mockParams = { page_id: "profile" };
    const { getByText } = renderPage(state);
    expect(getByText("account.profile").getAttribute("href")).toBe(
      "/account/profile",
    );
    expect(getByText("admin.groups").getAttribute("href")).toBe(
      "/account/groups",
    );
    expect(getByText("admin.tenants").getAttribute("href")).toBe(
      "/account/tenants",
    );
  });

  it("fetches the user profile on the profile sub-page", () => {
    mockParams = { page_id: "profile" };
    state.api.user.profile.value = null;
    renderPage(state);
    expect(engine_rest.user.profile.get).toHaveBeenCalled();
  });

  it("renders the profile details from the populated signal", () => {
    mockParams = { page_id: "profile" };
    signal_response(state.api.user.profile, {
      id: "demo",
      firstName: "Ada",
      lastName: "Lovelace",
      email: "ada@example.com",
    });
    const { getByText } = renderPage(state);
    expect(getByText("Ada")).toBeTruthy();
    expect(getByText("Lovelace")).toBeTruthy();
    expect(getByText("ada@example.com")).toBeTruthy();
  });

  it("renders editable inputs on the profile edit page and saves via the update fn", () => {
    mockParams = { page_id: "profile", selection_id: "edit" };
    signal_response(state.api.user.profile, {
      id: "demo",
      firstName: "Ada",
      lastName: "Lovelace",
      email: "ada@example.com",
    });
    state.user_profile.value = {
      data: {
        id: "demo",
        firstName: "Ada",
        lastName: "Lovelace",
        email: "ada@example.com",
      },
    };
    engine_rest.user.profile.update.mockResolvedValue(undefined);
    const { container } = renderPage(state);

    const firstName = container.querySelector("#first-name");
    expect(firstName).toBeTruthy();
    expect(firstName.value).toBe("Ada");

    fireEvent.input(firstName, { target: { value: "Grace" } });

    const form = container.querySelector("form");
    fireEvent.submit(form);
    expect(engine_rest.user.profile.update).toHaveBeenCalled();
    expect(engine_rest.user.profile.update.mock.lastCall[0]).toBe(state);
  });

  it("fetches and renders the user's groups", () => {
    mockParams = { page_id: "groups" };
    signal_response(state.api.user.group.list, [
      { id: "g1", name: "Admins", type: "WORKFLOW" },
    ]);
    const { getByText } = renderPage(state);
    expect(getByText("g1").getAttribute("href")).toBe("/admin/groups/g1");
    expect(getByText("Admins")).toBeTruthy();
  });

  it("fetches group membership on mount when not loaded", () => {
    mockParams = { page_id: "groups" };
    state.api.user.group.list.value = null;
    renderPage(state);
    expect(engine_rest.group.by_member).toHaveBeenCalled();
  });

  it("fetches and renders the user's tenants", () => {
    mockParams = { page_id: "tenants" };
    signal_response(state.api.tenant.by_member, [{ id: "t1", name: "Acme" }]);
    const { getByText } = renderPage(state);
    expect(getByText("t1").getAttribute("href")).toBe("/admin/tenants/t1");
    expect(getByText("Acme")).toBeTruthy();
  });

  it("renders the language settings selector", () => {
    mockParams = { page_id: "settings" };
    const { container, getByText } = renderPage(state);
    expect(getByText("account.language")).toBeTruthy();
    const select = container.querySelector("#language-select");
    expect(select).toBeTruthy();
    fireEvent.change(select, { target: { value: "de-DE" } });
    // changeLanguage is the mocked i18n fn from setup; just assert no throw and
    // the option exists.
    expect(select.querySelector('option[value="de-DE"]')).toBeTruthy();
  });
});
