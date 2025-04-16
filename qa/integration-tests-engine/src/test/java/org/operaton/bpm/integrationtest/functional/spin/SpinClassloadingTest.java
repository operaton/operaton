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
package org.operaton.bpm.integrationtest.functional.spin;

import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.operaton.bpm.engine.variable.Variables.*;

/**
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class SpinClassloadingTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment(name="pa")
  public static final WebArchive createPaDeployment() {

    var webArchive = initWebArchiveDeployment().addAsResource("org/operaton/bpm/integrationtest/functional/spin/SpinClassloadingTest.bpmn")
            .addClass(XmlSerializable.class)
            .addClass(SpinVariableDelegate.class)
            .addClass(SpinJsonPathDelegate.class);
    TestContainer.addContainerSpecificResourcesForSpin(webArchive);
    return webArchive;
  }

  @Deployment(name="client-app")
  public static final WebArchive createClientAppDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class)
        .addClass(AbstractFoxPlatformIntegrationTest.class);

    TestContainer.addContainerSpecificResourcesForNonPa(webArchive);

    return webArchive;
  }

  @Test
  @OperateOnDeployment("client-app")
  public void testDeserialization() {

    String serializedValue = "<?xml version=\"1.0\" encoding=\"utf-8\"?><xmlserializable><property>value</property></xmlserializable>";

    runtimeService.startProcessInstanceByKey("testProcess",
      createVariables()
        .putValueTyped("xmlSerializable",
            serializedObjectValue(serializedValue)
              .serializationDataFormat(SerializationDataFormats.XML)
              .objectTypeName("org.operaton.bpm.integrationtest.functional.spin.XmlSerializable")
            .create()));

  }

}
