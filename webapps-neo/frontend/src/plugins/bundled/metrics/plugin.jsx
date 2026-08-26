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
};

// State branch — mounted at state.api.plugins.metrics.
const make_signals = () => ({
  version: signal(null),
  process_starts: signal(null),
  flow_nodes: signal(null),
  running: signal(null),
  completed: signal(null),
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

const MetricsPage = () => {
  const { state, api: metrics, signals } = use_plugin_api(PLUGIN_ID);
  const [t] = useTranslation();

  useEffect(() => {
    metrics.version(state);
    metrics.process_starts(state);
    metrics.flow_nodes(state);
    metrics.running(state);
    metrics.completed(state);
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
      <section class="metrics-charts">
        <article class="metrics-snapshot">
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
