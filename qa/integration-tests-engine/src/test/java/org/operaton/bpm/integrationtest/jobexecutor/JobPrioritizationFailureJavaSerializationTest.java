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
package org.operaton.bpm.integrationtest.jobexecutor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobPriorityProvider;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.Variables.SerializationDataFormats;
import org.operaton.bpm.integrationtest.jobexecutor.beans.PriorityBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestContainer;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class JobPrioritizationFailureJavaSerializationTest extends AbstractFoxPlatformIntegrationTest {

  protected ProcessInstance processInstance;

  private ProcessEngine engine1;

  public static final String VARIABLE_CLASS_NAME = "org.operaton.bpm.integrationtest.jobexecutor.beans.PriorityBean";
  public static final String PRIORITY_BEAN_INSTANCE_FILE = "priorityBean.instance";

  @BeforeEach
  void setEngines() {
    ProcessEngineService engineService = BpmPlatform.getProcessEngineService();
    engine1 = engineService.getProcessEngine("engine1");

    // unregister process application so that context switch cannot be performed
    unregisterProcessApplication();
  }

  protected void unregisterProcessApplication() {
    org.operaton.bpm.engine.repository.Deployment deployment =
      engine1.getRepositoryService().createDeploymentQuery().singleResult();

    engine1.getManagementService().unregisterProcessApplication(deployment.getId(), false);
  }

  @Deployment(order = 1)
  public static WebArchive createDeployment() {
    final WebArchive webArchive = initWebArchiveDeployment("paJavaSerialization1.war", "org/operaton/bpm/integrationtest/processes-javaSerializationEnabled-pa1.xml")
      .addClass(PriorityBean.class)
      .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/JobPrioritizationTest.priorityProcess.bpmn20.xml");

    TestContainer.addContainerSpecificProcessEngineConfigurationClass(webArchive);
    return webArchive;
  }

  @Deployment(name = "dummy-client", order = 2)
  public static WebArchive createDummyClientDeployment() {
    return initWebArchiveDeployment("paJavaSerialization2.war", "org/operaton/bpm/integrationtest/processes-javaSerializationEnabled-pa2.xml")
      .addAsResource(new ByteArrayAsset(serializeJavaObjectValue(new PriorityBean())), PRIORITY_BEAN_INSTANCE_FILE);
  }

  @AfterEach
  void tearDown() {
    if (processInstance != null) {
      engine1.getRuntimeService().deleteProcessInstance(processInstance.getId(), "");
    }
  }


  @Test
  @OperateOnDeployment("dummy-client")
  void testGracefulDegradationOnMissingBean() {
    // when
    processInstance = engine1.getRuntimeService().startProcessInstanceByKey("priorityProcess");

    // then the job was created successfully and has the default priority on bean evaluation failure
    Job job = engine1.getManagementService().createJobQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
    Assertions.assertEquals(DefaultJobPriorityProvider.DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE, job.getPriority());
  }

  @Test
  @OperateOnDeployment("dummy-client")
  void testGracefulDegradationOnMissingClassJava() {
    // given
    byte[] serializedPriorityBean = readByteArrayFromClasspath(PRIORITY_BEAN_INSTANCE_FILE);
    String encodedPriorityBean = StringUtil.fromBytes(Base64.getEncoder().encode(serializedPriorityBean), processEngine);

    Map<String, Object> variables = Variables.createVariables().putValue(
        "priorityBean",
        Variables.serializedObjectValue(encodedPriorityBean)
          .serializationDataFormat(SerializationDataFormats.JAVA)
          .objectTypeName(VARIABLE_CLASS_NAME)
          .create());

    // when
    processInstance = engine1.getRuntimeService().startProcessInstanceByKey("priorityProcess", variables);

    // then the job was created successfully and has the default priority although
    // the bean could not be resolved due to a missing class
    Job job = engine1.getManagementService().createJobQuery().processInstanceId(processInstance.getProcessInstanceId()).singleResult();
    Assertions.assertEquals(DefaultJobPriorityProvider.DEFAULT_PRIORITY_ON_RESOLUTION_FAILURE, job.getPriority());
  }

  protected static byte[] serializeJavaObjectValue(Serializable object) {

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new ObjectOutputStream(baos).writeObject(object);
      return baos.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static byte[] readByteArrayFromClasspath(String path) {
    try {
      InputStream inStream = JobPrioritizationFailureJavaSerializationTest.class.getClassLoader().getResourceAsStream(path);
      byte[] serializedValue = IoUtil.readInputStream(inStream, "");
      inStream.close();
      return serializedValue;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
