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
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.management.ActivityStatistics;
import org.operaton.bpm.engine.management.IncidentStatistics;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ActivityStatisticsQueryTest extends PluggableProcessEngineTest {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  public void testActivityStatisticsQueryWithoutFailedJobs() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess").singleResult();

    List<ActivityStatistics> statistics =
        managementService.createActivityStatisticsQuery(definition.getId()).list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(1);
    assertThat(activityResult.getId()).isEqualTo("theServiceTask");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  public void testActivityStatisticsQueryWithIncidents() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);

    List<IncidentStatistics> incidentStatistics = activityResult.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  public void testActivityStatisticsQueryWithIncidentType() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
        .includeIncidentsForType("failedJob")
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);

    List<IncidentStatistics> incidentStatistics = activityResult.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  public void testActivityStatisticsQueryWithInvalidIncidentType() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
        .includeIncidentsForType("invalid")
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);

    List<IncidentStatistics> incidentStatistics = activityResult.getIncidentStatistics();
    assertThat(incidentStatistics).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testCallActivityWithIncidentsWithoutFailedJobs.bpmn20.xml")
  public void testActivityStatisticsQueryWithIncidentsWithoutFailedJobs() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callExampleSubProcess");

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
        .includeIncidents()
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);

    assertThat(activityResult.getId()).isEqualTo("callSubProcess");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0); // has no failed jobs

    List<IncidentStatistics> incidentStatistics = activityResult.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1); //... but has one incident
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQuery.bpmn20.xml")
  public void testActivityStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");
    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess").singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(1);
    assertThat(activityResult.getId()).isEqualTo("theTask");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0);
    assertThat(activityResult.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQuery.bpmn20.xml")
  public void testActivityStatisticsQueryCount() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");
    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess").singleResult();

    long count =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .count();

    assertThat(count).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQuery.bpmn20.xml")
  public void testManyInstancesActivityStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");
    runtimeService.startProcessInstanceByKey("ExampleProcess");
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess").singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(3);
    assertThat(activityResult.getId()).isEqualTo("theTask");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0);
    assertThat(activityResult.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml")
  public void testParallelMultiInstanceActivityStatisticsQueryIncludingFailedJobIncidents() {
    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("MIExampleProcess").singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(3);
    assertThat(activityResult.getId()).isEqualTo("theTask");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0);
    assertThat(activityResult.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml")
  public void testParallelMultiInstanceActivityStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("MIExampleProcess").singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(3);
    assertThat(activityResult.getId()).isEqualTo("theTask");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0);
    assertThat(activityResult.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testSubprocessStatisticsQuery.bpmn20.xml")
  public void testSubprocessActivityStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    ProcessDefinition definition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess")
        .singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics result = statistics.get(0);
    assertThat(result.getInstances()).isEqualTo(1);
    assertThat(result.getId()).isEqualTo("subProcessTask");
  }

  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testCallActivityStatisticsQuery.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml"})
  public void testCallActivityActivityStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("callExampleSubProcess");

    testRule.executeAvailableJobs();

    ProcessDefinition definition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess")
        .singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics result = statistics.get(0);
    assertThat(result.getInstances()).isEqualTo(1);
    assertThat(result.getFailedJobs()).isEqualTo(0);
    assertThat(result.getIncidentStatistics()).isEmpty();

    ProcessDefinition callSubProcessDefinition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("callExampleSubProcess")
        .singleResult();

    List<ActivityStatistics> callSubProcessStatistics =
        managementService
        .createActivityStatisticsQuery(callSubProcessDefinition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(callSubProcessStatistics).hasSize(1);

    result = callSubProcessStatistics.get(0);
    assertThat(result.getInstances()).isEqualTo(1);
    assertThat(result.getFailedJobs()).isEqualTo(0);
    assertThat(result.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testActivityStatisticsQueryWithIntermediateTimer.bpmn20.xml")
  public void testActivityStatisticsQueryWithIntermediateTimer() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");
    ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess").singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics activityResult = statistics.get(0);
    assertThat(activityResult.getInstances()).isEqualTo(1);
    assertThat(activityResult.getId()).isEqualTo("theTimer");
    assertThat(activityResult.getFailedJobs()).isEqualTo(0);
    assertThat(activityResult.getIncidentStatistics()).isEmpty();
  }

  @Test
  public void testNullProcessDefinitionParameter() {
    var activityStatisticsQuery = managementService.createActivityStatisticsQuery(null);
    try {
      activityStatisticsQuery.list();
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testParallelGatewayStatisticsQuery.bpmn20.xml")
  public void testActivityStatisticsQueryPagination() {

    ProcessDefinition definition =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ParGatewayExampleProcess")
        .singleResult();

    runtimeService.startProcessInstanceById(definition.getId());

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .listPage(0, 1);

    assertThat(statistics).hasSize(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testParallelGatewayStatisticsQuery.bpmn20.xml")
  public void testParallelGatewayActivityStatisticsQuery() {

    ProcessDefinition definition =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ParGatewayExampleProcess")
        .singleResult();

    runtimeService.startProcessInstanceById(definition.getId());

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .list();

    assertThat(statistics).hasSize(2);

    for (ActivityStatistics result : statistics) {
      assertThat(result.getInstances()).isEqualTo(1);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testNonInterruptingBoundaryEventStatisticsQuery.bpmn20.xml")
  @Test
  public void testNonInterruptingBoundaryEventActivityStatisticsQuery() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    Job boundaryJob = managementService.createJobQuery().singleResult();
    managementService.executeJob(boundaryJob.getId());

    // when
    List<ActivityStatistics> activityStatistics = managementService
      .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
      .list();

    // then
    assertThat(activityStatistics).hasSize(2);

    ActivityStatistics userTaskStatistics = getStatistics(activityStatistics, "task");
    assertThat(userTaskStatistics).isNotNull();
    assertThat(userTaskStatistics.getId()).isEqualTo("task");
    assertThat(userTaskStatistics.getInstances()).isEqualTo(1);

    ActivityStatistics afterBoundaryStatistics = getStatistics(activityStatistics, "afterBoundaryTask");
    assertThat(afterBoundaryStatistics).isNotNull();
    assertThat(afterBoundaryStatistics.getId()).isEqualTo("afterBoundaryTask");
    assertThat(afterBoundaryStatistics.getInstances()).isEqualTo(1);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testAsyncInterruptingEventSubProcessStatisticsQuery.bpmn20.xml")
  @Test
  public void testAsyncInterruptingEventSubProcessActivityStatisticsQuery() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    runtimeService.correlateMessage("Message");

    // when
    ActivityStatistics activityStatistics = managementService
        .createActivityStatisticsQuery(processInstance.getProcessDefinitionId())
        .singleResult();

    // then
    assertThat(activityStatistics).isNotNull();
    assertThat(activityStatistics.getId()).isEqualTo("eventSubprocess");
    assertThat(activityStatistics.getInstances()).isEqualTo(1);
  }

  protected ActivityStatistics getStatistics(List<ActivityStatistics> activityStatistics, String activityId) {
    for (ActivityStatistics statistics : activityStatistics) {
      if (activityId.equals(statistics.getId())) {
        return statistics;
      }
    }

    return null;
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  public void testQueryByIncidentsWithFailedTimerStartEvent() {

    ProcessDefinition definition =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process")
        .singleResult();

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics result = statistics.get(0);

    assertThat(result.getId()).isEqualTo("theStart");

    // there is no running activity instance
    assertThat(result.getInstances()).isEqualTo(0);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // but there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  public void testQueryByIncidentTypeWithFailedTimerStartEvent() {

    ProcessDefinition definition =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process")
        .singleResult();

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeIncidentsForType(Incident.FAILED_JOB_HANDLER_TYPE)
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isEqualTo(0);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // but there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  public void testQueryByFailedJobsWithFailedTimerStartEvent() {

    ProcessDefinition definition =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process")
        .singleResult();

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isEqualTo(0);
    // but there is one failed timer job
    assertThat(result.getFailedJobs()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  public void testQueryByFailedJobsAndIncidentsWithFailedTimerStartEvent() {

    ProcessDefinition definition =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("process")
        .singleResult();

    testRule.executeAvailableJobs();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ActivityStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isEqualTo(0);
    // but there is one failed timer job
    assertThat(result.getFailedJobs()).isEqualTo(1);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // and there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Ignore("CAM-126")
  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQuery.bpmn20.xml")
  public void testActivityStatisticsQueryWithNoInstances() {

    ProcessDefinition definition =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess")
        .singleResult();

    List<ActivityStatistics> statistics =
        managementService
        .createActivityStatisticsQuery(definition.getId())
        .list();

    assertThat(statistics).hasSize(1);
    ActivityStatistics result = statistics.get(0);
    assertThat(result.getId()).isEqualTo("theTask");
    assertThat(result.getInstances()).isEqualTo(0);
    assertThat(result.getFailedJobs()).isEqualTo(0);

  }
}
