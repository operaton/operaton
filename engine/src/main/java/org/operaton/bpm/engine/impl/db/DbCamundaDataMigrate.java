package org.operaton.bpm.engine.impl.db;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

public class DbCamundaDataMigrate {

  public static void main(String[] args) {
    ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResourceDefault()
        .setCamundaCompatibilityMode(ProcessEngineConfigurationImpl.DB_CAMUNDA_COMPATIBILITY_MIGRATE_DATA)
        .buildProcessEngine();
  }
}
