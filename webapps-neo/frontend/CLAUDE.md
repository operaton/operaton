# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Operaton BPM Web Apps — the frontend SPA for the [Operaton](https://github.com/operaton/operaton) open-source BPMN process engine. Built with **Preact**, **Vite**, and **Preact Signals**.

## Commands

| Task              | Command                                           |
| ----------------- | ------------------------------------------------- |
| Dev server        | `npm run dev` (serves at `http://127.0.0.1:5173`) |
| Build             | `npm run build`                                   |
| Run all tests     | `npx vitest run`                                  |
| Run tests (watch) | `npm test`                                        |
| Run a single test | `npx vitest run src/path/to/file.test.js`         |
| Format            | `npx prettier --write .`                          |
| Lint              | `npx eslint src/`                                 |
| Start backend     | `docker compose up` (Operaton on port 8084)       |

## Architecture

### State Management

Global state is a deeply nested object of Preact `signal()` instances created in `src/state.js`. The state tree mirrors the API resource structure (e.g. `state.api.process.definition.list`). It is provided via Preact Context (`AppState.Provider`) from the root `App` component. Prefer signals over React-style hooks.

### API Layer (`src/api/`)

- `helper.jsx` exports generic HTTP wrappers (`GET`, `POST`, `PUT`, `DELETE`) that take a URL path, global state, and a target signal
- Each file in `resources/` defines domain-specific API functions using those wrappers
- `engine_rest.jsx` aggregates all resource modules into a single `engine_rest` object
- `RequestState` component handles the 4 response states (`NOT_INITIALIZED`, `LOADING`, `SUCCESS`, `ERROR`) and conditionally renders content
- API function naming: `{method}_{endpoint_name}` (e.g. `get_process_definitions`)
- URL construction: prepend with `${_url_engine_rest(state)}`

### Routing

`preact-iso` with `LocationProvider` and `Router` in `src/index.jsx`. Pages live in `src/pages/`, reusable components in `src/components/`.

### Data Flow

Parent components call `engine_rest.*` functions → results land in signals from `AppState` context → child components render via `<RequestState signal={...} on_success={() => ...} />`.

### Environment

- `VITE_BACKEND`: JSON array of backend server configs (`name`, `url`, optional `c7_mode`)
- Dev proxy: `/api` requests forwarded to `http://localhost:8084` (configured in `vite.config.js`)
- Dev server binds to `127.0.0.1` (not `localhost`) to avoid CORS issues

### Backend

The backend is called Operaton which is a fork of Camunda 7. You can use Camunda 7 as a reference.

## Coding Conventions

- **Arrow functions**: `const foo = () => ..`, not `function foo() {}`
- **Naming**: `snake_case` for functions, `CamelCase` for JSX components
- **CSS classes**: `class="foo bar"`, not `className={}`
- **Semantic HTML**: `<a>` for navigation, `<button>` for actions, never `onClick` on `<div>`
- **Variable grouping**: declare multiple `const` with commas
- **CSS**: Vanilla CSS only, use CSS custom properties, prefer classless styling, prefer cascading over many single classes. Styles live in `public/css/style.css` and `src/css/`. Scope page-specific tweaks under the page id (e.g. `#processes table { … }`) rather than coining a bespoke class (`processes-table`); only add a class when the element is genuinely idiosyncratic. Navs are `<nav><menu><li><a></a></li>…</menu></nav>`; active link uses `aria-current="page"`, not an `.active` class
- **File creation**: avoid creating unnecessary files; only create new files for reusable components
- **Tests**: `*.test.js` / `*.test.jsx` colocated with source, using `describe`/`it`/`expect` (vitest globals), `@testing-library/preact` for component tests
- **i18n**: `react-i18next`, translations in `public/locales/{locale}/translation.json` (en-US, de-DE), always store text strings of the UI in the translation files
- **Icons**: JSX components in `src/assets/icons.jsx`, sourced from heroicons.com
- **Accessibility**: semantic HTML, keyboard navigation, skip links, ARIA attributes
- **Container commands**: when showing a `docker` command, always show the equivalent `podman` command as well
