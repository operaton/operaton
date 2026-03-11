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
package org.operaton.bpm.integrationtest.functional.scriptengine;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
@ExtendWith(ArquillianExtension.class)
public abstract class AbstractScriptEngineSupportTest extends AbstractFoxPlatformIntegrationTest {

  public static final String PROCESS_ID = "testProcess";
  public static final String EXAMPLE_SCRIPT = "execution.setVariable('foo', 'bar')";
  public static final String EXAMPLE_SPIN_SCRIPT = "execution.setVariable('bar', S('<baz/>').name())";

  public String processInstanceId;

  protected static StringAsset createScriptTaskProcess(String scriptFormat, String scriptTextPlain, String scriptTextSpin) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(PROCESS_ID)
        .operatonHistoryTimeToLive(180)
      .startEvent()
      .scriptTask()
        .scriptFormat(scriptFormat)
        .scriptText(scriptTextPlain)
      .scriptTask()
        .scriptFormat(scriptFormat)
        .scriptText(scriptTextSpin)
      .userTask()
      .endEvent()
      .done();
    return new StringAsset(Bpmn.convertToString(modelInstance));
  }

  @Test
  public void shouldSetVariable() {
    processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_ID).getId();
    Object foo = runtimeService.getVariable(processInstanceId, "foo");
    Object bar = runtimeService.getVariable(processInstanceId, "bar");
    assertThat(foo).isNotNull();
    assertThat(bar).isNotNull();
    assertThat(foo).isEqualTo("bar");
    assertThat(bar).isEqualTo("baz");
  }

}
