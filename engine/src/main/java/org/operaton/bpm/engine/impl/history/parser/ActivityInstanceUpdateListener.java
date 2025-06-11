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
package org.operaton.bpm.engine.impl.history.parser;

import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.HistoryEventProducer;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author Daniel Meyer
 *
 */
public class ActivityInstanceUpdateListener extends HistoryTaskListener {

  public ActivityInstanceUpdateListener(HistoryEventProducer historyEventProducer) {
    super(historyEventProducer);
  }

  @Override
  protected HistoryEvent createHistoryEvent(DelegateTask task, ExecutionEntity execution) {
    ensureHistoryLevelInitialized();
    if(historyLevel.isHistoryEventProduced(HistoryEventTypes.ACTIVITY_INSTANCE_UPDATE, execution)) {
      return eventProducer.createActivityInstanceUpdateEvt(execution, task);
    } else {
      return null;
    }
  }


}
