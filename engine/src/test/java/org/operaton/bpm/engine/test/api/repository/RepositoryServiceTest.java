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
package org.operaton.bpm.engine.test.api.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.RepositoryServiceImpl;
import org.operaton.bpm.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.core.model.CallableElement;
import org.operaton.bpm.engine.impl.history.event.UserOperationLogEntryEventEntity;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.pvm.PvmActivity;
import org.operaton.bpm.engine.impl.pvm.PvmTransition;
import org.operaton.bpm.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.repository.CalledProcessDefinition;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.CaseDefinitionQuery;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinitionQuery;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinitionQuery;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.TestExecutionListener;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.ResourceSuffixes.BPMN_RESOURCE_SUFFIXES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Roman Smirnov
 */
class RepositoryServiceTest {

  private static final String NAMESPACE = "xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL'";
  private static final String TARGET_NAMESPACE = "targetNamespace='" + BpmnParse.OPERATON_BPMN_EXTENSIONS_NS + "'";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  
  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  ManagementService managementService;
  HistoryService historyService;
  IdentityService identityService;

  boolean enforceHistoryTimeToLiveBefore;

  @BeforeEach
  void setUp() {
    this.enforceHistoryTimeToLiveBefore = processEngineConfiguration.isEnforceHistoryTimeToLive();
  }

