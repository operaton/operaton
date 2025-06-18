package org.operaton.bpm.engine.impl.diagnostics;

import java.util.Map;
import java.util.Set;

/** Simple container for collected diagnostics information. */
public class DiagnosticsData {
  protected String applicationServer;
  protected String operatonIntegration;
  protected Set<String> webapps;
  protected Map<String, Long> metrics;
  protected Map<String, Long> commands;

  public DiagnosticsData(String applicationServer, String operatonIntegration,
      Set<String> webapps, Map<String, Long> metrics, Map<String, Long> commands) {
    this.applicationServer = applicationServer;
    this.operatonIntegration = operatonIntegration;
    this.webapps = webapps;
    this.metrics = metrics;
    this.commands = commands;
  }

  public String getApplicationServer() {
    return applicationServer;
  }

  public String getOperatonIntegration() {
    return operatonIntegration;
  }

  public Set<String> getWebapps() {
    return webapps;
  }

  public Map<String, Long> getMetrics() {
    return metrics;
  }

  public Map<String, Long> getCommands() {
    return commands;
  }
}
