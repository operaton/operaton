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
package org.operaton.bpm.engine.cdi.test.bpmn;

import java.util.HashMap;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class SignalEventTest extends CdiProcessEngineTestCase {

  @Named
  @Dependent
  public static class SignalReceivedDelegate implements JavaDelegate {

    @Inject
    private BusinessProcess businessProcess;

    public void execute(DelegateExecution execution) {
      businessProcess.setVariable("processName", "catchSignal-visited (was " + businessProcess.getVariable("processName")  + ")");
    }
  }

  @Named
  @Dependent
  public static class SendSignalDelegate implements JavaDelegate {

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private BusinessProcess businessProcess;

    public void execute(DelegateExecution execution) {
      businessProcess.setVariable("processName", "throwSignal-visited (was " + businessProcess.getVariable("processName")  + ")");

      String signalProcessInstanceId = (String) execution.getVariable("signalProcessInstanceId");
      String executionId = runtimeService.createExecutionQuery().processInstanceId(signalProcessInstanceId).signalEventSubscriptionName("alert").singleResult().getId();

      runtimeService.signalEventReceived("alert", executionId);
    }

  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/cdi/test/bpmn/SignalEventTests.catchAlertSignalBoundaryWithReceiveTask.bpmn20.xml",
                          "org/operaton/bpm/engine/cdi/test/bpmn/SignalEventTests.throwAlertSignalWithDelegate.bpmn20.xml"})
  public void testSignalCatchBoundaryWithVariables() {
    HashMap<String, Object> variables1 = new HashMap<>();
    variables1.put("processName", "catchSignal");
    ProcessInstance piCatchSignal = runtimeService.startProcessInstanceByKey("catchSignal", variables1);

    HashMap<String, Object> variables2 = new HashMap<>();
    variables2.put("processName", "throwSignal");
    variables2.put("signalProcessInstanceId", piCatchSignal.getProcessInstanceId());
    ProcessInstance piThrowSignal = runtimeService.startProcessInstanceByKey("throwSignal", variables2);

    assertThat(runtimeService.createExecutionQuery().processInstanceId(piCatchSignal.getProcessInstanceId()).activityId("receiveTask").count()).isEqualTo(1);
    assertThat(runtimeService.createExecutionQuery().processInstanceId(piThrowSignal.getProcessInstanceId()).activityId("receiveTask").count()).isEqualTo(1);

    assertThat(runtimeService.getVariable(piCatchSignal.getId(), "processName")).isEqualTo("catchSignal-visited (was catchSignal)");
    assertThat(runtimeService.getVariable(piThrowSignal.getId(), "processName")).isEqualTo("throwSignal-visited (was throwSignal)");

    // clean up
    runtimeService.signal(piCatchSignal.getId());
    runtimeService.signal(piThrowSignal.getId());
  }

}
