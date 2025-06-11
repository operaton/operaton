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
package org.operaton.bpm.engine.test.api.history.removaltime.cleanup;

import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEventType;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.runtime.ProcessInstance;

/**
 * @author Tassilo Weidner
 */
public class CustomHistoryLevelRemovalTime implements HistoryLevel {

  private HistoryEventTypes[] eventTypes;

  @Override
  public int getId() {
    return 47;
  }

  @Override
  public String getName() {
    return "customHistoryLevel";
  }

  @Override
  public boolean isHistoryEventProduced(HistoryEventType eventType, Object entity) {
    if (eventTypes != null) {
      for (HistoryEventType historyEventType : this.eventTypes) {
        if (eventType.equals(historyEventType)) {
          return true;
        }
      }
    }

    return eventType.equals(HistoryEventTypes.PROCESS_INSTANCE_END) || isRootProcessInstance(entity);
  }

  public void setEventTypes(HistoryEventTypes... eventTypes) {
    this.eventTypes = eventTypes;
  }

  protected boolean isRootProcessInstance(Object entity) {
    if (entity instanceof ProcessInstance processInstance) {
      return processInstance.getId().equals(processInstance.getRootProcessInstanceId());
    }

    return false;
  }
}
