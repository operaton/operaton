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

import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.jobexecutor.beans.DemoDelegate;
import org.operaton.bpm.integrationtest.jobexecutor.beans.DemoVariableClass;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test Operaton container job exectuor.
 * FAILING ATM!
 * Expected a job with an exception but it isn't left in db with 0 retries, instead it is completely removed from the job table!
 *
 * @author christian.lipphardt@camunda.com
 */
@ExtendWith(ArquillianExtension.class)
public class FailingJobBoundaryTimerWithDelegateVariablesTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addClass(DemoDelegate.class)
            .addClass(DemoVariableClass.class)
            .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/ImmediatelyFailing.bpmn20.xml");
  }

  @Test
  void testFailingJobBoundaryTimerWithDelegateVariables() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("ImmediatelyFailing");

    List<Job> jobs = managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).list();
    assertThat(jobs).hasSize(1);
    assertThat(jobs.get(0).getRetries()).isEqualTo(3);

    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getProcessInstanceId()).activityId("usertask1").count()).isEqualTo(1);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getProcessInstanceId()).count()).isEqualTo(2);

    assertThat(managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).executable().count()).isEqualTo(1);

    waitForJobExecutorToProcessAllJobs();

    assertThat(managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).executable().count()).isEqualTo(0); // should be 0, because it has failed 3 times
    assertThat(managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).withException().count()).isEqualTo(1); // should be 1, because job failed!

    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getProcessInstanceId()).activityId("usertask1").count()).isEqualTo(1);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getProcessInstanceId()).count()).isEqualTo(2);

    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId()); // complete task with failed job => complete process

    assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getProcessInstanceId()).count()).isEqualTo(0);
    assertThat(managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).count()).isEqualTo(0); // should be 0, because process is finished.
  }

}
