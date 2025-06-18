package org.operaton.bpm.engine.impl.diagnostics;

/** Lightweight representation of the application server information. */
public class ApplicationServerInfo {
  protected String vendor;
  protected String version;

  public ApplicationServerInfo(String vendor, String version) {
    this.vendor = vendor;
    this.version = version;
  }

  public ApplicationServerInfo(String version) {
    this.vendor = org.operaton.bpm.engine.impl.util.ParseUtil.parseServerVendor(version);
    this.version = version;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
