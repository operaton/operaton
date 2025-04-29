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
package org.operaton.bpm.integrationtest.functional.classloading.jobexecution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
  See https://jira.camunda.com/browse/CAM-9913
 */
@RunWith(Arquillian.class)
public class ClassloadingByJobPriorityTest extends AbstractFoxPlatformIntegrationTest {

  protected static final BpmnModelInstance process = Bpmn.createExecutableProcess("asyncTaskProcess")
                                                         .operatonHistoryTimeToLive(180)
                                                         .startEvent()
                                                         .serviceTask()
                                                           .operatonExpression("${true}")
                                                           .operatonAsyncBefore()
                                                         .endEvent()
                                                         .done();

  @Deployment(name= "engineWithPriorityJobAcquisition")
  public static WebArchive processArchive() {
    WebArchive webArchive = initWebArchiveDeployment("processApp.war",
        "org/operaton/bpm/integrationtest/functional/classloading/jobexecution/engineWithAcquireJobsByPriority.xml")
               .addClass(AbstractFoxPlatformIntegrationTest.class)
               .addAsResource(modelAsAsset(process), "ClassloadingByJobPriorityTest.testDeployProcessArchive.bpmn");
    TestContainer.addContainerSpecificResources(webArchive);
    TestContainer.addContainerSpecificProcessEngineConfigurationClass(webArchive);

    return webArchive;
  }

  @Test
  @OperateOnDeployment("engineWithPriorityJobAcquisition")
  public void testDeployProcessArchive() {
    // given
    ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) processEngineService
                                                       .getProcessEngine("engineWithJobPriority")
                                                       .getProcessEngineConfiguration();
    configuration.getRuntimeService().startProcessInstanceByKey("asyncTaskProcess");

    // when
    waitForJobExecutorToProcessAllJobs(12000);

    // then
    List<Job> availableJobs = configuration.getManagementService().createJobQuery().noRetriesLeft().list();
    assertThat(availableJobs).isEmpty();
  }

  protected static Asset modelAsAsset(BpmnModelInstance modelInstance) {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(byteStream, modelInstance);

    byte[] bytes = byteStream.toByteArray();
    return new ByteArrayAsset(bytes);
  }

}