  @AfterEach
  void tearDown() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerActivateProcessDefinitionHandler.TYPE);
      return null;
    });

    // restore config to the test's previous state
    processEngineConfiguration.setEnforceHistoryTimeToLive(enforceHistoryTimeToLiveBefore);
  }

  private void checkDeployedBytes(InputStream deployedResource, byte[] utf8Bytes) throws IOException {
    byte[] deployedBytes = new byte[utf8Bytes.length];
    deployedResource.read(deployedBytes);

    for (int i = 0; i < utf8Bytes.length; i++) {
      assertThat(deployedBytes[i]).isEqualTo(utf8Bytes[i]);
    }
  }

  @Test
  void testUTF8DeploymentMethod() throws Exception {
    //given utf8 charset
    Charset utf8Charset = StandardCharsets.UTF_8;
    Charset defaultCharset = processEngineConfiguration.getDefaultCharset();
    processEngineConfiguration.setDefaultCharset(utf8Charset);

    //and model instance with umlauts
    String umlautsString = "äöüÄÖÜß";
    String resourceName = "deployment.bpmn";
    BpmnModelInstance instance = Bpmn.createExecutableProcess("umlautsProcess").operatonHistoryTimeToLive(180).startEvent(umlautsString).done();
    String instanceAsString = Bpmn.convertToString(instance);

    //when instance is deployed via addString method
    var deployment = repositoryService.createDeployment()
                                                                               .addString(resourceName, instanceAsString)
                                                                               .deploy();

    //then bytes are saved in utf-8 format
    InputStream inputStream = repositoryService.getResourceAsStream(deployment.getId(), resourceName);
    byte[] utf8Bytes = instanceAsString.getBytes(utf8Charset);
    checkDeployedBytes(inputStream, utf8Bytes);
    repositoryService.deleteDeployment(deployment.getId());


    //when model instance is deployed via addModelInstance method
    deployment = repositoryService.createDeployment().addModelInstance(resourceName, instance).deploy();

    //then also the bytes are saved in utf-8 format
    inputStream = repositoryService.getResourceAsStream(deployment.getId(), resourceName);
    checkDeployedBytes(inputStream, utf8Bytes);

    repositoryService.deleteDeployment(deployment.getId());
    processEngineConfiguration.setDefaultCharset(defaultCharset);
  }

  @Deployment(resources = {
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testStartProcessInstanceById() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions).hasSize(1);

    ProcessDefinition processDefinition = processDefinitions.get(0);
    assertThat(processDefinition.getKey()).isEqualTo("oneTaskProcess");
    assertThat(processDefinition.getId()).isNotNull();
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testFindProcessDefinitionById() {
    List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(definitions).hasSize(1);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitions.get(0).getId()).singleResult();
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertThat(processDefinition).isNotNull();
    assertThat(processDefinition.getKey()).isEqualTo("oneTaskProcess");
    assertThat(processDefinition.getName()).isEqualTo("The One Task Process");

    processDefinition = repositoryService.getProcessDefinition(definitions.get(0).getId());
    assertThat(processDefinition.getDescription()).isEqualTo("This is a process for testing purposes");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  void testDeleteDeploymentWithRunningInstances() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions).hasSize(1);
    ProcessDefinition processDefinition = processDefinitions.get(0);
    var deploymentId = processDefinition.getDeploymentId();

    runtimeService.startProcessInstanceById(processDefinition.getId());

    // Try to delete the deployment
    assertThatThrownBy(() -> repositoryService.deleteDeployment(deploymentId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Deletion of process definition without cascading failed.");
  }

  @Test
  void testDeleteDeploymentSkipCustomListeners() {
    DeploymentBuilder deploymentBuilder =
        repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/api/repository/RepositoryServiceTest.testDeleteProcessInstanceSkipCustomListeners.bpmn20.xml");

    String deploymentId = deploymentBuilder.deploy().getId();

    runtimeService.startProcessInstanceByKey("testProcess");

    repositoryService.deleteDeployment(deploymentId, true, false);
    assertThat(TestExecutionListener.collectedEvents).hasSize(1);
    TestExecutionListener.reset();

    deploymentId = deploymentBuilder.deploy().getId();

    runtimeService.startProcessInstanceByKey("testProcess");

    repositoryService.deleteDeployment(deploymentId, true, true);
    assertThat(TestExecutionListener.collectedEvents).isEmpty();
    TestExecutionListener.reset();

  }

  @Test
  void testDeleteDeploymentSkipCustomTaskListeners() {
    DeploymentBuilder deploymentBuilder =
        repositoryService
          .createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/api/repository/RepositoryServiceTest.testDeleteProcessInstanceSkipCustomTaskListeners.bpmn20.xml");

    String deploymentId = deploymentBuilder.deploy().getId();

    runtimeService.startProcessInstanceByKey("testProcess");

    RecorderTaskListener.getRecordedEvents().clear();

    repositoryService.deleteDeployment(deploymentId, true, false);
    assertThat(RecorderTaskListener.getRecordedEvents()).hasSize(1);
    RecorderTaskListener.clear();

    deploymentId = deploymentBuilder.deploy().getId();

    runtimeService.startProcessInstanceByKey("testProcess");

    repositoryService.deleteDeployment(deploymentId, true, true);
    assertThat(RecorderTaskListener.getRecordedEvents()).isEmpty();
    RecorderTaskListener.clear();
  }

  @Test
  void testDeleteDeploymentSkipIoMappings() {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/RepositoryServiceTest.testDeleteDeploymentSkipIoMappings.bpmn20.xml");

    String deploymentId = deploymentBuilder.deploy().getId();
    runtimeService.startProcessInstanceByKey("ioMappingProcess");

    // Try to delete the deployment
    assertThatCode(() -> repositoryService.deleteDeployment(deploymentId, true, false, true))
        .doesNotThrowAnyException();
  }

  @Test
  void testDeleteDeploymentWithoutSkipIoMappings() {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/RepositoryServiceTest.testDeleteDeploymentSkipIoMappings.bpmn20.xml");

    String deploymentId = deploymentBuilder.deploy().getId();
    runtimeService.startProcessInstanceByKey("ioMappingProcess");

    // Try to delete the deployment
    assertThatThrownBy(() -> repositoryService.deleteDeployment(deploymentId, true, false, false))
      .isInstanceOf(Exception.class)
      .hasMessageContaining("Exception when output mapping is executed");

    repositoryService.deleteDeployment(deploymentId, true, false, true);
  }

  @Test
  void testDeleteDeploymentNullDeploymentId() {
    assertThatThrownBy(() -> repositoryService.deleteDeployment(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("deploymentId is null");
  }

  @Test
  void testDeleteDeploymentCascadeNullDeploymentId() {
    assertThatThrownBy(() -> repositoryService.deleteDeployment(null, true))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("deploymentId is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  void testDeleteDeploymentCascadeWithRunningInstances() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions).hasSize(1);
    ProcessDefinition processDefinition = processDefinitions.get(0);

    runtimeService.startProcessInstanceById(processDefinition.getId());

    // Try to delete the deployment, no exception should be thrown
    repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/repository/one.cmmn"})
  @Test
  void testDeleteDeploymentClearsCache() {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();

    // fetch definition ids
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    String caseDefinitionId = repositoryService.createCaseDefinitionQuery().singleResult().getId();
    // fetch CMMN model to be placed to in the cache
    repositoryService.getCmmnModelInstance(caseDefinitionId);

    DeploymentCache deploymentCache = processEngineConfiguration.getDeploymentCache();

    // ensure definitions and models are part of the cache
    assertThat(deploymentCache.getProcessDefinitionCache().get(processDefinitionId)).isNotNull();
    assertThat(deploymentCache.getBpmnModelInstanceCache().get(processDefinitionId)).isNotNull();
    assertThat(deploymentCache.getCaseDefinitionCache().get(caseDefinitionId)).isNotNull();
    assertThat(deploymentCache.getCmmnModelInstanceCache().get(caseDefinitionId)).isNotNull();

    // when the deployment is deleted
    repositoryService.deleteDeployment(deploymentId, true);

    // then the definitions and models are removed from the cache
    assertThat(deploymentCache.getProcessDefinitionCache().get(processDefinitionId)).isNull();
    assertThat(deploymentCache.getBpmnModelInstanceCache().get(processDefinitionId)).isNull();
    assertThat(deploymentCache.getCaseDefinitionCache().get(caseDefinitionId)).isNull();
    assertThat(deploymentCache.getCmmnModelInstanceCache().get(caseDefinitionId)).isNull();
  }

  @Test
  void testFindDeploymentResourceNamesNullDeploymentId() {
    assertThatThrownBy(() -> repositoryService.getDeploymentResourceNames(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("deploymentId is null");
  }

  @Test
  void testFindDeploymentResourcesNullDeploymentId() {
    assertThatThrownBy(() -> repositoryService.getDeploymentResources(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("deploymentId is null");
  }

  @Test
  void testDeploymentWithDelayedProcessDefinitionActivation() {

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    Date inThreeDays = new Date(startTime.getTime() + (3 * 24 * 60 * 60 * 1000));

    // Deploy process, but activate after three days
    var deployment = repositoryService.createDeployment()
            .addClasspathResource("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
            .addClasspathResource("org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml")
            .activateProcessDefinitionsOn(inThreeDays)
            .deploy();

    assertThat(repositoryService.createDeploymentQuery().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();

    // Shouldn't be able to start a process instance
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("oneTaskProcess"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContainingIgnoringCase("suspended");

    List<Job> jobs = managementService.createJobQuery().list();
    managementService.executeJob(jobs.get(0).getId());
    managementService.executeJob(jobs.get(1).getId());

    assertThat(repositoryService.createDeploymentQuery().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(2);

    // Should be able to start process instance
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    // Cleanup
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  void testDeploymentWithDelayedProcessDefinitionAndJobDefinitionActivation() {

    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    Date inThreeDays = new Date(startTime.getTime() + (3 * 24 * 60 * 60 * 1000));

    // Deploy process, but activate after three days
    var deployment = repositoryService.createDeployment()
            .addClasspathResource("org/operaton/bpm/engine/test/api/oneAsyncTask.bpmn")
            .activateProcessDefinitionsOn(inThreeDays)
            .deploy();

    assertThat(repositoryService.createDeploymentQuery().count()).isOne();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();

    assertThat(managementService.createJobDefinitionQuery().count()).isOne();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isOne();
    assertThat(managementService.createJobDefinitionQuery().active().count()).isZero();

    // Shouldn't be able to start a process instance
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("oneTaskProcess"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContainingIgnoringCase("suspended");

    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    assertThat(repositoryService.createDeploymentQuery().count()).isOne();

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isOne();
    assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isOne();

    assertThat(managementService.createJobDefinitionQuery().count()).isOne();
    assertThat(managementService.createJobDefinitionQuery().suspended().count()).isZero();
    assertThat(managementService.createJobDefinitionQuery().active().count()).isOne();

    // Should be able to start process instance
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();

    // Cleanup
    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  void testGetResourceAsStreamUnexistingResourceInExistingDeployment() {
    // Get hold of the deployment id
    var deployment = repositoryService.createDeploymentQuery().singleResult();
    var deploymentId = deployment.getId();

    assertThatThrownBy(() -> repositoryService.getResourceAsStream(deploymentId, "org/operaton/bpm/engine/test/api/unexistingProcess.bpmn.xml"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no resource found with name");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  void testGetResourceAsStreamUnexistingDeployment() {
    assertThatThrownBy(() -> repositoryService.getResourceAsStream("unexistingdeployment", "org/operaton/bpm/engine/test/api/unexistingProcess.bpmn.xml"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no resource found with name");
  }


  @Test
  void testGetResourceAsStreamNullArguments() {
    assertThatThrownBy(() -> repositoryService.getResourceAsStream(null, "resource"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("deploymentId is null");

    assertThatThrownBy(() -> repositoryService.getResourceAsStream("deployment", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("resourceName is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/repository/one.cmmn" })
  @Test
  void testGetCaseDefinition() {
    CaseDefinitionQuery query = repositoryService.createCaseDefinitionQuery();

    CaseDefinition caseDefinition = query.singleResult();
    String caseDefinitionId = caseDefinition.getId();

    CaseDefinition definition = repositoryService.getCaseDefinition(caseDefinitionId);

    assertThat(definition).isNotNull();
    assertThat(definition.getId()).isEqualTo(caseDefinitionId);
  }

  @Test
  void testGetCaseDefinitionByInvalidId() {
    assertThatThrownBy(() -> repositoryService.getCaseDefinition("invalid"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("no deployed case definition found with id 'invalid'");

    assertThatThrownBy(() -> repositoryService.getCaseDefinition(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("caseDefinitionId is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/repository/one.cmmn" })
  @Test
  void testGetCaseModel() {
    CaseDefinitionQuery query = repositoryService.createCaseDefinitionQuery();

    CaseDefinition caseDefinition = query.singleResult();
    String caseDefinitionId = caseDefinition.getId();

    InputStream caseModel = repositoryService.getCaseModel(caseDefinitionId);

    assertThat(caseModel).isNotNull();

    byte[] readInputStream = IoUtil.readInputStream(caseModel, "caseModel");
    String model = new String(readInputStream, UTF_8);

    assertThat(model).contains("<case id=\"one\" name=\"One\">");

    IoUtil.closeSilently(caseModel);
  }

  @Test
  void testGetCaseModelByInvalidId() {
    assertThatThrownBy(() -> repositoryService.getCaseModel("invalid"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no deployed case definition found with id 'invalid'");

    assertThatThrownBy(() -> repositoryService.getCaseModel(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("caseDefinitionId is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/repository/one.dmn" })
  @Test
  void testGetDecisionDefinition() {
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    DecisionDefinition decisionDefinition = query.singleResult();
    String decisionDefinitionId = decisionDefinition.getId();

    DecisionDefinition definition = repositoryService.getDecisionDefinition(decisionDefinitionId);

    assertThat(definition).isNotNull();
    assertThat(definition.getId()).isEqualTo(decisionDefinitionId);
  }

  @Test
  void testGetDecisionDefinitionByInvalidId() {
    assertThatThrownBy(() -> repositoryService.getDecisionDefinition("invalid"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("no deployed decision definition found with id 'invalid'");

    assertThatThrownBy(() -> repositoryService.getDecisionDefinition(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("decisionDefinitionId is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/repository/drg.dmn" })
  @Test
  void testGetDecisionRequirementsDefinition() {
    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();

    DecisionRequirementsDefinition decisionRequirementsDefinition = query.singleResult();
    String decisionRequirementsDefinitionId = decisionRequirementsDefinition.getId();

    DecisionRequirementsDefinition definition = repositoryService.getDecisionRequirementsDefinition(decisionRequirementsDefinitionId);

    assertThat(definition).isNotNull();
    assertThat(definition.getId()).isEqualTo(decisionRequirementsDefinitionId);
  }

  @Test
  void testGetDecisionRequirementsDefinitionByInvalidId() {
    assertThatThrownBy(() -> repositoryService.getDecisionRequirementsDefinition("invalid"))
      .isInstanceOf(Exception.class)
      .hasMessageContaining("no deployed decision requirements definition found with id 'invalid'");

    assertThatThrownBy(() -> repositoryService.getDecisionRequirementsDefinition(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("decisionRequirementsDefinitionId is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/repository/one.dmn" })
  @Test
  void testGetDecisionModel() {
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();

    DecisionDefinition decisionDefinition = query.singleResult();
    String decisionDefinitionId = decisionDefinition.getId();

    InputStream decisionModel = repositoryService.getDecisionModel(decisionDefinitionId);

    assertThat(decisionModel).isNotNull();

    byte[] readInputStream = IoUtil.readInputStream(decisionModel, "decisionModel");
    String model = new String(readInputStream, UTF_8);

    assertThat(model).contains("<decision id=\"one\" name=\"One\">");

    IoUtil.closeSilently(decisionModel);
  }

  @Test
  void testGetDecisionModelByInvalidId() {
    assertThatThrownBy(() -> repositoryService.getDecisionModel("invalid"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no deployed decision definition found with id 'invalid'");

    assertThatThrownBy(() -> repositoryService.getDecisionModel(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("decisionDefinitionId is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/repository/drg.dmn" })
  @Test
  void testGetDecisionRequirementsModel() {
    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();

    DecisionRequirementsDefinition decisionRequirementsDefinition = query.singleResult();
    String decisionRequirementsDefinitionId = decisionRequirementsDefinition.getId();

    InputStream decisionRequirementsModel = repositoryService.getDecisionRequirementsModel(decisionRequirementsDefinitionId);

    assertThat(decisionRequirementsModel).isNotNull();

    byte[] readInputStream = IoUtil.readInputStream(decisionRequirementsModel, "decisionRequirementsModel");
    String model = new String(readInputStream, UTF_8);

    assertThat(model).contains("<definitions id=\"dish\" name=\"Dish\" namespace=\"test-drg\"");
    IoUtil.closeSilently(decisionRequirementsModel);
  }

  @Test
  void testGetDecisionRequirementsModelByInvalidId() {
    assertThatThrownBy(() -> repositoryService.getDecisionRequirementsModel("invalid"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no deployed decision requirements definition found with id 'invalid'");

    assertThatThrownBy(() -> repositoryService.getDecisionRequirementsModel(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("decisionRequirementsDefinitionId is null");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/repository/drg.dmn",
                           "org/operaton/bpm/engine/test/repository/drg.png" })
  @Test
  void testGetDecisionRequirementsDiagram() {

    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();

    DecisionRequirementsDefinition decisionRequirementsDefinition = query.singleResult();
    String decisionRequirementsDefinitionId = decisionRequirementsDefinition.getId();

    InputStream actualDrd = repositoryService.getDecisionRequirementsDiagram(decisionRequirementsDefinitionId);

    assertThat(actualDrd).isNotNull();
  }

  @Test
  void testGetDecisionRequirementsDiagramByInvalidId() {
    try {
      repositoryService.getDecisionRequirementsDiagram("invalid");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("no deployed decision requirements definition found with id 'invalid'", e.getMessage());
    }

    try {
      repositoryService.getDecisionRequirementsDiagram(null);
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("decisionRequirementsDefinitionId is null", e.getMessage());
    }
  }

  @Test
  void testDeployRevisedProcessAfterDeleteOnOtherProcessEngine() {

    // Setup both process engines
    ProcessEngine processEngine1 = new StandaloneProcessEngineConfiguration().setProcessEngineName("reboot-test-schema")
        .setDatabaseSchemaUpdate(org.operaton.bpm.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
        .setJdbcUrl("jdbc:h2:mem:activiti-process-cache-test;DB_CLOSE_DELAY=1000")
        .setJobExecutorActivate(false)
        .setEnforceHistoryTimeToLive(false)
        .buildProcessEngine();

    RepositoryService repositoryService1 = processEngine1.getRepositoryService();

    ProcessEngine processEngine2 = new StandaloneProcessEngineConfiguration().setProcessEngineName("reboot-test")
        .setDatabaseSchemaUpdate(org.operaton.bpm.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
        .setJdbcUrl("jdbc:h2:mem:activiti-process-cache-test;DB_CLOSE_DELAY=1000")
        .setJobExecutorActivate(false)
        .setEnforceHistoryTimeToLive(false)
        .buildProcessEngine();

    RepositoryService repositoryService2 = processEngine2.getRepositoryService();
    RuntimeService runtimeService2 = processEngine2.getRuntimeService();
    TaskService taskService2 = processEngine2.getTaskService();

    // Deploy first version of process: start->originalTask->end on first process engine
    String deploymentId = repositoryService1.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/RepositoryServiceTest.testDeployRevisedProcessAfterDeleteOnOtherProcessEngine.v1.bpmn20.xml")
      .deploy()
      .getId();

    // Start process instance on second engine
    String processDefinitionId = repositoryService2.createProcessDefinitionQuery().singleResult().getId();
    runtimeService2.startProcessInstanceById(processDefinitionId);
    Task task = taskService2.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("original task");

    // Delete the deployment on second process engine
    repositoryService2.deleteDeployment(deploymentId, true);
    assertThat(repositoryService2.createDeploymentQuery().count()).isZero();
    assertThat(runtimeService2.createProcessInstanceQuery().count()).isZero();

    // deploy a revised version of the process: start->revisedTask->end on first process engine
    //
    // Before the bugfix, this would set the cache on the first process engine,
    // but the second process engine still has the original process definition in his cache.
    // Since there is a deployment delete in between, the new generated process definition id is the same
    // as in the original deployment, making the second process engine using the old cached process definition.
    deploymentId = repositoryService1.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/RepositoryServiceTest.testDeployRevisedProcessAfterDeleteOnOtherProcessEngine.v2.bpmn20.xml")
      .deploy()
      .getId();

    // Start process instance on second process engine -> must use revised process definition
    processDefinitionId = repositoryService2.createProcessDefinitionQuery().singleResult().getId();
    runtimeService2.startProcessInstanceById(processDefinitionId);
    task = taskService2.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("revised task");

    // cleanup
    repositoryService1.deleteDeployment(deploymentId, true);
    processEngine1.close();
    processEngine2.close();
  }

  @Test
  void testDeploymentPersistence() {
    var deployment = repositoryService
      .createDeployment()
      .name("strings")
      .addString("org/operaton/bpm/engine/test/test/HelloWorld.string", "hello world")
      .addString("org/operaton/bpm/engine/test/test/TheAnswer.string", "42")
      .deploy();
    assertThat(deployment).isNotNull();

    var deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(1);
    deployment = deployments.get(0);

    assertThat(deployment.getName()).isEqualTo("strings");
    assertThat(deployment.getDeploymentTime()).isNotNull();

    String deploymentId = deployment.getId();
    List<String> resourceNames = repositoryService.getDeploymentResourceNames(deploymentId);
    Set<String> expectedResourceNames = new HashSet<>();
    expectedResourceNames.add("org/operaton/bpm/engine/test/test/HelloWorld.string");
    expectedResourceNames.add("org/operaton/bpm/engine/test/test/TheAnswer.string");
    assertThat(new HashSet<>(resourceNames)).isEqualTo(expectedResourceNames);

    InputStream resourceStream = repositoryService.getResourceAsStream(deploymentId, "org/operaton/bpm/engine/test/test/HelloWorld.string");
    assertThat(IoUtil.readInputStream(resourceStream, "test")).containsExactly("hello world".getBytes());

    resourceStream = repositoryService.getResourceAsStream(deploymentId, "org/operaton/bpm/engine/test/test/TheAnswer.string");
    assertThat(IoUtil.readInputStream(resourceStream, "test")).containsExactly("42".getBytes());

    repositoryService.deleteDeployment(deploymentId);
  }

  @Test
  void testProcessDefinitionPersistence() {
    String deploymentId = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml")
      .deploy()
      .getId();

    List<ProcessDefinition> processDefinitions = repositoryService
      .createProcessDefinitionQuery()
      .list();

    assertThat(processDefinitions).hasSize(2);

    repositoryService.deleteDeployment(deploymentId);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/dmn/Example.dmn"})
  @Test
  void testDecisionDefinitionUpdateTimeToLiveWithUserOperationLog() {
    //given
    identityService.setAuthenticatedUserId("userId");
    DecisionDefinition decisionDefinition = findOnlyDecisionDefinition();
    Integer orgTtl = decisionDefinition.getHistoryTimeToLive();

    //when
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), 6);

    //then
    decisionDefinition = findOnlyDecisionDefinition();
    assertThat(decisionDefinition.getHistoryTimeToLive().intValue()).isEqualTo(6);

    UserOperationLogQuery operationLogQuery = historyService.createUserOperationLogQuery()
      .operationType(UserOperationLogEntry.OPERATION_TYPE_UPDATE_HISTORY_TIME_TO_LIVE)
      .entityType(EntityTypes.DECISION_DEFINITION);

    UserOperationLogEntry ttlEntry = operationLogQuery.property("historyTimeToLive").singleResult();
    UserOperationLogEntry definitionIdEntry = operationLogQuery.property("decisionDefinitionId").singleResult();
    UserOperationLogEntry definitionKeyEntry = operationLogQuery.property("decisionDefinitionKey").singleResult();

    assertThat(ttlEntry).isNotNull();
    assertThat(definitionIdEntry).isNotNull();
    assertThat(definitionKeyEntry).isNotNull();

    assertThat(ttlEntry.getOrgValue()).isEqualTo(orgTtl.toString());
    assertThat(ttlEntry.getNewValue()).isEqualTo("6");
    assertThat(definitionIdEntry.getNewValue()).isEqualTo(decisionDefinition.getId());
    assertThat(definitionKeyEntry.getNewValue()).isEqualTo(decisionDefinition.getKey());

    assertThat(ttlEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(definitionIdEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(definitionKeyEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/dmn/Example.dmn"})
  @Test
  void testDecisionDefinitionUpdateTimeToLiveNull() {
    //given
    DecisionDefinition decisionDefinition = findOnlyDecisionDefinition();

    //when
    repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinition.getId(), null);

    //then
    decisionDefinition = repositoryService.getDecisionDefinition(decisionDefinition.getId());
    assertThat(decisionDefinition.getHistoryTimeToLive()).isNull();

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/dmn/Example.dmn"})
  @Test
  void testDecisionDefinitionUpdateTimeToLiveNegative() {
    //given
    DecisionDefinition decisionDefinition = findOnlyDecisionDefinition();
    var decisionDefinitionId = decisionDefinition.getId();

    //when/then
    assertThatThrownBy(() -> repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, -1))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("greater than");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessDefinitionUpdateTimeToLive() {
    //given
    ProcessDefinition processDefinition = findOnlyProcessDefinition();

    //when
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinition.getId(), 6);

    //then
    processDefinition = findOnlyProcessDefinition();
    assertThat(processDefinition.getHistoryTimeToLive().intValue()).isEqualTo(6);

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessDefinitionUpdateTimeToLiveNull() {
    //given
    ProcessDefinition processDefinition = findOnlyProcessDefinition();

    //when
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinition.getId(), null);

    //then
    processDefinition = findOnlyProcessDefinition();
    assertThat(processDefinition.getHistoryTimeToLive()).isNull();

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessDefinitionUpdateTimeToLiveNegative() {
    //given
    ProcessDefinition processDefinition = findOnlyProcessDefinition();
    var processDefinitionId = processDefinition.getId();

    //when/then
    assertThatThrownBy(() -> repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitionId, -1))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("greater than");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testProcessDefinitionUpdateHistoryTimeToLiveWithUserOperationLog() {
    //given
    ProcessDefinition processDefinition = findOnlyProcessDefinition();
    Integer timeToLiveOrgValue = processDefinition.getHistoryTimeToLive();
    identityService.setAuthenticatedUserId("userId");

    //when
    Integer timeToLiveNewValue = 6;
    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinition.getId(), timeToLiveNewValue);

    //then
    List<UserOperationLogEntry> opLogEntries = historyService.createUserOperationLogQuery().list();
    assertThat(opLogEntries).hasSize(1);
    final UserOperationLogEntryEventEntity userOperationLogEntry = (UserOperationLogEntryEventEntity)opLogEntries.get(0);

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_UPDATE_HISTORY_TIME_TO_LIVE);
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(processDefinition.getKey());
    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    assertThat(userOperationLogEntry.getProperty()).isEqualTo("historyTimeToLive");
    assertThat(Integer.valueOf(userOperationLogEntry.getOrgValue())).isEqualTo(timeToLiveOrgValue);
    assertThat(Integer.valueOf(userOperationLogEntry.getNewValue())).isEqualTo(timeToLiveNewValue);

  }

  @Test
  void testGetProcessModelByInvalidId() {
    assertThatThrownBy(() -> repositoryService.getProcessModel("invalid"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("no deployed process definition found with id 'invalid'");
  }

  @Test
  void testGetProcessModelByNullId() {
    assertThatThrownBy(() -> repositoryService.getProcessModel(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("The process definition id is mandatory");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCaseDefinitionUpdateHistoryTimeToLiveWithUserOperationLog() {
    // given
    identityService.setAuthenticatedUserId("userId");

    // there exists a deployment containing a case definition with key "oneTaskCase"
    CaseDefinition caseDefinition = findOnlyCaseDefinition();

    // when
    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinition.getId(), 6);

    // then
    caseDefinition = findOnlyCaseDefinition();

    assertThat(caseDefinition.getHistoryTimeToLive().intValue()).isEqualTo(6);

    UserOperationLogQuery operationLogQuery = historyService.createUserOperationLogQuery()
      .operationType(UserOperationLogEntry.OPERATION_TYPE_UPDATE_HISTORY_TIME_TO_LIVE)
      .entityType(EntityTypes.CASE_DEFINITION)
      .caseDefinitionId(caseDefinition.getId());

    UserOperationLogEntry ttlEntry = operationLogQuery.property("historyTimeToLive").singleResult();
    UserOperationLogEntry definitionKeyEntry = operationLogQuery.property("caseDefinitionKey").singleResult();

    assertThat(ttlEntry).isNotNull();
    assertThat(definitionKeyEntry).isNotNull();

    // original time-to-live value is null
    assertThat(ttlEntry.getOrgValue()).isNull();
    assertThat(ttlEntry.getNewValue()).isEqualTo("6");
    assertThat(definitionKeyEntry.getNewValue()).isEqualTo(caseDefinition.getKey());

    assertThat(ttlEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(definitionKeyEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testUpdateHistoryTimeToLiveNull() {
    // given
    // there exists a deployment containing a case definition with key "oneTaskCase"

    CaseDefinition caseDefinition = findOnlyCaseDefinition();

    // when
    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinition.getId(), null);

    // then
    caseDefinition = findOnlyCaseDefinition();

    assertThat(caseDefinition.getHistoryTimeToLive()).isNull();
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void shouldFailToUpdateHistoryTimeToLiveOnCaseDefinitionHTTLUpdate() {
    // given
    CaseDefinition caseDefinition = findOnlyCaseDefinition();
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);
    String caseDefinitionId = caseDefinition.getId();
    // when
    assertThatThrownBy(() -> repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitionId, null))
        // then
        .isInstanceOf(NotAllowedException.class)
        .hasMessage("Null historyTimeToLive values are not allowed");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void shouldFailToUpdateHistoryTimeToLiveOnProcessDefinitionHTTLUpdate() {
    // given
    ProcessDefinition processDefinition = findOnlyProcessDefinition();
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);
    String processDefinitionId = processDefinition.getId();

    // when
    assertThatThrownBy(() -> repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitionId, null))
        // then
        .isInstanceOf(NotAllowedException.class)
        .hasMessage("Null historyTimeToLive values are not allowed");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/dmn/Example.dmn"})
  @Test
  void shouldFailToUpdateHistoryTimeToLiveOnDecisionDefinitionHTTLUpdate() {
    // given
    DecisionDefinition decisionDefinition = findOnlyDecisionDefinition();
    String decisionDefinitionId = decisionDefinition.getId();
    processEngineConfiguration.setEnforceHistoryTimeToLive(true);

    // when
    assertThatThrownBy(() -> repositoryService.updateDecisionDefinitionHistoryTimeToLive(decisionDefinitionId, null))
        // then
        .isInstanceOf(NotAllowedException.class)
        .hasMessage("Null historyTimeToLive values are not allowed");
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testUpdateHistoryTimeToLiveNegative() {
    // given
    // there exists a deployment containing a case definition with key "oneTaskCase"

    CaseDefinition caseDefinition = findOnlyCaseDefinition();
    var caseDefinitionId = caseDefinition.getId();

    // when/then
    assertThatThrownBy(() -> repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinitionId, -1))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("greater than");
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testUpdateHistoryTimeToLiveInCache() {
    // given
    // there exists a deployment containing a case definition with key "oneTaskCase"

    CaseDefinition caseDefinition = findOnlyCaseDefinition();

    // assume
    assertThat(caseDefinition.getHistoryTimeToLive()).isNull();

    // when
    repositoryService.updateCaseDefinitionHistoryTimeToLive(caseDefinition.getId(), 10);

    CaseDefinition definition = repositoryService.getCaseDefinition(caseDefinition.getId());
    assertThat(definition.getHistoryTimeToLive()).isEqualTo(Integer.valueOf(10));
  }

  private CaseDefinition findOnlyCaseDefinition() {
    List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().list();
    assertThat(caseDefinitions)
            .isNotNull()
            .hasSize(1);
    return caseDefinitions.get(0);
  }

  private ProcessDefinition findOnlyProcessDefinition() {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(1);
    return processDefinitions.get(0);
  }

  private DecisionDefinition findOnlyDecisionDefinition() {
    List<DecisionDefinition> decisionDefinitions = repositoryService.createDecisionDefinitionQuery().list();
    assertThat(decisionDefinitions)
            .isNotNull()
            .hasSize(1);
    return decisionDefinitions.get(0);
  }

  @Test
  void testProcessDefinitionIntrospection() {
    String deploymentId = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml")
      .deploy()
      .getId();

    String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
    ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(procDefId);

    assertThat(processDefinition.getId()).isEqualTo(procDefId);
    assertThat(processDefinition.getName()).isEqualTo("Process One");
    assertThat(processDefinition.getProperty("documentation")).isEqualTo("the first process");

    PvmActivity start = processDefinition.findActivity("start");
    assertThat(start).isNotNull();
    assertThat(start.getId()).isEqualTo("start");
    assertThat(start.getProperty("name")).isEqualTo("S t a r t");
    assertThat(start.getProperty("documentation")).isEqualTo("the start event");
    assertThat(start.getActivities()).isEqualTo(Collections.emptyList());
    List<PvmTransition> outgoingTransitions = start.getOutgoingTransitions();
    assertThat(outgoingTransitions).hasSize(1);
    assertThat(outgoingTransitions.get(0).getProperty(BpmnParse.PROPERTYNAME_CONDITION_TEXT)).isEqualTo("${a == b}");

    PvmActivity end = processDefinition.findActivity("end");
    assertThat(end).isNotNull();
    assertThat(end.getId()).isEqualTo("end");

    PvmTransition transition = outgoingTransitions.get(0);
    assertThat(transition.getId()).isEqualTo("flow1");
    assertThat(transition.getProperty("name")).isEqualTo("Flow One");
    assertThat(transition.getProperty("documentation")).isEqualTo("The only transitions in the process");
    assertThat(transition.getSource()).isSameAs(start);
    assertThat(transition.getDestination()).isSameAs(end);

    repositoryService.deleteDeployment(deploymentId);
  }

  @Test
  void testProcessDefinitionQuery() {
    String deployment1Id = repositoryService
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml")
      .deploy()
      .getId();

    List<ProcessDefinition> processDefinitions = repositoryService
      .createProcessDefinitionQuery()
      .orderByProcessDefinitionName().asc().orderByProcessDefinitionVersion().asc()
      .list();

    assertThat(processDefinitions).hasSize(2);

    String deployment2Id = repositoryService
            .createDeployment()
            .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml")
            .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml")
            .deploy()
            .getId();

    assertThat(repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionName().asc().count()).isEqualTo(4);
    assertThat(repositoryService.createProcessDefinitionQuery().latestVersion().orderByProcessDefinitionName().asc().count()).isEqualTo(2);

    deleteDeployments(Arrays.asList(deployment1Id, deployment2Id));
  }

  @Test
  void testGetProcessDefinitions() {
    List<String> deploymentIds = new ArrayList<>();
    deploymentIds.add(deployProcessString("<definitions " + NAMESPACE + " " + TARGET_NAMESPACE + ">" + "  <process id='IDR' name='Insurance Damage Report 1' isExecutable='true'><startEvent id='start'/></process></definitions>"));
    deploymentIds.add(deployProcessString("<definitions " + NAMESPACE + " " + TARGET_NAMESPACE + ">" + "  <process id='IDR' name='Insurance Damage Report 2' isExecutable='true'><startEvent id='start'/></process></definitions>"));
    deploymentIds.add(deployProcessString("<definitions " + NAMESPACE + " " + TARGET_NAMESPACE + ">" + "  <process id='IDR' name='Insurance Damage Report 3' isExecutable='true'><startEvent id='start'/></process></definitions>"));
    deploymentIds.add(deployProcessString("<definitions " + NAMESPACE + " " + TARGET_NAMESPACE + ">" + "  <process id='EN' name='Expense Note 1' isExecutable='true'><startEvent id='start'/></process></definitions>"));
    deploymentIds.add(deployProcessString("<definitions " + NAMESPACE + " " + TARGET_NAMESPACE + ">" + "  <process id='EN' name='Expense Note 2' isExecutable='true'><startEvent id='start'/></process></definitions>"));

    List<ProcessDefinition> processDefinitions = repositoryService
      .createProcessDefinitionQuery()
      .orderByProcessDefinitionKey().asc()
      .orderByProcessDefinitionVersion().desc()
      .list();

    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(5);

    ProcessDefinition processDefinition = processDefinitions.get(0);
    assertThat(processDefinition.getKey()).isEqualTo("EN");
    assertThat(processDefinition.getName()).isEqualTo("Expense Note 2");
    assertThat(processDefinition.getId()).startsWith("EN:2");
    assertThat(processDefinition.getVersion()).isEqualTo(2);

    processDefinition = processDefinitions.get(1);
    assertThat(processDefinition.getKey()).isEqualTo("EN");
    assertThat(processDefinition.getName()).isEqualTo("Expense Note 1");
    assertThat(processDefinition.getId()).startsWith("EN:1");
    assertThat(processDefinition.getVersion()).isEqualTo(1);

    processDefinition = processDefinitions.get(2);
    assertThat(processDefinition.getKey()).isEqualTo("IDR");
    assertThat(processDefinition.getName()).isEqualTo("Insurance Damage Report 3");
    assertThat(processDefinition.getId()).startsWith("IDR:3");
    assertThat(processDefinition.getVersion()).isEqualTo(3);

    processDefinition = processDefinitions.get(3);
    assertThat(processDefinition.getKey()).isEqualTo("IDR");
    assertThat(processDefinition.getName()).isEqualTo("Insurance Damage Report 2");
    assertThat(processDefinition.getId()).startsWith("IDR:2");
    assertThat(processDefinition.getVersion()).isEqualTo(2);

    processDefinition = processDefinitions.get(4);
    assertThat(processDefinition.getKey()).isEqualTo("IDR");
    assertThat(processDefinition.getName()).isEqualTo("Insurance Damage Report 1");
    assertThat(processDefinition.getId()).startsWith("IDR:1");
    assertThat(processDefinition.getVersion()).isEqualTo(1);

    deleteDeployments(deploymentIds);
  }

  @Test
  void testDeployIdenticalProcessDefinitions() {
    List<String> deploymentIds = new ArrayList<>();
    deploymentIds.add(deployProcessString("<definitions " + NAMESPACE + " " + TARGET_NAMESPACE + "><process id='IDR' name='Insurance Damage Report' isExecutable='true'><startEvent id='start'/></process></definitions>"));
    deploymentIds.add(deployProcessString("<definitions " + NAMESPACE + " " + TARGET_NAMESPACE + "><process id='IDR' name='Insurance Damage Report' isExecutable='true'><startEvent id='start'/></process></definitions>"));

    List<ProcessDefinition> processDefinitions = repositoryService
      .createProcessDefinitionQuery()
      .orderByProcessDefinitionKey().asc()
      .orderByProcessDefinitionVersion().desc()
      .list();

    assertThat(processDefinitions)
            .isNotNull()
            .hasSize(2);

    ProcessDefinition processDefinition = processDefinitions.get(0);
    assertThat(processDefinition.getKey()).isEqualTo("IDR");
    assertThat(processDefinition.getName()).isEqualTo("Insurance Damage Report");
    assertThat(processDefinition.getId()).startsWith("IDR:2");
    assertThat(processDefinition.getVersion()).isEqualTo(2);

    processDefinition = processDefinitions.get(1);
    assertThat(processDefinition.getKey()).isEqualTo("IDR");
    assertThat(processDefinition.getName()).isEqualTo("Insurance Damage Report");
    assertThat(processDefinition.getId()).startsWith("IDR:1");
    assertThat(processDefinition.getVersion()).isEqualTo(1);

    deleteDeployments(deploymentIds);
  }

  @Test
  @Deployment(resources = {
    "org/operaton/bpm/engine/test/api/repository/call-activities-with-references.bpmn",
    "org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml",
    "org/operaton/bpm/engine/test/api/repository/three_.cmmn"
  })
  void shouldReturnStaticCalledProcessDefinitions() {
    //given
    testRule.deploy("org/operaton/bpm/engine/test/api/repository/second-process.bpmn20.xml");
    testRule.deployForTenant("someTenant", "org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml");

    ProcessDefinition processDefinition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("TestCallActivitiesWithReferences")
      .singleResult();

    String callingProcessId = processDefinition.getId();

    //when
    Collection<CalledProcessDefinition> mappings = repositoryService.getStaticCalledProcessDefinitions(callingProcessId);

    //then
    //cmmn tasks are not resolved
    assertThat(mappings).hasSize(4);

    assertThat(mappings.stream()
      .filter(def -> def.getId().startsWith("process:1:"))
      .flatMap(def -> def.getCalledFromActivityIds().stream())
      .toList())
      .containsExactlyInAnyOrder("deployment_1", "version_1");

    assertThat(mappings).extracting("name", "version", "key","calledFromActivityIds", "versionTag", "callingProcessDefinitionId")
      .contains(
        Tuple.tuple("Process One", 1, "processOne", List.of("tenant_reference_1"), null, callingProcessId),
        Tuple.tuple("Second Test Process", 2, "process", List.of("latest_reference_1"), null, callingProcessId),
        Tuple.tuple("Failing Process", 1, "failingProcess", List.of("version_tag_reference_1"), "ver_tag_2", callingProcessId));

    for (CalledProcessDefinition called : mappings) {
      ProcessDefinition deployedProcessDefinition = repositoryService.getProcessDefinition(called.getId());
      assertThat(called)
        .usingRecursiveComparison()
        .ignoringFields("calledFromActivityIds", "callingProcessDefinitionId", "suspended")
        .isEqualTo(deployedProcessDefinition);
    }
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/repository/dynamic-call-activities.bpmn" })
  void shouldNotTryToResolveDynamicCalledElementBinding() {
    //given
    ProcessDefinition processDefinition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("DynamicCallActivities")
      .singleResult();

    List<ActivityImpl> callActivities = ((ProcessDefinitionImpl) repositoryService
      .getProcessDefinition(processDefinition.getId())).getActivities().stream()
      .filter(act -> act.getActivityBehavior() instanceof CallActivityBehavior)
      .peek(activity -> {
        CallableElement callableElement = ((CallActivityBehavior) activity.getActivityBehavior()).getCallableElement();
        CallableElement spy = Mockito.spy(callableElement);
        ((CallActivityBehavior) activity.getActivityBehavior()).setCallableElement(spy);
      }).toList();

    //when
    Collection<CalledProcessDefinition> mappings = repositoryService.getStaticCalledProcessDefinitions(processDefinition.getId());

    //then
    //check that we never try to resolve any of the dynamic bindings
    for (ActivityImpl activity : callActivities) {
      CallableElement callableElement = ((CallActivityBehavior) activity.getActivityBehavior()).getCallableElement();
      Mockito.verify(callableElement, Mockito.never()).getDefinitionKey(Mockito.any());
      Mockito.verify(callableElement, Mockito.never()).getVersion(Mockito.any());
      Mockito.verify(callableElement, Mockito.never()).getVersionTag(Mockito.any());
      Mockito.verify(callableElement, Mockito.never()).getDefinitionTenantId(Mockito.any(), Mockito.anyString());
      Mockito.verify(callableElement, Mockito.times(1)).hasDynamicReferences();
    }

    assertThat(mappings).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml" )
  void shouldReturnEmptyListIfNoCallActivityExists(){
    //given
    ProcessDefinition processDefinition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey("process")
      .singleResult();

    //when
    Collection<CalledProcessDefinition> maps = repositoryService.getStaticCalledProcessDefinitions(processDefinition.getId());

    //then
    assertThat(maps).isEmpty();
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/repository/nested-call-activities.bpmn",
      "org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml" })
  void shouldReturnCalledProcessDefinitionsForNestedCallActivities() {
    //given
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("nested-call-activities")
        .singleResult();

    //when
    Collection<CalledProcessDefinition> calledProcessDefinitions = repositoryService
        .getStaticCalledProcessDefinitions(processDefinition.getId());

    //then
    assertThat(calledProcessDefinitions).hasSize(1);
    CalledProcessDefinition calledProcessDefinition = new ArrayList<>(calledProcessDefinitions).get(0);
    assertThat(calledProcessDefinition.getKey()).isEqualTo("failingProcess");
    assertThat(
        calledProcessDefinition.getCalledFromActivityIds().stream().distinct().toList()).hasSize(8);
  }

  @Test
  void testGetStaticCallActivityMappingShouldThrowIfProcessDoesNotExist(){
    //given //when //then
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> repositoryService.getStaticCalledProcessDefinitions("notExistingId"));
  }

  @Test
  void shouldReturnCorrectProcessesForCallActivityWithTenantId(){
    //given
    final String processOne = "org/operaton/bpm/engine/test/api/repository/processOne.bpmn20.xml";
    final String processTwo = "org/operaton/bpm/engine/test/api/repository/processTwo.bpmn20.xml";

    final String aTenant = "aTenant";
    final String anotherTenant = "anotherTenant";

    String id = testRule.deployForTenantAndGetDefinition(aTenant,
      "org/operaton/bpm/engine/test/api/repository/call_activities_with_tenants.bpmn").getId();
    testRule.deployForTenant(anotherTenant, processTwo);
    String sameTenantProcessOne = testRule.deployForTenantAndGetDefinition(aTenant, processOne).getId();
    String otherTenantProcessOne = testRule.deployForTenantAndGetDefinition(anotherTenant, processOne).getId();
    // these two processes should not be picked up even though they are newer because they are not deployed for a tenant.
    testRule.deploy(processOne);
    testRule.deploy(processTwo);

    //when
    Collection<CalledProcessDefinition> mappings = repositoryService.getStaticCalledProcessDefinitions(id);

    //then
    assertThat(mappings).hasSize(2);

    assertThat(mappings.stream()
      .filter(def -> def.getId().equals(sameTenantProcessOne))
      .flatMap(def -> def.getCalledFromActivityIds().stream())
      .toList())
      .containsExactlyInAnyOrder("null_tenant_reference_same_tenant", "explicit_same_tenant_reference");

    assertThat(mappings).extracting("id","calledFromActivityIds", "callingProcessDefinitionId")
      .contains(
        Tuple.tuple(otherTenantProcessOne, List.of("explicit_other_tenant_reference"), id));
  }

  private String deployProcessString(String processString) {
    String resourceName = "xmlString." + BPMN_RESOURCE_SUFFIXES[0];
    return repositoryService.createDeployment().addString(resourceName, processString).deploy().getId();
  }

  private void deleteDeployments(Collection<String> deploymentIds) {
    for (String deploymentId : deploymentIds) {
      repositoryService.deleteDeployment(deploymentId);
    }
  }

}
