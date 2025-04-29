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
package org.operaton.bpm.engine.spring.test.components.jobexecutor;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import static org.operaton.bpm.engine.test.util.JobExecutorHelper.waitForJobExecutorToProcessAllJobs;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Pablo Ganga
 */
@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/components/SpringjobExecutorTest-context.xml")
class SpringJobExecutorTest extends SpringProcessEngineTestCase {

  @Deployment(resources = {"org/operaton/bpm/engine/spring/test/components/SpringTimersProcess.bpmn20.xml",
    "org/operaton/bpm/engine/spring/test/components/SpringJobExecutorRollBack.bpmn20.xml"})
  @Test
  void happyJobExecutorPath() {

		ProcessInstance instance = runtimeService.startProcessInstanceByKey("process1");

    assertThat(instance).isNotNull();

		waitForJobExecutorToProcessAllJobs(processEngineConfiguration, 10000L, 1000L);

		List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
    assertThat(activeTasks).isEmpty();
	}

  @Deployment(resources = {"org/operaton/bpm/engine/spring/test/components/SpringTimersProcess.bpmn20.xml",
    "org/operaton/bpm/engine/spring/test/components/SpringJobExecutorRollBack.bpmn20.xml"})
  @Test
  void rollbackJobExecutorPath() {

    // shutdown job executor first, otherwise waitForJobExecutorToProcessAllJobs will not actually start it....
    processEngineConfiguration.getJobExecutor().shutdown();

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("errorProcess1");

    assertThat(instance).isNotNull();

    waitForJobExecutorToProcessAllJobs(processEngineConfiguration, 10000L, 1000L);

    List<Task> activeTasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
    assertThat(activeTasks).hasSize(1);
  }


}
