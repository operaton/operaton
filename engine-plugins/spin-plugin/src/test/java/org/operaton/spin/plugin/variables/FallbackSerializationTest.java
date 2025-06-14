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
package org.operaton.spin.plugin.variables;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class FallbackSerializationTest {
  RuntimeService runtimeService;
  protected static final String ONE_TASK_PROCESS = "org/operaton/spin/plugin/oneTaskProcess.bpmn20.xml";

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void serializationOfUnknownFormat() {
    // given
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    ObjectValue objectValue = Variables.serializedObjectValue("foo")
      .serializationDataFormat("application/foo")
      .objectTypeName("org.operaton.Foo")
      .create();

    String processInstanceId = instance.getId();
    runtimeService.setVariable(processInstanceId, "var", objectValue);

    // then
    assertThatThrownBy(() -> runtimeService.getVariable(processInstanceId, "var"))
            .isInstanceOf(ProcessEngineException.class)
            .hasMessageContaining("Fallback serializer cannot handle deserialized objects");

    ObjectValue returnedValue = runtimeService.getVariableTyped(processInstanceId, "var", false);
    assertFalse(returnedValue.isDeserialized());
    assertEquals("application/foo", returnedValue.getSerializationDataFormat());
    assertEquals("foo", returnedValue.getValueSerialized());
    assertEquals("org.operaton.Foo", returnedValue.getObjectTypeName());

  }
}
