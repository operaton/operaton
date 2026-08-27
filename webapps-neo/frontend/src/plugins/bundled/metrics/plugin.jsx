/**
 * Engine Metrics — the reference bundled plugin.
 *
 * A top-level PAGE (route + nav + hotkey + GoTo) at /plugin/metrics.
 *
 * It owns its API namespace (engine_rest.plugins.metrics.*), its state branch
 * (state.api.plugins.metrics.*), its translations and its styles — sharing no
 * host signals, which proves namespace isolation. Bundled plugins import the
 * host directly; remote no-build plugins use window.__OPERATON_PLUGIN_HOST__.
 */
import { useEffect } from "preact/hooks";
import { useTranslation } from "react-i18next";
import { signal } from "@preact/signals";
import { GET } from "../../../api/helper.jsx";
import { RequestState } from "../../../api/engine_rest.jsx";
import { PLUGIN_POINTS } from "../../points.js";
import { use_plugin_api } from "../../plugin_api.jsx";
import "./metrics.css";

const PLUGIN_ID = "metrics";

// URL-ready `startDate` param n months before now. The engine rejects the
// trailing "Z" from toISOString() (it wants a numeric offset, yyyy-MM-dd'T'HH:
// mm:ss.SSSZ), and the value must be encoded so the "+" survives the query
// string rather than decoding to a space.
const months_ago_param = (n) => {
  const date = new Date();
  date.setMonth(date.getMonth() - n);
  return encodeURIComponent(date.toISOString().replace("Z", "+0000"));
};

// Start-of-day (UTC) N days ago, encoded like months_ago_param. Midnight so the
// interval buckets line up with calendar days.
const days_ago_midnight_param = (n) => {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() - n);
  date.setUTCHours(0, 0, 0, 0);
  return encodeURIComponent(date.toISOString().replace("Z", "+0000"));
};

// API namespace — mounted at engine_rest.plugins.metrics. Each function follows
// the host convention (state, ...args) => VERB(url, state, signal), so auth,
// error shapes and RESPONSE_STATE all come for free from helper.jsx.
const api = {
  version: (state) =>
    GET("/version", state, state.api.plugins[PLUGIN_ID].version),
  process_starts: (state) =>
    GET(
      `/metrics/root-process-instance-start/sum?startDate=${months_ago_param(12)}`,
      state,
      state.api.plugins[PLUGIN_ID].process_starts,
    ),
  flow_nodes: (state) =>
    GET(
      `/metrics/activity-instance-start/sum?startDate=${months_ago_param(12)}`,
      state,
      state.api.plugins[PLUGIN_ID].flow_nodes,
    ),
  // Snapshot for the "running vs. completed" donut. Running comes from the
  // runtime (currently active), completed from history (`finished` = no longer
  // running, i.e. completed or terminated).
  running: (state) =>
    GET("/process-instance/count", state, state.api.plugins[PLUGIN_ID].running),
  completed: (state) =>
    GET(
      "/history/process-instance/count?finished=true",
      state,
      state.api.plugins[PLUGIN_ID].completed,
    ),
  // Daily process-start time series (last 30 days) for the chart. Uses
  // root-process-instance-start so only top-level starts count, not call activities.
  activity_series: (state) =>
    GET(
      `/metrics?name=root-process-instance-start&startDate=${days_ago_midnight_param(29)}&interval=86400`,
      state,
      state.api.plugins[PLUGIN_ID].activity_series,
    ),
  // Per-definition runtime counts (+ incidents) for the "top processes" ranking.
  definition_stats: (state) =>
    GET(
      "/process-definition/statistics?incidents=true",
      state,
      state.api.plugins[PLUGIN_ID].definition_stats,
    ),
  // Completed (historic) user-task counts by task name for the "top tasks" ranking.
  top_tasks: (state) =>
    GET(
      "/history/task/report?reportType=count&groupBy=taskName",
      state,
      state.api.plugins[PLUGIN_ID].top_tasks,
    ),
};

