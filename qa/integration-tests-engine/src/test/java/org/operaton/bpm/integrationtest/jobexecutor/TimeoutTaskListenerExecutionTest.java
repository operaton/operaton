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
package org.operaton.bpm.integrationtest.jobexecutor;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.jobexecutor.beans.SampleTaskListenerBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ArquillianExtension.class)
public class TimeoutTaskListenerExecutionTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/TimeoutTaskListenerExecution.bpmn20.xml")
            .addClass(SampleTaskListenerBean.class);
  }

  @Test
  void testProcessExecution() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process");

    waitForJobExecutorToProcessAllJobs();

    List<ProcessInstance> finallyRunningInstances = runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).list();
    assertThat(finallyRunningInstances.size()).isEqualTo(1);

    Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
    assertThat(task).isNotNull();

    Object variable = taskService.getVariable(task.getId(), "called");
    assertThat(variable).isNotNull();

    assertThat((boolean) variable).isTrue();
  }
}
