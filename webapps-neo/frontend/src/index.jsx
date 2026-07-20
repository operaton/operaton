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

const servers = JSON.parse(import.meta.env.VITE_BACKEND);

const swap_server = (e, state) => {
  const server = servers.find((s) => s.url === e.target.value);
  state.server.value = server;
  localStorage.setItem("server", JSON.stringify(server));
};

const Routing = () => {
  const state = useContext(AppState),
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
            path="/decisions/:decision_id?"
            component={DecisionsPage}
          />
          {/*<Route path="/tasks/start/:id" component={TasksPage} />*/}
          <Route
            path="/tasks/:task_id?/:tab?"
            component={TasksPage}
          />
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
        <h1>Operaton Web Apps Login</h1>
        <span>
          <a href="https://docs.operaton.org/docs/documentation/webapps/">
            Documentation
          </a>
          &nbsp;-&nbsp;
          <a href="https://github.com/operaton/web-apps">Source</a>
        </span>
        <br />
        <label>
          Server Selection <br />
          <select onChange={(e) => swap_server(e, state)}>
            <option disabled>
              ℹ️ Choose a server to retrieve your processes
            </option>
            {servers.map((server) => (
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
        {is_oauth ? (
          <button
            type="button"
            onClick={() => engine_rest.auth.start_oauth_login()}
          >
            Login with SSO
          </button>
        ) : (
          <form onSubmit={login} class=".form-horizontal">
            <label for="username">User name*</label>
            <input
              name="username"
              id="username"
              onInput={(e) =>
                (credentials.value = {
                  ...credentials.peek(),
                  username: e.currentTarget.value,
                })
              }
              required
            />

            <label for="password">Password*</label>
            <input
              name="password"
              type="password"
              id="password"
              onInput={(e) =>
                (credentials.value = {
                  ...credentials.peek(),
                  password: e.currentTarget.value,
                })
              }
              required
            />

            <button type="submit">Login</button>
          </form>
        )}
      </section>
    );
  }
};

render(<App />, document.getElementById("app"));
