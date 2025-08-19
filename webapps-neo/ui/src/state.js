/**
 * state.js
 *
 * Global app state using Preact signals.
 */

import { signal } from '@preact/signals'
import { createContext } from 'preact'

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
  const server = signal(get_stored_server())
  // TODO remove 'demo' when we have working authentication
  const auth = {
    logged_in: signal({ data: "unknown" }),
    credentials: signal({username: null, password: null}),
    user: { id: signal() },
    login_response: signal(null),
    logout_response: signal(null),
  }

  const deployments_page = {
    selected_resource: signal(null),
    selected_deployment: signal(null),
    selected_process_statistics: signal(null),
  }
  const history_mode = signal(false)
  const user_profile = signal(null)
  const task_claim_result = signal(null)
  const task_assign_result = signal(null)

  const api = {
    authorization: {
      all: signal(null),
      update: signal(null),
      delete: signal(null)
    },
    engine: {
      telemetry: signal(null)
    },
    user: {
      count: signal(null),
      list: signal(null),
      create: signal(null),
      // todo: remove demo user when login is implemented
      profile: signal({ id: "demo" }),
      group: {
        list: signal(null)
      },
      credentials: signal(null),
      unlock: signal(null),
    },
    group: {
      list: signal(null),
      create: signal(null),
      add_user: signal(null)
    },
    tenant: {
      list: signal(null),
      by_member: signal(null),
      create: signal(null),
      add_user: signal(null)
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
      },
      instance: {
        called: signal(null),
        one: signal(null),
        list: signal(null),
        count: signal(null),
        variables: signal(null),
      }
    },
    task: {
      list: signal(null),
      one: signal(null),
      by_process_instance: signal(null),
      form: signal(null),
      rendered_form: signal(null),
      deployed_form: signal(null),
      claim_result: signal(null),
      unclaim_result: signal(null),
      assign_result: signal(null),
      submit_form: signal(null),
      add_group: signal(null),
      delete_group: signal(null),
      identity_links: signal(null),
    },
    deployment: {
      one: signal(null),
      all: signal(null),
      resources: signal(null),
      resource: signal(null),
      delete: signal(null)
    },
    decision: {
      definitions: signal(null),
      definition: signal(null),
      dmn: signal(null),
    },
    history: {
      incident: {
        by_process_definition: signal(null),
        by_process_instance: signal(null)
      },
      user_operation: signal(null),
    },
    job_definition: {
      all: {
        by_process_definition: signal(null)
      }
    }
  }

  return {
    server,
    auth,
    api,
    deployments_page,
    history_mode,
    user_profile,
    task_claim_result,
    task_assign_result
  }
}

const AppState = createContext(undefined)

const get_stored_server = () => {
  if (localStorage.getItem('server')) {
    return JSON.parse(localStorage.getItem('server'))
  }

  const stored_server = JSON.parse(import.meta.env.VITE_BACKEND)[0]
  localStorage.setItem('server', JSON.stringify(stored_server))

  return stored_server
}

export { createAppState, AppState }