// State branch — mounted at state.api.plugins.metrics.
const make_signals = () => ({
  version: signal(null),
  process_starts: signal(null),
  flow_nodes: signal(null),
  running: signal(null),
  completed: signal(null),
  activity_series: signal(null),
  definition_stats: signal(null),
  top_tasks: signal(null),
});

const MetricValue = ({ signal: signl, format = (v) => v }) => (
  <RequestState
    signal={signl}
    on_success={() => <p class="metric-value">{format(signl.value.data)}</p>}
  />
);

// Two-segment SVG donut. The circle's circumference is normalised to 100
// (r = 100 / 2π) so dash lengths are read straight as percentages. Segments are
// laid clockwise from 12 o'clock: `completed` first (offset 25), then `running`
// starting where it ends (offset 25 − completed%).
const Donut = ({ running, completed }) => {
  const total = running + completed;
  const completed_pct = total ? (completed / total) * 100 : 0;
  const running_pct = total ? (running / total) * 100 : 0;
  return (
    <svg class="metrics-donut" viewBox="0 0 42 42" aria-hidden="true">
      <circle class="donut-track" cx="21" cy="21" r="15.91549" />
      <circle
        class="donut-seg donut-completed"
        cx="21"
        cy="21"
        r="15.91549"
        stroke-dasharray={`${completed_pct} ${100 - completed_pct}`}
        stroke-dashoffset="25"
      />
      <circle
        class="donut-seg donut-running"
        cx="21"
        cy="21"
        r="15.91549"
        stroke-dasharray={`${running_pct} ${100 - running_pct}`}
        stroke-dashoffset={`${25 - completed_pct}`}
      />
      <text class="donut-center" x="21" y="21">
        {total}
      </text>
    </svg>
  );
};

// Turn the sparse metric buckets (only days with activity are returned) into a
// gap-free daily series of the last `days`, missing days filled with 0.
const build_activity_series = (buckets, days = 30) => {
  const by_day = {};
  for (const b of buckets) by_day[b.timestamp.slice(0, 10)] = b.value;
  const series = [];
  const day = new Date();
  day.setUTCHours(0, 0, 0, 0);
  day.setUTCDate(day.getUTCDate() - (days - 1));
  for (let i = 0; i < days; i++) {
    const key = day.toISOString().slice(0, 10);
    series.push({ key, value: by_day[key] ?? 0 });
    day.setUTCDate(day.getUTCDate() + 1);
  }
  return series;
};

// "2026-08-26" -> "26.08."
const short_day = (key) => `${key.slice(8, 10)}.${key.slice(5, 7)}.`;

const ActivityChart = ({ buckets }) => {
  const [t] = useTranslation();
  const series = build_activity_series(buckets);
  const max = Math.max(1, ...series.map((p) => p.value));
  const total = series.reduce((sum, p) => sum + p.value, 0);
  if (total === 0) {
    return <p class="fade-in">{t("plugins.metrics.no-activity")}</p>;
  }
  return (
    <>
      <div
        class="activity-chart"
        role="img"
        aria-label={t("plugins.metrics.activity")}
      >
        {series.map((p) => (
          <div class="activity-col" key={p.key}>
            <span class="activity-tip">{`${short_day(p.key)} ${p.value}`}</span>
            <div
              class="activity-bar"
              style={{ blockSize: `${(p.value / max) * 100}%` }}
            />
          </div>
        ))}
      </div>
      <div class="activity-axis">
        <span>{short_day(series[0].key)}</span>
        <span>{short_day(series[series.length - 1].key)}</span>
      </div>
    </>
  );
};

