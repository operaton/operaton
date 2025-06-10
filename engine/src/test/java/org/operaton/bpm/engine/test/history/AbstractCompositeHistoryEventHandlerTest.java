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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
public abstract class AbstractCompositeHistoryEventHandlerTest {

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;

  HistoryEventHandler originalHistoryEventHandler;

  /**
   * The counter used to check the amount of triggered events.
   */
  int countCustomHistoryEventHandler;

  /**
   * Perform common setup.
   */
  @BeforeEach
  public void setUp() {
    // save current history event handler
    originalHistoryEventHandler = processEngineConfiguration.getHistoryEventHandler();
    // clear the event counter
    countCustomHistoryEventHandler = 0;
  }

  @AfterEach
  public void tearDown() {
    // reset original history event handler
    processEngineConfiguration.setHistoryEventHandler(originalHistoryEventHandler);
  }

  /**
   * The helper method to execute the test task.
   */
  protected void startProcessAndCompleteUserTask() {
    runtimeService.startProcessInstanceByKey("HistoryLevelTest");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
  }

  /**
   * A {@link HistoryEventHandler} implementation to count the history events.
   */
  protected class CustomDbHistoryEventHandler implements HistoryEventHandler {

    @Override
    public void handleEvent(HistoryEvent historyEvent) {
      // take into account only variable related events
      if (historyEvent instanceof HistoricVariableUpdateEventEntity) {
        // emulate the history event processing and persisting
        countCustomHistoryEventHandler++;
      }
    }

    @Override
    public void handleEvents(List<HistoryEvent> historyEvents) {
      for (HistoryEvent historyEvent : historyEvents) {
        handleEvent(historyEvent);
      }
    }

  }

}
