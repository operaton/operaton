import { useContext, useEffect } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { AppState } from "../state.js";
import { useLocation, useRoute } from "preact-iso";
import engine_rest, { RequestState } from "../api/engine_rest.jsx";
import { DmnViewer } from "../components/DMNViewer.jsx";
import { ListFilter } from "../components/ListFilter.jsx";
import { ListSearch } from "../components/ListSearch.jsx";
import { app_path } from "../config.js";
import { parse_list_query, write_list_query } from "../helper/list_query.js";

const SORT_OPTIONS = [
  { key: "name", nameKey: "decisions.sort.name" },
  { key: "key", nameKey: "decisions.sort.key" },
  { key: "category", nameKey: "decisions.sort.category" },
  { key: "id", nameKey: "decisions.sort.id" },
  { key: "version", nameKey: "decisions.sort.version" },
  { key: "deploymentId", nameKey: "decisions.sort.deploymentId" },
  { key: "tenantId", nameKey: "decisions.sort.tenantId" },
];

const load_decisions = (state, query) => {
  const { sortBy, sortOrder, criteria } = parse_list_query(query);
  const params = {
    ...criteria,
    ...(sortBy ? { sortBy } : {}),
    ...(sortOrder ? { sortOrder } : {}),
  };
  void engine_rest.decision.get_decision_definitions(state, params);
};