// Collapse the per-definition statistics (one row per version) into one row per
// process key: sum running instances and incidents across versions, and keep the
// newest version's id as the link target.
const top_processes = (stats, limit = 5) => {
  const by_key = {};
  for (const row of stats) {
    const def = row.definition;
    const entry = (by_key[def.key] ??= {
      key: def.key,
      name: def.name || def.key,
      instances: 0,
      incidents: 0,
      version: -1,
      definition_id: def.id,
    });
    entry.instances += row.instances;
    entry.incidents += (row.incidents ?? []).reduce(
      (sum, i) => sum + i.incidentCount,
      0,
    );
    if (def.version > entry.version) {
      entry.version = def.version;
      entry.definition_id = def.id;
    }
  }
  return Object.values(by_key)
    .filter((e) => e.instances > 0)
    .sort((a, b) => b.instances - a.instances)
    .slice(0, limit);
};

const TopProcesses = ({ stats }) => {
  const [t] = useTranslation();
  const rows = top_processes(stats);
  if (rows.length === 0) {
    return <p class="fade-in">{t("plugins.metrics.no-running")}</p>;
  }
  const max = rows[0].instances;
  return (
    <ol class="ranking">
      {rows.map((r) => (
        <li key={r.key}>
          <a href={`/processes/${r.definition_id}`}>
            <span class="ranking-name" title={r.name}>
              {r.name}
            </span>
            <span class="ranking-bar-track">
              <span
                class="ranking-bar"
                style={{ inlineSize: `${(r.instances / max) * 100}%` }}
              >
                {r.incidents > 0 && (
                  <span
                    class="ranking-bar-incidents"
                    style={{
                      inlineSize: `${Math.min(r.incidents / r.instances, 1) * 100}%`,
                    }}
                  />
                )}
              </span>
            </span>
            <span class="ranking-meta">
              {r.incidents > 0 && (
                <span class="ranking-incidents">({r.incidents})</span>
              )}
              <span class="ranking-count">{r.instances}</span>
            </span>
          </a>
        </li>
      ))}
    </ol>
  );
};

// Top completed user tasks by volume. Rows come pre-grouped per task name (per
// definition); we just sort and take the leaders. Reuses the `.ranking` styles.
const TopTasks = ({ rows }) => {
  const [t] = useTranslation();
  const top = [...rows].sort((a, b) => b.count - a.count).slice(0, 5);
  if (top.length === 0) {
    return <p class="fade-in">{t("plugins.metrics.no-tasks")}</p>;
  }
  const max = top[0].count;
  return (
    <ol class="ranking">
      {top.map((r) => (
        <li key={`${r.taskName}@${r.processDefinitionId}`}>
          <a href={`/processes/${r.processDefinitionId}`}>
            <span
              class="ranking-name"
              title={`${r.taskName} · ${r.processDefinitionName}`}
            >
              {r.taskName}
              <span class="ranking-sub"> · {r.processDefinitionName}</span>
            </span>
            <span class="ranking-bar-track">
              <span
                class="ranking-bar"
                style={{ inlineSize: `${(r.count / max) * 100}%` }}
              />
            </span>
            <span class="ranking-meta">
              <span class="ranking-count">{r.count}</span>
            </span>
          </a>
        </li>
      ))}
    </ol>
  );
};

