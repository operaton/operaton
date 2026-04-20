/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.DetermineHistoryLevelCmd;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyManager;

/**
 * Default implementation of {@link HistoryLevelSetupCommand}.
 *
 * <p>Verifies that the history level configured in
 * {@link ProcessEngineConfigurationImpl#getHistoryLevel()} matches the one persisted in the
 * database, writing the configured level to the database if none is present yet.
 *
 * @since 2.1
 */
public class DefaultHistoryLevelSetupCommand implements HistoryLevelSetupCommand {

  private static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  @Override
  public Void execute(CommandContext commandContext) {

    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();

    checkStartupLockExists(commandContext);

    HistoryLevel databaseHistoryLevel = new DetermineHistoryLevelCmd(processEngineConfiguration.getHistoryLevels()).execute(commandContext);
    determineAutoHistoryLevel(processEngineConfiguration, databaseHistoryLevel);

    HistoryLevel configuredHistoryLevel = processEngineConfiguration.getHistoryLevel();

    if (databaseHistoryLevel == null) {

      acquireExclusiveLock(commandContext);
      databaseHistoryLevel = new DetermineHistoryLevelCmd(processEngineConfiguration.getHistoryLevels()).execute(commandContext);

      if (databaseHistoryLevel == null) {
        LOG.noHistoryLevelPropertyFound();
        HistoryLevelUtils.dbCreateHistoryLevel(commandContext);
      }
    } else {
      if (configuredHistoryLevel.getId() != databaseHistoryLevel.getId()) {
        throw new ProcessEngineException("historyLevel mismatch: configuration says %s and database says %s".formatted(configuredHistoryLevel, databaseHistoryLevel));
      }
    }

    return null;
  }

  protected void determineAutoHistoryLevel(ProcessEngineConfigurationImpl engineConfiguration, HistoryLevel databaseHistoryLevel) {
    HistoryLevel configuredHistoryLevel = engineConfiguration.getHistoryLevel();

    if (configuredHistoryLevel == null
        && ProcessEngineConfiguration.HISTORY_AUTO.equals(engineConfiguration.getHistory())) {

      // automatically determine history level or use default AUDIT
      if (databaseHistoryLevel != null) {
        engineConfiguration.setHistoryLevel(databaseHistoryLevel);
      } else {
        engineConfiguration.setHistoryLevel(engineConfiguration.getDefaultHistoryLevel());
      }
    }
  }

  protected void checkStartupLockExists(CommandContext commandContext) {
    PropertyEntity historyStartupProperty = commandContext.getPropertyManager().findPropertyById("startup.lock");
    if (historyStartupProperty == null) {
      LOG.noStartupLockPropertyFound();
    }
  }

  protected void acquireExclusiveLock(CommandContext commandContext) {
    PropertyManager propertyManager = commandContext.getPropertyManager();
    propertyManager.acquireExclusiveLockForStartup();
  }
}
