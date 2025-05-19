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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.management.DeploymentStatistics;
import org.operaton.bpm.engine.management.IncidentStatistics;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

class DeploymentStatisticsQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected RepositoryService repositoryService;

  @Test
  void testDeploymentStatisticsQuery() {
    String deploymentName = "my deployment";

    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml")
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testParallelGatewayStatisticsQuery.bpmn20.xml")
        .name(deploymentName)
        .deploy();
    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ParGatewayExampleProcess");

    List<DeploymentStatistics> statistics =
        managementService.createDeploymentStatisticsQuery().includeFailedJobs().list();

    assertThat(statistics).hasSize(1);

    DeploymentStatistics result = statistics.get(0);
    assertThat(result.getInstances()).isEqualTo(2);
    assertThat(result.getFailedJobs()).isZero();

    assertThat(result.getId()).isEqualTo(deployment.getId());
    assertThat(result.getName()).isEqualTo(deploymentName);

    // only compare time on second level (i.e. drop milliseconds)
    Calendar cal1 = Calendar.getInstance();
    cal1.setTime(deployment.getDeploymentTime());
    cal1.set(Calendar.MILLISECOND, 0);

    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(result.getDeploymentTime());
    cal2.set(Calendar.MILLISECOND, 0);

    assertThat(cal2).isEqualTo(cal1);

    repositoryService.deleteDeployment(deployment.getId(), true);
  }

  @Test
  void testDeploymentStatisticsQueryCountAndPaging() {
    org.operaton.bpm.engine.repository.Deployment deployment = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml")
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testParallelGatewayStatisticsQuery.bpmn20.xml")
        .deploy();

    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ParGatewayExampleProcess");

    org.operaton.bpm.engine.repository.Deployment anotherDeployment = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml")
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testParallelGatewayStatisticsQuery.bpmn20.xml")
        .deploy();

    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ParGatewayExampleProcess");

    long count = managementService.createDeploymentStatisticsQuery().includeFailedJobs().count();

    assertThat(count).isEqualTo(2);

    List<DeploymentStatistics> statistics = managementService.createDeploymentStatisticsQuery().includeFailedJobs().listPage(0, 1);
    assertThat(statistics).hasSize(1);

    repositoryService.deleteDeployment(deployment.getId(), true);
    repositoryService.deleteDeployment(anotherDeployment.getId(), true);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml"})
  void testDeploymentStatisticsQueryWithFailedJobs() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService.createDeploymentStatisticsQuery().includeFailedJobs().list();

    DeploymentStatistics result = statistics.get(0);
    assertThat(result.getFailedJobs()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml"})
  void testDeploymentStatisticsQueryWithIncidents() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService.createDeploymentStatisticsQuery().includeIncidents().list();

    assertThat(statistics)
            .isNotEmpty()
            .hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml"})
  void testDeploymentStatisticsQueryWithIncidentType() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeIncidentsForType("failedJob")
        .list();

    assertThat(statistics)
            .isNotEmpty()
            .hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml"})
  void testDeploymentStatisticsQueryWithInvalidIncidentType() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeIncidentsForType("invalid")
        .list();

    assertThat(statistics)
            .isNotEmpty()
            .hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();
    assertThat(incidentStatistics).isEmpty();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testStatisticsQueryWithFailedJobs.bpmn20.xml"})
  void testDeploymentStatisticsQueryWithIncidentsAndFailedJobs() {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("fail", true);

    runtimeService.startProcessInstanceByKey("MIExampleProcess");
    runtimeService.startProcessInstanceByKey("ExampleProcess", parameters);

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeIncidents()
        .includeFailedJobs()
        .list();

    assertThat(statistics)
            .isNotEmpty()
            .hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    assertThat(result.getFailedJobs()).isEqualTo(1);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(1);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testCallActivityWithIncidentsWithoutFailedJobs.bpmn20.xml")
  void testDeploymentStatisticsQueryWithTwoIncidentsAndOneFailedJobs() {
    runtimeService.startProcessInstanceByKey("callExampleSubProcess");

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeIncidents()
        .includeFailedJobs()
        .list();

    assertThat(statistics)
            .isNotEmpty()
            .hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    // has one failed job
    assertThat(result.getFailedJobs()).isEqualTo(1);

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();
    assertThat(incidentStatistics)
            .isNotEmpty()
            .hasSize(1);

    IncidentStatistics incident = incidentStatistics.get(0);
    assertThat(incident.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
    assertThat(incident.getIncidentCount()).isEqualTo(2); // ...but two incidents
  }


  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testMultiInstanceStatisticsQuery.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testParallelGatewayStatisticsQuery.bpmn20.xml"})
  void testDeploymentStatisticsQueryWithoutRunningInstances() {
    List<DeploymentStatistics> statistics =
        managementService.createDeploymentStatisticsQuery().includeFailedJobs().list();

    assertThat(statistics).hasSize(1);

    DeploymentStatistics result = statistics.get(0);
    assertThat(result.getInstances()).isZero();
    assertThat(result.getFailedJobs()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  void testQueryByIncidentsWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isZero();

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // but there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  void testQueryByIncidentTypeWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeIncidentsForType(Incident.FAILED_JOB_HANDLER_TYPE)
        .list();

    assertThat(statistics).hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isZero();

    List<IncidentStatistics> incidentStatistics = result.getIncidentStatistics();

    // but there is one incident for the failed timer job
    assertThat(incidentStatistics).hasSize(1);

    IncidentStatistics incidentStatistic = incidentStatistics.get(0);
    assertThat(incidentStatistic.getIncidentCount()).isEqualTo(1);
    assertThat(incidentStatistic.getIncidentType()).isEqualTo(Incident.FAILED_JOB_HANDLER_TYPE);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  void testQueryByFailedJobsWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs()
        .list();

    assertThat(statistics).hasSize(1);

    DeploymentStatistics result = statistics.get(0);

    // there is no running instance
    assertThat(result.getInstances()).isZero();
    // but there is one failed timer job
    assertThat(result.getFailedJobs()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/StatisticsTest.testFailedTimerStartEvent.bpmn20.xml")
  @Test
  void testQueryByFailedJobsAndIncidentsWithFailedTimerStartEvent() {

    testRule.executeAvailableJobs();

    List<DeploymentStatistics> statistics =
        managementService
        .createDeploymentStatisticsQuery()
        .includeFailedJobs()
        .includeIncidents()
        .list();

    assertThat(statistics).hasSize(1);

    DeploymentStatistics result = statistics.get(0);

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
}
