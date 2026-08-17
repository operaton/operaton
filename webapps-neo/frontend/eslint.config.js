import preact from "eslint-config-preact";
import jsxA11y from "eslint-plugin-jsx-a11y";

/**
 * Flat config (ESLint 9+). Replaces the old `eslintConfig` key in package.json.
 *
 * eslint-config-preact ships as a flat-config array, but its entries carry no
 * `files`, so they would only apply to the default `.js`/`.mjs`/`.cjs` set —
 * every `.jsx` component would go unlinted. Re-targeting each entry at both
 * extensions is what makes `npm run lint` cover the whole source tree.
 */
export default [
  {
    ignores: [
      "dist/**",
      "test-results/**",
      "playwright-report/**",
      // Standalone Node scripts for the local load-test fixtures, not app code.
      "dev-fixtures/**",
    ],
  },
  ...preact.map((config) => ({ ...config, files: ["**/*.js", "**/*.jsx"] })),
  // Static accessibility linting on JSX — the fast, run-on-every-commit layer
  // (eslint-config-preact@2 no longer bundles jsx-a11y). Complements the
  // runtime axe/pa11y page scans.
  {
    ...jsxA11y.flatConfigs.recommended,
    files: ["**/*.jsx"],
    settings: {
      // Preact uses the native `for`/`class` attributes, not React's
      // htmlFor/className. Teach jsx-a11y that `for` is a label-association
      // attribute so it doesn't flag correctly-associated Preact labels.
      "jsx-a11y": { attributes: { for: ["htmlFor", "for"] } },
    },
  },
  {
    // Playwright fixtures take a `use` callback, which the React Hooks rule
    // mistakes for a hook call.
    files: ["e2e/**"],
    rules: { "react-hooks/rules-of-hooks": "off" },
  },
];
