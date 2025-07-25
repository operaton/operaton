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
import org.operaton.bpm.model.bpmn.instance.Activity;
import org.operaton.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.operaton.bpm.model.bpmn.instance.Event;

public abstract class AbstractCompensateEventDefinitionBuilder<B extends AbstractCompensateEventDefinitionBuilder<B>> extends AbstractRootElementBuilder<B, CompensateEventDefinition>{

  protected AbstractCompensateEventDefinitionBuilder(BpmnModelInstance modelInstance, CompensateEventDefinition element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  public B activityRef(String activityId) {
    Activity activity = modelInstance.getModelElementById(activityId);

    if (activity == null) {
      throw new BpmnModelException("Activity with id '" + activityId + "' does not exist");
    }
    Event event = (Event) element.getParentElement();
    if (activity.getParentElement() != event.getParentElement()) {
      throw new BpmnModelException("Activity with id '" + activityId + "' must be in the same scope as '" + event.getId() + "'");
    }

    element.setActivity(activity);
    return myself;
  }

  public B waitForCompletion(boolean waitForCompletion) {
    element.setWaitForCompletion(waitForCompletion);
    return myself;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public <T extends AbstractFlowNodeBuilder> T compensateEventDefinitionDone() {
    return (T) ((Event) element.getParentElement()).builder();
  }
}
