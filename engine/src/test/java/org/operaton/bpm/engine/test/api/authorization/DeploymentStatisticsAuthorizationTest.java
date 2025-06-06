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
package org.operaton.bpm.engine.test.api.authorization;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.DEPLOYMENT;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.management.DeploymentStatistics;
import org.operaton.bpm.engine.management.DeploymentStatisticsQuery;
import org.operaton.bpm.engine.management.IncidentStatistics;

/**
 * @author Roman Smirnov
 *
 */
class DeploymentStatisticsAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_INCIDENT_PROCESS_KEY = "process";
  protected static final String TIMER_START_PROCESS_KEY = "timerStartProcess";
  protected static final String TIMER_BOUNDARY_PROCESS_KEY = "timerBoundaryProcess";

  protected String firstDeploymentId;
  protected String secondDeploymentId;
  protected String thirdDeploymentId;

  @Override
  @BeforeEach
  public void setUp() {
    firstDeploymentId = createDeployment("first", "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml").getId();
    secondDeploymentId = createDeployment("second", "org/operaton/bpm/engine/test/api/authorization/timerStartEventProcess.bpmn20.xml").getId();
    thirdDeploymentId = createDeployment("third", "org/operaton/bpm/engine/test/api/authorization/timerBoundaryEventProcess.bpmn20.xml").getId();
    super.setUp();
  }

  @Override
  @AfterEach
  public void tearDown() {
    super.tearDown();
    deleteDeployment(firstDeploymentId);
    deleteDeployment(secondDeploymentId);
    deleteDeployment(thirdDeploymentId);
  }

  // deployment statistics query without process instance authorizations /////////////////////////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    // given

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnDeployment() {
    // given
    createGrantAuthorization(DEPLOYMENT, firstDeploymentId, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    verifyQueryResults(query, 1);

    DeploymentStatistics statistics = query.singleResult();
    verifyStatisticsResult(statistics, 0, 0, 0);
  }

  @Test
  void testQueryWithMultiple() {
    // given
    createGrantAuthorization(DEPLOYMENT, firstDeploymentId, userId, READ);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryWithReadPermissionOnAnyDeployment() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    verifyQueryResults(query, 3);

    List<DeploymentStatistics> result = query.list();
    for (DeploymentStatistics statistics : result) {
      verifyStatisticsResult(statistics, 0, 0, 0);
    }
  }

  @Test
  void shouldNotFindStatisticsWithRevokedReadPermissionOnAnyDeployment() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, ANY, READ);
    createRevokeAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // deployment statistics query (including process instances) /////////////////////////////////////////////

  @Test
  void testQueryWithReadPermissionOnProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 1, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryWithReadPermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryWithReadInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, TIMER_BOUNDARY_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryWithReadInstancePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  // deployment statistics query (including failed jobs) /////////////////////////////////////////////

  @Test
  void testQueryIncludingFailedJobsWithReadPermissionOnProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 1, 1, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingFailedJobsWithReadPermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 3, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingFailedJobsWithReadInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 3, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingFailedJobsWithReadInstancePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 3, 0);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  // deployment statistics query (including incidents) /////////////////////////////////////////////

  @Test
  void testQueryIncludingIncidentsWithReadPermissionOnProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 1, 0, 1);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingIncidentsWithReadPermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 3);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingIncidentsWithReadInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 3);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingIncidentsWithReadInstancePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 3);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  // deployment statistics query (including failed jobs and incidents) /////////////////////////////////////////////

  @Test
  void testQueryIncludingFailedJobsAndIncidentsWithReadPermissionOnProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    String processInstanceId = startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 1, 1, 1);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingFailedJobsAndIncidentsWithReadPermissionOnAnyProcessInstance() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 3, 3);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingFailedJobsAndIncidentsWithReadInstancePermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 3, 3);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 0, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  @Test
  void testQueryIncludingFailedJobsAndIncidentsWithReadInstancePermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_START_PROCESS_KEY);

    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(TIMER_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    DeploymentStatisticsQuery query = managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents();

    // then
    List<DeploymentStatistics> statistics = query.list();

    for (DeploymentStatistics deploymentStatistics : statistics) {
      String id = deploymentStatistics.getId();
      if (id.equals(firstDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 3, 3);
      } else if (id.equals(secondDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else if (id.equals(thirdDeploymentId)) {
        verifyStatisticsResult(deploymentStatistics, 3, 0, 0);
      } else {
        fail("Unexpected deployment");
      }
    }
  }

  // helper ///////////////////////////////////////////////////////////////////////////

  protected void verifyStatisticsResult(DeploymentStatistics statistics, int instances, int failedJobs, int incidents) {
    assertThat(statistics.getInstances()).as("Instances").isEqualTo(instances);
    assertThat(statistics.getFailedJobs()).as("Failed Jobs").isEqualTo(failedJobs);

    List<IncidentStatistics> incidentStatistics = statistics.getIncidentStatistics();
    if (incidents == 0) {
      assertThat(incidentStatistics).as("Incidents supposed to be empty").isEmpty();
    }
    else {
      // the test does have only one type of incidents
      assertThat(incidentStatistics.get(0).getIncidentCount()).as("Incidents").isEqualTo(incidents);
    }
  }

}
