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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.junit.Test;

/**
 * @author Thorben Lindhauer
 *
 */
public class UserOperationLogJobTest extends AbstractUserOperationLogTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetJobPriority() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // when I set a job priority
    managementService.setJobPriority(job.getId(), 42);

    // then an op log entry is written
    UserOperationLogEntry userOperationLogEntry = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY)
            .singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertThat(userOperationLogEntry.getJobId()).isEqualTo(job.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("priority");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("42");
    assertThat(userOperationLogEntry.getOrgValue()).isEqualTo("0");

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(userOperationLogEntry.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetRetries() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // when I set the job retries
    managementService.setJobRetries(job.getId(), 4);

    // then an op log entry is written
    UserOperationLogEntry userOperationLogEntry = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES)
            .singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertThat(userOperationLogEntry.getJobId()).isEqualTo(job.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("retries");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("4");
    assertThat(userOperationLogEntry.getOrgValue()).isEqualTo("3");

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(userOperationLogEntry.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetRetriesByJobDefinitionId() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // when I set the job retries
    managementService.setJobRetriesByJobDefinitionId(job.getJobDefinitionId(), 4);

    // then an op log entry is written
    UserOperationLogEntry userOperationLogEntry = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES)
            .singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull(userOperationLogEntry.getJobId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("retries");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("4");
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertNull(job.getProcessInstanceId(), userOperationLogEntry.getProcessInstanceId());
    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetRetriesAsync() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // when I set the job retries
    Batch batch = managementService.setJobRetriesAsync(Arrays.asList(job.getId()), 4);

    // then three op log entries are written
    UserOperationLogQuery query = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);
    assertThat(query.count()).isEqualTo(3);

    // check 'retries' entry
    UserOperationLogEntry userOperationLogEntry = query.property("retries").singleResult();
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull(userOperationLogEntry.getJobId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("retries");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("4");
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertNull(job.getJobDefinitionId(), userOperationLogEntry.getJobDefinitionId());
    assertNull(job.getProcessInstanceId(), userOperationLogEntry.getProcessInstanceId());
    assertNull(job.getProcessDefinitionId(), userOperationLogEntry.getProcessDefinitionId());
    assertNull(job.getProcessDefinitionKey(), userOperationLogEntry.getProcessDefinitionKey());
    assertNull(job.getDeploymentId(), userOperationLogEntry.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // check 'nrOfInstances' entry
    userOperationLogEntry = query.property("nrOfInstances").singleResult();
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull(userOperationLogEntry.getJobId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("nrOfInstances");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("1");
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertNull(job.getJobDefinitionId(), userOperationLogEntry.getJobDefinitionId());
    assertNull(job.getProcessInstanceId(), userOperationLogEntry.getProcessInstanceId());
    assertNull(job.getProcessDefinitionId(), userOperationLogEntry.getProcessDefinitionId());
    assertNull(job.getProcessDefinitionKey(), userOperationLogEntry.getProcessDefinitionKey());
    assertNull(job.getDeploymentId(), userOperationLogEntry.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    
    // check 'async' entry
    userOperationLogEntry = query.property("async").singleResult();
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull(userOperationLogEntry.getJobId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("async");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertNull(job.getJobDefinitionId(), userOperationLogEntry.getJobDefinitionId());
    assertNull(job.getProcessInstanceId(), userOperationLogEntry.getProcessInstanceId());
    assertNull(job.getProcessDefinitionId(), userOperationLogEntry.getProcessDefinitionId());
    assertNull(job.getProcessDefinitionKey(), userOperationLogEntry.getProcessDefinitionKey());
    assertNull(job.getDeploymentId(), userOperationLogEntry.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    
    managementService.deleteBatch(batch.getId(), true);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetRetriesAsyncProcessInstanceId() {
    // given a job
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // when I set the job retries
    Batch batch = managementService.setJobRetriesAsync(Arrays.asList(processInstance.getId()), (ProcessInstanceQuery) null, 4);

    // then three op log entries are written
    UserOperationLogQuery query = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);
    assertThat(query.count()).isEqualTo(3);

    // check 'retries' entry
    UserOperationLogEntry userOperationLogEntry = query.property("retries").singleResult();
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull(userOperationLogEntry.getJobId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("retries");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("4");
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertNull(job.getJobDefinitionId(), userOperationLogEntry.getJobDefinitionId());
    assertNull(job.getProcessInstanceId(), userOperationLogEntry.getProcessInstanceId());
    assertNull(job.getProcessDefinitionId(), userOperationLogEntry.getProcessDefinitionId());
    assertNull(job.getProcessDefinitionKey(), userOperationLogEntry.getProcessDefinitionKey());
    assertNull(job.getDeploymentId(), userOperationLogEntry.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);

    // check 'nrOfInstances' entry
    userOperationLogEntry = query.property("nrOfInstances").singleResult();
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull(userOperationLogEntry.getJobId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("nrOfInstances");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("1");
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertNull(job.getJobDefinitionId(), userOperationLogEntry.getJobDefinitionId());
    assertNull(job.getProcessInstanceId(), userOperationLogEntry.getProcessInstanceId());
    assertNull(job.getProcessDefinitionId(), userOperationLogEntry.getProcessDefinitionId());
    assertNull(job.getProcessDefinitionKey(), userOperationLogEntry.getProcessDefinitionKey());
    assertNull(job.getDeploymentId(), userOperationLogEntry.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    
    // check 'async' entry
    userOperationLogEntry = query.property("async").singleResult();
    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertNull(userOperationLogEntry.getJobId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_SET_JOB_RETRIES);

    assertThat(userOperationLogEntry.getProperty()).isEqualTo("async");
    assertThat(userOperationLogEntry.getNewValue()).isEqualTo("true");
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertNull(job.getJobDefinitionId(), userOperationLogEntry.getJobDefinitionId());
    assertNull(job.getProcessInstanceId(), userOperationLogEntry.getProcessInstanceId());
    assertNull(job.getProcessDefinitionId(), userOperationLogEntry.getProcessDefinitionId());
    assertNull(job.getProcessDefinitionKey(), userOperationLogEntry.getProcessDefinitionKey());
    assertNull(job.getDeploymentId(), userOperationLogEntry.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    
    managementService.deleteBatch(batch.getId(), true);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testSetJobDueDate() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // and set the job due date
    Date newDate = new Date(ClockUtil.getCurrentTime().getTime() + 2 * 1000);
    managementService.setJobDuedate(job.getId(), newDate);

    // then one op log entry is written
    UserOperationLogQuery query = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_SET_DUEDATE);
    assertThat(query.count()).isEqualTo(1);

    // assert details
    UserOperationLogEntry entry = query.singleResult();
    assertThat(entry.getJobId()).isEqualTo(job.getId());
    assertThat(entry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(entry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(entry.getProperty()).isEqualTo("duedate");
    assertNull(entry.getOrgValue());
    assertThat(new Date(Long.parseLong(entry.getNewValue()))).isEqualTo(newDate);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/timer/TimerRecalculationTest.testFinishedJob.bpmn20.xml"})
  @Test
  public void testRecalculateJobDueDate() {
    // given a job
    HashMap<String, Object> variables1 = new HashMap<>();
    Date duedate = ClockUtil.getCurrentTime();
    variables1.put("dueDate", duedate);

    runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables1);
    Job job = managementService.createJobQuery().singleResult();

    // when I recalculate the job due date
    managementService.recalculateJobDuedate(job.getId(), false);

    // then one op log entry is written
    UserOperationLogQuery query = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_RECALC_DUEDATE);
    assertThat(query.count()).isEqualTo(2);

    // assert details
    UserOperationLogEntry entry = query.property("duedate").singleResult();
    assertThat(entry.getJobId()).isEqualTo(job.getId());
    assertThat(entry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(entry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(entry.getProperty()).isEqualTo("duedate");
    assertTrue(DateUtils.truncatedEquals(duedate, new Date(Long.parseLong(entry.getOrgValue())), Calendar.SECOND));
    assertTrue(DateUtils.truncatedEquals(duedate, new Date(Long.parseLong(entry.getNewValue())), Calendar.SECOND));
    
    entry = query.property("creationDateBased").singleResult();
    assertThat(entry.getJobId()).isEqualTo(job.getId());
    assertThat(entry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(entry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(entry.getProperty()).isEqualTo("creationDateBased");
    assertNull(entry.getOrgValue());
    assertFalse(Boolean.parseBoolean(entry.getNewValue()));
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testDelete() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // when I delete a job
    managementService.deleteJob(job.getId());

    // then an op log entry is written
    UserOperationLogEntry userOperationLogEntry = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_DELETE)
            .singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertThat(userOperationLogEntry.getJobId()).isEqualTo(job.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);

    assertNull(userOperationLogEntry.getProperty());
    assertNull(userOperationLogEntry.getNewValue());
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(userOperationLogEntry.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testExecute() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    Job job = managementService.createJobQuery().singleResult();

    // when I execute a job manually
    managementService.executeJob(job.getId());

    // then an op log entry is written
    UserOperationLogEntry userOperationLogEntry = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_EXECUTE)
            .singleResult();
    assertNotNull(userOperationLogEntry);

    assertThat(userOperationLogEntry.getEntityType()).isEqualTo(EntityTypes.JOB);
    assertThat(userOperationLogEntry.getJobId()).isEqualTo(job.getId());

    assertThat(userOperationLogEntry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_EXECUTE);

    assertNull(userOperationLogEntry.getProperty());
    assertNull(userOperationLogEntry.getNewValue());
    assertNull(userOperationLogEntry.getOrgValue());

    assertThat(userOperationLogEntry.getUserId()).isEqualTo(USER_ID);

    assertThat(userOperationLogEntry.getJobDefinitionId()).isEqualTo(job.getJobDefinitionId());
    assertThat(userOperationLogEntry.getProcessInstanceId()).isEqualTo(job.getProcessInstanceId());
    assertThat(userOperationLogEntry.getProcessDefinitionId()).isEqualTo(job.getProcessDefinitionId());
    assertThat(userOperationLogEntry.getProcessDefinitionKey()).isEqualTo(job.getProcessDefinitionKey());
    assertThat(userOperationLogEntry.getDeploymentId()).isEqualTo(job.getDeploymentId());
    assertThat(userOperationLogEntry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
  }
  
  @Deployment(resources = {"org/operaton/bpm/engine/test/history/asyncTaskProcess.bpmn20.xml"})
  @Test
  public void testExecuteByJobExecutor() {
    // given a job
    runtimeService.startProcessInstanceByKey("asyncTaskProcess");
    assertThat(managementService.createJobQuery().count()).isEqualTo(1L);

    // when a job is executed by the job executor
    testRule.waitForJobExecutorToProcessAllJobs(TimeUnit.MILLISECONDS.convert(5L, TimeUnit.SECONDS));

    // then no op log entry is written
    assertThat(managementService.createJobQuery().count()).isEqualTo(0L);
    long logEntriesCount = historyService
            .createUserOperationLogQuery()
            .operationType(UserOperationLogEntry.OPERATION_TYPE_EXECUTE)
            .count();
    assertThat(logEntriesCount).isEqualTo(0L);
  }
}