const MetricsPage = () => {
  const { state, api: metrics, signals } = use_plugin_api(PLUGIN_ID);
  const [t] = useTranslation();

  useEffect(() => {
    metrics.version(state);
    metrics.process_starts(state);
    metrics.flow_nodes(state);
    metrics.running(state);
    metrics.completed(state);
    metrics.activity_series(state);
    metrics.definition_stats(state);
    metrics.top_tasks(state);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <main id="content" class="metrics-page fade-in">
      <h1>{t("plugins.metrics.title")}</h1>
      <section class="metrics-cards">
        <article>
          <h2>{t("plugins.metrics.version")}</h2>
          <MetricValue signal={signals.version} format={(d) => d.version} />
        </article>
        <article>
          <h2>{t("plugins.metrics.process-starts")}</h2>
          <MetricValue
            signal={signals.process_starts}
            format={(d) => d.result}
          />
        </article>
        <article>
          <h2>{t("plugins.metrics.flow-nodes")}</h2>
          <MetricValue signal={signals.flow_nodes} format={(d) => d.result} />
        </article>
      </section>
      <section class="metrics-bento">
        <article class="bento-3">
          <h2>{t("plugins.metrics.activity")}</h2>
          <RequestState
            signal={signals.activity_series}
            on_success={() => (
              <ActivityChart buckets={signals.activity_series.value.data} />
            )}
          />
        </article>
        <article class="metrics-ranking bento-2">
          <h2>{t("plugins.metrics.top-processes")}</h2>
          <RequestState
            signal={signals.definition_stats}
            on_success={() => <TopProcesses stats={signals.definition_stats.value.data} />}
          />
        </article>
        <article class="metrics-snapshot bento-2">
          <h2>{t("plugins.metrics.snapshot")}</h2>
          <RequestState
            signal={[signals.running, signals.completed]}
            on_success={() => {
              const running = signals.running.value.data.count;
              const completed = signals.completed.value.data.count;
              return (
                <div class="snapshot-body">
                  <Donut running={running} completed={completed} />
                  <ul class="snapshot-legend">
                    <li>
                      <a href="/processes">
                        <span class="dot dot-running" />
                        {t("plugins.metrics.running")}
                        <b>{running}</b>
                      </a>
                    </li>
                    <li>
                      <a href="/processes?history=true">
                        <span class="dot dot-completed" />
                        {t("plugins.metrics.completed")}
                        <b>{completed}</b>
                      </a>
                    </li>
                  </ul>
                </div>
              );
            }}
          />
        </article>
        <article class="metrics-ranking bento-3">
          <h2>{t("plugins.metrics.top-tasks")}</h2>
          <RequestState
            signal={signals.top_tasks}
            on_success={() => <TopTasks rows={signals.top_tasks.value.data} />}
          />
        </article>
      </section>
    </main>
  );
};

const translations = {
  "en-US": {
    plugins: {
      metrics: {
        nav: "Metrics",
        title: "Engine Metrics",
        version: "Engine version",
        "process-starts": "Process starts (12 mo)",
        "flow-nodes": "Flow nodes executed (12 mo)",
        snapshot: "Process instances: running vs. completed",
        running: "Running",
        completed: "Completed",
        activity: "Process starts per day (last 30 days)",
        "no-activity": "No process starts in the last 30 days.",
        "top-processes": "Top processes by running instances",
        "no-running": "No running instances.",
        "top-tasks": "Top user tasks by volume",
        "no-tasks": "No completed user tasks.",
      },
    },
  },
  "de-DE": {
    plugins: {
      metrics: {
        nav: "Kennzahlen",
        title: "Engine-Kennzahlen",
        version: "Engine-Version",
        "process-starts": "Prozessstarts (12 Mon.)",
        "flow-nodes": "Ausgeführte Flow-Knoten (12 Mon.)",
        snapshot: "Prozessinstanzen: laufend vs. abgeschlossen",
        running: "Laufend",
        completed: "Abgeschlossen",
        activity: "Prozessstarts pro Tag (letzte 30 Tage)",
        "no-activity": "Keine Prozessstarts in den letzten 30 Tagen.",
        "top-processes": "Top-Prozesse nach laufenden Instanzen",
        "no-running": "Keine laufenden Instanzen.",
        "top-tasks": "Top-User-Tasks nach Aufkommen",
        "no-tasks": "Keine abgeschlossenen User-Tasks.",
      },
    },
  },
};

export default [
  {
    id: PLUGIN_ID,
    point: PLUGIN_POINTS.PAGE,
    priority: 0,
    properties: {
      path: "/plugin/metrics",
      href: "/plugin/metrics",
      nameKey: "plugins.metrics.nav",
      hotkey: "alt+shift+8",
    },
    Component: MetricsPage,
    api,
    signals: make_signals,
    translations,
  },
];
