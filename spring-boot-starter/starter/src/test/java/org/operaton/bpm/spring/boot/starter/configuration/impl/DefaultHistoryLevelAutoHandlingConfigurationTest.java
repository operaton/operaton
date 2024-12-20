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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.jdbc.HistoryLevelDeterminator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHistoryLevelAutoHandlingConfigurationTest {

  @Mock
  private SpringProcessEngineConfiguration springProcessEngineConfiguration;

  @Mock
  private HistoryLevelDeterminator historyLevelDeterminator;

  private OperatonBpmProperties operatonBpmProperties;

  private DefaultHistoryLevelAutoHandlingConfiguration historyLevelAutoHandlingConfiguration;

  @Before
  public void before() {
    operatonBpmProperties = new OperatonBpmProperties();
    historyLevelAutoHandlingConfiguration = new DefaultHistoryLevelAutoHandlingConfiguration();
    historyLevelAutoHandlingConfiguration.operatonBpmProperties = operatonBpmProperties;
    historyLevelAutoHandlingConfiguration.historyLevelDeterminator = historyLevelDeterminator;
  }

  @Test
  public void acceptTest() {
    when(historyLevelDeterminator.determineHistoryLevel()).thenReturn("audit");
    historyLevelAutoHandlingConfiguration.preInit(springProcessEngineConfiguration);
    verify(historyLevelDeterminator).determineHistoryLevel();
    verify(springProcessEngineConfiguration).setHistory(Mockito.anyString());
  }

  @Test
  public void notAcceptTest() {
    when(historyLevelDeterminator.determineHistoryLevel()).thenReturn(null);
    historyLevelAutoHandlingConfiguration.preInit(springProcessEngineConfiguration);
    verify(historyLevelDeterminator).determineHistoryLevel();
    verify(springProcessEngineConfiguration, times(0)).setHistory(Mockito.anyString());
  }

}
