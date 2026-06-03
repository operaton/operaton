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

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.entitymanager.DbEntityManager;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyEntity;

/**
 * Static utility methods for reading and writing the history level property in the database.
 *
 * @since 2.1
 */
public final class HistoryLevelUtils {

  private static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  private HistoryLevelUtils() {
    // utility class
  }

  /**
   * Returns the history level stored in the database, or {@code null} if none is found.
   */
  public static Integer databaseHistoryLevel(CommandContext commandContext) {
    try {
      PropertyEntity historyLevelProperty = commandContext.getPropertyManager().findPropertyById("historyLevel");
      return historyLevelProperty != null ? Integer.valueOf(historyLevelProperty.getValue()) : null;
    } catch (Exception e) {
      LOG.couldNotSelectHistoryLevel(e.getMessage());
      return null;
    }
  }

  /**
   * Inserts the currently configured history level into the database.
   */
  public static void dbCreateHistoryLevel(CommandContext commandContext) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    HistoryLevel configuredHistoryLevel = processEngineConfiguration.getHistoryLevel();
    PropertyEntity property = new PropertyEntity("historyLevel", Integer.toString(configuredHistoryLevel.getId()));
    commandContext.getSession(DbEntityManager.class).insert(property);
    LOG.creatingHistoryLevelPropertyInDatabase(configuredHistoryLevel);
  }
}
