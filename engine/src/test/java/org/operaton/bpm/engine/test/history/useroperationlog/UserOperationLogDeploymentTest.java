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
package org.operaton.bpm.engine.test.history.useroperationlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class UserOperationLogDeploymentTest extends AbstractUserOperationLogTest {

  protected static final String DEPLOYMENT_NAME = "my-deployment";
  protected static final String RESOURCE_NAME = "path/to/my/process.bpmn";
  protected static final String PROCESS_KEY = "process";

  @After
  public void tearDown() {


    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    for (Deployment deployment : deployments) {
      repositoryService.deleteDeployment(deployment.getId(), true, true);
    }
  }

  @Test
  public void testCreateDeployment() {
    // when
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    // then
    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery().singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(deployment.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("duplicateFilterEnabled");
    assertNull(userOperationLogEntry.getOrgValue());
    assertFalse(Boolean.parseBoolean(userOperationLogEntry.getNewValue()));

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertNull(userOperationLogEntry.getJobDefinitionId());
    assertNull(userOperationLogEntry.getProcessInstanceId());
    assertNull(userOperationLogEntry.getProcessDefinitionId());
    assertNull(userOperationLogEntry.getProcessDefinitionKey());
    assertNull(userOperationLogEntry.getCaseInstanceId());
    assertNull(userOperationLogEntry.getCaseDefinitionId());
  }

  @Test
  public void testCreateDeploymentPa() {
    // given
    EmbeddedProcessApplication application = new EmbeddedProcessApplication();

    // when
    Deployment deployment = repositoryService
        .createDeployment(application.getReference())
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    // then
    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery().singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(deployment.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("duplicateFilterEnabled");
    assertNull(userOperationLogEntry.getOrgValue());
    assertFalse(Boolean.parseBoolean(userOperationLogEntry.getNewValue()));

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertNull(userOperationLogEntry.getJobDefinitionId());
    assertNull(userOperationLogEntry.getProcessInstanceId());
    assertNull(userOperationLogEntry.getProcessDefinitionId());
    assertNull(userOperationLogEntry.getProcessDefinitionKey());
    assertNull(userOperationLogEntry.getCaseInstanceId());
    assertNull(userOperationLogEntry.getCaseDefinitionId());
  }

  @Test
  public void testPropertyDuplicateFiltering() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    // when
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model)
        .enableDuplicateFiltering(false)
        .deploy();

    // then
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isEqualTo(2);

    // (1): duplicate filter enabled property
    UserOperationLogEntry logDuplicateFilterEnabledProperty = query.property("duplicateFilterEnabled").singleResult();
    assertNotNull(logDuplicateFilterEnabledProperty);

    assertThat(logDuplicateFilterEnabledProperty.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(logDuplicateFilterEnabledProperty.getDeploymentId()).isEqualTo(deployment.getId());
    assertThat(logDuplicateFilterEnabledProperty.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);

    assertThat(logDuplicateFilterEnabledProperty.getUserId()).isEqualTo(USER_ID);

    assertThat(logDuplicateFilterEnabledProperty.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(logDuplicateFilterEnabledProperty.getProperty()).isEqualTo("duplicateFilterEnabled");
    assertNull(logDuplicateFilterEnabledProperty.getOrgValue());
    assertTrue(Boolean.parseBoolean(logDuplicateFilterEnabledProperty.getNewValue()));

    // (2): deploy changed only
    UserOperationLogEntry logDeployChangedOnlyProperty = query.property("deployChangedOnly").singleResult();
    assertNotNull(logDeployChangedOnlyProperty);

    assertThat(logDeployChangedOnlyProperty.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(logDeployChangedOnlyProperty.getDeploymentId()).isEqualTo(deployment.getId());
    assertThat(logDeployChangedOnlyProperty.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(logDeployChangedOnlyProperty.getUserId()).isEqualTo(USER_ID);

    assertThat(logDeployChangedOnlyProperty.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(logDeployChangedOnlyProperty.getProperty()).isEqualTo("deployChangedOnly");
    assertNull(logDeployChangedOnlyProperty.getOrgValue());
    assertFalse(Boolean.parseBoolean(logDeployChangedOnlyProperty.getNewValue()));

    // (3): operation id
    assertThat(logDeployChangedOnlyProperty.getOperationId()).isEqualTo(logDuplicateFilterEnabledProperty.getOperationId());
  }

  @Test
  public void testPropertiesDuplicateFilteringAndDeployChangedOnly() {
    // given
    BpmnModelInstance model = createProcessWithServiceTask(PROCESS_KEY);

    // when
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, model)
        .enableDuplicateFiltering(true)
        .deploy();

    // then
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isEqualTo(2);

    // (1): duplicate filter enabled property
    UserOperationLogEntry logDuplicateFilterEnabledProperty = query.property("duplicateFilterEnabled").singleResult();
    assertNotNull(logDuplicateFilterEnabledProperty);
    assertThat(logDuplicateFilterEnabledProperty.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(logDuplicateFilterEnabledProperty.getDeploymentId()).isEqualTo(deployment.getId());
    assertThat(logDuplicateFilterEnabledProperty.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(logDuplicateFilterEnabledProperty.getUserId()).isEqualTo(USER_ID);

    assertThat(logDuplicateFilterEnabledProperty.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(logDuplicateFilterEnabledProperty.getProperty()).isEqualTo("duplicateFilterEnabled");
    assertNull(logDuplicateFilterEnabledProperty.getOrgValue());
    assertTrue(Boolean.parseBoolean(logDuplicateFilterEnabledProperty.getNewValue()));

    // (2): deploy changed only
    UserOperationLogEntry logDeployChangedOnlyProperty = query.property("deployChangedOnly").singleResult();
    assertNotNull(logDeployChangedOnlyProperty);

    assertThat(logDeployChangedOnlyProperty.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(logDeployChangedOnlyProperty.getDeploymentId()).isEqualTo(deployment.getId());
    assertThat(logDeployChangedOnlyProperty.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(logDeployChangedOnlyProperty.getUserId()).isEqualTo(USER_ID);

    assertThat(logDeployChangedOnlyProperty.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(logDeployChangedOnlyProperty.getProperty()).isEqualTo("deployChangedOnly");
    assertNull(logDeployChangedOnlyProperty.getOrgValue());
    assertTrue(Boolean.parseBoolean(logDeployChangedOnlyProperty.getNewValue()));

    // (3): operation id
    assertThat(logDeployChangedOnlyProperty.getOperationId()).isEqualTo(logDuplicateFilterEnabledProperty.getOperationId());
  }

  @Test
  public void testDeleteDeploymentCascadingShouldKeepCreateUserOperationLog() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE);

    assertThat(query.count()).isEqualTo(1);

    // when
    repositoryService.deleteDeployment(deployment.getId(), true);

    // then
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testDeleteDeploymentWithoutCascadingShouldKeepCreateUserOperationLog() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE);

    assertThat(query.count()).isEqualTo(1);

    // when
    repositoryService.deleteDeployment(deployment.getId(), false);

    // then
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testDeleteDeployment() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    // when
    repositoryService.deleteDeployment(deployment.getId(), false);

    // then
    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry log = query.singleResult();
    assertNotNull(log);

    assertThat(log.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(log.getDeploymentId()).isEqualTo(deployment.getId());

    assertThat(log.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    assertThat(log.getProperty()).isEqualTo("cascade");
    assertNull(log.getOrgValue());
    assertFalse(Boolean.parseBoolean(log.getNewValue()));

    assertThat(log.getUserId()).isEqualTo(USER_ID);

    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertNull(log.getJobDefinitionId());
    assertNull(log.getProcessInstanceId());
    assertNull(log.getProcessDefinitionId());
    assertNull(log.getProcessDefinitionKey());
    assertNull(log.getCaseInstanceId());
    assertNull(log.getCaseDefinitionId());
  }

  @Test
  public void testDeleteDeploymentCascading() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    // when
    repositoryService.deleteDeployment(deployment.getId(), true);

    // then
    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry log = query.singleResult();
    assertNotNull(log);

    assertThat(log.getEntityType()).isEqualTo(EntityTypes.DEPLOYMENT);
    assertThat(log.getDeploymentId()).isEqualTo(deployment.getId());

    assertThat(log.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    assertThat(log.getProperty()).isEqualTo("cascade");
    assertNull(log.getOrgValue());
    assertTrue(Boolean.parseBoolean(log.getNewValue()));

    assertThat(log.getUserId()).isEqualTo(USER_ID);

    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertNull(log.getJobDefinitionId());
    assertNull(log.getProcessInstanceId());
    assertNull(log.getProcessDefinitionId());
    assertNull(log.getProcessDefinitionKey());
    assertNull(log.getCaseInstanceId());
    assertNull(log.getCaseDefinitionId());
  }

  @Test
  public void testDeleteProcessDefinitionCascadingShouldKeepCreateUserOperationLog() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                                                 .deploymentId(deployment.getId())
                                                 .singleResult();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE);

    assertThat(query.count()).isEqualTo(1);

    // when
    repositoryService.deleteProcessDefinition(procDef.getId(), true);

    // then
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testDeleteProcessDefinitiontWithoutCascadingShouldKeepCreateUserOperationLog() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                                                 .deploymentId(deployment.getId())
                                                 .singleResult();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_CREATE);

    assertThat(query.count()).isEqualTo(1);

    // when
    repositoryService.deleteProcessDefinition(procDef.getId());

    // then
    assertThat(query.count()).isEqualTo(1);
  }

  @Test
  public void testDeleteProcessDefinition() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                                                 .deploymentId(deployment.getId())
                                                 .singleResult();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    // when
    repositoryService.deleteProcessDefinition(procDef.getId(), false);

    // then
    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry log = query.singleResult();
    assertNotNull(log);

    assertThat(log.getEntityType()).isEqualTo(EntityTypes.PROCESS_DEFINITION);
    assertThat(log.getProcessDefinitionId()).isEqualTo(procDef.getId());
    assertThat(log.getProcessDefinitionKey()).isEqualTo(procDef.getKey());
    assertThat(log.getDeploymentId()).isEqualTo(deployment.getId());

    assertThat(log.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    assertThat(log.getProperty()).isEqualTo("cascade");
    assertFalse(Boolean.parseBoolean(log.getOrgValue()));
    assertFalse(Boolean.parseBoolean(log.getNewValue()));

    assertThat(log.getUserId()).isEqualTo(USER_ID);

    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertNull(log.getJobDefinitionId());
    assertNull(log.getProcessInstanceId());
    assertNull(log.getCaseInstanceId());
    assertNull(log.getCaseDefinitionId());
  }

  @Test
  public void testDeleteProcessDefinitionCascading() {
    // given
    Deployment deployment = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(RESOURCE_NAME, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                                                 .deploymentId(deployment.getId())
                                                 .singleResult();

    UserOperationLogQuery query = historyService
        .createUserOperationLogQuery()
        .operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    // when
    repositoryService.deleteProcessDefinition(procDef.getId(), true);

    // then
    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry log = query.singleResult();
    assertNotNull(log);

    assertThat(log.getEntityType()).isEqualTo(EntityTypes.PROCESS_DEFINITION);
    assertThat(log.getProcessDefinitionId()).isEqualTo(procDef.getId());
    assertThat(log.getProcessDefinitionKey()).isEqualTo(procDef.getKey());
    assertThat(log.getDeploymentId()).isEqualTo(deployment.getId());

    assertThat(log.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    assertThat(log.getProperty()).isEqualTo("cascade");
    assertFalse(Boolean.parseBoolean(log.getOrgValue()));
    assertTrue(Boolean.parseBoolean(log.getNewValue()));

    assertThat(log.getUserId()).isEqualTo(USER_ID);

    assertThat(log.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertNull(log.getJobDefinitionId());
    assertNull(log.getProcessInstanceId());
    assertNull(log.getCaseInstanceId());
    assertNull(log.getCaseDefinitionId());
  }

  protected BpmnModelInstance createProcessWithServiceTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .serviceTask()
        .operatonExpression("${true}")
      .endEvent()
    .done();
  }

}
