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

import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.application.ProcessApplicationContext;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.value.SerializableValue;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.JsonDataFormatConfigurator;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.JsonSerializable;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.operaton.spin.spi.DataFormatConfigurator;

import static org.operaton.bpm.application.ProcessApplicationContext.withProcessApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class PaContextSwitchTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name = "pa1")
  public static WebArchive createDeployment1() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "pa1.war")
        .addAsResource("META-INF/processes.xml")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addClass(ProcessApplication1.class)
        .addClass(JsonSerializable.class)
        .addClass(RuntimeServiceDelegate.class)
        .addAsResource("org/operaton/bpm/integrationtest/functional/spin/paContextSwitch.bpmn20.xml")
        .addClass(JsonDataFormatConfigurator.class)
        .addAsServiceProvider(DataFormatConfigurator.class, JsonDataFormatConfigurator.class);

    TestContainer.addSpinJacksonJsonDataFormat(webArchive);

    return webArchive;
  }

  @Deployment(name = "pa2")
  public static WebArchive createDeployment2() {
    return ShrinkWrap.create(WebArchive.class, "pa2.war")
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())
        .addAsResource("META-INF/processes.xml")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(ProcessApplication2.class);
  }

  /**
   * This test ensures that when the {@link ProcessApplicationContext} API is used,
   * the context switch is only performed for outer-most command and not if a second, nested
   * command is executed; => in nested commands, the engine is already in the correct context
   */
  @Test
  @OperateOnDeployment("pa1")
  void testNoContextSwitchOnInnerCommand() throws Exception {

    ProcessInstance pi = withProcessApplicationContext((Callable<ProcessInstance>) () -> runtimeService.startProcessInstanceByKey("process"), "pa2");

    JsonSerializable expectedJsonSerializable = RuntimeServiceDelegate.createJsonSerializable();
    String expectedJsonString = expectedJsonSerializable.toExpectedJsonString(JsonDataFormatConfigurator.getDateFormat());

    SerializableValue serializedValue = runtimeService.getVariableTyped(pi.getId(), RuntimeServiceDelegate.VARIABLE_NAME, false);
    String actualJsonString = serializedValue.getValueSerialized();

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode actualJsonTree = objectMapper.readTree(actualJsonString);
    JsonNode expectedJsonTree = objectMapper.readTree(expectedJsonString);
    // JsonNode#equals makes a deep comparison
    assertThat(actualJsonTree).isEqualTo(expectedJsonTree);

  }
}
