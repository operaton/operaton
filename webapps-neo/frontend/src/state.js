/**
 * state.js
 *
 * Global app state using Preact signals.
 */

import { signal } from "@preact/signals";
import { createContext } from "preact";

/**
 * Create the global app state by invoking the function in the root [Tasks.jsx`]
 * (./src/Tasks.jsx) by using `<AppState.Provider value={createAppState()}>`.
 *
 * To add new entries to the state expand the list of definitions in a flat
 *
 *
 * @returns {Object} exposing all defined signals
 */
const createAppState = () => {
  const server = signal(get_stored_server());
  const auth = {
    mode: import.meta.env.VITE_AUTH_MODE || "basic",
    logged_in: signal({ data: "unknown" }),
    credentials: signal({ username: null, password: null }),
    token: signal(null),
    user: { id: signal() },
    login_response: signal(null),
    logout_response: signal(null),
  };

  const deployments_page = {
    selected_resource: signal(null),
    selected_deployment: signal(null),
    selected_process_statistics: signal(null),
  };
  const user_profile = signal(null);
  const user_profile_edit = signal({});
  const user_profile_edit_response = signal(undefined);
  const task_claim_result = signal(null);
  const task_assign_result = signal(null);

  const api = {
    authorization: {
      all: signal(null),
      create: signal(null),
      update: signal(null),
      delete: signal(null),
    },
    engine: {
      telemetry: signal(null),
    },
    user: {
      count: signal(null),
      list: signal(null),
      create: signal(null),
      // todo: remove demo user when login is implemented
      profile: signal({ id: "demo" }),
      update: signal(null),
      delete: signal(null),
      group: {
        list: signal(null),
      },
      credentials: signal(null),
      unlock: signal(null),
    },
    group: {
      list: signal(null),
      create: signal(null),
      update: signal(null),
      delete: signal(null),
      members: signal(null),
      add_user: signal(null),
      remove_member: signal(null),
    },
    migration: {
      generate: signal(null),
      validation: signal(null),
      execution: signal(null),
    },
    batch: {
      list: signal(null),
      one: signal(null),
      delete: signal(null),
      update: signal(null),
    },
    tenant: {
      list: signal(null),
      by_member: signal(null),
      create: signal(null),
      update: signal(null),
      delete: signal(null),
      user_members: signal(null),
      group_members: signal(null),
      add_user: signal(null),
      remove_user: signal(null),
      add_group: signal(null),
      remove_group: signal(null),
    },
    process: {
      definition: {
        one: signal(null),
        list: signal(null),
        called: signal(null),
        diagram: signal(null),
        statistics: signal(null),
        submit_form: signal(null),
        start_form: signal(null),
        deployed_start_form: signal(null),
        rendered_form: signal(null),
        activity_instance_statistics: signal(null),
        suspend: signal(null),
        remove: signal(null),
      },
      instance: {
        called: signal(null),
        one: signal(null),
        list: signal(null),
        count: signal(null),
        variables: signal(null),
        by_defintion_id: signal(null),
        activity_instances: signal(null),
        modification: signal(null),
      },
    },
    task: {
      list: signal(null),
      one: signal(null),
      by_process_instance: signal(null),
      form: signal(null),
      rendered_form: signal(null),
      deployed_form: signal(null),
      form_variables: signal(null),
      claim_result: signal(null),
      unclaim_result: signal(null),
      assign_result: signal(null),
      submit_form: signal(null),
      add_group: signal(null),
      delete_group: signal(null),
      identity_links: signal(null),
      comment: {
        list: signal(null),
        create: signal(null),
      },
    },
    filter: {
      list: signal(null),
      one: signal(null),
      create: signal(null),
      update: signal(null),
      delete: signal(null),
    },
    deployment: {
      one: signal(null),
      all: signal(null),
      resources: signal(null),
      resource: signal(null),
      delete: signal(null),
    },
    decision: {
      definitions: signal(null),
      definition: signal(null),
      dmn: signal(null),
    },
    history: {
      incident: {
        by_process_definition: signal(null),
        by_process_instance: signal(null),
      },
      user_operation: signal(null),
    },
    job_definition: {
      all: {
        by_process_definition: signal(null),
      },
    },
  };

  return {
    server,
    auth,
    api,
    deployments_page,
    user_profile,
    user_profile_edit,
    user_profile_edit_response,
    task_claim_result,
    task_assign_result,
  };
};

const AppState = createContext(undefined);

const get_stored_server = () => {
  const servers = JSON.parse(import.meta.env.VITE_BACKEND),
    stored = localStorage.getItem("server");

  if (stored) {
    const parsed = JSON.parse(stored);
    if (servers.some((s) => s.url === parsed.url)) return parsed;
  }

  const server = servers[0];
  localStorage.setItem("server", JSON.stringify(server));
  return server;
};

export { createAppState, AppState };