const DecisionsPage = () => {
  const state = useContext(AppState),
    {
      api: {
        decision: { definition, dmn },
      },
    } = state,
    {
      params: { decision_id },
      query,
    } = useRoute();

  useEffect(() => {
    load_decisions(state, query);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(query)]);

  useEffect(() => {
    if (decision_id) {
      void engine_rest.decision.get_decision_definition(state, decision_id);
      void engine_rest.decision.get_dmn_xml(state, decision_id);
    }
    // Clear stale per-decision data so navigating between decisions doesn't
    // render the previous decision's metadata or DMN diagram briefly.
    return () => {
      definition.value = null;
      dmn.value = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decision_id]);

  return (
    <main id="content" class="decisions fade-in">
      <DecisionsList />
      <DecisionDetails />
    </main>
  );
};

const DecisionsList = () => {
  const state = useContext(AppState),
    {
      api: {
        decision: { definitions },
      },
    } = state,
    { params, query } = useRoute(),
    { route } = useLocation(),
    [t] = useTranslation();

  const parsed = parse_list_query(query);
  const list_current = {
    sortBy: parsed.sortBy ?? "name",
    sortOrder: parsed.sortOrder ?? "asc",
  };
  // The name search rides the generic list criteria as a nameLike term.
  const search_term = (parsed.criteria.nameLike ?? "").replace(/^%+|%+$/g, "");

  const apply_patch = (patch) => {
    route(write_list_query(window.location.href, patch), true);
  };
  const on_search = (term) =>
    apply_patch({ criteria: term ? { nameLike: `%${term}%` } : {} });

  return (
    <div id="decision-list">
      <h2 class="screen-hidden">{t("decisions.queried-decisions")}</h2>
      <ListSearch
        label={t("decisions.search")}
        value={search_term}
        on_search={on_search}
      />
      <ListFilter
        sort_options={SORT_OPTIONS}
        current={list_current}
        defaults={{ sortBy: "name", sortOrder: "asc" }}
        on_change={apply_patch}
      />
      <RequestState
        signal={definitions}
        on_success={() => (
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
                  aria-selected={params.decision_id === decision.id}
                >
                  <td>
                    <a href={app_path(`/decisions/${decision.id}`)}>
                      {decision?.name || decision?.id}
                    </a>
                  </td>
                  <td>{decision.key}</td>
                  <td>{decision.version}</td>
                  <td>{decision.versionTag}</td>
                  <td>{decision.historyTimeToLive}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      />
    </div>
  );
};

const DecisionDetails = () => {
  const {
      params: { decision_id, panel },
    } = useRoute(),
    [t] = useTranslation();

  if (!decision_id) {
    return (
      <div id="decision-details">
        <p class="info-box">{t("decisions.select-details")}</p>
      </div>
    );
  }

  return (
    <div id="decision-details">
      <nav>
        <menu>
          <li>
            <a
              href={app_path(`/decisions/${decision_id}`)}
              aria-current={!panel ? "page" : undefined}
            >
              {t("decisions.tabs.details")}
            </a>
          </li>
          <li>
            <a
              href={app_path(`/decisions/${decision_id}/instances`)}
              aria-current={panel === "instances" ? "page" : undefined}
            >
              {t("decisions.tabs.instances")}
            </a>
          </li>
        </menu>
      </nav>
      {panel === "instances" ? <DecisionInstances /> : <DecisionDefinition />}
    </div>
  );
};

const DecisionDefinition = () => {
  const state = useContext(AppState),
    {
      api: {
        decision: { definition, dmn },
      },
    } = state,
    [t] = useTranslation();

  return (
    <>
      <RequestState
        signal={definition}
        on_nothing={() => (
          <p class="info-box">{t("decisions.select-details")}</p>
        )}
        on_success={() => {
          const {
            id,
            key,
            name,
            version,
            versionTag,
            tenantId,
            deploymentId,
            decisionRequirementsDefinitionId,
            historyTimeToLive,
          } = definition.value.data;

          return (
            <div>
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
          );
        }}
      />

      <div id="diagram-container" />

      <RequestState
        signal={dmn}
        on_nothing={() => (
          <p class="info-box">{t("decisions.select-diagram")}</p>
        )}
        on_success={() => (
          <DmnViewer
            xml={dmn.value.data.dmnXml}
            container="#diagram-container"
          />
        )}
      />
    </>
  );
};

const DecisionInstances = () => {
  const state = useContext(AppState),
    {
      api: {
        decision: { instances, instance },
      },
    } = state,
    {
      params: { decision_id },
    } = useRoute(),
    [t] = useTranslation();

  useEffect(() => {
    void engine_rest.decision.get_decision_instances(state, decision_id);
    instance.value = null;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decision_id]);

  const load_more = () =>
    void engine_rest.decision.get_decision_instances(
      state,
      decision_id,
      instances.value?.data?.length ?? 0,
    );

  const select = (id) =>
    void engine_rest.decision.get_decision_instance(state, id);

  return (
    <div>
      <h3>{t("decisions.tabs.instances")}</h3>
      <RequestState
        signal={instances}
        on_success={() => {
          const rows = instances.value?.data ?? [];
          if (rows.length === 0)
            return <p class="info-box">{t("decisions.instances.empty")}</p>;
          return (
            <>
              <table>
                <thead>
                  <tr>
                    <th>{t("common.id")}</th>
                    <th>{t("decisions.instances.evaluation-time")}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((di) => (
                    <tr key={di.id}>
                      <td>
                        <button
                          type="button"
                          class="link"
                          onClick={() => select(di.id)}
                        >
                          {di.id.substring(0, 8)}
                        </button>
                      </td>
                      <td>
                        <time datetime={di.evaluationTime}>
                          {di.evaluationTime?.substring(0, 19)}
                        </time>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {instances.value?.hasMore && (
                <div class="button-group">
                  <button type="button" onClick={load_more}>
                    {t("common.load-more")}
                  </button>
                </div>
              )}
            </>
          );
        }}
      />
      <DecisionInstanceDetail />
    </div>
  );
};

const DecisionInstanceDetail = () => {
  const state = useContext(AppState),
    {
      api: {
        decision: { instance },
      },
    } = state,
    [t] = useTranslation();

  if (instance.value?.data == null) return null;

  const { inputs = [], outputs = [] } = instance.value.data;

  return (
    <div>
      <h3>{t("decisions.instances.inputs")}</h3>
      <table>
        <thead>
          <tr>
            <th>{t("common.name")}</th>
            <th>{t("common.type")}</th>
            <th>{t("common.value")}</th>
          </tr>
        </thead>
        <tbody>
          {inputs.map((i) => (
            <tr key={i.id}>
              <td>{i.clauseName ?? i.clauseId}</td>
              <td>{i.type}</td>
              <td>{String(i.value)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <h3>{t("decisions.instances.outputs")}</h3>
      <table>
        <thead>
          <tr>
            <th>{t("common.name")}</th>
            <th>{t("common.type")}</th>
            <th>{t("common.value")}</th>
          </tr>
        </thead>
        <tbody>
          {outputs.map((o) => (
            <tr key={o.id}>
              <td>{o.clauseName ?? o.variableName ?? o.clauseId}</td>
              <td>{o.type}</td>
              <td>{String(o.value)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export { DecisionsPage };
