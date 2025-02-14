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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(1);
    assertThat(statistics.getFailedJobs()).isEqualTo(0);
    assertThat(statistics.getIncidentStatistics()).isEmpty();
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(1);
    assertThat(statistics.getFailedJobs()).isEqualTo(0);
    assertThat(statistics.getIncidentStatistics()).isEmpty();
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(0);
    assertThat(statistics.getIncidentStatistics()).isEmpty();
  }

  @Test
  public void testQueryIncludingInstancesWithReadInstancePermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(ONE_INCIDENT_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_DEFINITION, ONE_INCIDENT_PROCESS_KEY, userId, READ, READ_INSTANCE);

    // when
    ActivityStatistics statistics = managementService.createActivityStatisticsQuery(processDefinitionId).singleResult();

    // then
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(0);
    assertThat(statistics.getIncidentStatistics()).isEmpty();
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
    assertThat(statistics).isNull();
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(1);
    assertThat(statistics.getFailedJobs()).isEqualTo(1);
    assertThat(statistics.getIncidentStatistics()).isEmpty();
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(3);
    assertThat(statistics.getIncidentStatistics()).isEmpty();
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(3);
    assertThat(statistics.getIncidentStatistics()).isEmpty();
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
    assertThat(statistics).isNull();
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(1);
    assertThat(statistics.getFailedJobs()).isEqualTo(0);
    assertThat(statistics.getIncidentStatistics()).isNotEmpty();
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(1);
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(0);
    assertThat(statistics.getIncidentStatistics()).isNotEmpty();
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(3);
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(0);
    assertThat(statistics.getIncidentStatistics()).isNotEmpty();
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(3);
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
    assertThat(statistics).isNull();
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(1);
    assertThat(statistics.getFailedJobs()).isEqualTo(1);
    assertThat(statistics.getIncidentStatistics()).isNotEmpty();
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(1);
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(3);
    assertThat(statistics.getIncidentStatistics()).isNotEmpty();
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(3);
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
    assertThat(statistics).isNotNull();
    assertThat(statistics.getId()).isEqualTo("scriptTask");
    assertThat(statistics.getInstances()).isEqualTo(3);
    assertThat(statistics.getFailedJobs()).isEqualTo(3);
    assertThat(statistics.getIncidentStatistics()).isNotEmpty();
    IncidentStatistics incidentStatistics = statistics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(3);
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

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(3);
    assertThat(activityResult.getId()).isEqualTo("scriptTask");
    assertThat(activityResult.getFailedJobs()).isEqualTo(3);
    assertThat(activityResult.getIncidentStatistics()).isNotEmpty();
    IncidentStatistics incidentStatistics = activityResult.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(3);
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

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(3);
    assertThat(activityResult.getId()).isEqualTo("scriptTask");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0);
    assertThat(activityResult.getIncidentStatistics()).isEmpty();
  }

}
