import { useContext, useEffect } from "preact/hooks";
import { useSignal } from "@preact/signals";
import { useTranslation } from "react-i18next";
import engine_rest, { RequestState } from "../api/engine_rest.jsx";
import { AppState } from "../state.js";
import { plugins_for } from "../plugins/registry.js";
import { PLUGIN_POINTS } from "../plugins/points.js";

const HIDDEN_SECTIONS = "dashboard.hidden-sections";

const read_hidden = () => {
  try {
    return new Set(JSON.parse(localStorage.getItem(HIDDEN_SECTIONS) ?? "[]"));
  } catch {
    return new Set();
  }
};

/**
 * A dashboard section the reader can fold away. Which ones are folded is this
 * browser's own business, so it lives in localStorage and survives a reload.
 */
const CollapsibleSection = ({ id, title, link, children }) => {
  const hidden = useSignal(read_hidden().has(id));

  const toggle = (e) => {
    hidden.value = !e.currentTarget.open;
    const all = read_hidden();
    hidden.value ? all.add(id) : all.delete(id);
    try {
      localStorage.setItem(HIDDEN_SECTIONS, JSON.stringify([...all]));
    } catch {
      // A browser that refuses storage still folds, it just forgets.
    }
  };

  return (
    <details class="dashboard-section" open={!hidden.value} onToggle={toggle}>
      <summary>
        <h3>{title}</h3>
        {link}
      </summary>
      {children}
    </details>
  );
};

