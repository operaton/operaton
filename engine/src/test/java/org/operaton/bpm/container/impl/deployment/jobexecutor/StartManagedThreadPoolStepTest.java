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
package org.operaton.bpm.container.impl.deployment.jobexecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.container.impl.RuntimeContainerDelegateImpl;
import org.operaton.bpm.container.impl.deployment.Attachments;
import org.operaton.bpm.container.impl.jmx.MBeanServiceContainer;
import org.operaton.bpm.container.impl.jmx.services.JmxManagedThreadPool;
import org.operaton.bpm.container.impl.metadata.BpmPlatformXmlImpl;
import org.operaton.bpm.container.impl.metadata.JobExecutorXmlImpl;
import org.operaton.bpm.container.impl.metadata.spi.BpmPlatformXml;
import org.operaton.bpm.container.impl.metadata.spi.JobExecutorXml;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEngineXml;
import org.operaton.bpm.container.impl.spi.DeploymentOperation;
import org.operaton.bpm.container.impl.spi.DeploymentOperationStep;
import org.operaton.bpm.container.impl.spi.PlatformService;
import org.operaton.bpm.container.impl.spi.ServiceTypes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Ronny Bräunlich
 *
 */
class StartManagedThreadPoolStepTest {

  private final MBeanServiceContainer container = new MBeanServiceContainer();

  private DeploymentOperation deploymentOperation;

  private JobExecutorXmlImpl jobExecutorXml;

  private BpmPlatformXml bpmPlatformXml;

  private StartManagedThreadPoolStep step;

  @BeforeEach
  void setUp(){
    step = new StartManagedThreadPoolStep();
    deploymentOperation = new DeploymentOperation("name", container, Collections.<DeploymentOperationStep> emptyList());
    jobExecutorXml = new JobExecutorXmlImpl();
    bpmPlatformXml = new BpmPlatformXmlImpl(jobExecutorXml, Collections.<ProcessEngineXml>emptyList());
    deploymentOperation.addAttachment(Attachments.BPM_PLATFORM_XML, bpmPlatformXml);
  }

  @AfterEach
  void tearDown(){
    container.stopService(ServiceTypes.BPM_PLATFORM, RuntimeContainerDelegateImpl.SERVICE_NAME_EXECUTOR);
  }

  @Test
  void performOperationStepWithDefaultProperties() {
    Map<String, String> properties = new HashMap<>();
    jobExecutorXml.setProperties(properties);
    step.performOperationStep(deploymentOperation);

    PlatformService<JmxManagedThreadPool> service = container.getService(getObjectNameForExecutor());
    ThreadPoolExecutor executor = service.getValue().getThreadPoolExecutor();

    //since no jobs will start, remaining capacity is sufficent to check the size
    assertThat(executor.getQueue().remainingCapacity()).isEqualTo(3);
    assertThat(executor.getCorePoolSize()).isEqualTo(3);
    assertThat(executor.getMaximumPoolSize()).isEqualTo(10);
    assertThat(executor.getKeepAliveTime(TimeUnit.MILLISECONDS)).isZero();
  }

  @Test
  void performOperationStepWithPropertiesInXml() {
    Map<String, String> properties = new HashMap<>();
    String queueSize = "5";
    String corePoolSize = "12";
    String maxPoolSize = "20";
    String keepAliveTime = "100";
    properties.put(JobExecutorXml.CORE_POOL_SIZE, corePoolSize );
    properties.put(JobExecutorXml.KEEP_ALIVE_TIME, keepAliveTime);
    properties.put(JobExecutorXml.MAX_POOL_SIZE, maxPoolSize);
    properties.put(JobExecutorXml.QUEUE_SIZE, queueSize);
    jobExecutorXml.setProperties(properties);
    step.performOperationStep(deploymentOperation);

    PlatformService<JmxManagedThreadPool> service = container.getService(getObjectNameForExecutor());
    ThreadPoolExecutor executor = service.getValue().getThreadPoolExecutor();

    //since no jobs will start, remaining capacity is sufficent to check the size
    assertThat(executor.getQueue().remainingCapacity()).isEqualTo(Integer.parseInt(queueSize));
    assertThat(executor.getCorePoolSize()).isEqualTo(Integer.parseInt(corePoolSize));
    assertThat(executor.getMaximumPoolSize()).isEqualTo(Integer.parseInt(maxPoolSize));
    assertThat(executor.getKeepAliveTime(TimeUnit.MILLISECONDS)).isEqualTo(Long.parseLong(keepAliveTime));
  }

  private ObjectName getObjectNameForExecutor(){
    String localName = MBeanServiceContainer.composeLocalName(ServiceTypes.BPM_PLATFORM, RuntimeContainerDelegateImpl.SERVICE_NAME_EXECUTOR);
    return MBeanServiceContainer.getObjectName(localName);
  }
}
