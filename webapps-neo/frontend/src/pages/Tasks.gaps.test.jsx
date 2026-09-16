import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { h } from "preact";
import { render, cleanup } from "@testing-library/preact";

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
let mockQuery = {};
vi.mock("preact-iso", () => ({
  useRoute: () => ({ params: mockParams, query: mockQuery }),
  useLocation: () => ({ route: vi.fn(), path: "/tasks", query: mockQuery }),
}));

import { AppState } from "../state.js";
import { TasksPage, FILTER_KEYS } from "./Tasks.jsx";
import { filter_from_form } from "../components/FilterEditForm.jsx";
import { create_mock_state } from "../test/helpers.js";

const keys = () => FILTER_KEYS.map((k) => k.key);

const renderPage = (state) =>
  render(h(AppState.Provider, { value: state }, h(TasksPage, {})));

/**
 * What the task list cannot do yet.
 *
 * Each of these marks a capability the web apps are expected to have and this
 * one has not. They are written as `it.fails`, so the suite stays green while
 * the gap is open and turns red the day it is closed — at which point the test
 * is already written and only has to lose its `.fails`.
 */
describe("not built yet", () => {
  let state;
  beforeEach(() => {
    state = create_mock_state();
    mockParams = {};
    mockQuery = {};
  });
  afterEach(cleanup);

  describe("sorting by more than one criterion", () => {
    it.fails("keeps a second sort criterion", () => {
      const state_query = { sortBy: "created", sortOrder: "desc" };
      expect(state_query).toHaveProperty("sortings");
    });
  });

  describe("variables of a filter as columns of the list", () => {
    it.fails("shows a filter's variables as columns", () => {
      const { container } = renderPage(state);
      expect(container.querySelector("th.filter-variable")).not.toBeNull();
    });
  });
});
