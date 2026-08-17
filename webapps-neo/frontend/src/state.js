/**
 * state.js
 *
 * Global app state using Preact signals.
 */

import { signal } from "@preact/signals";
import { createContext } from "preact";
import { plugin_state_branches } from "./plugins/registry.js";

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
      retry: signal(null),
      saved_filters: signal(null),
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
        list_startable: signal(null),
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
        saved_filters: signal(null),
      },
      instance: {
        called: signal(null),
        one: signal(null),
        list: signal(null),
        count: signal(null),
        variables: signal(null),
        variables_update: signal(null),
        by_defintion_id: signal(null),
        activity_instances: signal(null),
        modification: signal(null),
        suspend: signal(null),
        delete: signal(null),
        saved_filters: signal(null),
      },
    },
    incident: {
      by_process_instance: signal(null),
      by_process_definition: signal(null),
      annotation: signal(null),
    },
    job: {
      by_process_instance: signal(null),
      update: signal(null),
      stacktrace: signal(null),
    },
    external_task: {
      by_process_instance: signal(null),
      update: signal(null),
      error_details: signal(null),
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
      complete: signal(null),
      add_group: signal(null),
      delete_group: signal(null),
      identity_links: signal(null),
      comment: {
        list: signal(null),
        create: signal(null),
      },
      attachment: {
        list: signal(null),
        create: signal(null),
        delete: signal(null),
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
      // The process definition matching a deployment resource. Its own slot
      // (not process.definition.one) because the engine returns it as an
      // *array* here, whereas process.definition.one holds a single object.
      process_definition: signal(null),
      delete: signal(null),
      create: signal(null),
      saved_filters: signal(null),
    },
    decision: {
      definitions: signal(null),
      definition: signal(null),
      dmn: signal(null),
      instances: signal(null),
      instance: signal(null),
      saved_filters: signal(null),
    },
    history: {
      incident: {
        by_process_definition: signal(null),
        by_process_instance: signal(null),
      },
      task: {
        by_process_instance: signal(null),
      },
      activity_instance: {
        by_process_instance: signal(null),
      },
      variable_instance: {
        by_process_instance: signal(null),
      },
      process_instance: {
        called: signal(null),
        // Historic instance detail / list. Separate from the runtime
        // process.instance.{one,list} because the historic payload has a
        // different shape (state/startTime/endTime/hasMore); sharing one slot
        // let a stale runtime shape render in history mode and vice versa.
        one: signal(null),
        list: signal(null),
      },
      batch: {
        list: signal(null),
        one: signal(null),
      },
      user_operation: signal(null),
      user_operation_annotation: signal(null),
    },
    job_definition: {
      all: {
        by_process_definition: signal(null),
      },
      update: signal(null),
    },
    // Plugin-contributed signal branches, as state.api.plugins.<plugin-id>.
    // Populated from the registry, which is frozen before the first render.
    plugins: plugin_state_branches(),
  };

  return {
    server,
    auth,
    api,
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
