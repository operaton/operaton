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
package org.operaton.bpm.integrationtest.functional.connect;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.connect.Connectors;
import org.operaton.connect.spi.Connector;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>Smoke-test Make sure operaton connect can be used in a process application </p>
 *
 * @author Daniel Meyer
 */
@ExtendWith(ArquillianExtension.class)
public class PaConnectSupportTest extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive createDeployment() {
    return initWebArchiveDeployment()
      .addAsResource("org/operaton/bpm/integrationtest/functional/connect/PaConnectSupportTest.connectorServiceTask.bpmn20.xml")
      .addClass(TestConnector.class)
      .addClass(TestConnectorRequest.class)
      .addClass(TestConnectorResponse.class)
      .addClass(TestConnectors.class);
  }

  @Test
  void httpConnectorShouldBeAvailable() {
    assertThat(Connectors.<Connector<?>>http()).isNotNull();
  }

  @Test
  void soapConnectorShouldBeAvailable() {
    assertThat(Connectors.<Connector<?>>soap()).isNotNull();
  }

  @Test
  void connectorServiceTask() {
    TestConnector connector = new TestConnector();
    TestConnectors.registerConnector(connector);

    runtimeService.startProcessInstanceByKey("testProcess");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    String payload = (String) taskService.getVariable(task.getId(), "payload");
    assertThat(payload).isEqualTo("Hello world!");

    TestConnectors.unregisterConnector(connector.getId());
  }

}
