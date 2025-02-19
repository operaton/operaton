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
package org.operaton.bpm.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Falko Menge
 */
public class DelegateTaskTest {

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  private static final String FOLLOW_UP_DATE_STRING = "2019-01-01T01:00:00";

  private static final Date FOLLOW_UP_DATE;

  static {
    try {
      FOLLOW_UP_DATE = DATE_FORMAT.parse(FOLLOW_UP_DATE_STRING);
    } catch (ParseException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private RuntimeService runtimeService;
  private TaskService taskService;

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
  }

  /**
   * @see <a href="http://jira.codehaus.org/browse/ACT-380">http://jira.codehaus.org/browse/ACT-380</a>
   */
  @Test
  @Deployment
  public void testGetCandidates() {
    runtimeService.startProcessInstanceByKey("DelegateTaskTest.testGetCandidates");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    @SuppressWarnings("unchecked")
    Set<String> candidateUsers = (Set<String>) taskService.getVariable(task.getId(), DelegateTaskTestTaskListener.VARNAME_CANDIDATE_USERS);
    assertThat(candidateUsers).containsExactlyInAnyOrder("kermit", "gonzo");

    @SuppressWarnings("unchecked")
    Set<String> candidateGroups = (Set<String>) taskService.getVariable(task.getId(), DelegateTaskTestTaskListener.VARNAME_CANDIDATE_GROUPS);
    assertThat(candidateGroups).containsExactlyInAnyOrder("management", "accountancy");
  }

  @Test
  public void testGetFollowUpDate() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .userTask()
        .operatonFollowUpDate(FOLLOW_UP_DATE_STRING)
        .operatonTaskListenerClass("create", GetFollowUpDateListener.class)
      .endEvent()
      .done();

    testRule.deploy(modelInstance);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // then
    String processInstanceId = processInstance.getId();
    Date followUpDate = (Date) runtimeService.getVariable(processInstanceId, "followUp");

    assertThat(followUpDate).isEqualTo(FOLLOW_UP_DATE);
  }

  @Test
  public void testSetFollowUpDate() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .userTask()
        .operatonTaskListenerClass("create", SetFollowUpDateListener.class)
      .endEvent()
      .done();

    testRule.deploy(modelInstance);

    // when
    runtimeService.startProcessInstanceByKey("process");

    // then
    Task task = taskService.createTaskQuery().singleResult();
    Date followUpDate = task.getFollowUpDate();

    assertThat(followUpDate).isEqualTo(FOLLOW_UP_DATE);
  }

  @Test
  public void testLastUpdated() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask()
          .operatonTaskListenerClass(TaskListener.EVENTNAME_UPDATE, LastUpdateListener.class)
        .endEvent()
        .done();

    testRule.deploy(modelInstance);
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getProcessInstanceId();
    Date beforeUpdate = new Date(ClockUtil.getCurrentTime().getTime() - 1000L);
    Task task = taskService.createTaskQuery().singleResult();
    task.setPriority(0);

    // when
    taskService.saveTask(task);

    // then

    Date lastUpdated = (Date) runtimeService.getVariable(processInstanceId, "lastUpdated");
    assertThat(lastUpdated)
      .isNotNull()
      .isAfter(beforeUpdate);
  }

  public static class GetFollowUpDateListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
      Date followUpDate = delegateTask.getFollowUpDate();
      assertThat(followUpDate).isNotNull();

      delegateTask.setVariable("followUp", followUpDate);
    }

  }

  public static class SetFollowUpDateListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
      delegateTask.setFollowUpDate(FOLLOW_UP_DATE);
    }

  }

  public static class LastUpdateListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
      Date lastUpdated = delegateTask.getLastUpdated();

      delegateTask.setVariable("lastUpdated", lastUpdated);
    }

  }

}
