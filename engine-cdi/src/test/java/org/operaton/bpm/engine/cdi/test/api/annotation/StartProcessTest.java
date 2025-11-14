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
package org.operaton.bpm.engine.cdi.test.api.annotation;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.impl.annotation.StartProcessInterceptor;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.DeclarativeProcessController;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcase for assuring that the {@link StartProcessInterceptor} behaves as
 * expected.
 *
 * @author Daniel Meyer
 */
class StartProcessTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/annotation/StartProcessTest.bpmn20.xml")
  void testStartProcessByKey() {

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();

    getBeanInstance(DeclarativeProcessController.class).startProcessByKey();
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNotNull();

    String variable = businessProcess.getVariable("name");
    assertThat(variable).isEqualTo("operaton");

    TypedValue nameTypedValue = businessProcess.getVariableTyped("name");
    assertThat(nameTypedValue).isInstanceOf(StringValue.class);
    assertThat(nameTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(nameTypedValue.getValue()).isEqualTo("operaton");

    assertThat((String)businessProcess.getVariable("untypedName")).isEqualTo("untypedName");

    TypedValue untypedNameTypedValue = businessProcess.getVariableTyped("untypedName");
    assertThat(untypedNameTypedValue).isInstanceOf(StringValue.class);
    assertThat(untypedNameTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(untypedNameTypedValue.getValue()).isEqualTo("untypedName");


    assertThat((String)businessProcess.getVariable("typedName")).isEqualTo("typedName");

    TypedValue typedNameTypedValue = businessProcess.getVariableTyped("typedName");
    assertThat(typedNameTypedValue).isInstanceOf(StringValue.class);
    assertThat(typedNameTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(typedNameTypedValue.getValue()).isEqualTo("typedName");

    businessProcess.startTask(taskService.createTaskQuery().singleResult().getId());
    businessProcess.completeTask();
  }


}
