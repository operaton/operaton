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
package org.operaton.bpm.integrationtest.functional.migration;
import java.util.List;

import java.io.ByteArrayOutputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.integrationtest.functional.migration.beans.TimerBean;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class MigrationContextSwitchBeansTest extends AbstractFoxPlatformIntegrationTest {

  public static final BpmnModelInstance ONE_TASK_PROCESS = Bpmn.createExecutableProcess("oneTaskProcess")
      .operatonHistoryTimeToLive(180)
    .startEvent()
    .userTask("userTask")
    .endEvent()
    .done();

  public static final BpmnModelInstance BOUNDARY_EVENT_PROCESS = Bpmn.createExecutableProcess("boundaryProcess")
      .operatonHistoryTimeToLive(180)
    .startEvent()
    .userTask("userTask")
    .boundaryEvent()
    .timerWithDuration("${timerBean.duration}")
    .endEvent()
    .moveToNode("userTask")
    .endEvent()
    .done();

  @Deployment(name = "sourceDeployment")
  public static WebArchive createSourceDeplyoment() {
    return initWebArchiveDeployment("source.war")
      .addAsResource(modelAsAsset(ONE_TASK_PROCESS), "oneTaskProcess.bpmn20.xml");
  }

  @Deployment(name = "targetDeployment")
  public static WebArchive createTargetDeplyoment() {
    return initWebArchiveDeployment("target.war")
      .addClass(TimerBean.class)
      .addAsResource(modelAsAsset(BOUNDARY_EVENT_PROCESS), "boundaryProcess.bpmn20.xml");
  }

  @Deployment(name="clientDeployment")
  public static WebArchive clientDeployment() {
    WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "client.war")
            .addAsLibraries(DeploymentHelper.getTestingLibs())
            .addClass(AbstractFoxPlatformIntegrationTest.class);

    TestContainer.addContainerSpecificResources(webArchive);

    return webArchive;

  }

  @Test
  @OperateOnDeployment("clientDeployment")
  void testCreateBoundaryTimer() {
    // given
    ProcessDefinition sourceDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("oneTaskProcess")
        .singleResult();

    ProcessDefinition targetDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("boundaryProcess")
        .singleResult();

    String pi = runtimeService.startProcessInstanceById(sourceDefinition.getId()).getId();

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(List.of(pi))
      .execute();

    // then
    Job timerJob = managementService.createJobQuery().processDefinitionKey("boundaryProcess").singleResult();
    assertThat(timerJob).isNotNull();
    assertThat(timerJob.getDuedate()).isNotNull();
  }

  protected static Asset modelAsAsset(BpmnModelInstance modelInstance) {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(byteStream, modelInstance);

    byte[] bytes = byteStream.toByteArray();
    return new ByteArrayAsset(bytes);
  }

}
