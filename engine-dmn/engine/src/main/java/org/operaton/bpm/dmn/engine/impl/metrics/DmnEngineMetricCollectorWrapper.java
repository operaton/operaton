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
package org.operaton.bpm.dmn.engine.impl.metrics;

import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionEvaluationListener;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionLogicEvaluationEvent;
import org.operaton.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.operaton.bpm.dmn.engine.spi.DmnEngineMetricCollector;

public class DmnEngineMetricCollectorWrapper implements DmnEngineMetricCollector, DmnDecisionEvaluationListener {

  protected final DmnEngineMetricCollector collector;

  public DmnEngineMetricCollectorWrapper(DmnEngineMetricCollector collector) {
    this.collector = collector;
  }

    /**
   * Notify method for handling DmnDecisionTableEvaluationEvent.
   */
  @Override
  public void notify(DmnDecisionTableEvaluationEvent evaluationEvent) {
    // the wrapper listen for decision evaluation events
  }

    /**
   * Notifies the collector with the decision result and required decision results from the evaluation event.
   */
  @Override
  public void notify(DmnDecisionEvaluationEvent evaluationEvent) {
    notifyCollector(evaluationEvent.getDecisionResult());

    for (DmnDecisionLogicEvaluationEvent event : evaluationEvent.getRequiredDecisionResults()) {
      notifyCollector(event);
    }
  }

    /**
   * Notifies the collector with the provided evaluation event if it is an instance of DmnDecisionTableEvaluationEvent.
   * This method is implemented to work specifically with decision table evaluation events.
   * 
   * @param evaluationEvent the evaluation event to be notified to the collector
   */
  protected void notifyCollector(DmnDecisionLogicEvaluationEvent evaluationEvent) {
    if (evaluationEvent instanceof DmnDecisionTableEvaluationEvent) {
      collector.notify((DmnDecisionTableEvaluationEvent) evaluationEvent);
    }
    // ignore other evaluation events since the collector is implemented as decision table evaluation listener
  }

    /**
   * Returns the number of executed decision instances.
   *
   * @return the number of executed decision instances
   */
  @Override
  public long getExecutedDecisionInstances() {
    return collector.getExecutedDecisionInstances();
  }

    /**
   * Returns the number of executed decision elements.
   *
   * @return the number of executed decision elements
   */
  @Override
  public long getExecutedDecisionElements() {
    return collector.getExecutedDecisionElements();
  }

    /**
   * Clears all executed decision instances.
   * 
   * @return the number of cleared executed decision instances
   */
  @Override
  public long clearExecutedDecisionInstances() {
    return collector.clearExecutedDecisionInstances();
  }

    /**
   * Clears the executed decision elements
   *
   * @return the number of cleared executed decision elements
   */
  @Override
  public long clearExecutedDecisionElements() {
    return collector.clearExecutedDecisionElements();
  }

}
