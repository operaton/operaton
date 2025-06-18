package org.operaton.bpm.engine.impl.diagnostics;

public class PlatformDiagnosticsRegistry {

  protected static String applicationServer;

  private PlatformDiagnosticsRegistry() {
  }

  public static synchronized String getApplicationServer() {
    return applicationServer;
  }

  public static synchronized void setApplicationServer(String applicationServerVersion) {
    if (applicationServer == null) {
      applicationServer = applicationServerVersion;
    }
  }

  public static synchronized void clear() {
    applicationServer = null;
  }
}
