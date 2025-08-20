/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonHistoryConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultHistoryConfiguration extends AbstractOperatonConfiguration implements OperatonHistoryConfiguration {

  protected HistoryEventHandler historyEventHandler;

  public DefaultHistoryConfiguration(OperatonBpmProperties operatonBpmProperties,
                                     HistoryEventHandler historyEventHandler) {
    super(operatonBpmProperties);
    this.historyEventHandler = historyEventHandler;
  }

  @Override
  public void preInit(SpringProcessEngineConfiguration configuration) {
    String historyLevel = operatonBpmProperties.getHistoryLevel();
    if (historyLevel != null) {
      configuration.setHistory(historyLevel);
    }
    if (historyEventHandler != null) {
      logger.debug("registered history event handler: {}", historyEventHandler.getClass());
      configuration.getCustomHistoryEventHandlers().add(historyEventHandler);
    }
  }

}
