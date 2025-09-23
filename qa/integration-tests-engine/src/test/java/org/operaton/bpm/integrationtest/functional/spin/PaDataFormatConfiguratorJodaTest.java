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

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.application.ProcessApplicationContext;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.JodaJsonDataFormatConfigurator;
import org.operaton.bpm.integrationtest.functional.spin.dataformat.JodaJsonSerializable;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.operaton.spin.spi.DataFormatConfigurator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class PaDataFormatConfiguratorJodaTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "PaDataFormatTest.war")
        .addAsResource("META-INF/processes.xml")
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(ReferenceStoringProcessApplication.class)
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addAsResource("org/operaton/bpm/integrationtest/oneTaskProcess.bpmn")
        .addClass(JodaJsonSerializable.class)
        .addClass(JodaJsonDataFormatConfigurator.class)
        .addAsServiceProvider(DataFormatConfigurator.class, JodaJsonDataFormatConfigurator.class);

    TestContainer.addSpinJacksonJsonDataFormat(webArchive);
    TestContainer.addJodaTimeJacksonModule(webArchive);

    return webArchive;

  }

  @Test
  void testPaLocalJodaConfiguration() throws Exception {
    // given a process instance
    final ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // when setting a variable in the context of a process application
    Date date = new Date(JodaJsonSerializable.ONE_DAY_IN_MILLIS * 10); // 10th of January 1970
    JodaJsonSerializable jsonSerializable = new JodaJsonSerializable(new DateTime(date.getTime()));

    try {
      ProcessApplicationContext.setCurrentProcessApplication(ReferenceStoringProcessApplication.instance);
      runtimeService.setVariable(pi.getId(),
        "jsonSerializable",
        Variables.objectValue(jsonSerializable).serializationDataFormat(SerializationDataFormats.JSON).create());
    } finally {
      ProcessApplicationContext.clear();
    }

    // then the process-application-local data format has been used to serialize the value
    ObjectValue objectValue = runtimeService.getVariableTyped(pi.getId(), "jsonSerializable", false);

    String serializedValue = objectValue.getValueSerialized();
    String expectedSerializedValue = jsonSerializable.toExpectedJsonString();

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode actualJsonTree = objectMapper.readTree(serializedValue);
    JsonNode expectedJsonTree = objectMapper.readTree(expectedSerializedValue);
    // JsonNode#equals makes a deep comparison
    assertThat(actualJsonTree).isEqualTo(expectedJsonTree);
  }

}
