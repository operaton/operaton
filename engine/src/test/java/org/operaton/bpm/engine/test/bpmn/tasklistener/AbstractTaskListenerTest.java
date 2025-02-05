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
package org.operaton.bpm.engine.test.bpmn.tasklistener;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.RecorderTaskListener;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.UserTaskBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;

public abstract class AbstractTaskListenerTest {

  public ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  public   ProcessEngineTestRule     testRule   = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RuntimeService                 runtimeService;
  protected TaskService                    taskService;
  protected ManagementService              managementService;
  protected HistoryService                 historyService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @Before
  public void setUp() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    managementService = engineRule.getManagementService();
    historyService = engineRule.getHistoryService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @Before
  public void resetListeners() {
    RecorderTaskListener.clear();
  }

  protected void createAndDeployModelWithTaskEventsRecorderOnUserTask(String... eventTypes) {
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(eventTypes, null, null, null);
    testRule.deploy(model);
  }

  protected void createAndDeployModelWithTaskEventsRecorderOnUserTaskWithAssignee(String assignee,
                                                                                  String... eventTypes) {
    BpmnModelInstance model = createModelWithTaskEventsRecorderOnAssignedUserTask(eventTypes,
                                                                                  assignee,
                                                                                  null,
                                                                                  null);
    testRule.deploy(model);
  }

  protected BpmnModelInstance createModelWithTaskEventsRecorderOnAssignedUserTask(String[] eventTypes, String assignee, String customListenerEventType, Class<? extends TaskListener> taskListenerClass) {
    UserTaskBuilder userTaskModelBuilder = Bpmn.createExecutableProcess("process")
                                               .startEvent()
                                               .userTask("task");

    if (assignee != null) {
      userTaskModelBuilder.operatonAssignee("kermit");
    }

    for (String eventType : eventTypes) {
      userTaskModelBuilder.operatonTaskListenerClass(eventType, RecorderTaskListener.class);
    }

    if (taskListenerClass != null) {
      userTaskModelBuilder.operatonTaskListenerClass(customListenerEventType, taskListenerClass);
    }

    return userTaskModelBuilder
        .endEvent()
        .done();
  }
}