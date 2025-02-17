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
package org.operaton.bpm.engine.test.standalone.db.entitymanager;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GLOBAL;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.impl.test.TestHelper.assertAndEnsureCleanDbAndCache;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.Tenant;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.ManagementServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.management.DatabasePurgeReport;
import org.operaton.bpm.engine.impl.management.PurgeReport;
import org.operaton.bpm.engine.impl.persistence.deploy.cache.CachePurgeReport;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.api.identity.TestPermissions;
import org.operaton.bpm.engine.test.api.identity.TestResource;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public class PurgeDatabaseTest {

  protected static final String PROCESS_DEF_KEY = "test";
  protected static final String PROCESS_MODEL_NAME = "test.bpmn20.xml";
  @Rule
  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  private ProcessEngineConfigurationImpl processEngineConfiguration;
  private String databaseTablePrefix;

  @Before
  public void setUp() {
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    processEngineConfiguration.setDbMetricsReporterActivate(true);
    databaseTablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
  }

  @After
  public void cleanUp() {
    processEngineConfiguration.setDbMetricsReporterActivate(false);
  }

  @Test
  public void testPurge() {
    // given data
    BpmnModelInstance test = Bpmn.createExecutableProcess(PROCESS_DEF_KEY).startEvent().endEvent().done();
    engineRule.getRepositoryService().createDeployment().addModelInstance(PROCESS_MODEL_NAME, test).deploy();
    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY);

    // when purge is executed
    ManagementServiceImpl managementService = (ManagementServiceImpl) engineRule.getManagementService();
    managementService.purge();

    // then no more data exist
    assertAndEnsureCleanDbAndCache(engineRule.getProcessEngine(), true);
  }

  @Test
  public void testPurgeWithExistingProcessInstance() {
    //given process with variable and staying process instance in second user task
    BpmnModelInstance test = Bpmn.createExecutableProcess(PROCESS_DEF_KEY)
                                 .startEvent()
                                 .userTask()
                                 .userTask()
                                 .endEvent()
                                 .done();
    engineRule.getRepositoryService().createDeployment().addModelInstance(PROCESS_MODEL_NAME, test).deploy();

    VariableMap variables = Variables.createVariables();
    variables.put("key", "value");
    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, variables);
    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    engineRule.getTaskService().complete(task.getId());

    // when purge is executed
    ManagementServiceImpl managementService = (ManagementServiceImpl) engineRule.getManagementService();
    managementService.purge();

    // then no more data exist
    assertAndEnsureCleanDbAndCache(engineRule.getProcessEngine(), true);
  }

  @Test
  public void testPurgeWithAsyncProcessInstance() {
    // given process with variable and async process instance
    BpmnModelInstance test = Bpmn.createExecutableProcess(PROCESS_DEF_KEY)
      .startEvent()
      .operatonAsyncBefore()
      .userTask()
      .userTask()
      .endEvent()
      .done();
    engineRule.getRepositoryService().createDeployment().addModelInstance(PROCESS_MODEL_NAME, test).deploy();

    VariableMap variables = Variables.createVariables();
    variables.put("key", "value");
    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, variables);
    Job job = engineRule.getManagementService().createJobQuery().singleResult();
    engineRule.getManagementService().executeJob(job.getId());
    Task task = engineRule.getTaskService().createTaskQuery().singleResult();
    engineRule.getTaskService().complete(task.getId());

    // when purge is executed
    ManagementServiceImpl managementService = (ManagementServiceImpl) engineRule.getManagementService();
    managementService.purge();

    // then no more data exist
    assertAndEnsureCleanDbAndCache(engineRule.getProcessEngine(), true);
  }

  @Test
  public void testPurgeComplexProcess() {
    // given complex process with authentication
    // process is executed two times
    // metrics are reported

    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(PROCESS_DEF_KEY)
      .startEvent()
        .operatonAsyncBefore()
      .parallelGateway("parallel")
        .serviceTask("external")
          .operatonType("external")
          .operatonTopic("external")
        .boundaryEvent()
          .message("message")
        .moveToNode("parallel")
        .serviceTask()
          .operatonAsyncBefore()
          .operatonExpression("${1/0}")
        .moveToLastGateway()
        .userTask()
      .done();

    createAuthenticationData();
    engineRule.getRepositoryService().createDeployment().addModelInstance(PROCESS_MODEL_NAME, modelInstance).deploy();

    executeComplexBpmnProcess(true);
    executeComplexBpmnProcess(false);

    processEngineConfiguration.getDbMetricsReporter().reportNow();

    // when purge is executed
    ManagementServiceImpl managementService = (ManagementServiceImpl) engineRule.getManagementService();
    PurgeReport purge = managementService.purge();

    // then database and cache are empty
    assertAndEnsureCleanDbAndCache(engineRule.getProcessEngine(), true);

    // and report contains deleted data
    assertThat(purge.isEmpty()).isFalse();
    CachePurgeReport cachePurgeReport = purge.getCachePurgeReport();
    assertThat(cachePurgeReport.getReportValue(CachePurgeReport.PROCESS_DEF_CACHE)).hasSize(1);

    DatabasePurgeReport databasePurgeReport = purge.getDatabasePurgeReport();
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_ID_TENANT_MEMBER")).isEqualTo(2);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_EVENT_SUBSCR")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RE_DEPLOYMENT")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_EXT_TASK")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_ID_MEMBERSHIP")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_TASK")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_JOB")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_GE_BYTEARRAY")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_JOBDEF")).isEqualTo(2);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_ID_USER")).isEqualTo(2);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_EXECUTION")).isEqualTo(5);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_METER_LOG")).isEqualTo(12);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_VARIABLE")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RE_PROCDEF")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_ID_TENANT")).isEqualTo(2);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_ID_GROUP")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_AUTHORIZATION")).isEqualTo(2);

    if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_FULL)) {
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_INCIDENT")).isEqualTo(1);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_ACTINST")).isEqualTo(9);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_PROCINST")).isEqualTo(2);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_DETAIL")).isEqualTo(2);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_TASKINST")).isEqualTo(2);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_JOB_LOG")).isEqualTo(7);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_VARINST")).isEqualTo(2);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_OP_LOG")).isEqualTo(8);
    }

  }

  private void createAuthenticationData() {
    IdentityService identityService = engineRule.getIdentityService();
    Group group = identityService.newGroup("group");
    identityService.saveGroup(group);
    User user = identityService.newUser("user");
    User user2 = identityService.newUser("user2");
    identityService.saveUser(user);
    identityService.saveUser(user2);
    Tenant tenant = identityService.newTenant("tenant");
    identityService.saveTenant(tenant);
    Tenant tenant2 = identityService.newTenant("tenant2");
    identityService.saveTenant(tenant2);
    identityService.createMembership("user", "group");
    identityService.createTenantUserMembership("tenant", "user");
    identityService.createTenantUserMembership("tenant2", "user2");


    Resource resource1 = TestResource.RESOURCE1;
    // create global authorization which grants all permissions to all users (on resource1):
    AuthorizationService authorizationService = engineRule.getAuthorizationService();
    Authorization globalAuth = authorizationService.createNewAuthorization(AUTH_TYPE_GLOBAL);
    globalAuth.setResource(resource1);
    globalAuth.setResourceId(ANY);
    globalAuth.addPermission(TestPermissions.ALL);
    authorizationService.saveAuthorization(globalAuth);

    //grant user read auth on resource2
    Resource resource2 = TestResource.RESOURCE2;
    Authorization userGrant = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    userGrant.setUserId("user");
    userGrant.setResource(resource2);
    userGrant.setResourceId(ANY);
    userGrant.addPermission(TestPermissions.READ);
    authorizationService.saveAuthorization(userGrant);

    identityService.setAuthenticatedUserId("user");
  }

  private void executeComplexBpmnProcess(boolean complete) {
    VariableMap variables = Variables.createVariables();
    variables.put("key", "value");
    engineRule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEF_KEY, variables);
    //execute start event
    Job job = engineRule.getManagementService().createJobQuery().singleResult();
    engineRule.getManagementService().executeJob(job.getId());

    //fetch tasks and jobs
    List<LockedExternalTask> externalTasks = engineRule.getExternalTaskService().fetchAndLock(1, "worker").topic("external", 1500).execute();
    job = engineRule.getManagementService().createJobQuery().singleResult();
    Task task = engineRule.getTaskService().createTaskQuery().singleResult();

    //complete
    if (complete) {
      engineRule.getManagementService().setJobRetries(job.getId(), 0);
      engineRule.getManagementService().executeJob(job.getId());
      engineRule.getExternalTaskService().complete(externalTasks.get(0).getId(), "worker");
      engineRule.getTaskService().complete(task.getId());
    }
  }

  // CMMN //////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testPurgeCmmnProcess() {
    // given cmmn process which is not managed by process engine rule

    engineRule.getRepositoryService()
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/standalone/db/entitymanager/PurgeDatabaseTest.testPurgeCmmnProcess.cmmn")
      .deploy();
    VariableMap variables = Variables.createVariables();
    variables.put("key", "value");
    engineRule.getCaseService().createCaseInstanceByKey(PROCESS_DEF_KEY, variables);

    // when purge is executed
    ManagementServiceImpl managementService = (ManagementServiceImpl) engineRule.getManagementService();
    PurgeReport purge = managementService.purge();

    // then database and cache is cleaned
    assertAndEnsureCleanDbAndCache(engineRule.getProcessEngine(), true);

    // and report contains deleted entities
    assertThat(purge.isEmpty()).isFalse();
    CachePurgeReport cachePurgeReport = purge.getCachePurgeReport();
    assertThat(cachePurgeReport.getReportValue(CachePurgeReport.CASE_DEF_CACHE)).hasSize(1);

    DatabasePurgeReport databasePurgeReport = purge.getDatabasePurgeReport();
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RE_DEPLOYMENT")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_TASK")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_GE_BYTEARRAY")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RE_CASE_DEF")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_CASE_EXECUTION")).isEqualTo(3);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_VARIABLE")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RU_CASE_SENTRY_PART")).isEqualTo(2);

    if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_FULL)) {
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_DETAIL")).isEqualTo(1);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_TASKINST")).isEqualTo(1);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_VARINST")).isEqualTo(1);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_CASEINST")).isEqualTo(1);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_CASEACTINST")).isEqualTo(2);
    }
  }

  // DMN ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  public void testPurgeDmnProcess() {
    // given dmn process which is not managed by process engine rule
    engineRule.getRepositoryService()
      .createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/standalone/db/entitymanager/PurgeDatabaseTest.testPurgeDmnProcess.dmn")
      .deploy();
    VariableMap variables = Variables.createVariables()
      .putValue("key", "value")
      .putValue("season", "Test");
    engineRule.getDecisionService().evaluateDecisionByKey("decisionId").variables(variables).evaluate();

    // when purge is executed
    ManagementServiceImpl managementService = (ManagementServiceImpl) engineRule.getManagementService();
    PurgeReport purge = managementService.purge();

    // then database and cache is cleaned
    assertAndEnsureCleanDbAndCache(engineRule.getProcessEngine(), true);

    // and report contains deleted entities
    assertThat(purge.isEmpty()).isFalse();
    CachePurgeReport cachePurgeReport = purge.getCachePurgeReport();
    assertThat(cachePurgeReport.getReportValue(CachePurgeReport.DMN_DEF_CACHE)).hasSize(2);
    assertThat(cachePurgeReport.getReportValue(CachePurgeReport.DMN_REQ_DEF_CACHE)).hasSize(1);

    DatabasePurgeReport databasePurgeReport = purge.getDatabasePurgeReport();
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RE_DEPLOYMENT")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_GE_BYTEARRAY")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RE_DECISION_REQ_DEF")).isEqualTo(1);
    assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_RE_DECISION_DEF")).isEqualTo(2);

    if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_FULL)) {
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_DECINST")).isEqualTo(1);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_DEC_IN")).isEqualTo(1);
      assertThat((long) databasePurgeReport.getReportValue(databaseTablePrefix + "ACT_HI_DEC_OUT")).isEqualTo(1);
    }
  }
}
