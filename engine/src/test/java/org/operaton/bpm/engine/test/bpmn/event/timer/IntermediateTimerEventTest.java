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
package org.operaton.bpm.engine.test.bpmn.event.timer;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.joda.time.LocalDateTime;
import org.junit.Test;


public class IntermediateTimerEventTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testCatchingTimerEvent() {

    // Set the clock fixed
    Date startTime = new Date();

    // After process start, there should be timer created
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample");
    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi.getId());
    assertThat(jobQuery.count()).isEqualTo(1);

    // After setting the clock to time '50minutes and 5 seconds', the second timer should fire
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ((50 * 60 * 1000) + 5000)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    assertThat(jobQuery.count()).isZero();
    testRule.assertProcessEnded(pi.getProcessInstanceId());
  }

  @Deployment
  @Test
  public void testExpression() {
    // Set the clock fixed
    HashMap<String, Object> variables1 = new HashMap<>();
    variables1.put("dueDate", new Date());

    HashMap<String, Object> variables2 = new HashMap<>();
    variables2.put("dueDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

    // After process start, there should be timer created
    ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables1);
    ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables2);

    assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isEqualTo(1);
    assertThat(managementService.createJobQuery().processInstanceId(pi2.getId()).count()).isEqualTo(1);

    // After setting the clock to one second in the future the timers should fire
    List<Job> jobs = managementService.createJobQuery().executable().list();
    assertThat(jobs).hasSize(2);
    for (Job job : jobs) {
      managementService.executeJob(job.getId());
    }

    assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isZero();
    assertThat(managementService.createJobQuery().processInstanceId(pi2.getId()).count()).isZero();

    testRule.assertProcessEnded(pi1.getProcessInstanceId());
    testRule.assertProcessEnded(pi2.getProcessInstanceId());
  }
  
  @Deployment
  @Test
  public void testExpressionRecalculateCurrentDateBased() {
    // Set the clock fixed
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("duration", "PT1H");

    // After process start, there should be timer created
    ProcessInstanceWithVariables pi1 = (ProcessInstanceWithVariables) runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables);
    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi1.getId());
    assertThat(jobQuery.count()).isEqualTo(1);
    Job job = jobQuery.singleResult();
    Date firstDate = job.getDuedate();

    // After variable change and recalculation, there should still be one timer only, with a changed due date
    moveByMinutes(1);
    Date currentTime = ClockUtil.getCurrentTime();
    runtimeService.setVariable(pi1.getProcessInstanceId(), "duration", "PT15M");
    processEngine.getManagementService().recalculateJobDuedate(job.getId(), false);

    assertThat(jobQuery.count()).isEqualTo(1);
    job = jobQuery.singleResult();
    assertThat(job.getDuedate()).isNotEqualTo(firstDate);
    assertThat(firstDate.after(job.getDuedate())).isTrue();
    Date expectedDate = LocalDateTime.fromDateFields(currentTime).plusMinutes(15).toDate();
    assertThat(job.getDuedate()).isCloseTo(expectedDate, 1000l);
    
    // After waiting for sixteen minutes the timer should fire
    ClockUtil.setCurrentTime(new Date(firstDate.getTime() + TimeUnit.MINUTES.toMillis(16L)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isZero();
    testRule.assertProcessEnded(pi1.getProcessInstanceId());
  }
  
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testExpressionRecalculateCurrentDateBased.bpmn20.xml")
  @Test
  public void testExpressionRecalculateCreationDateBased() {
    // Set the clock fixed
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("duration", "PT1H");

    // After process start, there should be timer created
    ProcessInstanceWithVariables pi1 = (ProcessInstanceWithVariables) runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables);
    JobQuery jobQuery = managementService.createJobQuery().processInstanceId(pi1.getId());
    assertThat(jobQuery.count()).isEqualTo(1);
    Job job = jobQuery.singleResult();
    Date firstDate = job.getDuedate();

    // After variable change and recalculation, there should still be one timer only, with a changed due date
    moveByMinutes(65);// move past first due date
    runtimeService.setVariable(pi1.getProcessInstanceId(), "duration", "PT15M");
    processEngine.getManagementService().recalculateJobDuedate(job.getId(), true);

    assertThat(jobQuery.count()).isEqualTo(1);
    job = jobQuery.singleResult();
    assertThat(job.getDuedate()).isNotEqualTo(firstDate);
    assertThat(firstDate.after(job.getDuedate())).isTrue();
    Date expectedDate = LocalDateTime.fromDateFields(job.getCreateTime()).plusMinutes(15).toDate();
    assertThat(job.getDuedate()).isEqualTo(expectedDate);
    
    // After waiting for sixteen minutes the timer should fire
    ClockUtil.setCurrentTime(new Date(firstDate.getTime() + TimeUnit.MINUTES.toMillis(16L)));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    assertThat(managementService.createJobQuery().processInstanceId(pi1.getId()).count()).isZero();
    testRule.assertProcessEnded(pi1.getProcessInstanceId());
  }

  @Deployment
  @Test
  public void testTimeCycle() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    JobQuery query = managementService.createJobQuery();
    assertThat(query.count()).isEqualTo(1);

    String jobId = query.singleResult().getId();
    managementService.executeJob(jobId);

    assertThat(query.count()).isZero();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    testRule.assertProcessEnded(processInstanceId);
  }
  
  @Deployment
  @Test
  public void testRecalculateTimeCycleExpressionCurrentDateBased() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("cycle", "R/PT15M");
    String processInstanceId = runtimeService.startProcessInstanceByKey("process", variables).getId();

    JobQuery query = managementService.createJobQuery();
    assertThat(query.count()).isEqualTo(1);
    Job job = query.singleResult();
    Date oldDuedate = job.getDuedate();
    String jobId = job.getId();

    // when
    runtimeService.setVariable(processInstanceId, "cycle", "R/PT10M");
    managementService.recalculateJobDuedate(jobId, false);

    // then
    assertThat(query.count()).isEqualTo(1);
    assertThat(oldDuedate.after(query.singleResult().getDuedate())).isTrue();

    managementService.executeJob(jobId);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    testRule.assertProcessEnded(processInstanceId);
  }
  
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testRecalculateTimeCycleExpressionCurrentDateBased.bpmn20.xml")
  @Test
  public void testRecalculateTimeCycleExpressionCreationDateBased() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("cycle", "R/PT15M");
    String processInstanceId = runtimeService.startProcessInstanceByKey("process", variables).getId();

    JobQuery query = managementService.createJobQuery();
    assertThat(query.count()).isEqualTo(1);
    Job job = query.singleResult();
    Date oldDuedate = job.getDuedate();
    String jobId = job.getId();

    // when
    runtimeService.setVariable(processInstanceId, "cycle", "R/PT10M");
    managementService.recalculateJobDuedate(jobId, true);

    // then
    assertThat(query.count()).isEqualTo(1);
    Date newDuedate = query.singleResult().getDuedate();
    assertThat(oldDuedate.after(newDuedate)).isTrue();
    Date expectedDate = LocalDateTime.fromDateFields(job.getCreateTime()).plusMinutes(10).toDate();
    assertThat(newDuedate).isEqualTo(expectedDate);

    managementService.executeJob(jobId);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    testRule.assertProcessEnded(processInstanceId);
  }
  
  private void moveByMinutes(int minutes) {
    ClockUtil.setCurrentTime(new Date(ClockUtil.getCurrentTime().getTime() + ((minutes * 60 * 1000) + 5000)));
  }

}