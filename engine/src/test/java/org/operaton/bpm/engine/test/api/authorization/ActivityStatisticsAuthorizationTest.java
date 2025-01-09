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
package org.operaton.bpm.engine.test.api.authorization;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.management.ActivityStatistics;
import org.operaton.bpm.engine.management.ActivityStatisticsQuery;
import org.operaton.bpm.engine.management.IncidentStatistics;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class ActivityStatisticsAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_INCIDENT_PROCESS_KEY = "process";

  @Override
  @Before
  public void setUp() {
    testRule.deploy("org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml");
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    super.setUp();
  }

  // without any authorization

  @Test
  public void testQueryWithoutAuthorizations() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    // when
    ActivityStatisticsQuery activityStatisticsQuery = managementService.createActivityStatisticsQuery(processDefinitionId);
    assertThatThrownBy(activityStatisticsQuery::list)
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(READ.getName())
      .hasMessageContaining(ONE_INCIDENT_PROCESS_KEY)
      .hasMessageContaining(PROCESS_DEFINITION.resourceName());
  }

  // including instances //////////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingInstancesWithoutAuthorizationOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);

    // when
    ActivityStatisticsQuery query = managementService.createActivityStatisticsQuery(processDefinitionId);

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  public void testQueryIncludingInstancesWithReadPermissionOnOneProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    disableAuthorization();
    String processInstanceId = runtimeService.createProcessInstanceQuery().list().get(0).getId();
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ActivityStatistics statistics = managementService.createActivityStatisticsQuery(processDefinitionId).singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(1, statistics.getInstances());
    assertEquals(0, statistics.getFailedJobs());
    assertTrue(statistics.getIncidentStatistics().isEmpty());
  }

  @Test
  public void testQueryIncludingInstancesWithMany() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    disableAuthorization();
    String processInstanceId = runtimeService.createProcessInstanceQuery().list().get(0).getId();
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ActivityStatistics statistics = managementService.createActivityStatisticsQuery(processDefinitionId).singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(1, statistics.getInstances());
    assertEquals(0, statistics.getFailedJobs());
    assertTrue(statistics.getIncidentStatistics().isEmpty());
  }

  @Test
  public void testQueryIncludingInstancesWithReadPermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    ActivityStatistics statistics = managementService.createActivityStatisticsQuery(processDefinitionId).singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(0, statistics.getFailedJobs());
    assertTrue(statistics.getIncidentStatistics().isEmpty());
  }

  @Test
  public void testQueryIncludingInstancesWithReadInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ, READ_INSTANCE);

    // when
    ActivityStatistics statistics = managementService.createActivityStatisticsQuery(processDefinitionId).singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(0, statistics.getFailedJobs());
    assertTrue(statistics.getIncidentStatistics().isEmpty());
  }

  @Test
  public void shouldNotFindStatisticsWithRevokedReadInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ, READ_INSTANCE);
    createRevokeAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    ActivityStatisticsQuery statisticsQuery = managementService.createActivityStatisticsQuery(processDefinitionId);

    // then
    verifyQueryResults(statisticsQuery, 0);
  }

  // including failed jobs //////////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingFailedJobsWithoutAuthorizationOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeFailedJobs()
        .singleResult();

    // then
    assertNull(statistics);
  }

  @Test
  public void testQueryIncludingFailedJobsWithReadPermissionOnOneProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    disableAuthorization();
    String processInstanceId = runtimeService.createProcessInstanceQuery().list().get(0).getId();
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeFailedJobs()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(1, statistics.getInstances());
    assertEquals(1, statistics.getFailedJobs());
    assertTrue(statistics.getIncidentStatistics().isEmpty());
  }

  @Test
  public void testQueryIncludingFailedJobsWithReadPermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeFailedJobs()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(3, statistics.getFailedJobs());
    assertTrue(statistics.getIncidentStatistics().isEmpty());
  }

  @Test
  public void testQueryIncludingFailedJobsWithReadInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ, READ_INSTANCE);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeFailedJobs()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(3, statistics.getFailedJobs());
    assertTrue(statistics.getIncidentStatistics().isEmpty());
  }

  // including incidents //////////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingIncidentsWithoutAuthorizationOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .singleResult();

    // then
    assertNull(statistics);
  }

  @Test
  public void testQueryIncludingIncidentsWithReadPermissionOnOneProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    disableAuthorization();
    String processInstanceId = runtimeService.createProcessInstanceQuery().list().get(0).getId();
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(1, statistics.getInstances());
    assertEquals(0, statistics.getFailedJobs());
    assertFalse(statistics.getIncidentStatistics().isEmpty());
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertEquals(1, incidentStatistics.getIncidentCount());
  }

  @Test
  public void testQueryIncludingIncidentsWithReadPermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(0, statistics.getFailedJobs());
    assertFalse(statistics.getIncidentStatistics().isEmpty());
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertEquals(3, incidentStatistics.getIncidentCount());
  }

  @Test
  public void testQueryIncludingIncidentsWithReadInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ, READ_INSTANCE);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(0, statistics.getFailedJobs());
    assertFalse(statistics.getIncidentStatistics().isEmpty());
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertEquals(3, incidentStatistics.getIncidentCount());
  }

  // including incidents and failed jobs //////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingIncidentsAndFailedJobsWithoutAuthorizationOnProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .includeFailedJobs()
        .singleResult();

    // then
    assertNull(statistics);
  }

  @Test
  public void testQueryIncludingIncidentsAndFailedJobsWithReadPermissionOnOneProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    disableAuthorization();
    String processInstanceId = runtimeService.createProcessInstanceQuery().list().get(0).getId();
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .includeFailedJobs()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(1, statistics.getInstances());
    assertEquals(1, statistics.getFailedJobs());
    assertFalse(statistics.getIncidentStatistics().isEmpty());
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertEquals(1, incidentStatistics.getIncidentCount());
  }

  @Test
  public void testQueryIncludingIncidentsAndFailedJobsWithReadPermissionOnAnyProcessInstance() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .includeFailedJobs()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(3, statistics.getFailedJobs());
    assertFalse(statistics.getIncidentStatistics().isEmpty());
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertEquals(3, incidentStatistics.getIncidentCount());
  }

  @Test
  public void testQueryIncludingIncidentsAndFailedJobsWithReadInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ, READ_INSTANCE);

    // when
    ActivityStatistics statistics = managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeIncidents()
        .includeFailedJobs()
        .singleResult();

    // then
    assertNotNull(statistics);
    assertEquals("scriptTask", statistics.getId());
    assertEquals(3, statistics.getInstances());
    assertEquals(3, statistics.getFailedJobs());
    assertFalse(statistics.getIncidentStatistics().isEmpty());
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertEquals(3, incidentStatistics.getIncidentCount());
  }

  @Test
  public void testManyAuthorizationsActivityStatisticsQueryIncludingFailedJobsAndIncidents() {
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ, READ_INSTANCE);
    createGrantAuthorizationGroup(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, groupId, READ, READ_INSTANCE);

    List<ActivityStatistics> statistics =
      managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertEquals(1, statistics.size());

    ActivityStatistics activityResult = statistics.get(0);
    assertEquals(3, activityResult.getInstances());
    assertEquals("scriptTask", activityResult.getId());
    assertEquals(3, activityResult.getFailedJobs());
    assertFalse(activityResult.getIncidentStatistics().isEmpty());
    IncidentStatistics incidentStatistics = activityResult.getIncidentStatistics().get(0);
    assertEquals(3, incidentStatistics.getIncidentCount());
  }

  @Test
  public void testManyAuthorizationsActivityStatisticsQuery() {
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ, READ_INSTANCE);
    createGrantAuthorizationGroup(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, groupId, READ, READ_INSTANCE);

    List<ActivityStatistics> statistics =
      managementService
        .createActivityStatisticsQuery(processDefinitionId)
        .list();

    assertEquals(1, statistics.size());

    ActivityStatistics activityResult = statistics.get(0);
    assertEquals(3, activityResult.getInstances());
    assertEquals("scriptTask", activityResult.getId());
    assertEquals(0, activityResult.getFailedJobs());
    assertTrue(activityResult.getIncidentStatistics().isEmpty());
  }

}