export const DashboardPage = () => {
  const state = useContext(AppState),
    [t] = useTranslation();

  // Load the dashboard cards once on mount. Fetching lives in an effect, not
  // the component body, so signal writes don't re-trigger fetches (see #102).
  useEffect(() => {
    if (state.api.task.list.value === null)
      void engine_rest.task.get_tasks(state);
    if (state.api.process.definition.list.value === null)
      void engine_rest.process_definition.list(state);
    if (state.api.deployment.all.value === null)
      void engine_rest.deployment.all(state);
    if (state.api.decision.definitions.value === null)
      void engine_rest.decision.get_decision_definitions(state);
    void engine_rest.task.summary(state);
    void engine_rest.task.by_group(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const username =
    state.auth.user.id.value ?? state.auth.credentials.value?.username;

  return (
    <main id="content" class="dashboard fade-in">
      <h1 class="screen-hidden">{t("dashboard.greeting")}</h1>
      <h2>
        {t("dashboard.greeting")}
        {username ? `, ${username}` : ""}
      </h2>
      <div>
        <DashboardCard
          title={t("nav.tasks")}
          href="/tasks"
          signal={state.api.task.summary}
          render={(data) => (
            <>
              <strong>{data?.total ?? 0}</strong>
              <span>{t("dashboard.open-tasks")}</span>
              <dl class="task-breakdown">
                <dt>{t("dashboard.tasks-assigned")}</dt>
                <dd>{data?.assigned ?? 0}</dd>
                <dt>{t("dashboard.tasks-unassigned")}</dt>
                <dd>{data?.unassigned ?? 0}</dd>
                <dt>{t("dashboard.tasks-unattended")}</dt>
                <dd>{data?.unattended ?? 0}</dd>
              </dl>
            </>
          )}
        />
        <DashboardCard
          title={t("nav.processes")}
          href="/processes"
          signal={state.api.process.definition.list}
          render={(data) => {
            const definitions = data ?? [];
            const incidents = definitions.reduce(
              (sum, d) => sum + (d.incidents?.length ?? 0),
              0,
            );
            return (
              <>
                <strong>{definitions.length}</strong>
                <span>{t("dashboard.deployed-definitions")}</span>
                {incidents > 0 && (
                  <strong class="incidents">
                    {incidents}{" "}
                    {incidents !== 1
                      ? t("dashboard.incidents")
                      : t("dashboard.incident")}
                  </strong>
                )}
              </>
            );
          }}
        />
        <DashboardCard
          title={t("nav.decisions")}
          href="/decisions"
          signal={state.api.decision.definitions}
          render={(data) => {
            const decisions = data ?? [];
            return (
              <>
                <strong>{decisions.length}</strong>
                <span>{t("dashboard.decision-definitions")}</span>
              </>
            );
          }}
        />
        <DashboardCard
          title={t("nav.deployments")}
          href="/deployments"
          signal={state.api.deployment.all}
          render={(data) => {
            const deployments = data ?? [];
            return (
              <>
                <strong>{deployments.length}</strong>
                <span>{t("dashboard.deployments")}</span>
              </>
            );
          }}
        />
      </div>

      <CollapsibleSection
        id="tasks-by-group"
        title={t("dashboard.tasks-by-group")}
      >
        <RequestState
          signal={state.api.task.by_group}
          on_success={() => {
            const groups = state.api.task.by_group.value?.data ?? [];
            if (groups.length === 0)
              return <p>{t("dashboard.no-open-tasks")}</p>;
            return (
              <>
                <table>
                  <thead>
                    <tr>
                      <th>{t("dashboard.group")}</th>
                      <th>{t("dashboard.count")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {groups.map(({ groupName, taskCount }) => (
                      <tr key={groupName ?? "none"}>
                        <td>{groupName ?? t("dashboard.no-group")}</td>
                        <td>{taskCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <p class="hint">{t("dashboard.multiple-groups-hint")}</p>
              </>
            );
          }}
        />
      </CollapsibleSection>

      <CollapsibleSection id="incidents" title={t("dashboard.open-incidents")}>
        <RequestState
          signal={state.api.process.definition.list}
          on_success={() => {
            const definitions =
                state.api.process.definition.list.value?.data ?? [],
              incidents = definitions.flatMap((d) =>
                (d.incidents ?? []).map((i) => ({
                  ...i,
                  processName: d.definition?.name ?? d.definition?.key,
                })),
              );
            if (incidents.length === 0)
              return <p>{t("dashboard.no-incidents")}</p>;
            return (
              <table>
                <thead>
                  <tr>
                    <th>{t("common.type")}</th>
                    <th>{t("dashboard.process")}</th>
                    <th>{t("dashboard.count")}</th>
                  </tr>
                </thead>
                <tbody>
                  {incidents.map((i, idx) => (
                    <tr key={idx}>
                      <td>{i.incidentType ?? "–"}</td>
                      <td>
                        <a href={`/processes/${i.processDefinitionId ?? ""}`}>
                          {i.processName ?? "–"}
                        </a>
                      </td>
                      <td>{i.incidentCount ?? 0}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            );
          }}
        />
      </CollapsibleSection>

      <CollapsibleSection
        id="recent-tasks"
        title={t("dashboard.recent-tasks")}
        link={<a href="/tasks">{t("dashboard.see-all-tasks")}</a>}
      >
        <RequestState
          signal={state.api.task.list}
          on_success={() => {
            const tasks = state.api.task.list.value?.data ?? [];
            if (tasks.length === 0)
              return <p>{t("dashboard.no-open-tasks")}</p>;
            return (
              <table>
                <thead>
                  <tr>
                    <th>{t("common.name")}</th>
                    <th>{t("dashboard.assignee")}</th>
                    <th>{t("dashboard.created")}</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.slice(0, 10).map((task) => (
                    <tr key={task.id}>
                      <td>
                        <a href={`/tasks/${task.id}`}>
                          {task.name ?? t("dashboard.unnamed")}
                        </a>
                      </td>
                      <td>{task.assignee ?? "–"}</td>
                      <td>
                        {task.created ? (
                          <time datetime={task.created}>
                            {new Date(task.created).toLocaleDateString()}
                          </time>
                        ) : (
                          "–"
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            );
          }}
        />
      </CollapsibleSection>

      <CollapsibleSection
        id="process-definitions"
        title={t("dashboard.process-definitions")}
        link={<a href="/processes">{t("dashboard.see-all-processes")}</a>}
      >
        <RequestState
          signal={state.api.process.definition.list}
          on_success={() => {
            const definitions =
              state.api.process.definition.list.value?.data ?? [];
            if (definitions.length === 0)
              return <p>{t("dashboard.no-process-definitions")}</p>;
            return (
              <table>
                <thead>
                  <tr>
                    <th>{t("common.name")}</th>
                    <th>{t("common.key")}</th>
                    <th>{t("dashboard.instances")}</th>
                    <th>{t("dashboard.open-incidents")}</th>
                  </tr>
                </thead>
                <tbody>
                  {definitions.slice(0, 10).map((d) => (
                    <tr key={d.id}>
                      <td>
                        <a href={`/processes/${d.id}`}>
                          {d.definition?.name ?? d.definition?.key ?? "–"}
                        </a>
                      </td>
                      <td>{d.definition?.key ?? "–"}</td>
                      <td>{d.instances ?? 0}</td>
                      <td>{d.incidents?.length ?? 0}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            );
          }}
        />
      </CollapsibleSection>
      {plugins_for(PLUGIN_POINTS.DASHBOARD_WIDGET).map((plugin) => (
        <plugin.Component key={plugin.id} />
      ))}
    </main>
  );
};

const DashboardCard = ({ title, href, signal, render }) => (
  <a href={href}>
    <h3>{title}</h3>
    <RequestState
      signal={signal}
      on_success={() => render(signal.value?.data)}
    />
  </a>
);
