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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.impl.pvm.ProcessDefinitionBuilder;
import org.operaton.bpm.engine.impl.pvm.PvmExecution;
import org.operaton.bpm.engine.impl.pvm.PvmProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.PvmProcessInstance;
import org.operaton.bpm.engine.test.standalone.pvm.activities.WaitState;


/**
 * @author Tom Baeyens
 */
class PvmVariablesTest {

  @Test
  void testVariables() {
    PvmProcessDefinition processDefinition = new ProcessDefinitionBuilder()
      .createActivity("a")
        .initial()
        .behavior(new WaitState())
      .endActivity()
    .buildProcessDefinition();

    PvmProcessInstance processInstance = processDefinition.createProcessInstance();
    processInstance.setVariable("amount", 500L);
    processInstance.setVariable("msg", "hello world");
    processInstance.start();

    assertThat(processInstance.getVariable("amount")).isEqualTo(500L);
    assertThat(processInstance.getVariable("msg")).isEqualTo("hello world");

    PvmExecution activityInstance = processInstance.findExecution("a");
    assertThat(activityInstance.getVariable("amount")).isEqualTo(500L);
    assertThat(activityInstance.getVariable("msg")).isEqualTo("hello world");

    Map<String, Object> expectedVariables = new HashMap<>();
    expectedVariables.put("amount", 500L);
    expectedVariables.put("msg", "hello world");

    assertThat(activityInstance.getVariables()).isEqualTo(expectedVariables);
    assertThat(processInstance.getVariables()).isEqualTo(expectedVariables);
  }
}
