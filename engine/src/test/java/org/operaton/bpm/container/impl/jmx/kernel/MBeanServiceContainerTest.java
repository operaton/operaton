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
package org.operaton.bpm.container.impl.jmx.kernel;

import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.container.impl.jmx.MBeanServiceContainer;
import org.operaton.bpm.container.impl.jmx.kernel.util.FailingDeploymentOperationStep;
import org.operaton.bpm.container.impl.jmx.kernel.util.StartServiceDeploymentOperationStep;
import org.operaton.bpm.container.impl.jmx.kernel.util.StopServiceDeploymentOperationStep;
import org.operaton.bpm.container.impl.jmx.kernel.util.TestService;
import org.operaton.bpm.container.impl.jmx.kernel.util.TestServiceType;
import org.operaton.bpm.container.impl.spi.PlatformService;
import org.operaton.bpm.engine.ProcessEngineException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testcases for the {@link MBeanServiceContainer} Kernel.
 *
 * @author Daniel Meyer
 *
 */
class MBeanServiceContainerTest {

  private MBeanServiceContainer serviceContainer;

  private final String service1Name = TestServiceType.TYPE1.getTypeName() + ":type=service1";
  private final String service2Name = TestServiceType.TYPE1.getTypeName() + ":type=service2";
  private final String service3Name = TestServiceType.TYPE2.getTypeName() + ":type=service3";
  private final String service4Name = TestServiceType.TYPE2.getTypeName() + ":type=service4";

  private final ObjectName service1ObjectName = MBeanServiceContainer.getObjectName(service1Name);
  private final ObjectName service2ObjectName = MBeanServiceContainer.getObjectName(service2Name);
  private final ObjectName service3ObjectName = MBeanServiceContainer.getObjectName(service3Name);
  private final ObjectName service4ObjectName = MBeanServiceContainer.getObjectName(service4Name);

  private final TestService service1 = new TestService();
  private final TestService service2 = new TestService();
  private final TestService service3 = new TestService();
  private final TestService service4 = new TestService();

  @BeforeEach
  void setUp() {
    serviceContainer = new MBeanServiceContainer();
  }

  @AfterEach
  void tearDown() throws Exception {
    // make sure all MBeans are removed after each test
    MBeanServer mBeanServer = serviceContainer.getmBeanServer();
    if(mBeanServer.isRegistered(service1ObjectName)) {
      mBeanServer.unregisterMBean(service1ObjectName);
    }
    if(mBeanServer.isRegistered(service2ObjectName)) {
      mBeanServer.unregisterMBean(service2ObjectName);
    }
    if(mBeanServer.isRegistered(service3ObjectName)) {
      mBeanServer.unregisterMBean(service3ObjectName);
    }
    if(mBeanServer.isRegistered(service4ObjectName)) {
      mBeanServer.unregisterMBean(service4ObjectName);
    }
  }

  @Test
  void testStartService() {

    // initially the service is not present:
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNull();

    // we can start a service
    serviceContainer.startService(service1Name, service1);
    // and get it after that
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNotNull();
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isEqualTo(service1);
    // as long it is started, I cannot start a second service with the same name:
    assertThatThrownBy(() -> serviceContainer.startService(service1Name, service1))
        .isInstanceOf(Exception.class)
        .hasMessageContaining("service with same name already registered");

    // but, I can start a service with a different name:
    serviceContainer.startService(service2Name, service2);
    // and get it after that
    assertThat(serviceContainer.<TestService>getService(service2ObjectName)).isNotNull();

  }

  @Test
  void testStopService() {

    // start some service
    serviceContainer.startService(service1Name, service1);
    // it's there
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNotNull();

    // stop it:
    serviceContainer.stopService(service1Name);
    // now it's gone
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNull();

    assertThatThrownBy(() -> serviceContainer.stopService(service1Name))
        .isInstanceOf(Exception.class)
        .hasMessageContaining("no such service registered");

  }

  @Test
  void testGetServicesByType() {

    serviceContainer.startService(service1Name, service1);
    serviceContainer.startService(service2Name, service2);

    List<PlatformService<TestService>> servicesByType1 = serviceContainer.getServicesByType(TestServiceType.TYPE1);
    assertThat(servicesByType1).hasSize(2);

    List<PlatformService<TestService>> servicesByType2 = serviceContainer.getServicesByType(TestServiceType.TYPE2);
    assertThat(servicesByType2).isEmpty();

    serviceContainer.startService(service3Name, service3);
    serviceContainer.startService(service4Name, service4);

    servicesByType1 = serviceContainer.getServicesByType(TestServiceType.TYPE1);
    assertThat(servicesByType1).hasSize(2);

    servicesByType2 = serviceContainer.getServicesByType(TestServiceType.TYPE2);
    assertThat(servicesByType2).hasSize(2);

  }

