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
package org.operaton.bpm.integrationtest.functional.classloading.jobexecution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

/**
 * See CAM-10258
 */
@ExtendWith(ArquillianExtension.class)
public class ClassloadingDuringJobExecutionTest extends AbstractFoxPlatformIntegrationTest {
  protected static String process =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:operaton="http://operaton.org/schema/1.0/bpmn" targetNamespace="Examples">
      <process id="Process_1" name="ServiceTask_Throw_BMPN_Error" isExecutable="true" operaton:historyTimeToLive="P180D">
        <startEvent id="StartEvent_1">
        </startEvent>
        <sequenceFlow id="SequenceFlow_03wj6bv" sourceRef="StartEvent_1" targetRef="Task_1bkcm2v" />
        <endEvent id="EndEvent_0joyvpc">
        </endEvent>
        <sequenceFlow id="SequenceFlow_0mt1p11" sourceRef="Task_1bkcm2v" targetRef="EndEvent_0joyvpc" />
        <serviceTask id="Task_1bkcm2v" name="Throw BPMN Error" operaton:asyncBefore="true" operaton:expression="${true}">
          <extensionElements>
            <operaton:inputOutput>
              <operaton:outputParameter name="output">
                <operaton:script scriptFormat="Javascript">throw new org.operaton.bpm.engine.delegate.BpmnError("Test error thrown");</operaton:script>
              </operaton:outputParameter>
            </operaton:inputOutput>
          </extensionElements>
        </serviceTask>
      </process>
    </definitions>
    """;

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "client.war")
            .addClass(AbstractFoxPlatformIntegrationTest.class)
            .addClass(JobExecutorWaitUtils.class)
            .addAsLibraries(DeploymentHelper.getTestingLibs());
    TestContainer.addContainerSpecificResourcesForNonPa(deployment);
    return deployment;
  }

  @Test
  public void shouldLoadBPMNErorClassUsedInGroovyScriptDuringJobExecution() {
    // given
    String deploymentId = repositoryService.createDeployment()
        .addString("process.bpmn", process)
        .deploy().getId();
    runtimeService.startProcessInstanceByKey("Process_1");

    // when
    waitForJobExecutorToProcessAllJobs();

    // then
    List<Job> failedJobs = managementService.createJobQuery().noRetriesLeft().list();
    assertThat(!failedJobs.isEmpty()).isTrue();
    for (Job job : failedJobs) {
      String jobExceptionStacktrace = managementService.getJobExceptionStacktrace(job.getId());
      assertThat(jobExceptionStacktrace).doesNotContain("ClassNotFoundException");
      assertThat(jobExceptionStacktrace).contains("Test error thrown");
    }
    // clean up
    repositoryService.deleteDeployment(deploymentId, true);
  }
}
