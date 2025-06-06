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
package org.operaton.bpm.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.bpmn.async.RetryCmdDeployment.deployment;
import static org.operaton.bpm.engine.test.bpmn.async.RetryCmdDeployment.prepareCompensationEventProcess;
import static org.operaton.bpm.engine.test.bpmn.async.RetryCmdDeployment.prepareEscalationEventProcess;
import static org.operaton.bpm.engine.test.bpmn.async.RetryCmdDeployment.prepareMessageEventProcess;
import static org.operaton.bpm.engine.test.bpmn.async.RetryCmdDeployment.prepareSignalEventProcess;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Askar Akhmerov
 */
@Parameterized
public class FoxJobRetryCmdEventsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Parameter
  public RetryCmdDeployment deployment;

  @Parameters
  public static Collection<RetryCmdDeployment[]> scenarios() {
    return RetryCmdDeployment.asParameters(
        deployment()
            .withEventProcess(prepareSignalEventProcess()),
        deployment()
            .withEventProcess(prepareMessageEventProcess()),
        deployment()
            .withEventProcess(prepareEscalationEventProcess()),
        deployment()
            .withEventProcess(prepareCompensationEventProcess())
    );
  }

  private Deployment currentDeployment;

  @BeforeEach
  void setUp() {
    currentDeployment = testRule.deploy(deployment.getBpmnModelInstances());
  }

  @TestTemplate
  void testFailedIntermediateThrowingSignalEventAsync () {
    ProcessInstance pi = engineRule.getRuntimeService().startProcessInstanceByKey(RetryCmdDeployment.PROCESS_ID);
    assertJobRetries(pi);
  }

  @AfterEach
  void tearDown() {
    engineRule.getRepositoryService().deleteDeployment(currentDeployment.getId(),true,true);
  }

  protected void assertJobRetries(ProcessInstance pi) {
    assertThat(pi).isNotNull();

    Job job = fetchJob(pi.getProcessInstanceId());

    try {
      engineRule.getManagementService().executeJob(job.getId());
    } catch (Exception e) {
    }

    // update job
    job = fetchJob(pi.getProcessInstanceId());
    assertThat(job.getRetries()).isEqualTo(4);
  }

  protected Job fetchJob(String processInstanceId) {
    return engineRule.getManagementService().createJobQuery().processInstanceId(processInstanceId).singleResult();
  }


}
