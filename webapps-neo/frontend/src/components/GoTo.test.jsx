import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup, fireEvent } from "@testing-library/preact";

// Capture programmatic navigation triggered by selecting a result.
const route = vi.fn();
vi.mock("preact-iso", () => ({
  useLocation: () => ({ route }),
}));

// useHotkeys pulls in React, which is null under the Preact test alias; stub it.
vi.mock("react-hotkeys-hook", () => ({ useHotkeys: vi.fn() }));

import { AppState } from "../state.js";
import { GoTo } from "./GoTo.jsx";
import {
  create_mock_state,
  signal_response,
  RESPONSE_STATE,
} from "../test/helpers.js";

const renderGoTo = (state) =>
  render(h(AppState.Provider, { value: state }, h(GoTo, {})));

const type = (input, value) => fireEvent.input(input, { target: { value } });

describe("GoTo", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    route.mockClear();
  });
  afterEach(cleanup);

  it("shows the hint and no result items before anything is typed", () => {
    const { container, getByText } = renderGoTo(state);
    expect(getByText("goto.hint")).toBeTruthy();
    expect(container.querySelectorAll(".goto-item")).toHaveLength(0);
  });

  it("filters the static page entries by the query (label = translation key)", () => {
    const { container } = renderGoTo(state);
    const input = container.querySelector("input.goto-input");
    // PAGES labels are translation keys here; "tasks" matches goto.pages.tasks.
    type(input, "tasks");
    const items = Array.from(container.querySelectorAll(".goto-item"));
    const hrefs = items.map((a) => a.getAttribute("href"));
    expect(hrefs).toContain("/tasks");
    // A non-matching query should yield no page entries.
    type(input, "zzzznotarealpage");
    expect(
      Array.from(container.querySelectorAll(".goto-item")).map((a) =>
        a.getAttribute("href"),
      ),
    ).not.toContain("/tasks");
  });

  it("navigates and closes when a result is clicked", () => {
    const { container } = renderGoTo(state);
    const input = container.querySelector("input.goto-input");
    type(input, "decisions");
    const link = container.querySelector('.goto-item[href="/decisions"]');
    expect(link).toBeTruthy();
    fireEvent.click(link);
    expect(route).toHaveBeenCalledWith("/decisions");
    // navigate() resets the query, clearing the results list.
    expect(input.value).toBe("");
  });

  it("includes matching tasks from app state as results", () => {
    signal_response(
      state.api.task.list,
      [
        { id: "t1", name: "Approve invoice", definitionName: "Invoicing" },
        { id: "t2", name: "Something else", definitionName: "Other" },
      ],
      RESPONSE_STATE.SUCCESS,
    );
    const { container } = renderGoTo(state);
    const input = container.querySelector("input.goto-input");
    type(input, "invoice");
    const link = container.querySelector('.goto-item[href="/tasks/t1"]');
    expect(link).toBeTruthy();
    expect(link.textContent).toContain("Approve invoice");
    // The non-matching task should not appear.
    expect(container.querySelector('.goto-item[href="/tasks/t2"]')).toBeFalsy();
  });

  it("navigates to the selected item on Enter via keyboard", () => {
    const { container } = renderGoTo(state);
    const input = container.querySelector("input.goto-input");
    type(input, "processes");
    // First match (selected index 0) is the /processes page entry.
    fireEvent.keyDown(input, { key: "Enter" });
    expect(route).toHaveBeenCalledWith("/processes");
  });
});
