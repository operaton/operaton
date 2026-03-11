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
package org.operaton.bpm.engine.test.api.mgmt;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class JobDefinitionPriorityTest {

  protected static final long EXPECTED_DEFAULT_PRIORITY = 0;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/asyncTaskProcess.bpmn20.xml")
  @Test
  void testSetJobDefinitionPriority() {
    // given a process instance with a job with default priority and a corresponding job definition
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("asyncTaskProcess")
      .startBeforeActivity("task")
      .execute();

    Job job = managementService.createJobQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery()
      .jobDefinitionId(job.getJobDefinitionId()).singleResult();

    // when I set the job definition's priority
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42);

    // then the job definition's priority value has changed
    JobDefinition updatedDefinition = managementService.createJobDefinitionQuery()
        .jobDefinitionId(jobDefinition.getId()).singleResult();
    assertThat((long) updatedDefinition.getOverridingJobPriority()).isEqualTo(42);

    // the existing job's priority has not changed
    Job updatedExistingJob = managementService.createJobQuery().singleResult();
    assertThat(updatedExistingJob.getPriority()).isEqualTo(job.getPriority());

    // and a new job of that definition receives the updated priority
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("task")
      .execute();

    Job newJob = getJobThatIsNot(updatedExistingJob);
    assertThat(newJob.getPriority()).isEqualTo(42);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/asyncTaskProcess.bpmn20.xml")
  @Test
  void testSetJobDefinitionPriorityWithCascade() {
    // given a process instance with a job with default priority and a corresponding job definition
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("asyncTaskProcess")
      .startBeforeActivity("task")
      .execute();

    Job job = managementService.createJobQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery()
      .jobDefinitionId(job.getJobDefinitionId()).singleResult();

    // when I set the job definition's priority
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 52, true);

    // then the job definition's priority value has changed
    JobDefinition updatedDefinition = managementService.createJobDefinitionQuery()
        .jobDefinitionId(jobDefinition.getId()).singleResult();
    assertThat((long) updatedDefinition.getOverridingJobPriority()).isEqualTo(52);

    // the existing job's priority has changed as well
    Job updatedExistingJob = managementService.createJobQuery().singleResult();
    assertThat(updatedExistingJob.getPriority()).isEqualTo(52);

    // and a new job of that definition receives the updated priority
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("task")
      .execute();

    Job newJob = getJobThatIsNot(updatedExistingJob);
    assertThat(newJob.getPriority()).isEqualTo(52);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/jobPrioProcess.bpmn20.xml")
  @Test
  void testSetJobDefinitionPriorityOverridesBpmnPriority() {
    // given a process instance with a job with default priority and a corresponding job definition
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("jobPrioProcess")
      .startBeforeActivity("task2")
      .execute();

    Job job = managementService.createJobQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery()
      .jobDefinitionId(job.getJobDefinitionId()).singleResult();

    // when I set the job definition's priority
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 62);

    // then the job definition's priority value has changed
    JobDefinition updatedDefinition = managementService.createJobDefinitionQuery()
        .jobDefinitionId(jobDefinition.getId()).singleResult();
    assertThat((long) updatedDefinition.getOverridingJobPriority()).isEqualTo(62);

    // the existing job's priority is still the value as given in the BPMN XML
    Job updatedExistingJob = managementService.createJobQuery().singleResult();
    assertThat(updatedExistingJob.getPriority()).isEqualTo(5);

    // and a new job of that definition receives the updated priority
    // meaning that the updated priority overrides the priority specified in the BPMN XML
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("task2")
      .execute();

    Job newJob = getJobThatIsNot(updatedExistingJob);
    assertThat(newJob.getPriority()).isEqualTo(62);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/jobPrioProcess.bpmn20.xml")
  @Test
  void testSetJobDefinitionPriorityWithCascadeOverridesBpmnPriority() {
    // given a process instance with a job with default priority and a corresponding job definition
    ProcessInstance instance = runtimeService.createProcessInstanceByKey("jobPrioProcess")
      .startBeforeActivity("task2")
      .execute();

    Job job = managementService.createJobQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery()
      .jobDefinitionId(job.getJobDefinitionId()).singleResult();

    // when I set the job definition's priority
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 72, true);

    // then the job definition's priority value has changed
    JobDefinition updatedDefinition = managementService.createJobDefinitionQuery()
        .jobDefinitionId(jobDefinition.getId()).singleResult();
    assertThat((long) updatedDefinition.getOverridingJobPriority()).isEqualTo(72);

    // the existing job's priority has changed as well
    Job updatedExistingJob = managementService.createJobQuery().singleResult();
    assertThat(updatedExistingJob.getPriority()).isEqualTo(72);

    // and a new job of that definition receives the updated priority
    // meaning that the updated priority overrides the priority specified in the BPMN XML
    runtimeService.createProcessInstanceModification(instance.getId())
      .startBeforeActivity("task2")
      .execute();

    Job newJob = getJobThatIsNot(updatedExistingJob);
    assertThat(newJob.getPriority()).isEqualTo(72);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/jobPrioProcess.bpmn20.xml")
  @Test
  void testRedeployOverridesSetJobDefinitionPriority() {
    // given a process instance with a job with default priority and a corresponding job definition
    runtimeService.createProcessInstanceByKey("jobPrioProcess")
      .startBeforeActivity("task2")
      .execute();

    Job job = managementService.createJobQuery().singleResult();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery()
      .jobDefinitionId(job.getJobDefinitionId()).singleResult();

    // when I set the job definition's priority
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 72, true);

    // then the job definition's priority value has changed
    JobDefinition updatedDefinition = managementService.createJobDefinitionQuery()
      .jobDefinitionId(jobDefinition.getId()).singleResult();
    assertThat((long) updatedDefinition.getOverridingJobPriority()).isEqualTo(72);

    // the existing job's priority has changed as well
    Job updatedExistingJob = managementService.createJobQuery().singleResult();
    assertThat(updatedExistingJob.getPriority()).isEqualTo(72);

    // if the process definition is redeployed
    String secondDeploymentId = repositoryService.createDeployment().addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/jobPrioProcess.bpmn20.xml").deploy().getId();

    // then a new job will have the priority from the BPMN xml
    ProcessInstance secondInstance = runtimeService.createProcessInstanceByKey("jobPrioProcess")
      .startBeforeActivity("task2")
      .execute();

    Job newJob = managementService.createJobQuery().processInstanceId(secondInstance.getId()).singleResult();
    assertThat(newJob.getPriority()).isEqualTo(5);

    repositoryService.deleteDeployment(secondDeploymentId, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/asyncTaskProcess.bpmn20.xml")
  @Test
  void testResetJobDefinitionPriority() {

    // given a job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when I set a priority
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 1701);

    // and I reset the priority
    managementService.clearOverridingJobPriorityForJobDefinition(jobDefinition.getId());

    // then the job definition priority is still null
    JobDefinition updatedDefinition = managementService.createJobDefinitionQuery()
        .jobDefinitionId(jobDefinition.getId()).singleResult();
    assertThat(updatedDefinition.getOverridingJobPriority()).isNull();

    // and a new job instance does not receive the intermittently set priority
    runtimeService.createProcessInstanceByKey("asyncTaskProcess")
      .startBeforeActivity("task")
      .execute();

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getPriority()).isEqualTo(EXPECTED_DEFAULT_PRIORITY);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/asyncTaskProcess.bpmn20.xml")
  @Test
  void testResetJobDefinitionPriorityWhenPriorityIsNull() {

    // given a job definition with null priority
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    assertThat(jobDefinition.getOverridingJobPriority()).isNull();

    // when I set a priority
    managementService.clearOverridingJobPriorityForJobDefinition(jobDefinition.getId());

    // then the priority remains unchanged
    JobDefinition updatedDefinition = managementService.createJobDefinitionQuery()
        .jobDefinitionId(jobDefinition.getId()).singleResult();
    assertThat(updatedDefinition.getOverridingJobPriority()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/jobPrioProcess.bpmn20.xml")
  @Test
  void testGetJobDefinitionDefaultPriority() {
    // with a process with job definitions deployed
    // then the definitions have a default null priority, meaning that they don't override the
    // value in the BPMN XML
    List<JobDefinition> jobDefinitions = managementService.createJobDefinitionQuery().list();
    assertThat(jobDefinitions).hasSize(4);

    assertThat(jobDefinitions.get(0).getOverridingJobPriority()).isNull();
    assertThat(jobDefinitions.get(1).getOverridingJobPriority()).isNull();
    assertThat(jobDefinitions.get(2).getOverridingJobPriority()).isNull();
    assertThat(jobDefinitions.get(3).getOverridingJobPriority()).isNull();
  }

  @Test
  void testSetNonExistingJobDefinitionPriority() {
    assertThatThrownBy(
      () -> managementService.setOverridingJobPriorityForJobDefinition("someNonExistingJobDefinitionId", 42))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Job definition with id 'someNonExistingJobDefinitionId' does not exist");

    assertThatThrownBy(
      () -> managementService.setOverridingJobPriorityForJobDefinition("someNonExistingJobDefinitionId", 42, true))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Job definition with id 'someNonExistingJobDefinitionId' does not exist");
  }

  @Test
  void testResetNonExistingJobDefinitionPriority() {
    assertThatThrownBy(
      () -> managementService.clearOverridingJobPriorityForJobDefinition("someNonExistingJobDefinitionId"))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Job definition with id 'someNonExistingJobDefinitionId' does not exist");
  }

  @Test
  void testSetNullJobDefinitionPriority() {
    assertThatThrownBy(
      () -> managementService.setOverridingJobPriorityForJobDefinition(null, 42))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("jobDefinitionId is null");

    assertThatThrownBy(
      () -> managementService.setOverridingJobPriorityForJobDefinition(null, 42, true))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("jobDefinitionId is null");
  }

  @Test
  void testResetNullJobDefinitionPriority() {
    assertThatThrownBy(
      () -> managementService.clearOverridingJobPriorityForJobDefinition(null))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("jobDefinitionId is null");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/mgmt/asyncTaskProcess.bpmn20.xml")
  @Test
  void testSetJobDefinitionPriorityToExtremeValues() {
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // it is possible to set the max long value
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), Long.MAX_VALUE);
    jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    assertThat((long) jobDefinition.getOverridingJobPriority()).isEqualTo(Long.MAX_VALUE);

    // it is possible to set the min long value
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), Long.MIN_VALUE + 1); // +1 for informix
    jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    assertThat((long) jobDefinition.getOverridingJobPriority()).isEqualTo(Long.MIN_VALUE + 1);
  }

  protected Job getJobThatIsNot(Job other) {
    List<Job> jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(2);

    if (jobs.get(0).getId().equals(other.getId())) {
      return jobs.get(1);
    }
    else if (jobs.get(1).getId().equals(other.getId())){
      return jobs.get(0);
    }
    else {
      throw new ProcessEngineException("Job with id %s does not exist anymore".formatted(other.getId()));
    }
  }

}
