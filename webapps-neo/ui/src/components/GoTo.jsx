import * as Icons from '../assets/icons.jsx'
import api, { RequestState } from '../api/engine_rest.jsx'

import { createContext } from 'preact'
import { useContext } from 'preact/hooks'
import { signal } from '@preact/signals'
import { useHotkeys } from 'react-hotkeys-hook'

const get_stored_server = () => {
  if (localStorage.getItem('server')) {
    return JSON.parse(localStorage.getItem('server'))
  }

  const stored_server = JSON.parse(import.meta.env.VITE_BACKEND)[0]
  localStorage.setItem('server', JSON.stringify(stored_server))

  return stored_server
}

const createSearchState = () => {
  const server = signal(get_stored_server())

  const api = {
    process: {
      definition: {
        one: signal(null)
      },
      instance: {
        one: signal(null)
      }
    }
  }

  return {
    server,
    api
  }
}

const SearchState = createContext(undefined)

const GoTo = () =>
  <dialog id="global-search" class="fade-in">
    <SearchState.Provider value={createSearchState()}>
      <SearchComponent />
    </SearchState.Provider>
  </dialog>

const close = () => document.getElementById('global-search').close()
const show = () => document.getElementById('global-search').showModal()

const SearchComponent = () => {
  const
    state = useContext(SearchState),
    search = ({ target: { value } }) => {
      void api.process_definition.one(state, value)
      void api.process_instance.one(state, value)
    }

  useHotkeys('alt+s', () => setTimeout(show, 100))

  return <search class="col gap-2">
    <header className="row space-between">
      <h2>Go To</h2>
      <button
        class="neutral"
        onClick={close}>
        Close
      </button>
    </header>

    <label className="col gap-1">
      <small>Enter resource ID</small>
      <input
        autofocus
        type="search"
        placeholder="Search Operaton..."
        className="font-size-1"
        onKeyUp={search} />
    </label>

    <SearchResults />

  </search>
}

const SearchResults = () => {
  const
    state = useContext(SearchState)

  return <section>
    <h3 class="screen-hidden">Results</h3>
    <RequestState
      signal={state.api.process.definition.one}
      on_nothing={() => <></>}
      on_error={() => <></>}
      on_success={() =>
        <div>
          <h4>Process Definition</h4>
          <a href={`/processes/${state.api.process.definition.one.value.data.id}`}
             onClick={close}>
            {state.api.process.definition.one.value.data.key}
          </a>
        </div>} />


    <RequestState
      signal={state.api.process.instance.one}
      on_nothing={() => <></>}
      on_error={() => <></>}
      on_success={() =>
        <div>
          <h4>Process Instance</h4>
          <a href={`/processes/${state.api.process.instance.one.value.data.id}`}
             onClick={close}>
            {state.api.process.instance.one.value.data.key}
          </a>
        </div>}
    />
      {(state.api.process.definition.one.value !== null && state.api.process.instance.one.value !== null) ??
        <output id="no-search-results">
          Nothing to show
        </output>
      }
  </section>
}

export { GoTo }