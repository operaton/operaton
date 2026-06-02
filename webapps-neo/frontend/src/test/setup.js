/**
 * Global vitest setup, registered via `setupFiles` in vitest.config.js.
 *
 * - Defines `import.meta.env.VITE_BACKEND` so importing `state.js` / `index.jsx`
 *   (which parse it at module load) does not throw in the test environment.
 * - Mocks `react-i18next` so components render stable translation keys instead
 *   of resolved strings, letting tests assert on keys (e.g. "tasks.claim").
 * - Cleans up the rendered DOM and resets mocks after every test.
 */
import { afterEach, vi } from "vitest";
import { cleanup } from "@testing-library/preact";

// Provide a deterministic backend list for tests that read VITE_BACKEND.
import.meta.env.VITE_BACKEND = JSON.stringify([
  { name: "Test", url: "http://localhost:8080" },
  { name: "Other", url: "http://localhost:9090", c7_mode: true },
]);

vi.mock("react-i18next", () => ({
  // The real useTranslation returns an array [t, i18n, ready] that ALSO carries
  // .t / .i18n properties, so both `const [t] =` and `const { t } =` work.
  useTranslation: () => {
    const t = (key) => key;
    const i18n = { changeLanguage: vi.fn(), language: "en-US" };
    const result = [t, i18n, true];
    result.t = t;
    result.i18n = i18n;
    return result;
  },
  // Render children verbatim; expose the key via the `i18nKey` prop when present.
  Trans: ({ children, i18nKey }) => children ?? i18nKey ?? null,
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
