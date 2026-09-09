import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup } from "@testing-library/preact";
import { AppState } from "../state.js";
import { create_mock_state } from "../test/helpers.js";
import { require_app } from "./RequireApp.jsx";

const Page = () => h("div", { "data-testid": "page" }, "the page");
const Guarded = require_app(Page, "admin");

const renderGuarded = (state) =>
  render(h(AppState.Provider, { value: state }, h(Guarded, {})));

describe("require_app", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
  });
  afterEach(cleanup);

  it("renders the page when the application is authorized", () => {
    state.auth.authorized_apps.value = ["welcome", "admin"];
    const { queryByTestId } = renderGuarded(state);
    expect(queryByTestId("page")).not.toBeNull();
  });

  it("says why instead of rendering an empty page when it is not", () => {
    // Without the authorization the engine filters every query, so the page
    // would render but stay empty — which reads as "there is no data".
    state.auth.authorized_apps.value = ["welcome", "tasklist"];
    const { queryByTestId, getByText } = renderGuarded(state);
    expect(queryByTestId("page")).toBeNull();
    expect(getByText("no-access.title")).toBeTruthy();
  });

  it("renders the page when the server reported no applications", () => {
    state.auth.authorized_apps.value = null;
    const { queryByTestId } = renderGuarded(state);
    expect(queryByTestId("page")).not.toBeNull();
  });
});
