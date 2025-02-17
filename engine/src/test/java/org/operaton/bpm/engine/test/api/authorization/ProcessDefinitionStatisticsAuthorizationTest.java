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
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.operaton.bpm.engine.management.IncidentStatistics;
import org.operaton.bpm.engine.management.ProcessDefinitionStatistics;
import org.operaton.bpm.engine.management.ProcessDefinitionStatisticsQuery;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class ProcessDefinitionStatisticsAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";
  protected static final String ONE_INCIDENT_PROCESS_KEY = "process";

  @Override
  @Before
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/oneIncidentProcess.bpmn20.xml").getId();
    super.setUp();
  }

  // without running instances //////////////////////////////////////////////////////////

  @Test
  public void testQueryWithoutAuthorizations() {
    // given

    // when
    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  public void testQueryWithReadPermissionOnOneTaskProcess() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);

    // when
    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    // then
    verifyQueryResults(query, 1);

    ProcessDefinitionStatistics statistics = query.singleResult();
    assertThat(statistics.getKey()).isEqualTo(ONE_TASK_PROCESS_KEY);
    assertThat(statistics.getInstances()).isZero();
    assertThat(statistics.getFailedJobs()).isZero();
    assertThat(statistics.getIncidentStatistics()).isEmpty();
  }

  @Test
  public void testQueryWithMultiple() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ);
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  public void testQueryWithReadPermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    // then
    verifyQueryResults(query, 2);

    List<ProcessDefinitionStatistics> statistics = query.list();
    for (ProcessDefinitionStatistics result : statistics) {
      verifyStatisticsResult(result, 0, 0, 0);
    }
  }

  @Test
  public void shouldNotFindStatisticsWithRevokedReadPermissionOnAnyProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, ANY, "*", ALL);
    createRevokeAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // including instances //////////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingInstancesWithoutProcessInstanceAuthorizations() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    List<ProcessDefinitionStatistics> statistics = managementService.createProcessDefinitionStatisticsQuery().list();

    // then
    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics oneTaskProcessStatistics = getStatisticsByKey(statistics, ONE_TASK_PROCESS_KEY);
    verifyStatisticsResult(oneTaskProcessStatistics, 2, 0, 0);

    ProcessDefinitionStatistics oneIncidentProcessStatistics = getStatisticsByKey(statistics, ONE_INCIDENT_PROCESS_KEY);
    verifyStatisticsResult(oneIncidentProcessStatistics, 3, 0, 0);
  }

  // including failed jobs ////////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingFailedJobsWithoutProcessInstanceAuthorizations() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    List<ProcessDefinitionStatistics> statistics = managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .list();

    // then
    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics oneTaskProcessStatistics = getStatisticsByKey(statistics, ONE_TASK_PROCESS_KEY);
    verifyStatisticsResult(oneTaskProcessStatistics, 2, 0, 0);

    ProcessDefinitionStatistics oneIncidentProcessStatistics = getStatisticsByKey(statistics, ONE_INCIDENT_PROCESS_KEY);
    verifyStatisticsResult(oneIncidentProcessStatistics, 3, 3, 0);
  }

  // including incidents //////////////////////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingIncidentsWithoutProcessInstanceAuthorizations() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    List<ProcessDefinitionStatistics> statistics = managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidents()
        .list();

    // then
    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics oneTaskProcessStatistics = getStatisticsByKey(statistics, ONE_TASK_PROCESS_KEY);
    verifyStatisticsResult(oneTaskProcessStatistics, 2, 0, 0);

    ProcessDefinitionStatistics oneIncidentProcessStatistics = getStatisticsByKey(statistics, ONE_INCIDENT_PROCESS_KEY);
    verifyStatisticsResult(oneIncidentProcessStatistics, 3, 0, 3);
  }

  // including incidents and failed jobs ///////////////////////////////////////////////////////////////

  @Test
  public void testQueryIncludingIncidentsAndFailedJobsWithoutProcessInstanceAuthorizations() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);
    startProcessAndExecuteJob(ONE_INCIDENT_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ);

    // when
    List<ProcessDefinitionStatistics> statistics = managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidents()
        .includeFailedJobs()
        .list();

    // then
    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics oneTaskProcessStatistics = getStatisticsByKey(statistics, ONE_TASK_PROCESS_KEY);
    verifyStatisticsResult(oneTaskProcessStatistics, 2, 0, 0);

    ProcessDefinitionStatistics oneIncidentProcessStatistics = getStatisticsByKey(statistics, ONE_INCIDENT_PROCESS_KEY);
    verifyStatisticsResult(oneIncidentProcessStatistics, 3, 3, 3);
  }

  // helper ///////////////////////////////////////////////////////////////////////////

  protected void verifyStatisticsResult(ProcessDefinitionStatistics statistics, int instances, int failedJobs, int incidents) {
    assertThat(statistics.getInstances()).as("Instances").isEqualTo(instances);
    assertThat(statistics.getFailedJobs()).as("Failed Jobs").isEqualTo(failedJobs);

    List<IncidentStatistics> incidentStatistics = statistics.getIncidentStatistics();
    if (incidents == 0) {
      assertThat(incidentStatistics.isEmpty()).as("Incidents supposed to be empty").isTrue();
    }
    else {
      // the test does have only one type of incidents
      assertThat(incidentStatistics.get(0).getIncidentCount()).as("Incidents").isEqualTo(incidents);
    }
  }

  protected ProcessDefinitionStatistics getStatisticsByKey(List<ProcessDefinitionStatistics> statistics, String key) {
    for (ProcessDefinitionStatistics result : statistics) {
      if (key.equals(result.getKey())) {
        return result;
      }
    }
    fail("No statistics found for key '" + key + "'.");
    return null;
  }
}
