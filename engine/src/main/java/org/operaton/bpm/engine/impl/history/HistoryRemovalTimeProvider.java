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
package org.operaton.bpm.engine.impl.history;

import java.util.Date;

import org.operaton.bpm.engine.impl.batch.history.HistoricBatchEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricDecisionInstanceEntity;
import org.operaton.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;

/**
 * The provider is either invoked on root process instance start or end
 * based on the selected history removal time strategy.
 *
 * @author Tassilo Weidner
 */
public interface HistoryRemovalTimeProvider {

  /**
   * Calculates the removal time of historic entities related to processes.
   *
   * <p>
   * START: the removal time is set for each historic entity separately on occurrence (creation).
   *        {@link HistoricProcessInstanceEventEntity#getEndTime()} is {@code null}
   * </p>
   *
   * <p>
   * END:   the removal time is updated simultaneously for all historic entities which belong to
   *        the root process instance when it ends.
   *        {@link HistoricProcessInstanceEventEntity#getEndTime()} is not {@code null}
   * </p>
   *
   * @param historicRootProcessInstance which is either in state running or ended
   * @param processDefinition of the historic root process instance
   * @return the removal time for historic process instances
   */
  Date calculateRemovalTime(HistoricProcessInstanceEventEntity historicRootProcessInstance, ProcessDefinition processDefinition);

  /**
   * Calculates the removal time of historic entities related to decisions.
   *
   * @param historicRootDecisionInstance
   * @param decisionDefinition of the historic root decision instance
   * @return the removal time for historic decision instances
   */
  Date calculateRemovalTime(HistoricDecisionInstanceEntity historicRootDecisionInstance, DecisionDefinition decisionDefinition);

  /**
   * Calculates the removal time of historic batches.
   *
   * <p>
   * START: the removal time is set for the historic batch entity on start.
   *        {@link HistoricBatchEntity#getEndTime()} is {@code null}
   * </p>
   *
   * <p>
   * END:   the removal time is set for the historic batch entity on end.
   *        {@link HistoricBatchEntity#getEndTime()} is not {@code null}
   * </p>
   *
   * @param historicBatch which is either in state running or ended
   * @return the removal time of historic entities
   */
  Date calculateRemovalTime(HistoricBatchEntity historicBatch);

}