  @Test
  void testGetServiceValuesByType() {

    // start some services
    serviceContainer.startService(service1Name, service1);
    serviceContainer.startService(service2Name, service2);

    List<PlatformService<TestService>> servicesByType1 = serviceContainer.getServiceValuesByType(TestServiceType.TYPE1);
    assertThat(servicesByType1).containsExactlyInAnyOrder(service1, service2);

    List<PlatformService<TestService>> servicesByType2 = serviceContainer.getServicesByType(TestServiceType.TYPE2);
    assertThat(servicesByType2).isEmpty();

    // start more services
    serviceContainer.startService(service3Name, service3);
    serviceContainer.startService(service4Name, service4);

    servicesByType1 = serviceContainer.getServicesByType(TestServiceType.TYPE1);
    assertThat(servicesByType1).hasSize(2);

    servicesByType2 = serviceContainer.getServicesByType(TestServiceType.TYPE2);
    assertThat(servicesByType2).containsExactlyInAnyOrder(service3, service4);

  }

  @Test
  void testGetServiceNames() {

    // start some services
    serviceContainer.startService(service1Name, service1);
    serviceContainer.startService(service2Name, service2);

    Set<String> serviceNames = serviceContainer.getServiceNames(TestServiceType.TYPE1);
    assertThat(serviceNames).containsExactlyInAnyOrder(service1Name, service2Name);

    serviceNames = serviceContainer.getServiceNames(TestServiceType.TYPE2);
    assertThat(serviceNames).isEmpty();

    // start more services
    serviceContainer.startService(service3Name, service3);
    serviceContainer.startService(service4Name, service4);

    serviceNames = serviceContainer.getServiceNames(TestServiceType.TYPE1);
    assertThat(serviceNames).containsExactlyInAnyOrder(service1Name, service2Name);

    serviceNames = serviceContainer.getServiceNames(TestServiceType.TYPE2);
    assertThat(serviceNames).containsExactlyInAnyOrder(service3Name, service4Name);

  }

  @Test
  void testDeploymentOperation() {

    serviceContainer.createDeploymentOperation("test op")
      .addStep(new StartServiceDeploymentOperationStep(service1Name, service1))
      .addStep(new StartServiceDeploymentOperationStep(service2Name, service2))
      .execute();

    // both services were registered.
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isEqualTo(service1);
    assertThat(serviceContainer.<TestService>getService(service2ObjectName)).isEqualTo(service2);

  }

  @Test
  void testFailingDeploymentOperation() {
    var deploymentOperationBuilder = serviceContainer.createDeploymentOperation("test failing op")
        .addStep(new StartServiceDeploymentOperationStep(service1Name, service1))
        .addStep(new StartServiceDeploymentOperationStep(service2Name, service2))
        .addStep(new FailingDeploymentOperationStep());

    assertThatThrownBy(deploymentOperationBuilder::execute)
        .isInstanceOf(Exception.class)
        .hasMessageContaining("Exception while performing 'test failing op' => 'failing step'");

    // none of the services were registered
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNull();
    assertThat(serviceContainer.<TestService>getService(service2ObjectName)).isNull();

    // different step ordering //////////////////////////////////

    var deploymentOperationBuilder1 = serviceContainer.createDeploymentOperation("test failing op")
      .addStep(new StartServiceDeploymentOperationStep(service1Name, service1))
      .addStep(new StartServiceDeploymentOperationStep(service2Name, service2))
      .addStep(new FailingDeploymentOperationStep());

    assertThatThrownBy(deploymentOperationBuilder1::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Exception while performing 'test failing op' => 'failing step'");

    // none of the services were registered
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNull();
    assertThat(serviceContainer.<TestService>getService(service2ObjectName)).isNull();

  }

  @Test
  void testUndeploymentOperation() {

    // let's first start some services:
    serviceContainer.startService(service1Name, service1);
    serviceContainer.startService(service2Name, service2);

    // run a composite undeployment operation
    serviceContainer.createUndeploymentOperation("test op")
      .addStep(new StopServiceDeploymentOperationStep(service1Name))
      .addStep(new StopServiceDeploymentOperationStep(service2Name))
      .execute();

    // both services were stopped.
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNull();
    assertThat(serviceContainer.<TestService>getService(service2ObjectName)).isNull();

  }

  @Test
  void testFailingUndeploymentOperation() {

    // let's first start some services:
    serviceContainer.startService(service1Name, service1);
    serviceContainer.startService(service2Name, service2);

    // run a composite undeployment operation with a failing step
    serviceContainer.createUndeploymentOperation("test failing op")
      .addStep(new StopServiceDeploymentOperationStep(service1Name))
      .addStep(new FailingDeploymentOperationStep())                               // <- this step fails
      .addStep(new StopServiceDeploymentOperationStep(service2Name))
      .execute(); // this does not throw an exception even if some steps fail. (exceptions are logged)


    // both services were stopped.
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNull();
    assertThat(serviceContainer.<TestService>getService(service2ObjectName)).isNull();

    // different step ordering //////////////////////////////////

    serviceContainer.startService(service1Name, service1);
    serviceContainer.startService(service2Name, service2);

    // run a composite undeployment operation with a failing step
    serviceContainer.createUndeploymentOperation("test failing op")
      .addStep(new FailingDeploymentOperationStep())                               // <- this step fails
      .addStep(new StopServiceDeploymentOperationStep(service1Name))
      .addStep(new StopServiceDeploymentOperationStep(service2Name))
      .execute();

    // both services were stopped.
    assertThat(serviceContainer.<TestService>getService(service1ObjectName)).isNull();
    assertThat(serviceContainer.<TestService>getService(service2ObjectName)).isNull();

  }

}
