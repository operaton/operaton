import { useContext, useEffect } from 'preact/hooks'
import { useTranslation } from 'react-i18next'
import { AppState } from '../state.js'
import { useRoute } from 'preact-iso'
import engine_rest, { RequestState } from '../api/engine_rest.jsx'
import { DmnViewer } from '../components/DMNViewer.jsx'

const DecisionsPage = () => {
  const state = useContext(AppState),
    { api: { decision: { definition, dmn } } } = state,
    { params: { decision_id } } = useRoute()

  useEffect(() => {
    void engine_rest.decision.get_decision_definitions(state)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (decision_id) {
      void engine_rest.decision.get_decision_definition(state, decision_id)
      void engine_rest.decision.get_dmn_xml(state, decision_id)
    }
    // Clear stale per-decision data so navigating between decisions doesn't
    // render the previous decision's metadata or DMN diagram briefly.
    return () => {
      definition.value = null
      dmn.value = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decision_id])

  return (
    <main id="content" class="decisions fade-in">
      <DecisionsList />
      <DecisionDetails />
    </main>
  )
}

const DecisionsList = () => {
  const
    state = useContext(AppState),
    { api: { decision: { definitions } } } = state,
    { params } = useRoute(),
    [t] = useTranslation()

  return (
    <div id="decision-list">
      <h2 class="screen-hidden">{t("decisions.queried-decisions")}</h2>
      <RequestState
        signal={definitions}
        on_success={() =>
          <table>
            <thead>
              <tr>
                <th>{t("common.name")}</th>
                <th>{t("common.key")}</th>
                <th>{t("processes.version")}</th>
                <th>{t("decisions.version-tag")}</th>
                <th>{t("decisions.history-ttl")}</th>
              </tr>
            </thead>
            <tbody>
              {definitions.value?.data?.map((decision) => (
                <tr
                  key={decision.id}
                  class={params.decision_id === decision.id ? 'selected' : null}
                >
                  <td><a href={`/decisions/${decision.id}`}>
                    {decision?.name || decision?.id}
                  </a></td>
                  <td>{decision.key}</td>
                  <td>{decision.version}</td>
                  <td>{decision.versionTag}</td>
                  <td>{decision.historyTimeToLive}</td>
                </tr>
              ))}
            </tbody>
          </table>
        }
      />
    </div>
  )
}

const DecisionDetails = () => {
  const state = useContext(AppState),
    { api: { decision: { definition, dmn } } } = state,
    { params: { decision_id } } = useRoute(),
    [t] = useTranslation()

  return <div id="decision-details">
    <RequestState
      signal={definition}
      on_nothing={() => <p class="info-box">{t("decisions.select-details")}</p>}
      on_success={() => {
        const {
          id, key, name, version, versionTag, tenantId, deploymentId,
          decisionRequirementsDefinitionId, historyTimeToLive,
          resource
        } = definition.value.data

        return <div>
          <h3>{t("decisions.definition-details")}</h3>
          <dl>
            <dt>{t("common.id")}</dt>
            <dd>{id}</dd>
            <dt>{t("processes.version")}</dt>
            <dd>{version}</dd>
            <dt>{t("decisions.version-tag")}</dt>
            <dd>{versionTag}</dd>
            <dt>{t("common.key")}</dt>
            <dd>{key}</dd>
            <dt>{t("common.name")}</dt>
            <dd>{name}</dd>
            <dt>{t("processes.tenant-id")}</dt>
            <dd>{tenantId}</dd>
            <dt>{t("decisions.deployment-id")}</dt>
            <dd>{deploymentId}</dd>
            <dt>{t("decisions.decision-requirements-id")}</dt>
            <dd>{decisionRequirementsDefinitionId}</dd>
            <dt>{t("decisions.history-ttl")}</dt>
            <dd>{historyTimeToLive}</dd>
          </dl>
        </div>
      }} />


    <div id="diagram-container" />

    <RequestState
      signal={dmn}
      on_nothing={() => <p class="info-box">{t("decisions.select-diagram")}</p>}
      on_success={() => <DmnViewer xml={dmn.value.data.dmnXml} container="#diagram-container" />} />
  </div>
}

export { DecisionsPage }
