package org.operaton.bpm.engine.impl.db;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

public class DbCamundaDataMigrate {

  public static void main(String[] args) {
    ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResourceDefault()
        .setDatabaseSchemaUpdate(ProcessEngineConfigurationImpl.DB_DATA_UPDATE_MIGRATE_FROM_CAMUNDA)
        .buildProcessEngine();
  }
}
