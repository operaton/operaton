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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricIncident;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 *
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricIncidentTest extends PluggableProcessEngineTest {

  private static String PROCESS_DEFINITION_KEY = "oneFailingServiceTaskProcess";

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testPropertiesOfHistoricIncident() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    Incident incident = runtimeService.createIncidentQuery().singleResult();
    assertNotNull(incident);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
    assertNotNull(historicIncident);

    // the last failure log entry correlates to the historic incident
    HistoricJobLog jobLog = getHistoricJobLogOrdered(incident.getConfiguration()).get(0);

    assertThat(historicIncident.getId()).isEqualTo(incident.getId());
    assertThat(historicIncident.getCreateTime()).isEqualTo(incident.getIncidentTimestamp());
    assertNull(historicIncident.getEndTime());
    assertThat(historicIncident.getIncidentType()).isEqualTo(incident.getIncidentType());
    assertThat(historicIncident.getIncidentMessage()).isEqualTo(incident.getIncidentMessage());
    assertThat(historicIncident.getExecutionId()).isEqualTo(incident.getExecutionId());
    assertThat(historicIncident.getActivityId()).isEqualTo(incident.getActivityId());
    assertThat(historicIncident.getProcessInstanceId()).isEqualTo(incident.getProcessInstanceId());
    assertThat(historicIncident.getProcessDefinitionId()).isEqualTo(incident.getProcessDefinitionId());
    assertThat(historicIncident.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
    assertThat(historicIncident.getCauseIncidentId()).isEqualTo(incident.getCauseIncidentId());
    assertThat(historicIncident.getRootCauseIncidentId()).isEqualTo(incident.getRootCauseIncidentId());
    assertThat(historicIncident.getConfiguration()).isEqualTo(incident.getConfiguration());
    assertThat(historicIncident.getJobDefinitionId()).isEqualTo(incident.getJobDefinitionId());
    assertThat(historicIncident.getHistoryConfiguration()).isEqualTo(jobLog.getId());
    assertNotNull(historicIncident.getFailedActivityId());
    assertThat(historicIncident.getFailedActivityId()).isEqualTo(incident.getFailedActivityId());

    assertTrue(historicIncident.isOpen());
    assertFalse(historicIncident.isDeleted());
    assertFalse(historicIncident.isResolved());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testCreateSecondHistoricIncident() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 1);

    testRule.executeAvailableJobs();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();
    assertThat(query.count()).isEqualTo(2);

    // the first historic incident has been resolved
    assertThat(query.resolved().count()).isEqualTo(1);

    query = historyService.createHistoricIncidentQuery();
    // a new historic incident exists which is open
    assertThat(query.open().count()).isEqualTo(1);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testJobLogReferenceWithMultipleHistoricIncidents() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 1);

    testRule.executeAvailableJobs();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();
    assertThat(query.count()).isEqualTo(2);

    List<HistoricJobLog> logs = getHistoricJobLogOrdered(jobId);

    // the first historic incident references the previous-to-last job log
    assertThat(query.resolved().singleResult().getHistoryConfiguration()).isEqualTo(logs.get(1).getId());

    query = historyService.createHistoricIncidentQuery();
    // the new historic incident references the latest job log
    assertThat(query.open().singleResult().getHistoryConfiguration()).isEqualTo(logs.get(0).getId());
  }


  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testSetHistoricIncidentToResolved() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 1);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
    assertNotNull(historicIncident);

    assertNotNull(historicIncident.getEndTime());

    assertFalse(historicIncident.isOpen());
    assertFalse(historicIncident.isDeleted());
    assertTrue(historicIncident.isResolved());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricIncidentQueryTest.testQueryByCauseIncidentId.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testSetHistoricIncidentToResolvedRecursive() {
    startProcessInstance("process");

    String jobId = managementService.createJobQuery().singleResult().getId();
    managementService.setJobRetries(jobId, 1);

    List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();

    for (HistoricIncident historicIncident : historicIncidents) {
      assertNotNull(historicIncident.getEndTime());

      assertFalse(historicIncident.isOpen());
      assertFalse(historicIncident.isDeleted());
      assertTrue(historicIncident.isResolved());
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testSetHistoricIncidentToDeleted() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();
    runtimeService.deleteProcessInstance(processInstanceId, null);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
    assertNotNull(historicIncident);

    assertNotNull(historicIncident.getEndTime());

    assertFalse(historicIncident.isOpen());
    assertTrue(historicIncident.isDeleted());
    assertFalse(historicIncident.isResolved());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricIncidentQueryTest.testQueryByCauseIncidentId.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testSetHistoricIncidentToDeletedRecursive() {
    startProcessInstance("process");

    String processInstanceId = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .singleResult()
        .getId();
    runtimeService.deleteProcessInstance(processInstanceId, null);

    List<HistoricIncident> historicIncidents = historyService.createHistoricIncidentQuery().list();

    for (HistoricIncident historicIncident : historicIncidents) {
      assertNotNull(historicIncident.getEndTime());

      assertFalse(historicIncident.isOpen());
      assertTrue(historicIncident.isDeleted());
      assertFalse(historicIncident.isResolved());
    }
  }

  @Deployment
  @Test
  public void testCreateHistoricIncidentForNestedExecution () {
    startProcessInstance("process");

    Execution execution = runtimeService.createExecutionQuery()
        .activityId("serviceTask")
        .singleResult();
    assertNotNull(execution);

    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery().singleResult();
    assertNotNull(historicIncident);

    HistoricJobLog jobLog = getHistoricJobLogOrdered(historicIncident.getConfiguration()).get(0);

    assertThat(historicIncident.getExecutionId()).isEqualTo(execution.getId());
    assertThat(historicIncident.getActivityId()).isEqualTo("serviceTask");
    assertThat(historicIncident.getHistoryConfiguration()).isEqualTo(jobLog.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricIncidentQueryTest.testQueryByCauseIncidentId.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testCreateRecursiveHistoricIncidents() {
    startProcessInstance("process");

    ProcessInstance pi1 = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("process")
        .singleResult();
    assertNotNull(pi1);

    ProcessInstance pi2 = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .singleResult();
    assertNotNull(pi2);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    HistoricIncident rootCauseHistoricIncident = query.processInstanceId(pi2.getId()).singleResult();
    assertNotNull(rootCauseHistoricIncident);

    // cause and root cause id is equal to the id of the root incident
    assertThat(rootCauseHistoricIncident.getCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());
    assertThat(rootCauseHistoricIncident.getRootCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());

    HistoricIncident historicIncident = query.processInstanceId(pi1.getId()).singleResult();
    assertNotNull(historicIncident);

    // cause and root cause id is equal to the id of the root incident
    assertThat(historicIncident.getCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());
    assertThat(historicIncident.getRootCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricIncidentQueryTest.testQueryByCauseIncidentId.bpmn20.xml",
  "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testJobLogReferenceForRecursiveHistoricIncident() {
    startProcessInstance("process");

    ProcessInstance pi1 = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("process")
        .singleResult();
    ProcessInstance pi2 = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .singleResult();

    HistoricIncident rootCauseHistoricIncident = historyService.createHistoricIncidentQuery()
        .processInstanceId(pi2.getId())
        .singleResult();
    HistoricIncident historicIncident = historyService.createHistoricIncidentQuery()
        .processInstanceId(pi1.getId())
        .singleResult();

    List<HistoricJobLog> logs = getHistoricJobLogOrdered(rootCauseHistoricIncident.getConfiguration());

    // the root incident is referencing the latest failure job log
    assertThat(rootCauseHistoricIncident.getHistoryConfiguration()).isEqualTo(logs.get(0).getId());

    // the parent incident links to no job log
    assertNull(historicIncident.getHistoryConfiguration());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricIncidentTest.testCreateRecursiveHistoricIncidentsForNestedCallActivities.bpmn20.xml",
      "org/operaton/bpm/engine/test/history/HistoricIncidentQueryTest.testQueryByCauseIncidentId.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testCreateRecursiveHistoricIncidentsForNestedCallActivities() {
    startProcessInstance("process1");

    ProcessInstance pi1 = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("process1")
        .singleResult();
    assertNotNull(pi1);

    ProcessInstance pi2 = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey("process")
        .singleResult();
    assertNotNull(pi2);

    ProcessInstance pi3 = runtimeService.createProcessInstanceQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .singleResult();
    assertNotNull(pi3);

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery();

    HistoricIncident rootCauseHistoricIncident = query.processInstanceId(pi3.getId()).singleResult();
    assertNotNull(rootCauseHistoricIncident);

    // cause and root cause id is equal to the id of the root incident
    assertThat(rootCauseHistoricIncident.getCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());
    assertThat(rootCauseHistoricIncident.getRootCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());

    HistoricIncident causeHistoricIncident = query.processInstanceId(pi2.getId()).singleResult();
    assertNotNull(causeHistoricIncident);

    // cause and root cause id is equal to the id of the root incident
    assertThat(causeHistoricIncident.getCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());
    assertThat(causeHistoricIncident.getRootCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());

    HistoricIncident historicIncident = query.processInstanceId(pi1.getId()).singleResult();
    assertNotNull(historicIncident);

    // cause and root cause id is equal to the id of the root incident
    assertThat(historicIncident.getCauseIncidentId()).isEqualTo(causeHistoricIncident.getId());
    assertThat(historicIncident.getRootCauseIncidentId()).isEqualTo(rootCauseHistoricIncident.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testDoNotCreateNewIncident() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    ProcessInstance pi = runtimeService.createProcessInstanceQuery().singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery().processInstanceId(pi.getId());
    HistoricIncident incident = query.singleResult();
    assertNotNull(incident);

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // set retries to 1 by job definition id
    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 1);

    // the incident still exists
    HistoricIncident tmp = query.singleResult();
    assertThat(tmp.getId()).isEqualTo(incident.getId());
    assertNull(tmp.getEndTime());
    assertTrue(tmp.isOpen());

    // execute the available job (should fail again)
    testRule.executeAvailableJobs();

    // the incident still exists and there
    // should be not a new incident
    assertThat(query.count()).isEqualTo(1);
    tmp = query.singleResult();
    assertThat(tmp.getId()).isEqualTo(incident.getId());
    assertNull(tmp.getEndTime());
    assertTrue(tmp.isOpen());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testJobLogReferenceWithNoNewIncidentCreatedOnFailure() {
    startProcessInstance(PROCESS_DEFINITION_KEY);
    ProcessInstance pi = runtimeService.createProcessInstanceQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery().processInstanceId(pi.getId());
    HistoricIncident incident = query.singleResult();

    String jobId = incident.getConfiguration();
    List<HistoricJobLog> logs = getHistoricJobLogOrdered(jobId);
    assertThat(incident.getHistoryConfiguration()).isEqualTo(logs.get(0).getId());

    // set retries to 2 by job definition id
    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 2);

    // execute the available job (should fail again)
    testRule.executeAvailableJobs(false);

    // the incident still exists, there is no new incident, the incident still references the old log entry
    assertThat(query.count()).isEqualTo(1);
    HistoricIncident incidentNew = query.singleResult();
    List<HistoricJobLog> logsNew = getHistoricJobLogOrdered(jobId);
    assertThat(incidentNew.getId()).isEqualTo(incident.getId());
    assertThat(incidentNew.getHistoryConfiguration()).isEqualTo(incident.getHistoryConfiguration());
    assertTrue(logsNew.size() > logs.size());

    // execute the available job (should fail again)
    testRule.executeAvailableJobs(false);

    // the incident still exists, there is no new incident, the incident references the new latest log entry
    assertThat(query.count()).isEqualTo(1);
    incidentNew = query.singleResult();
    logsNew = getHistoricJobLogOrdered(jobId);
    assertTrue(logsNew.size() > logs.size());
    assertThat(incidentNew.getId()).isEqualTo(incident.getId());
    assertNotEquals(incident.getHistoryConfiguration(), incidentNew.getHistoryConfiguration());
    assertThat(incidentNew.getHistoryConfiguration()).isEqualTo(logsNew.get(0).getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testJobLogReferenceWithNewIncidentCreatedOnSetRetriesAfterFailure() {
    startProcessInstance(PROCESS_DEFINITION_KEY, false);
    ProcessInstance pi = runtimeService.createProcessInstanceQuery().singleResult();
    Job job = managementService.createJobQuery().singleResult();

    ClockUtil.offset(2000L);
    testRule.executeAvailableJobs(false);

    List<HistoricJobLog> logs = getHistoricJobLogOrdered(job.getId());
    assertThat(logs).hasSize(2);
    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery().processInstanceId(pi.getId());
    assertThat(query.count()).isEqualTo(0);

    // set retries to 0
    managementService.setJobRetries(job.getId(), 0);

    // an incident is created, it references the latest log entry
    assertThat(query.count()).isEqualTo(1);
    HistoricIncident incident = query.singleResult();
    assertThat(incident.getHistoryConfiguration()).isEqualTo(logs.get(0).getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void testSetRetriesByJobDefinitionIdResolveIncident() {
    startProcessInstance(PROCESS_DEFINITION_KEY);

    ProcessInstance pi = runtimeService.createProcessInstanceQuery().singleResult();

    HistoricIncidentQuery query = historyService.createHistoricIncidentQuery().processInstanceId(pi.getId());
    HistoricIncident incident = query.singleResult();
    assertNotNull(incident);

    runtimeService.setVariable(pi.getId(), "fail", false);

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // set retries to 1 by job definition id
    managementService.setJobRetriesByJobDefinitionId(jobDefinition.getId(), 1);

    // the incident still exists
    HistoricIncident tmp = query.singleResult();
    assertThat(tmp.getId()).isEqualTo(incident.getId());
    assertNull(tmp.getEndTime());
    assertTrue(tmp.isOpen());

    // execute the available job (should succeed)
    testRule.executeAvailableJobs();

    // the incident still exists and is resolved
    assertThat(query.count()).isEqualTo(1);
    tmp = query.singleResult();
    assertThat(tmp.getId()).isEqualTo(incident.getId());
    assertNotNull(tmp.getEndTime());
    assertTrue(tmp.isResolved());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void shouldPropagateSetAnnotationToHistoricIncident() {
    // given
    String annotation = "my annotation";
    startProcessInstance(PROCESS_DEFINITION_KEY);
    HistoricIncidentQuery historicIncidentQuery = historyService.createHistoricIncidentQuery();
    HistoricIncident historicIncident = historicIncidentQuery.singleResult();

    // assume
    assertThat(historicIncident.getAnnotation()).isNull();

    // when
    runtimeService.setAnnotationForIncidentById(historicIncident.getId(), annotation);

    // then
    assertThat(historicIncidentQuery.singleResult().getAnnotation()).isEqualTo(annotation);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/runtime/oneFailingServiceProcess.bpmn20.xml"})
  @Test
  public void shouldPropagateClearAnnotationToHistoricIncident() {
    // given
    String annotation = "my annotation";
    startProcessInstance(PROCESS_DEFINITION_KEY);
    HistoricIncidentQuery historicIncidentQuery = historyService.createHistoricIncidentQuery();
    HistoricIncident historicIncident = historicIncidentQuery.singleResult();
    runtimeService.setAnnotationForIncidentById(historicIncident.getId(), annotation);

    // assume
    assertThat(historicIncidentQuery.singleResult().getAnnotation()).isEqualTo(annotation);

    // when
    runtimeService.clearAnnotationForIncidentById(historicIncident.getId());

    // then
    assertThat(historicIncidentQuery.singleResult().getAnnotation()).isNull();
  }

  protected void startProcessInstance(String key) {
    startProcessInstances(key, 1, true);
  }

  protected void startProcessInstance(String key, boolean recursive) {
    startProcessInstances(key, 1, recursive);
  }

  protected void startProcessInstances(String key, int numberOfInstances, boolean recursive) {
    for (int i = 0; i < numberOfInstances; i++) {
      runtimeService.startProcessInstanceByKey(key);
    }

    testRule.executeAvailableJobs(recursive);
  }

  protected List<HistoricJobLog> getHistoricJobLogOrdered(String jobId) {
    return historyService.createHistoricJobLogQuery()
        .failureLog()
        .jobId(jobId)
        .orderPartiallyByOccurrence().desc()
        .list();
  }

}
