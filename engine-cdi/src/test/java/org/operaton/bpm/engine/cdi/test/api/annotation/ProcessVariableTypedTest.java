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

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.DeclarativeProcessController;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Roman Smirnov
 *
 */
@RunWith(Arquillian.class)
public class ProcessVariableTypedTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/annotation/CompleteTaskTest.bpmn20.xml")
  public void testProcessVariableTypeAnnotation() {
    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    VariableMap variables = Variables.createVariables().putValue("injectedValue", "operaton");
    businessProcess.startProcessByKey("keyOfTheProcess", variables);

    TypedValue value = getBeanInstance(DeclarativeProcessController.class).getInjectedValue();
    assertThat(value).isInstanceOf(StringValue.class);
    assertThat(value.getType()).isEqualTo(ValueType.STRING);
    assertThat(value.getValue()).isEqualTo("operaton");
  }

}
