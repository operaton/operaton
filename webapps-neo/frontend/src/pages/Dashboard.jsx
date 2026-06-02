import { useContext } from "preact/hooks";
import { useTranslation } from "react-i18next";
import engine_rest, { RequestState } from "../api/engine_rest.jsx";
import { AppState } from "../state.js";

export const DashboardPage = () => {
  const state = useContext(AppState),
    [t] = useTranslation();

  if (state.api.task.list.value === null)
    void engine_rest.task.get_tasks(state);
  if (state.api.process.definition.list.value === null)
    void engine_rest.process_definition.list(state);
  if (state.api.deployment.all.value === null)
    void engine_rest.deployment.all(state);
  if (state.api.decision.definitions.value === null)
    void engine_rest.decision.get_decision_definitions(state);

  const username =
    state.auth.user.id.value ?? state.auth.credentials.value?.username;

  return (
    <main id="content" class="dashboard fade-in">
      <h2>
        {t("dashboard.greeting")}
        {username ? `, ${username}` : ""}
      </h2>
      <div>
        <DashboardCard
          title={t("nav.tasks")}
          href="/tasks"
          signal={state.api.task.list}
          render={(data) => {
            const tasks = data ?? [];
            return (
              <>
                <strong>{tasks.length}</strong>
                <span>{t("dashboard.open-tasks")}</span>
              </>
            );
          }}
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

      <section>
        <h3>{t("dashboard.open-incidents")}</h3>
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
      </section>

      <section>
        <header>
          <h3>{t("dashboard.recent-tasks")}</h3>
          <a href="/tasks">{t("dashboard.see-all-tasks")}</a>
        </header>
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
      </section>

      <section>
        <header>
          <h3>{t("dashboard.process-definitions")}</h3>
          <a href="/processes">{t("dashboard.see-all-processes")}</a>
        </header>
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
      </section>
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
