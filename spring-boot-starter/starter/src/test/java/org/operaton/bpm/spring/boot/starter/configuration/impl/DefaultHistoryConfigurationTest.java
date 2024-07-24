/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHistoryConfigurationTest {

  @Mock
  private SpringProcessEngineConfiguration springProcessEngineConfiguration;

  private OperatonBpmProperties operatonBpmProperties;

  private DefaultHistoryConfiguration defaultHistoryConfiguration;

  @Before
  public void before() {
    operatonBpmProperties = new OperatonBpmProperties();
    defaultHistoryConfiguration = new DefaultHistoryConfiguration();
    defaultHistoryConfiguration.operatonBpmProperties = operatonBpmProperties;
  }

  @Test
  public void defaultHistoryLevelTest() {
    defaultHistoryConfiguration.preInit(springProcessEngineConfiguration);
    verify(springProcessEngineConfiguration, times(1)).setHistory(HistoryLevel.HISTORY_LEVEL_FULL.getName());
  }

  @Test
  public void historyLevelTest() {
    operatonBpmProperties.setHistoryLevel(HistoryLevel.HISTORY_LEVEL_AUDIT.getName());
    defaultHistoryConfiguration.preInit(springProcessEngineConfiguration);
    verify(springProcessEngineConfiguration).setHistory(HistoryLevel.HISTORY_LEVEL_AUDIT.getName());
  }

  @Test
  public void noHistoryEventHandlerTest() {
    defaultHistoryConfiguration.preInit(springProcessEngineConfiguration);
    verify(springProcessEngineConfiguration, times(0)).setHistoryEventHandler(Mockito.any(HistoryEventHandler.class));
  }

  @Test
  public void historyEventHandlerTest() {
    HistoryEventHandler historyEventHandlerMock = mock(HistoryEventHandler.class);
    List customHandlersList = mock(List.class);
    when(springProcessEngineConfiguration.getCustomHistoryEventHandlers()).thenReturn(customHandlersList);

    defaultHistoryConfiguration.historyEventHandler = historyEventHandlerMock;
    springProcessEngineConfiguration.setCustomHistoryEventHandlers(customHandlersList);
    defaultHistoryConfiguration.preInit(springProcessEngineConfiguration);

    verify(springProcessEngineConfiguration).getCustomHistoryEventHandlers();
    verify(springProcessEngineConfiguration.getCustomHistoryEventHandlers()).add(historyEventHandlerMock);
  }
}
