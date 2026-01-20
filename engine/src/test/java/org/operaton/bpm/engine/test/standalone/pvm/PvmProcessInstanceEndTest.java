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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.pvm.ProcessDefinitionBuilder;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.test.standalone.pvm.activities.Automatic;
import org.operaton.bpm.engine.test.standalone.pvm.activities.WaitState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tom Baeyens
 */
class PvmProcessInstanceEndTest {

  @Test
  void testSimpleProcessInstanceEnd() {
    EventCollector eventCollector = new EventCollector();

    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
      .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .createActivity("start")
        .initial()
        .behavior(new Automatic())
        .transition("wait")
      .endActivity()
      .createActivity("wait")
        .behavior(new WaitState())
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START, eventCollector)
        .executionListener(org.operaton.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_END, eventCollector)
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.start();

    assertThat(eventCollector.getEvents()).hasSize(2)
      .allSatisfy(event -> {
        assertThat(event).startsWith("start on");
      });

    // when
    processInstance.deleteCascade("test");

    assertThat(eventCollector.getEvents()).hasSize(4);
    assertThat(eventCollector.getEvents().subList(2,3))
      .allSatisfy(event -> {
        assertThat(event).startsWith("end on");
      });
  }
}
