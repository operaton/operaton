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

import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.NotifyAcquisitionRejectedJobsHandler;
import org.operaton.bpm.engine.impl.jobexecutor.RejectedJobsHandler;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobHandler;
import org.operaton.bpm.engine.impl.jobexecutor.NotifyAcquisitionRejectedJobsHandler;
import org.operaton.bpm.engine.impl.jobexecutor.RejectedJobsHandler;
import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@SpringBootTest(classes = { TestApplication.class }, webEnvironment = WebEnvironment.NONE)
class DefaultJobConfigurationTest {

  private final SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
  private DefaultJobConfiguration jobConfiguration;
  private final OperatonBpmProperties properties = new OperatonBpmProperties();

  @Autowired
  JobExecutor jobExecutor;

  @BeforeEach
  void setUp() {
    jobConfiguration = new DefaultJobConfiguration(properties, jobExecutor);
  }

  @Test
  void delegate_to_specialized_configurations() {
    DefaultJobConfiguration configurationSpy = Mockito.spy(jobConfiguration);
    configurationSpy.preInit(processEngineConfiguration);
    verify(configurationSpy).configureJobExecutor(processEngineConfiguration);
    verify(configurationSpy).registerCustomJobHandlers(processEngineConfiguration);
  }

  @Test
  void addJobHandler() {
    JobHandler<?> jobHandler = mock(JobHandler.class);
    when(jobHandler.getType()).thenReturn("MockHandler");
    setField(jobConfiguration, "customJobHandlers", List.<JobHandler<?>>of(jobHandler));

    assertThat(processEngineConfiguration.getCustomJobHandlers()).isEmpty();
    jobConfiguration.registerCustomJobHandlers(processEngineConfiguration);

    assertThat(processEngineConfiguration.getCustomJobHandlers()).containsOnly(jobHandler);
  }

  @Test
  void shouldUseDefaultRejectedJobsHandler() {
    // given default configuration

    // when
    RejectedJobsHandler rejectedJobsHandler = jobExecutor.getRejectedJobsHandler();

    // then
    assertThat(rejectedJobsHandler).isInstanceOf(NotifyAcquisitionRejectedJobsHandler.class);
  }

}
