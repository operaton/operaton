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
package org.operaton.bpm.engine.cdi.test.api.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.impl.annotation.StartProcessInterceptor;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.DeclarativeProcessController;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

/**
 * Testcase for assuring that the {@link StartProcessInterceptor} behaves as
 * expected.
 *
 * @author Daniel Meyer
 */
@RunWith(Arquillian.class)
class StartProcessTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/annotation/StartProcessTest.bpmn20.xml")
  void startProcessByKey() {

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();

    getBeanInstance(DeclarativeProcessController.class).startProcessByKey();
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNotNull();

    assertThat(businessProcess.getVariable("name")).isEqualTo("operaton");

    TypedValue nameTypedValue = businessProcess.getVariableTyped("name");
    assertThat(nameTypedValue).isNotNull();
    assertThat(nameTypedValue instanceof StringValue).isTrue();
    assertThat(nameTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(nameTypedValue.getValue()).isEqualTo("operaton");

    assertThat(businessProcess.getVariable("untypedName")).isEqualTo("untypedName");

    TypedValue untypedNameTypedValue = businessProcess.getVariableTyped("untypedName");
    assertThat(untypedNameTypedValue).isNotNull();
    assertThat(untypedNameTypedValue instanceof StringValue).isTrue();
    assertThat(untypedNameTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(untypedNameTypedValue.getValue()).isEqualTo("untypedName");


    assertThat(businessProcess.getVariable("typedName")).isEqualTo("typedName");

    TypedValue typedNameTypedValue = businessProcess.getVariableTyped("typedName");
    assertThat(typedNameTypedValue).isNotNull();
    assertThat(typedNameTypedValue instanceof StringValue).isTrue();
    assertThat(typedNameTypedValue.getType()).isEqualTo(ValueType.STRING);
    assertThat(typedNameTypedValue.getValue()).isEqualTo("typedName");

    businessProcess.startTask(taskService.createTaskQuery().singleResult().getId());
    businessProcess.completeTask();
  }


}
