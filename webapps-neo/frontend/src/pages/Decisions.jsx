import { useContext } from 'preact/hooks'
import { AppState } from '../state.js'
import { useLocation, useRoute } from 'preact-iso'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import { DmnViewer } from '../components/DMNViewer.jsx'
import { effect } from '@preact/signals'

const DecisionsPage = () => {
  const state = useContext(AppState),
    { api: { decision: { definitions, definition, dmn } } } = state,
    { params: { decision_id } } = useRoute()
  if (definitions.value === null) {
    void engine_rest.decision.get_decision_definitions(state)
  }

  if (decision_id && definition.value === null) {
    void engine_rest.decision.get_decision_definition(state, decision_id)
  }

  if (decision_id && dmn.value === null) {
    void engine_rest.decision.get_dmn_xml(state, decision_id)
  }

  return (
    <div class="fade-in list-container">
      <h2 class="screen-hidden">Decisions</h2>
      <DecisionsList />
      <DecisionDetails />
    </div>
  )
}

const DecisionsList = () => {
  const
    state = useContext(AppState),
    { api: { decision: { definition, dmn } } } = state,
    { params } = useRoute(),
    { route } = useLocation(),
    reset_state = (decision_id) => {
      route(`/decisions/${decision_id}`)
      definition.value = null
      dmn.value = null
    }

  return (
    <div class="list-wrapper">
      <h3 class="screen-hidden">Queried decisions</h3>
      <ul class="list">
        {state.api.decision.definitions.value?.data?.map((decision) => (
          <li
            key={decision.id}
            class={params.decision_id === decision.id ? 'selected' : null}
          >
            <a href={`/decisions/${decision.id}`} onClick={() => reset_state(decision.id)}>
              <div class="title">
                {decision?.name || decision?.id}
              </div>
            </a>
          </li>
        )) ?? 'Loading...'}
      </ul>
    </div>
  )
}

const DecisionDetails = () => {
  const state = useContext(AppState),
    { api: { decision: { definition, dmn } } } = state,
    { params: { decision_id } } = useRoute()

  return <div class="p-2">
    <RequestState
      signal={definition}
      on_nothing={() => <p class="info-box">Select decision to view its details</p>}
      on_success={() => {
        const {
          id, key, name, version, versionTag, tenantId, deploymentId,
          decisionRequirementsDefinitionId, historyTimeToLive,
          resource
        } = definition.value.data

        return <div>
          <h3>Definition Details</h3>
          <dl>
            <dt>ID</dt>
            <dd>{id}</dd>
            <dt>Version</dt>
            <dd>{version}</dd>
            <dt>Version Tag</dt>
            <dd>{versionTag}</dd>
            <dt>Key</dt>
            <dd>{key}</dd>
            <dt>Name</dt>
            <dd>{name}</dd>
            <dt>Tenant ID</dt>
            <dd>{tenantId}</dd>
            <dt>Deployment ID</dt>
            <dd>{deploymentId}</dd>
            <dt>Decision Requirements Definition ID</dt>
            <dd>{decisionRequirementsDefinitionId}</dd>
            <dt>History Time To Live</dt>
            <dd>{historyTimeToLive}</dd>
          </dl>
        </div>
      }} />


    <div id="diagram-container" />

    <RequestState
      signal={dmn}
      on_nothing={() => <p class="info-box">Select decision to view its diagram</p>}
      on_success={() => <DmnViewer xml={dmn.value.data.dmnXml} container="#diagram-container" />} />
  </div>
}

export { DecisionsPage }
