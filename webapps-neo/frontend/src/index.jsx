import { render } from "preact";
import { Suspense } from "preact/compat";
import { LocationProvider, Route, Router } from "preact-iso";
import { AppState, createAppState } from "./state.js";
import "./helper/i18n";

import { Header } from "./components/Header.jsx";
import { GoTo } from "./components/GoTo.jsx";

import { Home } from "./pages/Home.jsx";
import { DashboardPage } from "./pages/Dashboard.jsx";
import { TasksPage } from "./pages/Tasks.jsx";
import { ProcessesPage } from "./pages/Processes.jsx";
import { MigrationsPage } from "./pages/Migrations.jsx";
import { AdminPage } from "./pages/Admin.jsx";
import { DeploymentsPage } from "./pages/Deployments.jsx";
import { BatchesPage } from "./pages/Batches.jsx";
import { NotFound } from "./pages/_404.jsx";
import { AccountPage } from "./pages/Account.jsx";

import "./css/style.css";
import "./css/components.css";

import { DecisionsPage } from "./pages/Decisions.jsx";
import { useContext } from "preact/hooks";
import engine_rest from "./api/engine_rest.jsx";
import { useSignal } from "@preact/signals";
import { is_oauth } from "./api/oauth.js";
import { get_config, load_config } from "./config.js";
import { useTranslation } from "react-i18next";
import { load_plugins } from "./plugins/loader.js";
import { install_plugin_host } from "./plugins/host.js";
import { plugins_for } from "./plugins/registry.js";
import { PLUGIN_POINTS } from "./plugins/points.js";

("use strict");

export const App = () => {
  return (
    <Suspense fallback="">
      <AppState.Provider value={createAppState()}>
        <Routing />
      </AppState.Provider>
    </Suspense>
  );
};

const languages = [
  { code: "en-US", label: "English" },
  { code: "de-DE", label: "Deutsch" },
  { code: "es-ES", label: "Español" },
  { code: "fr-FR", label: "Français" },
  { code: "nl-NL", label: "Nederlands" },
];

const swap_server = (e, state) => {
  const server = get_config().backends.find((s) => s.url === e.target.value);
  state.server.value = server;
  localStorage.setItem("server", JSON.stringify(server));
};

const Routing = () => {
  const { t, i18n } = useTranslation(),
    state = useContext(AppState),
    {
      auth: { logged_in },
    } = state,
    credentials = useSignal({
      username: null,
      password: null,
    }),
    login = (event) => {
      event.preventDefault();
      void engine_rest.auth.login(
        state,
        credentials.value.username,
        credentials.value.password,
      );
    };

  if (logged_in.value.data === "authenticated") {
    return (
      <LocationProvider>
        <Header />
        <Router>
          <Route path="/" component={DashboardPage} />
          <Route
            path="/decisions/:decision_id?/:panel?"
            component={DecisionsPage}
          />
          {/*<Route path="/tasks/start/:id" component={TasksPage} />*/}
          <Route path="/tasks/:task_id?/:tab?" component={TasksPage} />
          <Route
            path="/processes/:definition_id?/:panel?/:selection_id?/:sub_panel?"
            component={ProcessesPage}
          />
          <Route path="/migrations" component={MigrationsPage} />
          <Route
            path="/deployments/:deployment_id?/:resource_name?"
            component={DeploymentsPage}
          />
          <Route path="/batches/:batch_id?" component={BatchesPage} />
          <Route
            path="/admin/:page_id?/:selection_id?/:sub_selection_id?"
            component={AdminPage}
          />
          <Route
            path="/account/:page_id?/:selection_id?"
            component={AccountPage}
          />
          <Route path="/help" component={Home} />
          {plugins_for(PLUGIN_POINTS.PAGE).map((plugin) => (
            <Route
              key={plugin.id}
              path={plugin.properties.path}
              component={plugin.Component}
            />
          ))}
          <Route default component={NotFound} />
        </Router>
        <GoTo />
      </LocationProvider>
    );
  } else if (logged_in.value.data === "unknown") {
    void engine_rest.auth.is_authenticated(state);
  } else if (logged_in.value.data === "unauthenticated") {
    return (
      <section class="login-page">
        <img class="login-logo" src="/operaton-logo.svg" alt="Operaton" />

        <div class="login-content">
          <h1>{t("login.title")}</h1>

          <div class="login-card">
            {get_config().backends.length > 1 && (
              <>
                <label>
                  {t("login.backend")}
                  <small>{t("login.backend-hint")}</small>
                  <select onChange={(e) => swap_server(e, state)}>
                    <option disabled>{t("login.choose-server")}</option>
                    {get_config().backends.map((server) => (
                      <option
                        key={server.url}
                        value={server.url}
                        selected={state.server.value?.url === server.url}
                      >
                        {server.name} {server.c7_mode ? "(C7)" : ""}
                      </option>
                    ))}
                  </select>
                </label>

                <hr />
              </>
            )}

            {is_oauth() ? (
              <button
                type="button"
                onClick={() => engine_rest.auth.start_oauth_login()}
              >
                {t("login.sso")}
              </button>
            ) : (
              <form onSubmit={login}>
                <label for="username">{t("login.username")}</label>
                <input
                  type="text"
                  name="username"
                  id="username"
                  autocomplete="username"
                  onInput={(e) =>
                    (credentials.value = {
                      ...credentials.peek(),
                      username: e.currentTarget.value,
                    })
                  }
                  required
                />

                <label for="password">{t("login.password")}</label>
                <input
                  name="password"
                  type="password"
                  id="password"
                  autocomplete="current-password"
                  onInput={(e) =>
                    (credentials.value = {
                      ...credentials.peek(),
                      password: e.currentTarget.value,
                    })
                  }
                  required
                />

                <button type="submit">{t("login.submit")}</button>
              </form>
            )}
          </div>

          <label class="login-language">
            {t("login.language")}
            <select
              onChange={(e) => i18n.changeLanguage(e.currentTarget.value)}
            >
              {languages.map((lang) => (
                <option
                  key={lang.code}
                  value={lang.code}
                  selected={i18n.resolvedLanguage === lang.code}
                >
                  {lang.label}
                </option>
              ))}
            </select>
          </label>

          <span class="login-links">
            <a href="https://docs.operaton.org/docs/documentation/webapps/">
              {t("login.documentation")}
            </a>
            &nbsp;-&nbsp;
            <a href="https://github.com/operaton/web-apps">
              {t("login.source")}
            </a>
          </span>
        </div>
      </section>
    );
  }
};

// Expose host primitives for remote no-build plugins, then load every plugin
// (bundled + remote) before the first render so the registry is frozen and
// every seam — routes, nav, tabs, state — sees a stable plugin list. A broken
// plugin server can never block boot; the loader guards itself with a timeout.
install_plugin_host();
// The runtime configuration must be in place before anything reads it, and the
// plugin loader needs it to know where the manifest lives.
load_config()
  .then(load_plugins)
  .finally(() => render(<App />, document.getElementById("app")));
