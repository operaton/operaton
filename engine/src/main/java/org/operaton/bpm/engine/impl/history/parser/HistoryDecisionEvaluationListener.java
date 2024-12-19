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
package org.operaton.bpm.engine.impl.history.parser;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationListener;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.context.CoreExecutionContext;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.history.event.HistoryEventTypes;
import org.operaton.bpm.engine.impl.history.producer.DmnHistoryEventProducer;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.repository.DecisionDefinition;

public class HistoryDecisionEvaluationListener implements DmnDecisionEvaluationListener {

  protected DmnHistoryEventProducer eventProducer;
  protected HistoryLevel historyLevel;

  public HistoryDecisionEvaluationListener(DmnHistoryEventProducer historyEventProducer) {
    this.eventProducer = historyEventProducer;
  }

  @Override
  public void notify(DmnDecisionEvaluationEvent evaluationEvent) {
   HistoryEvent historyEvent = createHistoryEvent(evaluationEvent);

    if(historyEvent != null) {
      Context.getProcessEngineConfiguration()
        .getHistoryEventHandler()
        .handleEvent(historyEvent);
    }
  }

  protected HistoryEvent createHistoryEvent(DmnDecisionEvaluationEvent evaluationEvent) {
    if (historyLevel == null) {
      historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
    }
    DmnDecision decisionTable = evaluationEvent.getDecisionResult().getDecision();
    if(isDeployedDecisionTable(decisionTable) && historyLevel.isHistoryEventProduced(HistoryEventTypes.DMN_DECISION_EVALUATE, decisionTable)) {

      CoreExecutionContext<? extends CoreExecution> executionContext = Context.getCoreExecutionContext();
      if (executionContext != null) {
        CoreExecution coreExecution = executionContext.getExecution();

        if (coreExecution instanceof ExecutionEntity execution) {
          return eventProducer.createDecisionEvaluatedEvt(execution, evaluationEvent);
        }
        else if (coreExecution instanceof CaseExecutionEntity caseExecution) {
          return eventProducer.createDecisionEvaluatedEvt(caseExecution, evaluationEvent);
        }

      }

      return eventProducer.createDecisionEvaluatedEvt(evaluationEvent);

    } else {
      return null;
    }
  }

  protected boolean isDeployedDecisionTable(DmnDecision decision) {
    if(decision instanceof DecisionDefinition decisionDefinition) {
      return decisionDefinition.getId() != null;
    } else {
      return false;
    }
  }

}
