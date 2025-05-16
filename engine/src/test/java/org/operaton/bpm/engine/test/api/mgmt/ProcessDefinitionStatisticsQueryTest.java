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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.management.IncidentStatistics;
import org.operaton.bpm.engine.management.ProcessDefinitionStatistics;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

class ProcessDefinitionStatisticsQueryTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryWithFailedJobs() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(2);
    assertThat(definitionResult.getFailedJobs()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryWithIncidents() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(2);

    assertThat(definitionResult.getIncidentStatistics()).isNotEmpty();
    assertThat(definitionResult.getIncidentStatistics()).hasSize(1);

    IncidentStatistics incidentStatistics = definitionResult.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryWithIncidentType() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidentsForType("failedJob")
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(2);

    assertThat(definitionResult.getIncidentStatistics()).isNotEmpty();
    assertThat(definitionResult.getIncidentStatistics()).hasSize(1);

    IncidentStatistics incidentStatistics = definitionResult.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryWithInvalidIncidentType() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidentsForType("invalid")
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(2);

    assertThat(definitionResult.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryWithIncidentsAndFailedJobs() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidents()
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(2);
    assertThat(definitionResult.getFailedJobs()).isEqualTo(1);

    assertThat(definitionResult.getIncidentStatistics()).isNotEmpty();
    assertThat(definitionResult.getIncidentStatistics()).hasSize(1);

    IncidentStatistics incidentStatistics = definitionResult.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryWithoutRunningInstances() {
    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isZero();
    assertThat(definitionResult.getFailedJobs()).isZero();

    statistics =
        managementService.createProcessDefinitionStatisticsQuery().includeIncidents().list();

    assertThat(definitionResult.getIncidentStatistics()).isEmpty();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryCount() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    testRule.executeAvailableJobs();

    long count =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents()
        .count();

    assertThat(count).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml")
  void testMultiInstanceProcessDefinitionStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("MIExampleProcess");

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics result = statistics.get(0);
    assertThat(result.getInstances()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testSubprocessStatisticsQuery.bpmn20.xml")
  void testSubprocessProcessDefinitionStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("ExampleProcess");

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics result = statistics.get(0);
    assertThat(result.getInstances()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testCallActivityWithIncidentsWithoutFailedJobs.bpmn20.xml")
  void testCallActivityProcessDefinitionStatisticsQuery() {
    runtimeService.startProcessInstanceByKey("callExampleSubProcess");

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(2);

    for (ProcessDefinitionStatistics result : statistics) {
      if (result.getKey().equals("ExampleProcess")) {
        assertThat(result.getInstances()).isEqualTo(1);
        assertThat(result.getFailedJobs()).isEqualTo(1);
      } else if (result.getKey().equals("callExampleSubProcess")) {
        assertThat(result.getInstances()).isEqualTo(1);
        assertThat(result.getFailedJobs()).isZero();
      } else {
        fail(result + " was not expected.");
      }
    }
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryForMultipleVersions() {
    org.operaton.bpm.engine.repository.Deployment deployment =
        repositoryService.createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
          .deploy();

    List<ProcessDefinition> definitions =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess")
        .list();

    for (ProcessDefinition definition : definitions) {
      runtimeService.startProcessInstanceById(definition.getId());
    }

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(1);
    assertThat(definitionResult.getFailedJobs()).isZero();

    assertThat(definitionResult.getIncidentStatistics()).isEmpty();

    definitionResult = statistics.get(1);
    assertThat(definitionResult.getInstances()).isEqualTo(1);
    assertThat(definitionResult.getFailedJobs()).isZero();

    assertThat(definitionResult.getIncidentStatistics()).isEmpty();

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryForMultipleVersionsWithFailedJobsAndIncidents() {
    org.operaton.bpm.engine.repository.Deployment deployment =
        repositoryService.createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
          .deploy();

    List<ProcessDefinition> definitions =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess")
        .list();

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    for (ProcessDefinition definition : definitions) {
      runtimeService.startProcessInstanceById(definition.getId(), parameters);
    }

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(1);
    assertThat(definitionResult.getFailedJobs()).isEqualTo(1);

    List<IncidentStatistics> incidentStatistics = definitionResult.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);

    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1);

    definitionResult = statistics.get(1);
    assertThat(definitionResult.getInstances()).isEqualTo(1);
    assertThat(definitionResult.getFailedJobs()).isEqualTo(1);

    incidentStatistics = definitionResult.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    incident = incidentStatistics.get(0);

    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1);

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryForMultipleVersionsWithIncidentType() {
    org.operaton.bpm.engine.repository.Deployment deployment =
        repositoryService.createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
          .deploy();

    List<ProcessDefinition> definitions =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess")
        .list();

    for (ProcessDefinition definition : definitions) {
      runtimeService.startProcessInstanceById(definition.getId());
    }

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .includeIncidentsForType("failedJob")
        .list();

    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics definitionResult = statistics.get(0);
    assertThat(definitionResult.getInstances()).isEqualTo(1);
    assertThat(definitionResult.getFailedJobs()).isZero();

    assertThat(definitionResult.getIncidentStatistics()).isEmpty();

    definitionResult = statistics.get(1);
    assertThat(definitionResult.getInstances()).isEqualTo(1);
    assertThat(definitionResult.getFailedJobs()).isZero();

    assertThat(definitionResult.getIncidentStatistics()).isEmpty();

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryPagination() {
    org.operaton.bpm.engine.repository.Deployment deployment =
        repositoryService.createDeployment()
          .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQuery.bpmn20.xml")
          .deploy();

    List<ProcessDefinition> definitions =
        repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey("ExampleProcess")
        .list();

    for (ProcessDefinition definition : definitions) {
      runtimeService.startProcessInstanceById(definition.getId());
    }

    List<ProcessDefinitionStatistics> statistics =
        managementService.createProcessDefinitionStatisticsQuery().includeFailedJobs().listPage(0, 1);

    assertThat(statistics).hasSize(1);

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testCallActivityWithIncidentsWithoutFailedJobs.bpmn20.xml")
  void testProcessDefinitionStatisticsQueryWithIncidentsWithoutFailedJobs() {
    runtimeService.startProcessInstanceByKey("callExampleSubProcess");

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidents()
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(2);

    ProcessDefinitionStatistics callExampleSubProcessStaticstics = null;
    ProcessDefinitionStatistics exampleSubProcessStaticstics = null;

    for (ProcessDefinitionStatistics current : statistics) {
      if (current.getKey().equals("callExampleSubProcess")) {
        callExampleSubProcessStaticstics = current;
      } else if (current.getKey().equals("ExampleProcess")) {
        exampleSubProcessStaticstics = current;
      } else {
        fail(current.getKey() + " was not expected.");
      }
    }

    assertThat(callExampleSubProcessStaticstics).isNotNull();
    assertThat(exampleSubProcessStaticstics).isNotNull();

    // "super" process definition
    assertThat(callExampleSubProcessStaticstics.getInstances()).isEqualTo(1);
    assertThat(callExampleSubProcessStaticstics.getFailedJobs()).isZero();

    assertThat(callExampleSubProcessStaticstics.getIncidentStatistics()).isNotEmpty();
    assertThat(callExampleSubProcessStaticstics.getIncidentStatistics()).hasSize(1);

    IncidentStatistics incidentStatistics = callExampleSubProcessStaticstics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(1);

    // "called" process definition
    assertThat(exampleSubProcessStaticstics.getInstances()).isEqualTo(1);
    assertThat(exampleSubProcessStaticstics.getFailedJobs()).isEqualTo(1);

    assertThat(exampleSubProcessStaticstics.getIncidentStatistics()).isNotEmpty();
    assertThat(exampleSubProcessStaticstics.getIncidentStatistics()).hasSize(1);

    incidentStatistics = exampleSubProcessStaticstics.getIncidentStatistics().get(0);
    assertThat(incidentStatistics.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incidentStatistics.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  void testQueryByIncidentsWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isZero();

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // but there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  void testQueryByIncidentTypeWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidentsForType(Incident.FAILED_JOB_HANDLER_TYPE)
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isZero();

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // but there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  void testQueryByFailedJobsWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isZero();
    // but there is one failed timer job
    assertThat(result.getFailedJobs()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  void testQueryByFailedJobsAndIncidentsWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    ProcessDefinitionStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isZero();
    // but there is one failed timer job
    assertThat(result.getFailedJobs()).isEqualTo(1);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // and there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testCallActivityWithIncidentsWithoutFailedJobs.bpmn20.xml")
  void testIncludeRootIncidentsOnly() {
    runtimeService.startProcessInstanceByKey("callExampleSubProcess");

    testRule.executeAvailableJobs();

    List<ProcessDefinitionStatistics> statistics =
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeRootIncidents()
        .list();

    // two process definitions
    assertThat(statistics).hasSize(2);

    for (ProcessDefinitionStatistics definitionResult : statistics) {

      if (definitionResult.getKey().equals("callExampleSubProcess")) {
        // there is no root incidents
        assertThat(definitionResult.getIncidentStatistics()).isEmpty();

      } else if (definitionResult.getKey().equals("ExampleProcess")) {
        // there is one root incident
        assertThat(definitionResult.getIncidentStatistics()).isNotEmpty();
        assertThat(definitionResult.getIncidentStatistics()).hasSize(1);
        assertThat(definitionResult.getIncidentStatistics().get(0).getIncidentCount()).isEqualTo(1);

      } else {
        // fail if the process definition key does not match
        fail("");
      }
    }
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testCallActivityWithIncidentsWithoutFailedJobs.bpmn20.xml")
  void testIncludeRootIncidentsFails() {
    runtimeService.startProcessInstanceByKey("callExampleSubProcess");

    testRule.executeAvailableJobs();

    try {
        managementService
        .createProcessDefinitionStatisticsQuery()
        .includeIncidents()
        .includeRootIncidents()
        .list();
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("It is not possible to use includeIncident() and includeRootIncidents() to execute one query");
    }
  }

  @Test
  void testProcessDefinitionStatisticsProperties() {
    String resourceName = "org/operaton/bpm/engine/test/api/mgmt/ProcessDefinitionStatisticsQueryTest.testProcessDefinitionStatisticsProperties.bpmn20.xml";
    String deploymentId = testRule.deployForTenant("tenant1", resourceName).getId();

    ProcessDefinitionStatistics processDefinitionStatistics = managementService.createProcessDefinitionStatisticsQuery().singleResult();

    assertThat(processDefinitionStatistics.getKey()).isEqualTo("testProcess");
    assertThat(processDefinitionStatistics.getName()).isEqualTo("process name");
    assertThat(processDefinitionStatistics.getCategory()).isEqualTo("Examples");
    assertThat(processDefinitionStatistics.getDescription()).isNull(); // it is not parsed for the statistics query
    assertThat(processDefinitionStatistics.getTenantId()).isEqualTo("tenant1");
    assertThat(processDefinitionStatistics.getVersionTag()).isEqualTo("v0.1.0");
    assertThat(processDefinitionStatistics.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(processDefinitionStatistics.getResourceName()).isEqualTo(resourceName);
    assertThat(processDefinitionStatistics.getDiagramResourceName()).isNull();
    assertThat(processDefinitionStatistics.getVersion()).isEqualTo(1);
    assertThat(processDefinitionStatistics.getInstances()).isZero();
    assertThat(processDefinitionStatistics.getFailedJobs()).isZero();
    assertThat(processDefinitionStatistics.getIncidentStatistics()).isEmpty();
    assertThat(processDefinitionStatistics.isStartableInTasklist()).isTrue();
  }

}
