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
package org.operaton.bpm.model.bpmn.builder;

import org.operaton.bpm.model.bpmn.BpmnModelException;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.operaton.bpm.model.bpmn.instance.EndEvent;
import org.operaton.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.operaton.bpm.model.bpmn.instance.SubProcess;
import org.operaton.bpm.model.bpmn.instance.Transaction;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractBpmnModelElementBuilder<B extends AbstractBpmnModelElementBuilder<B, E>, E extends BpmnModelElementInstance> {

  protected final BpmnModelInstance modelInstance;
  protected final E element;
  protected final B myself;

  @SuppressWarnings("unchecked")
  protected AbstractBpmnModelElementBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
    this.modelInstance = modelInstance;
    myself = (B) selfType.cast(this);
    this.element = element;
  }

  /**
   * Finishes the process building.
   *
   * @return the model instance with the build process
   */
  public BpmnModelInstance done() {
    return modelInstance;
  }

  /**
   * Finishes the building of an embedded sub-process.
   *
   * @return the parent sub-process builder
   * @throws BpmnModelException if no parent sub-process can be found
   */
  public SubProcessBuilder subProcessDone() {
    BpmnModelElementInstance lastSubProcess = element.getScope();
    if (lastSubProcess instanceof SubProcess subProcess) {
      return subProcess.builder();
    }
    else {
      throw new BpmnModelException("Unable to find a parent subProcess.");
    }
  }

  public TransactionBuilder transactionDone() {
    BpmnModelElementInstance lastTransaction = element.getScope();
    if (lastTransaction instanceof Transaction transaction) {
      return new TransactionBuilder(modelInstance, transaction);
    }
    else {
      throw new BpmnModelException("Unable to find a parent transaction.");
    }
  }

  public AbstractThrowEventBuilder throwEventDefinitionDone() {
    ModelElementInstance lastEvent = element.getDomElement().getParentElement().getModelElementInstance();
    if (lastEvent instanceof IntermediateThrowEvent intermediateThrowEvent) {
      return new IntermediateThrowEventBuilder(modelInstance, intermediateThrowEvent);
    } else if (lastEvent instanceof EndEvent endEvent) {
      return new EndEventBuilder(modelInstance, endEvent);
    }
    else {
      throw new BpmnModelException("Unable to find a parent event.");
    }
  }

  public E getElement() {
    return element;
  }
}
