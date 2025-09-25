/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 
import { render } from "preact";
import { LocationProvider, Route, Router } from "preact-iso";
import { AppState, createAppState } from "./state.js";
import './helper/i18n';

import { Header } from "./components/Header.jsx";
import { GoTo } from "./components/GoTo.jsx";

import { Home } from "./pages/Home.jsx";
import { TasksPage } from "./pages/Tasks.jsx";
import { ProcessesPage } from "./pages/Processes.jsx";
import { AdminPage } from "./pages/Admin.jsx";
import { DeploymentsPage } from "./pages/Deployments.jsx";
import { NotFound } from "./pages/_404.jsx";
import { AccountPage } from "./pages/Account.jsx";

import "./css/layout.css";
import "./css/components.css";
import { DecisionsPage } from "./pages/Decisions.jsx";
import { useContext } from "preact/hooks";
import engine_rest from "./api/engine_rest.jsx";
import { useSignal } from "@preact/signals";

("use strict");

export const App = () => {
  return (
    <AppState.Provider value={createAppState()}>
      <Routing />
    </AppState.Provider>
  );
};

const servers = JSON.parse(import.meta.env.VITE_BACKEND)

const swap_server = (e, state) => {
  const server = servers.find(s => s.url === e.target.value)
  state.server.value = server
  localStorage.setItem('server', JSON.stringify(server))
}

const get_cookie = (/** @type {string} */ name) => {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";").shift();
};

const Routing = () => {
  const cookie = get_cookie("credentials"),
    state = useContext(AppState),
    {
      auth: { logged_in },
    } = state,
    credentials = useSignal({
      username: null,
      password: null,
      remember_login: false,
    }),
    login = (event) => {
      event.preventDefault();
      void engine_rest.auth.login(
        state,
        credentials.value.username,
        credentials.value.password,
        credentials.value.remember_login,
      );
    };


  if (cookie !== undefined && state.auth.logged_in.value.data !== "authenticated") {
    const { username, password } = JSON.parse(cookie);
    void engine_rest.auth.login(state, username, password, true);
  }

  if (logged_in.value.data === "authenticated") {
    return (
      <LocationProvider>
        <Header />
          <Router>
            <Route path="/" component={Home} />
            <Route path="/decisions" component={DecisionsPage} />
            <Route path="/decisions/:decision_id" component={DecisionsPage} />
            <Route path="/tasks" component={TasksPage} />
            {/*<Route path="/tasks/start/:id" component={TasksPage} />*/}
            <Route path="/tasks/:task_id" component={TasksPage} />
            <Route path="/tasks/:task_id/:tab" component={TasksPage} />
            <Route path="/processes" component={ProcessesPage} />
            <Route path="/processes/:definition_id" component={ProcessesPage} />
            <Route path="/processes/:definition_id/:panel" component={ProcessesPage} />
            <Route path="/processes/:definition_id/:panel/:selection_id" component={ProcessesPage} />
            <Route path="/processes/:definition_id/:panel/:selection_id/:sub_panel" component={ProcessesPage} />
            <Route path="/deployments" component={DeploymentsPage} />
            <Route path="/deployments/:deployment_id" component={DeploymentsPage} />
            <Route path="/deployments/:deployment_id/:resource_name" component={DeploymentsPage} />
            <Route path="/admin" component={AdminPage} />
            <Route path="/admin/:page_id" component={AdminPage} />
            <Route path="/admin/:page_id/:selection_id" component={AdminPage} />
            <Route path="/account" component={AccountPage} />
            <Route path="/account/:page_id" component={AccountPage} />
            <Route path="/account/:page_id/:selection_id" component={AccountPage} />
            <Route default component={NotFound} />
          </Router> 
        <GoTo />
      </LocationProvider>
    );
  } else if (logged_in.value.data === "unknown") {
    void engine_rest.auth.is_authenticated(state);
  } else if (logged_in.value.data === "unauthenticated") {
    return (
      <div class="col center p-3">
        <div>
          <p>Operaton Web Apps</p>
          <h1>Login</h1>
          <label className="row center gap-1 p-1">
            Server Selection
            <select
              onChange={(e) => swap_server(e, state)}>
              <option disabled>ℹ️ Choose a server to retrieve your processes
              </option>
              {servers.map(server =>
                <option key={server.url} value={server.url}
                        selected={state.server.value?.url === server.url}>
                  {server.name} {server.c7_mode ? '(C7)' : ''}
                </option>)}
            </select>
          </label>
          <form onSubmit={login} class=".form-horizontal">
            <label for="username">User name*</label>
            <input
              name="username"
              id="username"
              onInput={(e) => (credentials.value.username = e.currentTarget.value)}
              required
            />

            <label for="password">Password*</label>
            <input
              name="password"
              type="password"
              id="password"
              onInput={(e) => (credentials.value.password = e.currentTarget.value)}
              required
            />

            <label>Remember login? (DEVELOPMENT ONLY)</label>
            <input
              type="checkbox"
              name="remember_credentials"
              onInput={(e) => (credentials.value.remember_login = e.currentTarget.checked)}
            />

            <div class="button-group">
              <button type="submit">Login</button>
            </div>
          </form>
        </div>
      </div>
    );
  }
};

render(<App />, document.getElementById("app"));
