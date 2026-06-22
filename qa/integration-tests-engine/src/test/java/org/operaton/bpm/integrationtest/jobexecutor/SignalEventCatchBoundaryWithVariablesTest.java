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

import java.util.HashMap;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(ArquillianExtension.class)
public class SignalEventCatchBoundaryWithVariablesTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addClass(SendSignalDelegate.class)
            .addClass(SignalReceivedDelegate.class)
            .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/SignalEventCatchBoundaryWithVariablesTest.catchAlertSignalBoundaryWithReceiveTask.bpmn20.xml")
            .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/SignalEventCatchBoundaryWithVariablesTest.throwAlertSignalWithDelegate.bpmn20.xml");
  }

  @Test
  void testSignalCatchBoundaryWithVariables() {
    HashMap<String, Object> variables1 = new HashMap<>();
    variables1.put("processName", "catchSignal");
    ProcessInstance piCatchSignal = runtimeService.startProcessInstanceByKey("catchSignal", variables1);

    HashMap<String, Object> variables2 = new HashMap<>();
    variables2.put("processName", "throwSignal");
    variables2.put("signalProcessInstanceId", piCatchSignal.getProcessInstanceId());
    ProcessInstance piThrowSignal = runtimeService.startProcessInstanceByKey("throwSignal", variables2);

    waitForJobExecutorToProcessAllJobs();

    assertThat(runtimeService.createExecutionQuery().processInstanceId(piCatchSignal.getProcessInstanceId()).activityId("receiveTask").count()).isOne();
    assertThat(runtimeService.createExecutionQuery().processInstanceId(piThrowSignal.getProcessInstanceId()).activityId("receiveTask").count()).isOne();

    assertThat(runtimeService.getVariable(piCatchSignal.getId(), "processName")).isEqualTo("catchSignal-visited (was catchSignal)");
    assertThat(runtimeService.getVariable(piThrowSignal.getId(), "processName")).isEqualTo("throwSignal-visited (was throwSignal)");

    // clean up
    runtimeService.signal(piCatchSignal.getId());
    runtimeService.signal(piThrowSignal.getId());

    assertThat(runtimeService.createExecutionQuery().processInstanceId(piCatchSignal.getProcessInstanceId()).count()).isZero();
    assertThat(runtimeService.createExecutionQuery().processInstanceId(piThrowSignal.getProcessInstanceId()).count()).isZero();
  }

}
