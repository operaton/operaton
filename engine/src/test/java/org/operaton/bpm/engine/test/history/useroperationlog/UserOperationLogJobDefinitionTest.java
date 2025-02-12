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
package org.operaton.bpm.engine.test.history.useroperationlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.junit.Test;

/**
 * @author Thorben Lindhauer
 *
 */
public class UserOperationLogJobDefinitionTest extends AbstractUserOperationLogTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetOverridingPriority() {
    // For a given deployment
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();

    // given a job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when I set a job priority
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42);

    // then an op log entry is written
    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery().singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB_DEFINITION);
    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("overridingPriority");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("42");
    assertThat(userOperationLogEntry.getOrgValue()).isNull();

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(jobDefinition.getProcessDefinitionId());
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(jobDefinition.getProcessDefinitionKey());
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(deploymentId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testOverwriteOverridingPriority() {
    // given a job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // with an overriding priority
    ClockUtil.setCurrentTime(new Date(System.currentTimeMillis()));
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42);

    // when I overwrite that priority
    ClockUtil.setCurrentTime(new Date(System.currentTimeMillis() + 10000));
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 43);

    // then this is accessible via the op log
    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .orderByTimestamp().desc().listPage(0, 1).get(0);
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB_DEFINITION);
    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY);

    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("overridingPriority");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("43");
    assertThat(userOperationLogEntry.getOrgValue()).isEqualTo("42");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testClearOverridingPriority() {
    // for a given deployment
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();

    // given a job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // with an overriding priority
    ClockUtil.setCurrentTime(new Date(System.currentTimeMillis()));
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42);

    // when I clear that priority
    ClockUtil.setCurrentTime(new Date(System.currentTimeMillis() + 10000));
    managementService.clearOverridingJobPriorityForJobDefinition(jobDefinition.getId());

    // then this is accessible via the op log
    UserOperationLogEntry userOperationLogEntry = historyService.createUserOperationLogQuery()
        .orderByTimestamp().desc().listPage(0, 1).get(0);
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB_DEFINITION);
    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("overridingPriority");
    assertNull(userOperationLogEntry.getNewValue());
    assertThat(userOperationLogEntry.getOrgValue()).isEqualTo("42");

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(jobDefinition.getProcessDefinitionId());
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(jobDefinition.getProcessDefinitionKey());
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(deploymentId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetOverridingPriorityCascadeToJobs() {
    // given a job definition and job
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    Job job = managementService.createJobQuery().singleResult();

    // when I set an overriding priority with cascade=true
    managementService.setOverridingJobPriorityForJobDefinition(jobDefinition.getId(), 42, true);

    // then there are three op log entries
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(3);

    // (1): One for the process instance start
    UserOperationLogEntry processInstanceStartOpLogEntry = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.PROCESS_INSTANCE).singleResult();
    assertNotNull(processInstanceStartOpLogEntry);

    // (2): One for the job definition priority
    UserOperationLogEntry jobDefOpLogEntry = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.JOB_DEFINITION).singleResult();
    assertNotNull(jobDefOpLogEntry);

    // (3): and another one for the job priorities
    UserOperationLogEntry jobOpLogEntry = historyService.createUserOperationLogQuery()
        .entityType(EntityTypes.JOB).singleResult();
    assertNotNull(jobOpLogEntry);

    assertThat(jobOpLogEntry.getOperationId()).as("the two job related entries should be part of the same operation").isEqualTo(jobDefOpLogEntry.getOperationId());

    assertThat(jobOpLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull("id should null because it is a bulk update operation", jobOpLogEntry.getJobId());

    assertThat(jobOpLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY);

    assertThat(jobOpLogEntry.getProperty()).isEqualTo("priority");
    assertThat(jobOpLogEntry.getNewValue()).isEqualTo("42");
    assertNull("Original Value should be null because it is not known for bulk operations",
        jobOpLogEntry.getOrgValue());

    assertThat(jobOpLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(jobOpLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // these properties should be there to narrow down the bulk update (like a SQL WHERE clasue)
    assertThat(jobOpLogEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertNull("an unspecified set of process instances was affected by the operation",
        jobOpLogEntry.getProcessInstanceId());
    assertThat(jobOpLogEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(jobOpLogEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(jobOpLogEntry.getDeploymentId()).isEqualTo(deploymentId);
  }

}
