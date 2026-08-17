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
};

// State branch — mounted at state.api.plugins.metrics.
const make_signals = () => ({
  version: signal(null),
  process_starts: signal(null),
  flow_nodes: signal(null),
});

const MetricValue = ({ signal: signl, format = (v) => v }) => (
  <RequestState
    signal={signl}
    on_success={() => <p class="metric-value">{format(signl.value.data)}</p>}
  />
);

const MetricsPage = () => {
  const { state, api: metrics, signals } = use_plugin_api(PLUGIN_ID);
  const [t] = useTranslation();

  useEffect(() => {
    metrics.version(state);
    metrics.process_starts(state);
    metrics.flow_nodes(state);
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
