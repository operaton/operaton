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
package org.operaton.bpm.integrationtest.jboss;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class TestJobExecutorActivateFalse_JBOSS extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name="deployment1")
  public static WebArchive processArchive() {
    return initWebArchiveDeployment("test.war", "jobExecutorActivate-processes.xml");
  }

  @Test
  void shouldNotActiateJobExecutor() {

    ProcessEngine processEngine = processEngineService.getProcessEngine("jobExecutorActivate-FALSE-engine");
    ProcessEngineConfiguration configuration = processEngine.getProcessEngineConfiguration();
    JobExecutor jobExecutor = ((ProcessEngineConfigurationImpl)configuration).getJobExecutor();
    assertThat(jobExecutor.isActive()).isFalse();

    processEngine = processEngineService.getProcessEngine("jobExecutorActivate-UNDEFINED-engine");
    configuration = processEngine.getProcessEngineConfiguration();
    jobExecutor = ((ProcessEngineConfigurationImpl)configuration).getJobExecutor();
    assertThat(jobExecutor.isActive()).isTrue();

  }
}
