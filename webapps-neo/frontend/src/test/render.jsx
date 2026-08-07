/**
 * Rendering helpers for component/page tests. Kept separate from `helpers.js`
 * because importing `engine_rest` here would pull in the real `helper.jsx`,
 * which the API-resource tests deliberately mock.
 */
import { h } from "preact";
import { render } from "@testing-library/preact";
import { LocationProvider } from "preact-iso";
import { vi } from "vitest";
import { AppState } from "../state.js";
import real_engine_rest from "../api/engine_rest.jsx";
import { create_mock_state } from "./helpers.js";

/**
 * Deep-clone the real `engine_rest` object, replacing every function leaf with a
 * `vi.fn()`. Returns a mock with the exact nesting of the real API module so
 * tests can assert e.g. `engine.task.claim_task` was called.
 */
const mock_branch = (branch) => {
  const out = {};
  for (const [key, value] of Object.entries(branch)) {
    out[key] =
      typeof value === "function"
        ? vi.fn()
        : value && typeof value === "object"
          ? mock_branch(value)
          : value;
  }
  return out;
};

export const mock_engine_rest = () => mock_branch(real_engine_rest);

/**
 * Render `ui` wrapped in the AppState context and LocationProvider, so
 * preact-iso routing hooks (`useLocation`/`useRoute`) work.
 */
export const render_with_state = (ui, { state = create_mock_state() } = {}) => {
  const result = render(
    h(AppState.Provider, { value: state }, h(LocationProvider, null, ui)),
  );
  return { ...result, state };
};
