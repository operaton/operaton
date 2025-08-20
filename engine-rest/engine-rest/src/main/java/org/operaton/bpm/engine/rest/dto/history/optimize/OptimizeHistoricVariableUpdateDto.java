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
package org.operaton.bpm.engine.rest.dto.history.optimize;

import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.rest.dto.history.HistoricVariableUpdateDto;

public class OptimizeHistoricVariableUpdateDto extends HistoricVariableUpdateDto {

  protected long sequenceCounter;

  public long getSequenceCounter() {
    return sequenceCounter;
  }

  public void setSequenceCounter(long sequenceCounter) {
    this.sequenceCounter = sequenceCounter;
  }

  public static OptimizeHistoricVariableUpdateDto fromHistoricVariableUpdate(HistoricVariableUpdate historicVariableUpdate) {
    OptimizeHistoricVariableUpdateDto dto = new OptimizeHistoricVariableUpdateDto();
    fromHistoricVariableUpdate(dto, historicVariableUpdate);
    fromHistoricDetail(historicVariableUpdate, dto);
    if (historicVariableUpdate instanceof HistoryEvent historyEvent) {
      dto.setSequenceCounter(historyEvent.getSequenceCounter());
    }
    return dto;
  }

}
