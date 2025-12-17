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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.processDefinitionByDeployTime;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySortingAndCount;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Joram Barrez
 */
class ProcessDefinitionQueryTest extends AbstractDefinitionQueryTest {

  private static final String THIRD_DEPLOYMENT_NAME = "thirdDeployment";

  private String deploymentThreeId;

  @Override
  protected String getResourceOnePath() {
    return "org/operaton/bpm/engine/test/repository/one.bpmn20.xml";
  }

  @Override
  protected String getResourceTwoPath() {
    return "org/operaton/bpm/engine/test/repository/two.bpmn20.xml";
  }

  protected String getResourceThreePath() {
    return "org/operaton/bpm/engine/test/repository/three_.bpmn20.xml";
  }

  @BeforeEach
  void setUp() {
    deploymentThreeId = repositoryService.createDeployment().name(THIRD_DEPLOYMENT_NAME).addClasspathResource(getResourceThreePath()).deploy().getId();
  }

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
    repositoryService.deleteDeployment(deploymentThreeId, true);
  }

  @Test
  void testProcessDefinitionProperties() {
    List<ProcessDefinition> processDefinitions = repositoryService
      .createProcessDefinitionQuery()
      .orderByProcessDefinitionName().asc()
      .orderByProcessDefinitionVersion().asc()
      .orderByProcessDefinitionCategory().asc()
      .list();

    ProcessDefinition processDefinition = processDefinitions.get(0);
    assertThat(processDefinition.getKey()).isEqualTo("one");
    assertThat(processDefinition.getName()).isEqualTo("One");
    assertThat(processDefinition.getDescription()).isEqualTo("Desc one");
    assertThat(processDefinition.getId()).startsWith("one:1");
    assertThat(processDefinition.getCategory()).isEqualTo("Examples");
    assertThat(processDefinition.isStartableInTasklist()).isTrue();

    processDefinition = processDefinitions.get(1);
    assertThat(processDefinition.getKey()).isEqualTo("one");
    assertThat(processDefinition.getName()).isEqualTo("One");
    assertThat(processDefinition.getDescription()).isEqualTo("Desc one");
    assertThat(processDefinition.getId()).startsWith("one:2");
    assertThat(processDefinition.getCategory()).isEqualTo("Examples");
    assertThat(processDefinition.isStartableInTasklist()).isTrue();

    processDefinition = processDefinitions.get(2);
    assertThat(processDefinition.getKey()).isEqualTo("two");
    assertThat(processDefinition.getName()).isEqualTo("Two");
    assertThat(processDefinition.getDescription()).isNull();
    assertThat(processDefinition.getId()).startsWith("two:1");
    assertThat(processDefinition.getCategory()).isEqualTo("Examples2");
    assertThat(processDefinition.isStartableInTasklist()).isTrue();

    processDefinition = processDefinitions.get(3);
    assertThat(processDefinition.getKey()).isEqualTo("xyz_");
    assertThat(processDefinition.getName()).isEqualTo("Xyz_");
    assertThat(processDefinition.getDescription()).isNull();
    assertThat(processDefinition.getId()).startsWith("xyz_:1");
    assertThat(processDefinition.getCategory()).isEqualTo("xyz_");
    assertThat(processDefinition.isStartableInTasklist()).isFalse();
  }

  @Test
  void testQueryByDeploymentId() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentOneId);
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByInvalidDeploymentId() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().deploymentId("invalid");
    verifyQueryResults(query, 0);

    var processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    // when/then
    assertThatThrownBy(() -> processDefinitionQuery.deploymentId(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByDeploymentTimeAfter() {
    // given
    Date startTest = DateUtils.addSeconds(ClockUtil.now(), 5);
    ClockUtil.setCurrentTime(startTest);

    ClockUtil.setCurrentTime(DateUtils.addSeconds(startTest, 5));
    Deployment tempDeploymentOne = repositoryService.createDeployment()
        .addClasspathResource(getResourceOnePath()).addClasspathResource(getResourceTwoPath()).deploy();
    engineRule.manageDeployment(tempDeploymentOne);

    Date timeAfterDeploymentOne = DateUtils.addSeconds(ClockUtil.getCurrentTime(), 1);

    ClockUtil.setCurrentTime(DateUtils.addSeconds(timeAfterDeploymentOne, 5));
    Deployment tempDeploymentTwo = repositoryService.createDeployment()
        .addClasspathResource(getResourceOnePath()).deploy();
    engineRule.manageDeployment(tempDeploymentTwo);
    Date timeAfterDeploymentTwo = DateUtils.addSeconds(ClockUtil.getCurrentTime(), 1);

    ClockUtil.setCurrentTime(DateUtils.addSeconds(timeAfterDeploymentTwo, 5));
    Deployment tempDeploymentThree = repositoryService.createDeployment()
        .addClasspathResource(getResourceThreePath()).deploy();
    engineRule.manageDeployment(tempDeploymentThree);
    Date timeAfterDeploymentThree = DateUtils.addSeconds(ClockUtil.getCurrentTime(), 1);

    // when
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAfter(startTest).list();
    // then
    assertThat(processDefinitions).hasSize(4);

    // when
    processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAfter(timeAfterDeploymentOne).list();
    // then
    assertThat(processDefinitions).hasSize(2);
    assertThatProcessDefinitionsWereDeployedAfter(processDefinitions, timeAfterDeploymentOne);

    // when
    processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAfter(timeAfterDeploymentTwo).list();
    // then
    assertThat(processDefinitions).hasSize(1);
    assertThatProcessDefinitionsWereDeployedAfter(processDefinitions, timeAfterDeploymentTwo);

    // when
    processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAfter(timeAfterDeploymentThree).list();
    // then
    assertThat(processDefinitions).isEmpty();
  }

  @Test
  void testQueryByDeploymentTimeAt() throws Exception {
    // given
    //get rid of the milliseconds because of MySQL datetime precision
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");

    Date startTest = formatter.parse(formatter.format(DateUtils.addSeconds(ClockUtil.now(), 5)));
    ClockUtil.setCurrentTime(startTest);

    Date timeAtDeploymentOne = ClockUtil.getCurrentTime();
    Deployment tempDeploymentOne = repositoryService.createDeployment()
        .addClasspathResource(getResourceOnePath()).addClasspathResource(getResourceTwoPath()).deploy();
    engineRule.manageDeployment(tempDeploymentOne);

    Date timeAtDeploymentTwo = DateUtils.addSeconds(timeAtDeploymentOne, 5);
    ClockUtil.setCurrentTime(timeAtDeploymentTwo);
    Deployment tempDeploymentTwo = repositoryService.createDeployment()
        .addClasspathResource(getResourceOnePath()).deploy();
    engineRule.manageDeployment(tempDeploymentTwo);

    Date timeAtDeploymentThree = DateUtils.addSeconds(timeAtDeploymentTwo, 5);
    ClockUtil.setCurrentTime(timeAtDeploymentThree);
    Deployment tempDeploymentThree = repositoryService.createDeployment()
        .addClasspathResource(getResourceThreePath()).deploy();
    engineRule.manageDeployment(tempDeploymentThree);

    // then
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAt(timeAtDeploymentOne).list();
    assertThat(processDefinitions).hasSize(2);
    assertThatProcessDefinitionsWereDeployedAt(processDefinitions, timeAtDeploymentOne);

    processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAt(timeAtDeploymentTwo).list();
    assertThat(processDefinitions).hasSize(1);
    assertThatProcessDefinitionsWereDeployedAt(processDefinitions, timeAtDeploymentTwo);

    processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAt(timeAtDeploymentThree).list();
    assertThat(processDefinitions).hasSize(1);
    assertThatProcessDefinitionsWereDeployedAt(processDefinitions, timeAtDeploymentThree);

    processDefinitions = repositoryService.createProcessDefinitionQuery().deployedAt(DateUtils.addSeconds(ClockUtil.getCurrentTime(), 5)).list();
    assertThat(processDefinitions).isEmpty();
  }

  @Test
  void testQueryByName() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionName("Two");
    verifyQueryResults(query, 1);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionName("One");
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByInvalidName() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionName("invalid");
    verifyQueryResults(query, 0);

    var processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    // when/then
    assertThatThrownBy(() -> processDefinitionQuery.processDefinitionName(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNameLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%w%");
    verifyQueryResults(query, 1);
    query = query.processDefinitionNameLike("%z\\_%");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidNameLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%invalid%");
    verifyQueryResults(query, 0);
  }

  /**
   * CAM-8014
   *
   * Verify that search by name like returns results with case-insensitive
   */
  @Test
  void testQueryByNameLikeCaseInsensitive() {
    ProcessDefinitionQuery queryCaseInsensitive = repositoryService.createProcessDefinitionQuery()
      .processDefinitionNameLike("%OnE%");
    verifyQueryResults(queryCaseInsensitive, 2);
  }

  @Test
  void testQueryByKey() {
    // process one
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one");
    verifyQueryResults(query, 2);

    // process two
    query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("two");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByKeys() {

    // empty list
    assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKeyIn("a", "b").list()).isEmpty();


    // collect all definition keys
    List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
    String[] processDefinitionKeys = new String[list.size()];
    for (int i = 0; i < processDefinitionKeys.length; i++) {
      processDefinitionKeys[i] = list.get(i).getKey();
    }

    List<ProcessDefinition> keyInList = repositoryService.createProcessDefinitionQuery().processDefinitionKeyIn(processDefinitionKeys).list();
    for (ProcessDefinition processDefinition : keyInList) {
      boolean found = false;
      for (ProcessDefinition otherProcessDefinition : list) {
        if(otherProcessDefinition.getKey().equals(processDefinition.getKey())) {
          found = true; break;
        }
      }
      assertThat(found).withFailMessage("Expected to find process definition " + processDefinition).isTrue();
    }

    assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("dummyKey").processDefinitionKeyIn(processDefinitionKeys).count()).isZero();
  }

  @Test
  void testQueryByInvalidKey() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("invalid");
    verifyQueryResults(query, 0);

    var processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    // when/then
    assertThatThrownBy(() -> processDefinitionQuery.processDefinitionKey(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByKeyLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKeyLike("%o%");
    verifyQueryResults(query, 3);
    query = query.processDefinitionKeyLike("%z\\_%");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidKeyLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKeyLike("%invalid%");
    verifyQueryResults(query, 0);

    var processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    // when/then
    assertThatThrownBy(() -> processDefinitionQuery.processDefinitionKeyLike(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByResourceNameLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionResourceNameLike("%ee\\_%");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidResourceNameLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionResourceNameLike("%invalid%");
    verifyQueryResults(query, 0);

    var processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    // when/then
    assertThatThrownBy(() -> processDefinitionQuery.processDefinitionResourceNameLike(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByCategory() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionCategory("Examples");
    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByCategoryLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionCategoryLike("%Example%");
    verifyQueryResults(query, 3);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionCategoryLike("%amples2");
    verifyQueryResults(query, 1);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionCategoryLike("%z\\_%");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByVersion() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2);
    verifyQueryResults(query, 1);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1);
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryByInvalidVersion() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(3);
    verifyQueryResults(query, 0);

    // when/then
    var query2 = repositoryService.createProcessDefinitionQuery();
    assertThatThrownBy(() -> query2.processDefinitionVersion(-1))
      .isInstanceOf(ProcessEngineException.class);

    // and
    var query3 = repositoryService.createProcessDefinitionQuery();
    assertThatThrownBy(() -> query3.processDefinitionVersion(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByKeyAndVersion() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").processDefinitionVersion(1);
    verifyQueryResults(query, 1);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").processDefinitionVersion(2);
    verifyQueryResults(query, 1);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").processDefinitionVersion(3);
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryByLatest() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().latestVersion();
    verifyQueryResults(query, 3);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").latestVersion();
    verifyQueryResults(query, 1);

    query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("two").latestVersion();
    verifyQueryResults(query, 1);
  }

  @Test
  void testInvalidUsageOfLatest() {

    // when/then
    var query1 = repositoryService.createProcessDefinitionQuery()
      .processDefinitionId("test")
      .latestVersion();
    assertThatThrownBy(query1::list)
      .isInstanceOf(ProcessEngineException.class);

    // and
    var query2 = repositoryService.createProcessDefinitionQuery()
      .processDefinitionVersion(1)
      .latestVersion();
    assertThatThrownBy(query2::list)
      .isInstanceOf(ProcessEngineException.class);

    // and
    var query3 = repositoryService.createProcessDefinitionQuery().deploymentId("test").latestVersion();
    assertThatThrownBy(query3::list)
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQuerySorting() {

    // asc

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionId().asc();
    verifyQueryResults(query, 4);

    query = repositoryService.createProcessDefinitionQuery().orderByDeploymentId().asc();
    verifyQueryResults(query, 4);

    query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().asc();
    verifyQueryResults(query, 4);

    query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionVersion().asc();
    verifyQueryResults(query, 4);

    // desc

    query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionId().desc();
    verifyQueryResults(query, 4);

    query = repositoryService.createProcessDefinitionQuery().orderByDeploymentId().desc();
    verifyQueryResults(query, 4);

    query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().desc();
    verifyQueryResults(query, 4);

    query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionVersion().desc();
    verifyQueryResults(query, 4);

    // Typical use case
    query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().asc().orderByProcessDefinitionVersion().desc();
    List<ProcessDefinition> processDefinitions = query.list();
    assertThat(processDefinitions).hasSize(4);

    assertThat(processDefinitions.get(0).getKey()).isEqualTo("one");
    assertThat(processDefinitions.get(0).getVersion()).isEqualTo(2);
    assertThat(processDefinitions.get(1).getKey()).isEqualTo("one");
    assertThat(processDefinitions.get(1).getVersion()).isEqualTo(1);
    assertThat(processDefinitions.get(2).getKey()).isEqualTo("two");
    assertThat(processDefinitions.get(2).getVersion()).isEqualTo(1);
  }

  @Test
  void testQueryByMessageSubscription() {
    Deployment deployment = repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processWithNewBookingMessage.bpmn20.xml")
      .addClasspathResource("org/operaton/bpm/engine/test/api/repository/processWithNewInvoiceMessage.bpmn20.xml")
    .deploy();

    assertThat(repositoryService.createProcessDefinitionQuery()
      .messageEventSubscriptionName("newInvoiceMessage")
      .count()).isOne();

    assertThat(repositoryService.createProcessDefinitionQuery()
      .messageEventSubscriptionName("newBookingMessage")
      .count()).isOne();

    assertThat(repositoryService.createProcessDefinitionQuery()
      .messageEventSubscriptionName("bogus")
      .count()).isZero();

    repositoryService.deleteDeployment(deployment.getId());
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml"})
  void testQueryByIncidentId() {
    assertThat(repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("failingProcess")
      .count()).isOne();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.waitForJobExecutorToProcessAllJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .incidentId(incident.getId());

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidIncidentId() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    verifyQueryResults(query.incidentId("invalid"), 0);

    // when/then
    assertThatThrownBy(() -> query.incidentId(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml"})
  void testQueryByIncidentType() {
    assertThat(repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("failingProcess")
      .count()).isOne();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.waitForJobExecutorToProcessAllJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .incidentType(incident.getIncidentType());

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidIncidentType() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    verifyQueryResults(query.incidentType("invalid"), 0);

    // when/then
    assertThatThrownBy(() -> query.incidentType(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml"})
  void testQueryByIncidentMessage() {
    assertThat(repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("failingProcess")
      .count()).isOne();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.waitForJobExecutorToProcessAllJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    Incident incident = runtimeService.createIncidentQuery().processInstanceId(processInstance.getId()).singleResult();

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .incidentMessage(incident.getIncidentMessage());

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidIncidentMessage() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    verifyQueryResults(query.incidentMessage("invalid"), 0);

    // when/then
    assertThatThrownBy(() -> query.incidentMessage(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml"})
  void testQueryByIncidentMessageLike() {
    assertThat(repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("failingProcess")
      .count()).isOne();

    runtimeService.startProcessInstanceByKey("failingProcess");

    testRule.waitForJobExecutorToProcessAllJobs();

    List<Incident> incidentList = runtimeService.createIncidentQuery().list();
    assertThat(incidentList).hasSize(1);

    ProcessDefinitionQuery query = repositoryService
        .createProcessDefinitionQuery()
        .incidentMessageLike("%expected%");

    verifyQueryResults(query, 1);

    query = repositoryService
        .createProcessDefinitionQuery()
        .incidentMessageLike("%\\_expected%");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidIncidentMessageLike() {
    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    verifyQueryResults(query.incidentMessageLike("invalid"), 0);

    // when/then
    assertThatThrownBy(() -> query.incidentMessageLike(null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByProcessDefinitionIds() {

    // empty list
    assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionIdIn("a", "b").list()).isEmpty();


    // collect all ids
    List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().list();
    String[] ids = new String[list.size()];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = list.get(i).getId();
    }

    List<ProcessDefinition> idInList = repositoryService.createProcessDefinitionQuery().processDefinitionIdIn(ids).list();
    for (ProcessDefinition processDefinition : idInList) {
      boolean found = false;
      for (ProcessDefinition otherProcessDefinition : list) {
        if(otherProcessDefinition.getId().equals(processDefinition.getId())) {
          found = true; break;
        }
      }
      assertThat(found).withFailMessage("Expected to find process definition " + processDefinition).isTrue();
    }

    assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionId("dummyId").processDefinitionIdIn(ids).count()).isZero();
  }

  @Test
  void testQueryByLatestAndName() {
    String firstDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml")
        .deploy()
        .getId();

    String secondDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml")
        .deploy()
        .getId();

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    query
      .processDefinitionName("First Test Process")
      .latestVersion();

    verifyQueryResults(query, 1);

    ProcessDefinition result = query.singleResult();

    assertThat(result.getName()).isEqualTo("First Test Process");
    assertThat(result.getVersion()).isEqualTo(2);

    repositoryService.deleteDeployment(firstDeployment, true);
    repositoryService.deleteDeployment(secondDeployment, true);

  }

  @Test
  void testQueryByLatestAndName_NotFound() {
    String firstDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml")
        .deploy()
        .getId();

    String secondDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/second-process.bpmn20.xml")
        .deploy()
        .getId();

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    query
      .processDefinitionName("First Test Process")
      .latestVersion();

    verifyQueryResults(query, 0);

    repositoryService.deleteDeployment(firstDeployment, true);
    repositoryService.deleteDeployment(secondDeployment, true);

  }

  @Test
  void testQueryByLatestAndNameLike() {
    String firstDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml")
        .deploy()
        .getId();

    String secondDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/second-process.bpmn20.xml")
        .deploy()
        .getId();

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    query
      .processDefinitionNameLike("%Test Process")
      .latestVersion();

    verifyQueryResults(query, 1);

    ProcessDefinition result = query.singleResult();

    assertThat(result.getName()).isEqualTo("Second Test Process");
    assertThat(result.getVersion()).isEqualTo(2);

    query
      .processDefinitionNameLike("%Test%")
      .latestVersion();

    verifyQueryResults(query, 1);

    result = query.singleResult();

    assertThat(result.getName()).isEqualTo("Second Test Process");
    assertThat(result.getVersion()).isEqualTo(2);

    query
      .processDefinitionNameLike("Second%")
      .latestVersion();

    result = query.singleResult();

    assertThat(result.getName()).isEqualTo("Second Test Process");
    assertThat(result.getVersion()).isEqualTo(2);

    repositoryService.deleteDeployment(firstDeployment, true);
    repositoryService.deleteDeployment(secondDeployment, true);
  }

  @Test
  void testQueryByLatestAndNameLike_NotFound() {
    String firstDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/first-process.bpmn20.xml")
        .deploy()
        .getId();

    String secondDeployment = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/repository/second-process.bpmn20.xml")
        .deploy()
        .getId();

    ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();

    query
      .processDefinitionNameLike("First%")
      .latestVersion();

    verifyQueryResults(query, 0);

    repositoryService.deleteDeployment(firstDeployment, true);
    repositoryService.deleteDeployment(secondDeployment, true);
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml"})
  void testQueryByVersionTag() {
    assertThat(repositoryService.createProcessDefinitionQuery()
      .versionTag("ver_tag_2")
      .count()).isOne();
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml"})
  void testQueryByVersionTagLike() {
    assertThat(repositoryService.createProcessDefinitionQuery()
      .versionTagLike("ver\\_tag\\_%")
      .count()).isOne();
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {"org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml"})
  void testQueryByNoVersionTag() {
    // 4 definitions without and 1 definition with version tag are deployed
    assertThat(repositoryService.createProcessDefinitionQuery()
      .withoutVersionTag()
      .count()).isEqualTo(4);
  }

  @Test
  void testQueryOrderByDeployTime() {
    // given a deployment that is guaranteed to be deployed later than the default deployments
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(10));
    Deployment tempDeploymentOne = repositoryService.createDeployment()
        .addClasspathResource(getResourceOnePath()).addClasspathResource(getResourceOnePath()).deploy();
    engineRule.manageDeployment(tempDeploymentOne);

    // when
    ProcessDefinitionQuery processDefinitionOrderByDeploymentTimeAscQuery =
        repositoryService.createProcessDefinitionQuery().orderByDeploymentTime().asc();
    ProcessDefinitionQuery processDefinitionOrderByDeploymentTimeDescQuery =
        repositoryService.createProcessDefinitionQuery().orderByDeploymentTime().desc();

    // then
    verifySortingAndCount(processDefinitionOrderByDeploymentTimeAscQuery, 5,
        processDefinitionByDeployTime(engineRule.getProcessEngine()));
    verifySortingAndCount(processDefinitionOrderByDeploymentTimeDescQuery, 5,
        inverted(processDefinitionByDeployTime(engineRule.getProcessEngine())));
  }

  @Test
  @org.operaton.bpm.engine.test.Deployment(resources = {
      "org/operaton/bpm/engine/test/api/repository/failingProcessCreateOneIncident.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/repository/VersionTagTest.testParsingVersionTag.bpmn20.xml"
  })
  void testQueryOrderByVersionTag() {
    List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
      .versionTagLike("ver%tag%")
      .orderByVersionTag()
      .asc()
      .list();

    assertThat(processDefinitionList.get(1).getVersionTag()).isEqualTo("ver_tag_2");
  }

  @Test
  void testQueryByStartableInTasklist() {
    // given
    // three definitions with startableInTasklist=true
    // one definition with startableInTasklist=false

    // when
    List<ProcessDefinition> startable = repositoryService.createProcessDefinitionQuery().startableInTasklist().list();
    List<ProcessDefinition> notStartable = repositoryService.createProcessDefinitionQuery().notStartableInTasklist().list();

    // then
    assertThat(startable).hasSize(3);
    assertThat(startable).extracting("key").containsExactlyInAnyOrder("one", "one", "two");
    assertThat(notStartable).hasSize(1);
    assertThat(notStartable.get(0).getKey()).isEqualTo("xyz_");
  }

  @Test
  void testQueryByStartableInTasklistNestedProcess() {
    // given
    // startable super process
    // non-startable subprocess
    BpmnModelInstance[] nestedProcess = setupNestedProcess(false);
    String deploymentId = testRule.deploy(nestedProcess).getId();

    // when
    ProcessDefinition actualStartable = repositoryService.createProcessDefinitionQuery()
        .deploymentId(deploymentId)
        .startableInTasklist()
        .singleResult();

    ProcessDefinition actualNotStartable = repositoryService.createProcessDefinitionQuery()
        .deploymentId(deploymentId)
        .notStartableInTasklist()
        .singleResult();

    // then

    assertThat(actualStartable.getKey()).isEqualTo("calling");
    assertThat(actualNotStartable.getKey()).isEqualTo("called");

    // cleanup
    repositoryService.deleteDeployment(deploymentId);
  }

  @Test
  void testQueryByStartableInTasklistNestedProcessDeployedSecondTime() {
    // given
    // startable super process & subprocess
    BpmnModelInstance[] nestedProcess = setupNestedProcess(true);
    String deploymentId1 = testRule.deploy(nestedProcess).getId();

    // assume
    long processes = repositoryService.createProcessDefinitionQuery()
        .deploymentId(deploymentId1)
        .notStartableInTasklist()
        .count();
    assertThat(processes).isZero();

    // deploy second version
    // startable super process
    // non-startable subprocess
    nestedProcess = setupNestedProcess(false);
    String deploymentId2 = testRule.deploy(nestedProcess).getId();

    // when
    ProcessDefinition startable = repositoryService.createProcessDefinitionQuery()
        .deploymentId(deploymentId2)
        .startableInTasklist()
        .singleResult();
    ProcessDefinition notStartable = repositoryService.createProcessDefinitionQuery()
        .deploymentId(deploymentId2)
        .notStartableInTasklist()
        .singleResult();

    // then
    assertThat(startable.getKey()).isEqualTo("calling");
    assertThat(notStartable.getKey()).isEqualTo("called");

    // cleanup
    repositoryService.deleteDeployment(deploymentId1);
    repositoryService.deleteDeployment(deploymentId2);
  }

  protected BpmnModelInstance[] setupNestedProcess(boolean isStartableSubprocess) {
    BpmnModelInstance[] result = new BpmnModelInstance[2];
    result[0] = Bpmn.createExecutableProcess("calling")
        .startEvent()
        .callActivity()
          .calledElement("called")
        .endEvent()
        .done();

    result[1] = Bpmn.createExecutableProcess("called")
        .operatonStartableInTasklist(isStartableSubprocess)
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    return result;
  }

  protected void assertThatProcessDefinitionsWereDeployedAfter(List<ProcessDefinition> processDefinitions, Date deployedAfter) {
    for (ProcessDefinition processDefinition : processDefinitions) {
      assertThat(repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).singleResult().getDeploymentTime()).isAfter(deployedAfter);
    }
  }

  protected void assertThatProcessDefinitionsWereDeployedAt(List<ProcessDefinition> processDefinitions, Date deployedAt) {
    for (ProcessDefinition processDefinition : processDefinitions) {
      assertThat(repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).singleResult().getDeploymentTime()).isEqualTo(deployedAt);
    }
  }

  protected void cleanupDeployments(String... deploymentId) {
    for (String id : deploymentId) {
      repositoryService.deleteDeployment(id, true);
    }
  }
}
