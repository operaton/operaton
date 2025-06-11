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
package org.operaton.bpm.engine.test.standalone.pvm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.impl.pvm.ProcessDefinitionBuilder;
import org.operaton.bpm.engine.impl.pvm.PvmExecution;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.test.standalone.pvm.activities.Automatic;
import org.operaton.bpm.engine.test.standalone.pvm.activities.Decision;
import org.operaton.bpm.engine.test.standalone.pvm.activities.WaitState;

/**
 * @author Tom Baeyens
 */
class PvmTest {

  @Test
  void testPvmWaitState() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("a")
        .initial()
        .behavior(new WaitState())
        .transition("b")
      .endActivity()
      .createActivity("b")
        .behavior(new WaitState())
        .transition("c")
      .endActivity()
      .createActivity("c")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    PvmExecution activityInstance = processInstance.findExecution("a");
    assertThat(activityInstance).isNotNull();

    activityInstance.signal(null, null);

    activityInstance = processInstance.findExecution("b");
    assertThat(activityInstance).isNotNull();

    activityInstance.signal(null, null);

    activityInstance = processInstance.findExecution("c");
    assertThat(activityInstance).isNotNull();
  }

  @Test
  void testPvmAutomatic() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("a")
        .initial()
        .behavior(new Automatic())
        .transition("b")
      .endActivity()
      .createActivity("b")
        .behavior(new Automatic())
        .transition("c")
      .endActivity()
        .createActivity("c")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertThat(processInstance.findExecution("c")).isNotNull();
  }

  @Test
  void testPvmDecision() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("checkCredit")
      .endActivity()
      .createActivity("checkCredit")
        .behavior(new Decision())
        .transition("askDaughterOut", "wow")
        .transition("takeToGolf", "nice")
        .transition("ignore", "default")
      .endActivity()
      .createActivity("takeToGolf")
        .behavior(new WaitState())
      .endActivity()
      .createActivity("askDaughterOut")
        .behavior(new WaitState())
      .endActivity()
      .createActivity("ignore")
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.setVariable("creditRating", "Aaa-");
    processInstance.start();
    assertThat(processInstance.findExecution("takeToGolf")).isNotNull();

    processInstance = processDefinition.createProcessInstance();
    processInstance.setVariable("creditRating", "AAA+");
    processInstance.start();
    assertThat(processInstance.findExecution("askDaughterOut")).isNotNull();

    processInstance = processDefinition.createProcessInstance();
    processInstance.setVariable("creditRating", "bb-");
    processInstance.start();
    assertThat(processInstance.findExecution("ignore")).isNotNull();
  }
}
