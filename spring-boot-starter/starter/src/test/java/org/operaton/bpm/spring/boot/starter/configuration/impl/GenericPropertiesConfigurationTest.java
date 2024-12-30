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

import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.util.SpringBootStarterException;

import org.junit.Before;

import static org.junit.Assert.assertEquals;

public class GenericPropertiesConfigurationTest {

  private SpringProcessEngineConfiguration processEngineConfiguration;
  private GenericPropertiesConfiguration genericPropertiesConfiguration;
  private OperatonBpmProperties operatonBpmProperties;

  @Before
  public void init() {
    processEngineConfiguration = new SpringProcessEngineConfiguration();
    genericPropertiesConfiguration = new GenericPropertiesConfiguration();
    operatonBpmProperties = new OperatonBpmProperties();
    genericPropertiesConfiguration.operatonBpmProperties = operatonBpmProperties;
  }

  @Test
  public void genericBindingTestWithType() {
    final int batchPollTimeValue = Integer.MAX_VALUE;
    operatonBpmProperties.getGenericProperties().getProperties().put("batch-poll-time", batchPollTimeValue);
    genericPropertiesConfiguration.preInit(processEngineConfiguration);
    assertEquals(batchPollTimeValue, processEngineConfiguration.getBatchPollTime());
  }

  @Test
  public void genericBindingTestAsString() {
    final int batchPollTimeValue = Integer.MAX_VALUE;
    operatonBpmProperties.getGenericProperties().getProperties().put("batch-poll-time", Integer.valueOf(batchPollTimeValue).toString());
    genericPropertiesConfiguration.preInit(processEngineConfiguration);
    assertEquals(batchPollTimeValue, processEngineConfiguration.getBatchPollTime());
  }

  @Test(expected = SpringBootStarterException.class)
  public void genericBindingTestWithNotExistingProperty() {
    final int dontExistValue = Integer.MAX_VALUE;
    operatonBpmProperties.getGenericProperties().getProperties().put("dont-exist", dontExistValue);
    genericPropertiesConfiguration.preInit(processEngineConfiguration);
  }
}
