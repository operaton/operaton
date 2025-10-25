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
package org.operaton.bpm.engine.test.bpmn.executionlistener;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.producer.DefaultHistoryEventProducer;

public class ThrowingHistoryEventProducer extends DefaultHistoryEventProducer {

  public static final String EXCEPTION_MESSAGE = "Intended exception from history producer";
  public static final String ERROR_CODE = "508";

  protected String activityName;
  protected boolean failsWithException;

  public ThrowingHistoryEventProducer failsWithException() {
    this.failsWithException = true;
    return this;
  }

  public ThrowingHistoryEventProducer failsAtActivity(String activityName) {
    this.activityName = activityName;
    return this;
  }

  public void reset() {
    this.activityName = null;
    this.failsWithException = false;
  }

  @Override
  public HistoryEvent createActivityInstanceEndEvt(DelegateExecution execution) {
    String currentActivityName = execution.getCurrentActivityName();
    if (currentActivityName.equals(activityName)) {
      if (failsWithException) {
        throw new RuntimeException(EXCEPTION_MESSAGE);
      }
      throw new BpmnError(ERROR_CODE);
    }
    return super.createActivityInstanceEndEvt(execution);
  }

}
