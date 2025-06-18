package org.operaton.bpm.engine.impl.diagnostics;

import static org.operaton.bpm.engine.management.Metrics.ACTIVTY_INSTANCE_START;
import static org.operaton.bpm.engine.management.Metrics.EXECUTED_DECISION_ELEMENTS;
import static org.operaton.bpm.engine.management.Metrics.EXECUTED_DECISION_INSTANCES;
import static org.operaton.bpm.engine.management.Metrics.ROOT_PROCESS_INSTANCE_START;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.impl.metrics.Meter;
import org.operaton.bpm.engine.impl.metrics.MetricsRegistry;
import org.operaton.bpm.engine.impl.metrics.util.MetricsUtil;

/**
 * Collects metrics and command counts for diagnostic purposes.
 */
public class DiagnosticsCollector {

  protected static final Set<String> METRICS_TO_REPORT = Set.of(
      ROOT_PROCESS_INSTANCE_START,
      EXECUTED_DECISION_INSTANCES,
      EXECUTED_DECISION_ELEMENTS,
      ACTIVTY_INSTANCE_START);

  protected DiagnosticsRegistry diagnosticsRegistry;
  protected MetricsRegistry metricsRegistry;

  public DiagnosticsCollector(DiagnosticsRegistry diagnosticsRegistry,
                              MetricsRegistry metricsRegistry) {
    this.diagnosticsRegistry = diagnosticsRegistry;
    this.metricsRegistry = metricsRegistry;
  }

  public DiagnosticsData collectData() {
    Map<String, Long> metrics = calculateMetrics();
    Map<String, Long> commands = fetchAndResetCommandCounts();
    return new DiagnosticsData(
        diagnosticsRegistry.getApplicationServer() != null ?
            diagnosticsRegistry.getApplicationServer().getVersion() : null,
        diagnosticsRegistry.getOperatonIntegration(),
        diagnosticsRegistry.getWebapps(),
        metrics,
        commands);
  }

  protected Map<String, Long> fetchAndResetCommandCounts() {
    Map<String, Long> commandsToReport = new HashMap<>();
    Map<String, CommandCounter> originalCounts = diagnosticsRegistry.getCommands();

    synchronized (originalCounts) {
      for (Map.Entry<String, CommandCounter> counter : originalCounts.entrySet()) {
        long occurrences = counter.getValue().get();
        commandsToReport.put(counter.getKey(), occurrences);
      }
    }

    return commandsToReport;
  }

  protected Map<String, Long> calculateMetrics() {
    Map<String, Long> metrics = new HashMap<>();

    if (metricsRegistry != null) {
      Map<String, Meter> telemetryMeters = metricsRegistry.getDiagnosticsMeters();

      for (String metricToReport : METRICS_TO_REPORT) {
        long value = telemetryMeters.get(metricToReport).get();
        metrics.put(MetricsUtil.resolvePublicName(metricToReport), value);
      }
    }

    return metrics;
  }
}
