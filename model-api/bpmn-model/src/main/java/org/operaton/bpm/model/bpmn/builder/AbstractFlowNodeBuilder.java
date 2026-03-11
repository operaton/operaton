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

import org.operaton.bpm.model.bpmn.AssociationDirection;
import org.operaton.bpm.model.bpmn.BpmnModelException;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.*;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonExecutionListener;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFailedJobRetryTimeCycle;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractFlowNodeBuilder<B extends AbstractFlowNodeBuilder<B, E>, E extends FlowNode> extends AbstractFlowElementBuilder<B, E> {

  private SequenceFlowBuilder currentSequenceFlowBuilder;

  protected boolean compensationStarted;
  protected BoundaryEvent compensateBoundaryEvent;

  protected AbstractFlowNodeBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  private SequenceFlowBuilder getCurrentSequenceFlowBuilder() {
    if (currentSequenceFlowBuilder == null) {
      SequenceFlow sequenceFlow = createSibling(SequenceFlow.class);
      currentSequenceFlowBuilder = sequenceFlow.builder();
    }
    return currentSequenceFlowBuilder;
  }

  public B condition(String name, String condition) {
    if (name != null) {
      getCurrentSequenceFlowBuilder().name(name);
    }
    ConditionExpression conditionExpression = createInstance(ConditionExpression.class);
    conditionExpression.setTextContent(condition);
    getCurrentSequenceFlowBuilder().condition(conditionExpression);
    return myself;
  }

  protected void connectTarget(FlowNode target) {
    // check if compensation was started
    if (isBoundaryEventWithStartedCompensation()) {
        // the target activity should be marked for compensation
        if (target instanceof Activity activity) {
          activity.setForCompensation(true);
        }

        // connect the target via association instead of sequence flow
        connectTargetWithAssociation(target);
    }
    else if (isCompensationHandler()) {
      // cannot connect to a compensation handler
      throw new BpmnModelException("Only single compensation handler allowed. Call compensationDone() to continue main flow.");
    }
    else {
      // connect as sequence flow by default
      connectTargetWithSequenceFlow(target);
    }
  }

  protected void connectTargetWithSequenceFlow(FlowNode target) {
    getCurrentSequenceFlowBuilder().from(element).to(target);

    SequenceFlow sequenceFlow = getCurrentSequenceFlowBuilder().getElement();
    createEdge(sequenceFlow);
    currentSequenceFlowBuilder = null;
  }

  protected void connectTargetWithAssociation(FlowNode target) {
    Association association = modelInstance.newInstance(Association.class);
    association.setTarget(target);
    association.setSource(element);
    association.setAssociationDirection(AssociationDirection.One);
    element.getParentElement().addChildElement(association);

    createEdge(association);
  }

  public AbstractFlowNodeBuilder compensationDone(){
    if (compensateBoundaryEvent != null) {
      return compensateBoundaryEvent.getAttachedTo().builder();
    }
    else {
      throw new BpmnModelException("No compensation in progress. Call compensationStart() first.");
    }
  }

  public B sequenceFlowId(String sequenceFlowId) {
    getCurrentSequenceFlowBuilder().id(sequenceFlowId);
    return myself;
  }

  private <T extends FlowNode> T createTarget(Class<T> typeClass) {
    return createTarget(typeClass, null);
  }

  protected <T extends FlowNode> T createTarget(Class<T> typeClass, String identifier) {
    T target = createSibling(typeClass, identifier);

    BpmnShape targetBpmnShape = createBpmnShape(target);
    setCoordinates(targetBpmnShape);
    connectTarget(target);
    resizeSubProcess(targetBpmnShape);
    return target;
  }

  protected <T extends AbstractFlowNodeBuilder, F extends FlowNode> T createTargetBuilder(Class<F> typeClass) {
    return createTargetBuilder(typeClass, null);
  }

  @SuppressWarnings("unchecked")
  protected <T extends AbstractFlowNodeBuilder, F extends FlowNode> T createTargetBuilder(Class<F> typeClass, String id) {
    AbstractFlowNodeBuilder builder = createTarget(typeClass, id).builder();

    if (compensationStarted) {
      // pass on current boundary event to return after compensationDone call
      builder.compensateBoundaryEvent = compensateBoundaryEvent;
    }

    return (T) builder;

  }

  public ServiceTaskBuilder serviceTask() {
    return createTargetBuilder(ServiceTask.class);
  }

  public ServiceTaskBuilder serviceTask(String id) {
    return createTargetBuilder(ServiceTask.class, id);
  }

  public SendTaskBuilder sendTask() {
    return createTargetBuilder(SendTask.class);
  }

  public SendTaskBuilder sendTask(String id) {
    return createTargetBuilder(SendTask.class, id);
  }

  public UserTaskBuilder userTask() {
    return createTargetBuilder(UserTask.class);
  }

  public UserTaskBuilder userTask(String id) {
    return createTargetBuilder(UserTask.class, id);
  }

  public BusinessRuleTaskBuilder businessRuleTask() {
    return createTargetBuilder(BusinessRuleTask.class);
  }

  public BusinessRuleTaskBuilder businessRuleTask(String id) {
    return createTargetBuilder(BusinessRuleTask.class, id);
  }

  public ScriptTaskBuilder scriptTask() {
    return createTargetBuilder(ScriptTask.class);
  }

  public ScriptTaskBuilder scriptTask(String id) {
    return createTargetBuilder(ScriptTask.class, id);
  }

  public ReceiveTaskBuilder receiveTask() {
    return createTargetBuilder(ReceiveTask.class);
  }

  public ReceiveTaskBuilder receiveTask(String id) {
    return createTargetBuilder(ReceiveTask.class, id);
  }

  public ManualTaskBuilder manualTask() {
    return createTargetBuilder(ManualTask.class);
  }

  public ManualTaskBuilder manualTask(String id) {
    return createTargetBuilder(ManualTask.class, id);
  }

  public EndEventBuilder endEvent() {
    return createTarget(EndEvent.class).builder();
  }

  public EndEventBuilder endEvent(String id) {
    return createTarget(EndEvent.class, id).builder();
  }

  public ParallelGatewayBuilder parallelGateway() {
    return createTarget(ParallelGateway.class).builder();
  }

  public ParallelGatewayBuilder parallelGateway(String id) {
    return createTarget(ParallelGateway.class, id).builder();
  }

  public ExclusiveGatewayBuilder exclusiveGateway() {
    return createTarget(ExclusiveGateway.class).builder();
  }

  public InclusiveGatewayBuilder inclusiveGateway() {
    return createTarget(InclusiveGateway.class).builder();
  }

  public EventBasedGatewayBuilder eventBasedGateway() {
    return createTarget(EventBasedGateway.class).builder();
  }

  public ExclusiveGatewayBuilder exclusiveGateway(String id) {
    return createTarget(ExclusiveGateway.class, id).builder();
  }

  public InclusiveGatewayBuilder inclusiveGateway(String id) {
    return createTarget(InclusiveGateway.class, id).builder();
  }

  public IntermediateCatchEventBuilder intermediateCatchEvent() {
    return createTarget(IntermediateCatchEvent.class).builder();
  }

  public IntermediateCatchEventBuilder intermediateCatchEvent(String id) {
    return createTarget(IntermediateCatchEvent.class, id).builder();
  }

  public IntermediateThrowEventBuilder intermediateThrowEvent() {
    return createTarget(IntermediateThrowEvent.class).builder();
  }

  public IntermediateThrowEventBuilder intermediateThrowEvent(String id) {
    return createTarget(IntermediateThrowEvent.class, id).builder();
  }

  public CallActivityBuilder callActivity() {
    return createTarget(CallActivity.class).builder();
  }

  public CallActivityBuilder callActivity(String id) {
    return createTarget(CallActivity.class, id).builder();
  }

  public SubProcessBuilder subProcess() {
    return createTarget(SubProcess.class).builder();
  }

  public SubProcessBuilder subProcess(String id) {
    return createTarget(SubProcess.class, id).builder();
  }

  public TransactionBuilder transaction() {
    Transaction transaction = createTarget(Transaction.class);
    return new TransactionBuilder(modelInstance, transaction);
  }

  public TransactionBuilder transaction(String id) {
    Transaction transaction = createTarget(Transaction.class, id);
    return new TransactionBuilder(modelInstance, transaction);
  }

  public Gateway findLastGateway() {
    FlowNode lastGateway = element;
    while (true) {
      try {
        lastGateway = lastGateway.getPreviousNodes().singleResult();
        if (lastGateway instanceof Gateway gateway) {
          return gateway;
        }
      } catch (BpmnModelException e) {
        throw new BpmnModelException("Unable to determine an unique previous gateway of " + lastGateway.getId(), e);
      }
    }
  }

  @SuppressWarnings("rawtypes")
  public AbstractGatewayBuilder moveToLastGateway() {
    return findLastGateway().builder();
  }

  @SuppressWarnings("rawtypes")
  public AbstractFlowNodeBuilder moveToNode(String identifier) {
    ModelElementInstance instance = modelInstance.getModelElementById(identifier);
    if (instance instanceof FlowNode flowNode) {
      return flowNode.builder();
    } else {
      throw new BpmnModelException("Flow node not found for id " + identifier);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public <T extends AbstractActivityBuilder> T moveToActivity(String identifier) {
    ModelElementInstance instance = modelInstance.getModelElementById(identifier);
    if (instance instanceof Activity activity) {
      return (T) activity.builder();
    } else {
      throw new BpmnModelException("Activity not found for id " + identifier);
    }
  }

  @SuppressWarnings("rawtypes")
  public AbstractFlowNodeBuilder connectTo(String identifier) {
    ModelElementInstance target = modelInstance.getModelElementById(identifier);
    if (target == null) {
      throw new BpmnModelException("Unable to connect %s to element %s cause it not exists.".formatted(element.getId(), identifier));
    } else if (!(target instanceof FlowNode targetNode)) {
      throw new BpmnModelException("Unable to connect %s to element %s cause its not a flow node.".formatted(element.getId(), identifier));
    } else {
      connectTarget(targetNode);
      return targetNode.builder();
    }
  }

  /**
   * Sets the Operaton AsyncBefore attribute for the build flow node.
   *
   * @param asyncBefore
   *          boolean value to set
   * @return the builder object
   */
  public B operatonAsyncBefore(boolean asyncBefore) {
    element.setOperatonAsyncBefore(asyncBefore);
    return myself;
  }

  /**
   * Sets the Operaton asyncBefore attribute to true.
   *
   * @return the builder object
   */
  public B operatonAsyncBefore() {
    element.setOperatonAsyncBefore(true);
    return myself;
  }

  /**
   * Sets the Operaton asyncAfter attribute for the build flow node.
   *
   * @param asyncAfter
   *          boolean value to set
   * @return the builder object
   */
  public B operatonAsyncAfter(boolean asyncAfter) {
    element.setOperatonAsyncAfter(asyncAfter);
    return myself;
  }

  /**
   * Sets the Operaton asyncAfter attribute to true.
   *
   * @return the builder object
   */
  public B operatonAsyncAfter() {
    element.setOperatonAsyncAfter(true);
    return myself;
  }

  /**
   * Sets the Operaton exclusive attribute to true.
   *
   * @return the builder object
   */
  public B notOperatonExclusive() {
    element.setOperatonExclusive(false);
    return myself;
  }

  /**
   * Sets the operaton exclusive attribute for the build flow node.
   *
   * @param exclusive
   *          boolean value to set
   * @return the builder object
   */
  public B operatonExclusive(boolean exclusive) {
    element.setOperatonExclusive(exclusive);
    return myself;
  }

  public B operatonJobPriority(String jobPriority) {
    element.setOperatonJobPriority(jobPriority);
    return myself;
  }

  /**
   * Sets the operaton failedJobRetryTimeCycle attribute for the build flow node.
   *
   * @param retryTimeCycle
   *          the retry time cycle value to set
   * @return the builder object
   */
  public B operatonFailedJobRetryTimeCycle(String retryTimeCycle) {
    OperatonFailedJobRetryTimeCycle failedJobRetryTimeCycle = createInstance(OperatonFailedJobRetryTimeCycle.class);
    failedJobRetryTimeCycle.setTextContent(retryTimeCycle);

    addExtensionElement(failedJobRetryTimeCycle);

    return myself;
  }

  @SuppressWarnings("rawtypes")
  public B operatonExecutionListenerClass(String eventName, Class listenerClass) {
    return operatonExecutionListenerClass(eventName, listenerClass.getName());
  }

  public B operatonExecutionListenerClass(String eventName, String fullQualifiedClassName) {
    OperatonExecutionListener executionListener = createInstance(OperatonExecutionListener.class);
    executionListener.setOperatonEvent(eventName);
    executionListener.setOperatonClass(fullQualifiedClassName);

    addExtensionElement(executionListener);

    return myself;
  }

  public B operatonExecutionListenerExpression(String eventName, String expression) {
    OperatonExecutionListener executionListener = createInstance(OperatonExecutionListener.class);
    executionListener.setOperatonEvent(eventName);
    executionListener.setOperatonExpression(expression);

    addExtensionElement(executionListener);

    return myself;
  }

  public B operatonExecutionListenerDelegateExpression(String eventName, String delegateExpression) {
    OperatonExecutionListener executionListener = createInstance(OperatonExecutionListener.class);
    executionListener.setOperatonEvent(eventName);
    executionListener.setOperatonDelegateExpression(delegateExpression);

    addExtensionElement(executionListener);

    return myself;
  }

  public B compensationStart() {
    if (element instanceof BoundaryEvent boundaryEvent) {
      for (EventDefinition eventDefinition : boundaryEvent.getEventDefinitions()) {
        if(eventDefinition instanceof CompensateEventDefinition) {
          // if the boundary event contains a compensate event definition then
          // save the boundary event to later return to it and start a compensation

          compensateBoundaryEvent = boundaryEvent;
          compensationStarted = true;

          return myself;
        }
      }
    }

    throw new BpmnModelException("Compensation can only be started on a boundary event with a compensation event definition");
  }

  protected boolean isBoundaryEventWithStartedCompensation() {
    return compensationStarted && compensateBoundaryEvent != null;
  }

  protected boolean isCompensationHandler() {
    return !compensationStarted && compensateBoundaryEvent != null;
  }

}
