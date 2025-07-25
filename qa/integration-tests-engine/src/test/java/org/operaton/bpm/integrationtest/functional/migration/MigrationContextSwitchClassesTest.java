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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.integrationtest.functional.migration.beans.InstantiationListener;
import org.operaton.bpm.integrationtest.functional.migration.beans.RemovalListener;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ArquillianExtension.class)
public class MigrationContextSwitchClassesTest extends AbstractFoxPlatformIntegrationTest {

  public static final BpmnModelInstance oneTaskProcess(String key) {
    return  Bpmn.createExecutableProcess(key)
        .operatonHistoryTimeToLive(180)
      .startEvent()
      .userTask("userTask")
      .endEvent()
      .done();
  }

  public static final BpmnModelInstance subProcessProcess(String key) {
    return  Bpmn.createExecutableProcess(key)
        .operatonHistoryTimeToLive(180)
      .startEvent()
      .subProcess()
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_START, InstantiationListener.class.getName())
      .operatonExecutionListenerClass(ExecutionListener.EVENTNAME_END, RemovalListener.class.getName())
      .embeddedSubProcess()
        .startEvent()
        .userTask("userTask")
        .endEvent()
      .subProcessDone()
      .endEvent()
      .done();
  }

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
      .addClass(RemovalListener.class)
      .addAsResource(modelAsAsset(oneTaskProcess("sourceOneTaskProcess")), "oneTaskProcess.bpmn20.xml")
      .addAsResource(modelAsAsset(subProcessProcess("sourceSubProcess")), "subProcessProcess.bpmn20.xml");
  }

  @Deployment(name = "targetDeployment")
  public static WebArchive createTargetDeplyoment() {
    return initWebArchiveDeployment("target.war")
      .addClass(InstantiationListener.class)
      .addAsResource(modelAsAsset(oneTaskProcess("targetOneTaskProcess")), "oneTaskProcess.bpmn20.xml")
      .addAsResource(modelAsAsset(subProcessProcess("targetSubProcess")), "subProcessProcess.bpmn20.xml");
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
  void testCallStartListenerInTargetContext() {
    // given
    ProcessDefinition sourceDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("sourceOneTaskProcess")
        .singleResult();

    ProcessDefinition targetDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("targetSubProcess")
        .singleResult();

    String pi = runtimeService.startProcessInstanceById(sourceDefinition.getId()).getId();

    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(pi))
      .execute();

    // then
    assertThat((Boolean) runtimeService.getVariable(pi, InstantiationListener.VARIABLE_NAME)).isTrue();
  }


  @Test
  @OperateOnDeployment("clientDeployment")
  void testCallEndListenerInTargetContext() {
    // given
    ProcessDefinition sourceDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("sourceSubProcess")
        .singleResult();

    ProcessDefinition targetDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("targetOneTaskProcess")
        .singleResult();

    String pi = runtimeService.createProcessInstanceById(sourceDefinition.getId())
        .startBeforeActivity("userTask")
        .execute(true, true)  // avoid InstantiationListener which is not present in targetDeployment
        .getId();


    MigrationPlan migrationPlan = runtimeService.createMigrationPlan(sourceDefinition.getId(), targetDefinition.getId())
      .mapActivities("userTask", "userTask")
      .build();

    // when
    runtimeService.newMigration(migrationPlan)
      .processInstanceIds(Arrays.asList(pi))
      .execute();

    // then
    assertThat((Boolean) runtimeService.getVariable(pi, RemovalListener.VARIABLE_NAME)).isTrue();
  }

  protected static Asset modelAsAsset(BpmnModelInstance modelInstance) {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(byteStream, modelInstance);

    byte[] bytes = byteStream.toByteArray();
    return new ByteArrayAsset(bytes);
  }

}
