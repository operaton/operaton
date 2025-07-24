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
package org.operaton.bpm.integrationtest.functional.spin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;
import static org.operaton.spin.Spin.JSON;
import static org.operaton.spin.Spin.XML;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.spin.impl.util.SpinIoUtil;
import org.operaton.spin.json.SpinJsonNode;

/**
 * <p>Smoke-test Make sure operaton spin can be used in a process application </p>
 *
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
public class PaSpinSupportTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    return initWebArchiveDeployment()
        .addAsResource("org/operaton/bpm/integrationtest/oneTaskProcess.bpmn")
        .addAsResource("org/operaton/bpm/integrationtest/functional/spin/jackson146.json");
  }

  @Test
  void spinShouldBeAvailable() {
    Assertions.assertEquals("someXml", XML("<someXml />").xPath("/someXml").element().name());
  }

  @Test
  void spinCanBeUsedForVariableSerialization() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", Variables.createVariables()
        .putValue("serializedObject", serializedObjectValue("{\"foo\": \"bar\"}").serializationDataFormat("application/json").objectTypeName(HashMap.class.getName())));

    ObjectValue objectValue = runtimeService.getVariableTyped(pi.getId(), "serializedObject", true);

    HashMap<String, String> expected = new HashMap<>();
    expected.put("foo", "bar");

    Assertions.assertEquals(expected, objectValue.getValue());
  }

  @Test
  void spinPluginShouldBeRegistered() {

    List<ProcessEnginePlugin> processEnginePlugins = processEngineConfiguration.getProcessEnginePlugins();

    boolean spinPluginFound = false;

    for (ProcessEnginePlugin plugin : processEnginePlugins) {
      if (plugin.getClass().getName().contains("Spin")) {
        spinPluginFound = true;
        break;
      }
    }

    assertThat(spinPluginFound).isTrue();
  }

  @Test
  void testJacksonBug146() {
    InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/integrationtest/functional/spin/jackson146.json");
    String jackson146 = SpinIoUtil.inputStreamAsString(resourceAsStream);

    // this should not fail
    SpinJsonNode node = JSON(jackson146);

    // file has 4000 characters in length a
    // 20 characters per repeated JSON object
    assertEquals(200, node.prop("abcdef").elements().size());
  }

  @Test
  void testJacksonBug146AsVariable() {
    InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/integrationtest/functional/spin/jackson146.json");
    String jackson146 = SpinIoUtil.inputStreamAsString(resourceAsStream);

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", Variables.createVariables()
      .putValue("jackson146", serializedObjectValue(jackson146).serializationDataFormat("application/json").objectTypeName(HashMap.class.getName())));

    // file has 4000 characters in length a
    // 20 characters per repeated JSON object
    ObjectValue objectValue = runtimeService.getVariableTyped(pi.getId(), "jackson146", true);
    HashMap<String, List<Object>> map = (HashMap<String, List<Object>>) objectValue.getValue();

    assertEquals(200, map.get("abcdef").size());
  }

}
