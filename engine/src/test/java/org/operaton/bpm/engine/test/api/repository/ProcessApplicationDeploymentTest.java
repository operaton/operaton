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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.application.ProcessApplicationRegistration;
import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.application.ProcessApplicationManager;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentHandlerFactory;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.repository.ProcessApplicationDeployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.repository.ResumePreviousBy;
import org.operaton.bpm.engine.test.bpmn.deployment.VersionedDeploymentHandlerFactory;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeNotNull;

/**
 * @author Daniel Meyer
 *
 */
class ProcessApplicationDeploymentTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;
  ManagementService managementService;
  ProcessEngine processEngine;

  EmbeddedProcessApplication processApplication;
  DeploymentHandlerFactory defaultDeploymentHandlerFactory;
  DeploymentHandlerFactory customDeploymentHandlerFactory;

  ProcessApplicationManager processApplicationManager;
  DeploymentCache deploymentCache;
  Set<String> registeredDeployments;

  @BeforeEach
  void setUp() {
    defaultDeploymentHandlerFactory = processEngineConfiguration.getDeploymentHandlerFactory();
    customDeploymentHandlerFactory = new VersionedDeploymentHandlerFactory();
    processApplication = new EmbeddedProcessApplication();

    processApplicationManager = processEngineConfiguration.getProcessApplicationManager();
    deploymentCache = processEngineConfiguration.getDeploymentCache();
    registeredDeployments = processEngineConfiguration.getRegisteredDeployments();
  }

  @AfterEach
  void tearDown() {
    clearProcessApplicationDeployments();
    processApplication.undeploy();
    processEngineConfiguration.setDeploymentHandlerFactory(defaultDeploymentHandlerFactory);
    ClockUtil.reset();
  }

  @Test
  void testEmptyDeployment() {
    var deploymentBuilder = repositoryService.createDeployment(processApplication.getReference());
    var deploymentBuilder2 = repositoryService.createDeployment();
    assertThatThrownBy(deploymentBuilder::deploy).isInstanceOf(NotValidException.class);

    assertThatThrownBy(deploymentBuilder2::deploy).isInstanceOf(NotValidException.class);
  }

  @Test
  void testSimpleProcessApplicationDeployment() {
    // given
    ProcessApplicationDeployment deployment = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    // process is deployed:
    assertThatOneProcessIsDeployed();

    // when registration was performed:
    ProcessApplicationRegistration registration = deployment.getProcessApplicationRegistration();

    // then
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(1);
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testProcessApplicationDeploymentNoChanges() {
    // given: create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    assertThatOneProcessIsDeployed();

    // when
    // deploy update with no changes:
    ProcessApplicationDeployment deployment = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(false)
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    // no changes
    assertThatOneProcessIsDeployed();
    ProcessApplicationRegistration registration = deployment.getProcessApplicationRegistration();

    // then
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(1);
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testPartialChangesDeployAll() {
    // given
    BpmnModelInstance model1 = createEmptyModel("process1");
    BpmnModelInstance model2 = createEmptyModel("process2");

    // create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", model2));

    BpmnModelInstance changedModel2 = Bpmn.createExecutableProcess("process2")
        .startEvent()
        .done();

    // when
    // second deployment with partial changes:
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(false)
        .resumePreviousVersions()
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", changedModel2));

    // then
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(4);

    List<ProcessDefinition> processDefinitionsModel1 =
      repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process1")
        .orderByProcessDefinitionVersion().asc().list();

    // now there are two versions of process1 deployed
    assertThat(processDefinitionsModel1).hasSize(2);
    assertThat(processDefinitionsModel1.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitionsModel1.get(1).getVersion()).isEqualTo(2);

    // now there are two versions of process2 deployed
    List<ProcessDefinition> processDefinitionsModel2 =
        repositoryService
          .createProcessDefinitionQuery()
          .processDefinitionKey("process1")
          .orderByProcessDefinitionVersion().asc().list();

    assertThat(processDefinitionsModel2).hasSize(2);
    assertThat(processDefinitionsModel2.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitionsModel2.get(1).getVersion()).isEqualTo(2);

    // old deployment was resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(2);
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  /**
   * Test re-deployment of only those resources that have actually changed
   */
  @Test
  void testPartialChangesDeployChangedOnly() {
    BpmnModelInstance model1 = createEmptyModel("process1");
    BpmnModelInstance model2 = createEmptyModel("process2");

    // create initial deployment
    ProcessApplicationDeployment deployment1 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", model2));

    BpmnModelInstance changedModel2 = Bpmn.createExecutableProcess("process2")
        .startEvent()
        .done();

    // second deployment with partial changes:
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(true)
        .resumePreviousVersions()
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", changedModel2));

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);

    // there is one version of process1 deployed
    ProcessDefinition processDefinitionModel1 =
        repositoryService
          .createProcessDefinitionQuery()
          .processDefinitionKey("process1")
          .singleResult();

    assertThat(processDefinitionModel1).isNotNull();
    assertThat(processDefinitionModel1.getVersion()).isEqualTo(1);
    assertThat(processDefinitionModel1.getDeploymentId()).isEqualTo(deployment1.getId());

    // there are two versions of process2 deployed
    List<ProcessDefinition> processDefinitionsModel2 =
        repositoryService
          .createProcessDefinitionQuery()
          .processDefinitionKey("process2")
          .orderByProcessDefinitionVersion().asc().list();

    assertThat(processDefinitionsModel2).hasSize(2);
    assertThat(processDefinitionsModel2.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitionsModel2.get(1).getVersion()).isEqualTo(2);

    // old deployment was resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(2);

    BpmnModelInstance anotherChangedModel2 = Bpmn.createExecutableProcess("process2")
        .startEvent()
        .endEvent()
        .done();

    // testing with a third deployment to ensure the change check is not only performed against
    // the last version of the deployment
    ProcessApplicationDeployment deployment3 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .enableDuplicateFiltering(true)
        .resumePreviousVersions()
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", anotherChangedModel2)
        .name("deployment"));

    // there should still be one version of process 1
    assertThat(repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("process1")
      .count()).isOne();

    // there should be three versions of process 2
    assertThat(repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("process2")
        .count()).isEqualTo(3);

    // old deployments are resumed
    registration = deployment3.getProcessApplicationRegistration();
    deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(3);
  }

  @Test
  void testDuplicateFilteringDefaultBehavior() {
    // given
    BpmnModelInstance oldModel = Bpmn.createExecutableProcess("versionedProcess")
      .operatonVersionTag("3").done();
    BpmnModelInstance newModel = Bpmn.createExecutableProcess("versionedProcess")
      .operatonVersionTag("1").done();

    testRule.deploy(repositoryService.createDeployment(processApplication.getReference())
      .enableDuplicateFiltering(true)
      .addModelInstance("model", oldModel)
      .name("defaultDeploymentHandling"));

    // when
    testRule.deploy(repositoryService.createDeployment(processApplication.getReference())
      .enableDuplicateFiltering(true)
      .addModelInstance("model", newModel)
      .name("defaultDeploymentHandling"));

    // then
    long deploymentCount = repositoryService.createDeploymentQuery().count();
    assertThat(deploymentCount).isEqualTo(2);
  }

  @Test
  void testDuplicateFilteringCustomBehavior() {
    // given
    processEngineConfiguration.setDeploymentHandlerFactory( customDeploymentHandlerFactory);
    BpmnModelInstance oldModel = Bpmn.createExecutableProcess("versionedProcess")
      .operatonVersionTag("1").startEvent().done();
    BpmnModelInstance newModel = Bpmn.createExecutableProcess("versionedProcess")
      .operatonVersionTag("3").startEvent().done();

    Deployment deployment1 = testRule.deploy(
        repositoryService
            .createDeployment(processApplication.getReference())
            .enableDuplicateFiltering(true)
            .addModelInstance("model.bpmn", oldModel)
            .name("customDeploymentHandling"));

    // when
    testRule.deploy(repositoryService.createDeployment(processApplication.getReference())
      .enableDuplicateFiltering(true)
      .addModelInstance("model.bpmn", newModel)
      .name("customDeploymentHandling"));

    Deployment deployment3 = testRule.deploy(
        repositoryService
            .createDeployment(processApplication.getReference())
            .enableDuplicateFiltering(true)
            .addModelInstance("model.bpmn", oldModel)
            .name("customDeploymentHandling"));

    // then
    long deploymentCount = repositoryService.createDeploymentQuery().count();
    assertThat(deploymentCount).isEqualTo(2);
    assertThat(deployment3.getId()).isEqualTo(deployment1.getId());
  }

  @Test
  void testPartialChangesResumePreviousVersion() {
    BpmnModelInstance model1 = createEmptyModel("process1");
    BpmnModelInstance model2 = createEmptyModel("process2");

    // create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addModelInstance("process1.bpmn20.xml", model1));

    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(true)
        .resumePreviousVersions()
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", model2));

    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    assertThat(registration.getDeploymentIds()).hasSize(2);
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersions() {
    // create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    assertThatOneProcessIsDeployed();

    // deploy update with changes:
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(false)
        .resumePreviousVersions()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version2.bpmn20.xml"));

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion()
        .asc()
        .list();
    // now there are 2 process definitions deployed
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(2);

    // old deployment was resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(2);
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersionsDifferentKeys() {
    // create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    assertThatOneProcessIsDeployed();

    // deploy update with changes:
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .resumePreviousVersions()
        .addClasspathResource("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"));

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion()
        .asc()
        .list();
    // now there are 2 process definitions deployed
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(1);

    // and the old deployment was not resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(1);
    assertThat(deploymentIds.iterator().next()).isEqualTo(deployment2.getId());
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersionsDefaultBehavior() {
    // given
    BpmnModelInstance model1 = createEmptyModel("process1");
    BpmnModelInstance model2 = createEmptyModel("process2");

    // create initial deployment
    testRule.deploy(repositoryService.createDeployment(processApplication.getReference())
      .name("defaultDeploymentHandling")
      .addModelInstance("process1.bpmn20.xml", model1));

    // when
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("defaultDeploymentHandling")
        .enableDuplicateFiltering(true)
        .resumePreviousVersions()
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", model2));

    // then
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    assertThat(registration.getDeploymentIds()).hasSize(2);
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersionsCustomBehavior() {
    // given
    processEngineConfiguration.setDeploymentHandlerFactory(customDeploymentHandlerFactory);
    BpmnModelInstance oldModel = Bpmn.createExecutableProcess("process")
        .operatonVersionTag("1")
        .startEvent()
        .done();
    BpmnModelInstance newModel = Bpmn.createExecutableProcess("process")
        .operatonVersionTag("3")
        .startEvent()
        .done();

    // create initial deployment
    ProcessApplicationDeployment deployment1 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("customDeploymentHandling")
        .addModelInstance("process1.bpmn20.xml", oldModel));

    // when
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("customDeploymentHandling")
        .enableDuplicateFiltering(true)
        .resumePreviousVersions()
        .addModelInstance("process1.bpmn20.xml", newModel));

    ProcessApplicationDeployment deployment3 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("customDeploymentHandling")
        .enableDuplicateFiltering(true)
        .resumePreviousVersions()
        .addModelInstance("process1.bpmn20.xml", oldModel));

    // then
    // PA2 registers only it's own (new) version of the model
    ProcessApplicationRegistration registration2 = deployment2.getProcessApplicationRegistration();
    assertThat(registration2.getDeploymentIds()).hasSize(1);

    // PA3 deploys a duplicate version of the process. The duplicate deployment needs to be found
    // and registered (deployment1)
    ProcessApplicationRegistration registration3 = deployment3.getProcessApplicationRegistration();
    assertThat(registration3.getDeploymentIds()).hasSize(1);
    assertThat(registration3.getDeploymentIds().iterator().next()).isEqualTo(deployment1.getId());
  }

  @Test
  void testProcessApplicationDeploymentNoResume() {
    // create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    assertThatOneProcessIsDeployed();

    // deploy update with changes:
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(false)
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version2.bpmn20.xml"));

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion()
        .asc()
        .list();
    // now there are 2 process definitions deployed
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(2);

    // old deployment was NOT resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(1);
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersionsByDeploymentNameDefaultBehavior() {
    // create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    assertThatOneProcessIsDeployed();

    // deploy update with changes:
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(false)
        .resumePreviousVersions()
        .resumePreviousVersionsBy(ResumePreviousBy.RESUME_BY_DEPLOYMENT_NAME)
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version2.bpmn20.xml"));

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion()
        .asc()
        .list();
    // now there are 2 process definitions deployed
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(2);

    // old deployment was resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(2);
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersionsByDeploymentNameCustomBehavior() {
    // given
    BpmnModelInstance oldProcess =
        Bpmn.createExecutableProcess("process").operatonVersionTag("1").startEvent().done();
    BpmnModelInstance newProcess =
        Bpmn.createExecutableProcess("process").operatonVersionTag("2").startEvent().done();

    // set custom deployment handler
    processEngineConfiguration.setDeploymentHandlerFactory(customDeploymentHandlerFactory);

    // initial deployment is created
    testRule.deploy(
        repositoryService
            .createDeployment(processApplication.getReference())
            .name("deployment")
            .addModelInstance("version1.bpmn20.xml", oldProcess));

    // when
    // update with changes is deployed
    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(false)
        .resumePreviousVersions()
        .resumePreviousVersionsBy(ResumePreviousBy.RESUME_BY_DEPLOYMENT_NAME)
        .addModelInstance("version2.bpmn20.xml", newProcess));

    // then
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion()
        .asc()
        .list();
    // now there are 2 process definitions deployed
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(2);

    // old deployment was resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> paDeploymentIds = registration.getDeploymentIds();
    assertThat(paDeploymentIds).containsExactly(deployment2.getId());
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersionsByDeploymentNameDeployDifferentProcesses(){
    BpmnModelInstance process1 = createEmptyModel("process1");
    BpmnModelInstance process2 = createEmptyModel("process2");
    testRule.deploy(repositoryService
            .createDeployment(processApplication.getReference())
            .name("deployment")
            .addModelInstance("process1.bpmn", process1));

    assertThatOneProcessIsDeployed();

    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .resumePreviousVersions()
        .resumePreviousVersionsBy(ResumePreviousBy.RESUME_BY_DEPLOYMENT_NAME)
        .addModelInstance("process2.bpmn", process2));

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion()
        .asc()
        .list();
    // now there are 2 process definitions deployed but both with version 1
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(1);

    // old deployment was resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(2);
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testProcessApplicationDeploymentResumePreviousVersionsByDeploymentNameNoResume(){
    BpmnModelInstance process1 = createEmptyModel("process1");
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addModelInstance("process1.bpmn", process1));

    assertThatOneProcessIsDeployed();

    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("anotherDeployment")
        .resumePreviousVersions()
        .resumePreviousVersionsBy(ResumePreviousBy.RESUME_BY_DEPLOYMENT_NAME)
        .addModelInstance("process2.bpmn", process1));

    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion()
        .asc()
        .list();
    // there is a new version of the process
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(2);

    // but the old deployment was not resumed
    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    Set<String> deploymentIds = registration.getDeploymentIds();
    assertThat(deploymentIds).hasSize(1);
    assertThat(deploymentIds.iterator().next()).isEqualTo(deployment2.getId());
    assertThat(registration.getProcessEngineName()).isEqualTo(processEngine.getName());
  }

  @Test
  void testPartialChangesResumePreviousVersionByDeploymentName() {
    BpmnModelInstance model1 = createEmptyModel("process1");
    BpmnModelInstance model2 = createEmptyModel("process2");

    // create initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addModelInstance("process1.bpmn20.xml", model1));

    ProcessApplicationDeployment deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(true)
        .resumePreviousVersions()
        .resumePreviousVersionsBy(ResumePreviousBy.RESUME_BY_DEPLOYMENT_NAME)
        .addModelInstance("process1.bpmn20.xml", model1)
        .addModelInstance("process2.bpmn20.xml", model2));

    ProcessApplicationRegistration registration = deployment2.getProcessApplicationRegistration();
    assertThat(registration.getDeploymentIds()).hasSize(2);
  }

  @Test
  void testProcessApplicationDeploymentResumptionDoesNotCachePreviousBpmnModelInstance() {
    // given an initial deployment
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version1.bpmn20.xml"));

    deploymentCache.discardProcessDefinitionCache();

    // when an update with changes is deployed
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("deployment")
        .enableDuplicateFiltering(false)
        .resumePreviousVersions()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/version2.bpmn20.xml"));

    // then the cache is still empty
    assertThat(deploymentCache.getBpmnModelInstanceCache().isEmpty()).isTrue();
  }

  @Test
  void testDeploymentSourceShouldBeNull() {
    // given
    String key = "process";
    BpmnModelInstance model = createEmptyModel(key);
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
    testRule.deploy(repositoryService
        .createDeployment()
        .name("first-deployment-without-a-source")
        .addModelInstance("process.bpmn", model));

    assertThat(deploymentQuery.deploymentName("first-deployment-without-a-source")
        .singleResult()
        .getSource()).isNull();

    // when
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("second-deployment-with-a-source")
        .source(null)
        .addModelInstance("process.bpmn", model));

    // then
    assertThat(deploymentQuery.deploymentName("second-deployment-with-a-source")
        .singleResult()
        .getSource()).isNull();
  }

  @Test
  void testDeploymentSourceShouldNotBeNull() {
    // given
    String key = "process";
    BpmnModelInstance model = createEmptyModel(key);
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
    testRule.deploy(repositoryService
        .createDeployment()
        .name("first-deployment-without-a-source")
        .source("my-first-deployment-source")
        .addModelInstance("process.bpmn", model));

    assertThat(deploymentQuery.deploymentName("first-deployment-without-a-source")
        .singleResult()
        .getSource()).isEqualTo("my-first-deployment-source");

    // when
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("second-deployment-with-a-source")
        .source("my-second-deployment-source")
        .addModelInstance("process.bpmn", model));

    // then
    assertThat(deploymentQuery.deploymentName("second-deployment-with-a-source")
        .singleResult()
        .getSource()).isEqualTo("my-second-deployment-source");
  }

  @Test
  void testDefaultDeploymentSource() {
    // given
    String key = "process";
    BpmnModelInstance model = createEmptyModel(key);
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    // when
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("first-deployment-with-a-source")
        .addModelInstance("process.bpmn", model));

    // then
    assertThat(deploymentQuery.deploymentName("first-deployment-with-a-source")
        .singleResult()
        .getSource()).isEqualTo(ProcessApplicationDeployment.PROCESS_APPLICATION_DEPLOYMENT_SOURCE);
  }

  @Test
  void testOverwriteDeploymentSource() {
    // given
    String key = "process";
    BpmnModelInstance model = createEmptyModel(key);
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    // when
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("first-deployment-with-a-source")
        .source("my-source")
        .addModelInstance("process.bpmn", model));

    // then
    assertThat(deploymentQuery.deploymentName("first-deployment-with-a-source")
        .singleResult()
        .getSource()).isEqualTo("my-source");
  }

  @Test
  void testNullDeploymentSourceAwareDuplicateFilter() {
    // given
    String key = "process";
    String name = "my-deployment";

    BpmnModelInstance model = createEmptyModel(key);

    ProcessDefinitionQuery processDefinitionQuery = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(key);

    DeploymentQuery deploymentQuery = repositoryService
        .createDeploymentQuery()
        .deploymentName(name);

    // when

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .source(null)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .source(null)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    // then

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();
  }

  @Test
  void testNullAndProcessApplicationDeploymentSourceAwareDuplicateFilter() {
    // given

    String key = "process";
    String name = "my-deployment";

    BpmnModelInstance model = createEmptyModel(key);

    ProcessDefinitionQuery processDefinitionQuery = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(key);

    DeploymentQuery deploymentQuery = repositoryService
        .createDeploymentQuery()
        .deploymentName(name);

    // when

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .source(null)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    // then

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();
  }

  @Test
  void testProcessApplicationAndNullDeploymentSourceAwareDuplicateFilter() {
    // given

    String key = "process";
    String name = "my-deployment";

    BpmnModelInstance model = createEmptyModel(key);

    ProcessDefinitionQuery processDefinitionQuery = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(key);

    DeploymentQuery deploymentQuery = repositoryService
        .createDeploymentQuery()
        .deploymentName(name);

    // when

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .source(null)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    // then

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();
  }

  @Test
  void testProcessApplicationDeploymentSourceAwareDuplicateFilter() {
    // given

    String key = "process";
    String name = "my-deployment";

    BpmnModelInstance model = createEmptyModel(key);

    ProcessDefinitionQuery processDefinitionQuery = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(key);

    DeploymentQuery deploymentQuery = repositoryService
        .createDeploymentQuery()
        .deploymentName(name);

    // when

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    // then

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();
  }

  @Test
  void testSameDeploymentSourceAwareDuplicateFilter() {
    // given

    String key = "process";
    String name = "my-deployment";

    BpmnModelInstance model = createEmptyModel(key);

    ProcessDefinitionQuery processDefinitionQuery = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(key);

    DeploymentQuery deploymentQuery = repositoryService
        .createDeploymentQuery()
        .deploymentName(name);

    // when

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name(name)
        .source("cockpit")
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();

    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("my-deployment")
        .source("cockpit")
        .addModelInstance("process.bpmn", model)
        .enableDuplicateFiltering(true));

    // then

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();
  }

  @ParameterizedTest
  @CsvSource({
      "my-source1, my-source2",
      "null, my-source2",
      "my-source1, null"
  })
  void shouldDeployNewVersion (String firstSource, String secondSource) {
    // given
    firstSource = "null".equals(firstSource) ? null : firstSource;
    secondSource = "null".equals(secondSource) ? null : secondSource;

    String key = "process";
    String name = "my-deployment";

    BpmnModelInstance model = createEmptyModel(key);

    ProcessDefinitionQuery processDefinitionQuery = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey(key);

    DeploymentQuery deploymentQuery = repositoryService
      .createDeploymentQuery()
      .deploymentName(name);

    // when
    testRule.deploy(repositoryService
      .createDeployment(processApplication.getReference())
      .name(name)
      .source(firstSource)
      .addModelInstance("process.bpmn", model)
      .enableDuplicateFiltering(true));

    assertThat(processDefinitionQuery.count()).isOne();
    assertThat(deploymentQuery.count()).isOne();

    testRule.deploy(repositoryService
      .createDeployment(processApplication.getReference())
      .name(name)
      .source(secondSource)
      .addModelInstance("process.bpmn", model)
      .enableDuplicateFiltering(true));

    // then
    assertThat(processDefinitionQuery.count()).isEqualTo(2);
    assertThat(deploymentQuery.count()).isEqualTo(2);
  }

  @Test
  void testUnregisterProcessApplicationOnDeploymentDeletion() {
    // given a deployment with a process application registration
    Deployment deployment = testRule.deploy(repositoryService
        .createDeployment()
        .addModelInstance("process.bpmn", createEmptyModel("foo")));

    // and a process application registration
    managementService.registerProcessApplication(deployment.getId(),
                                                 processApplication.getReference());

    // when deleting the deployment
    repositoryService.deleteDeployment(deployment.getId(), true);

    // then the registration is removed
    assertThat(managementService.getProcessApplicationForDeployment(deployment.getId())).isNull();
  }

  /*
   * A delay is introduced between the two deployments so the test is valid when MySQL
   * is used. See https://jira.camunda.com/browse/CAM-11893 for more details.
   */
  @Test
  void shouldRegisterExistingDeploymentsOnLatestProcessDefinitionRemoval() {
    // given
    Date timeFreeze = new Date();
    ClockUtil.setCurrentTime(timeFreeze);
    BpmnModelInstance process1 = createEmptyModel("process");
    BpmnModelInstance process2 = Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

    DeploymentWithDefinitions deployment1 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("foo")
        .addModelInstance("process.bpmn", process1));

    // offset second deployment time to detect latest deployment with MySQL timestamps
    ClockUtil.offset(1000L);
    DeploymentWithDefinitions deployment2 = testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .name("foo")
        .addModelInstance("process.bpmn", process2)
        .resumePreviousVersions()
        .enableDuplicateFiltering(true));

    ProcessDefinition latestProcessDefinition = deployment2.getDeployedProcessDefinitions().get(0);

    // assume
    assumeNotNull(managementService.getProcessApplicationForDeployment(deployment1.getId()));
    assumeNotNull(managementService.getProcessApplicationForDeployment(deployment2.getId()));

    // delete latest process definition
    repositoryService.deleteProcessDefinition(latestProcessDefinition.getId());

    // stop process engine by clearing the caches
    clearProcessApplicationDeployments();

    // when
    testRule.deploy(repositoryService
        .createDeployment(processApplication.getReference())
        .addModelInstance("process.bpmn", process2)
        .resumePreviousVersions()
        .enableDuplicateFiltering(true)
        .name("foo"));

    // then
    assertThat(managementService.getProcessApplicationForDeployment(deployment1.getId())).isNotNull();
    assertThat(managementService.getProcessApplicationForDeployment(deployment2.getId())).isNotNull();
  }

  /*
   * Clears the deployment caches to simulate a stop of the process engine.
   */
  protected void clearProcessApplicationDeployments() {
    processApplicationManager.clearRegistrations();
    registeredDeployments.clear();
    deploymentCache.discardProcessDefinitionCache();
  }

  /**
   * Creates a process definition query and checks that only one process with version 1 is present.
   */
  protected void assertThatOneProcessIsDeployed() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
        .singleResult();
    assertThat(processDefinition).isNotNull();
    assertThat(processDefinition.getVersion()).isEqualTo(1);
  }

  protected BpmnModelInstance createEmptyModel(String key) {
    return Bpmn.createExecutableProcess(key).startEvent().done();
  }

}
