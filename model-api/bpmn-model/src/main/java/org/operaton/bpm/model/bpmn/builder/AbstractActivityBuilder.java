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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.Activity;
import org.operaton.bpm.model.bpmn.instance.BoundaryEvent;
import org.operaton.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonInputOutput;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonInputParameter;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonOutputParameter;
import org.operaton.bpm.model.bpmn.instance.dc.Bounds;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractActivityBuilder<B extends AbstractActivityBuilder<B, E>, E extends Activity> extends AbstractFlowNodeBuilder<B, E> {

  protected AbstractActivityBuilder(BpmnModelInstance modelInstance, E element, Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  public BoundaryEventBuilder boundaryEvent() {
    return boundaryEvent(null);
  }

  public BoundaryEventBuilder boundaryEvent(String id) {
    BoundaryEvent boundaryEvent = createSibling(BoundaryEvent.class, id);
    boundaryEvent.setAttachedTo(element);

    BpmnShape boundaryEventBpmnShape = createBpmnShape(boundaryEvent);
    setBoundaryEventCoordinates(boundaryEventBpmnShape);

    return boundaryEvent.builder();
  }

  public MultiInstanceLoopCharacteristicsBuilder multiInstance() {
    MultiInstanceLoopCharacteristics miCharacteristics
      = createChild(MultiInstanceLoopCharacteristics.class);

    return miCharacteristics.builder();
  }

  /**
   * Creates a new operaton input parameter extension element with the
   * given name and value.
   *
   * @param name the name of the input parameter
   * @param value the value of the input parameter
   * @return the builder object
   */
  public B operatonInputParameter(String name, String value) {
    OperatonInputOutput operatonInputOutput = getCreateSingleExtensionElement(OperatonInputOutput.class);

    OperatonInputParameter operatonInputParameter = createChild(operatonInputOutput, OperatonInputParameter.class);
    operatonInputParameter.setOperatonName(name);
    operatonInputParameter.setTextContent(value);

    return myself;
  }

  /**
   * Creates a new operaton output parameter extension element with the
   * given name and value.
   *
   * @param name the name of the output parameter
   * @param value the value of the output parameter
   * @return the builder object
   */
  public B operatonOutputParameter(String name, String value) {
    OperatonInputOutput operatonInputOutput = getCreateSingleExtensionElement(OperatonInputOutput.class);

    OperatonOutputParameter operatonOutputParameter = createChild(operatonInputOutput, OperatonOutputParameter.class);
    operatonOutputParameter.setOperatonName(name);
    operatonOutputParameter.setTextContent(value);

    return myself;
  }

  protected double calculateXCoordinate(Bounds boundaryEventBounds) {
    BpmnShape attachedToElement = findBpmnShape(element);

    double x = 0;

    if (attachedToElement != null) {

      Bounds attachedToBounds = attachedToElement.getBounds();

      Collection<BoundaryEvent> boundaryEvents = element.getParentElement().getChildElementsByType(BoundaryEvent.class);
      Collection<BoundaryEvent> attachedBoundaryEvents = new ArrayList<>();

      Iterator<BoundaryEvent> iterator = boundaryEvents.iterator();
      while (iterator.hasNext()) {
        BoundaryEvent tmp = iterator.next();
        if (tmp.getAttachedTo().equals(element)) {
          attachedBoundaryEvents.add(tmp);
        }
      }

      double attachedToX = attachedToBounds.getX();
      double attachedToWidth = attachedToBounds.getWidth();
      double boundaryWidth = boundaryEventBounds.getWidth();

      switch (attachedBoundaryEvents.size()) {
        case 2: {
          x = attachedToX + attachedToWidth / 2 + boundaryWidth / 2;
          break;
        }
        case 3: {
          x = attachedToX + attachedToWidth / 2 - 1.5 * boundaryWidth;
          break;
        }
        default: {
          x = attachedToX + attachedToWidth / 2 - boundaryWidth / 2;
          break;
        }
      }

    }

    return x;
  }

  protected void setBoundaryEventCoordinates(BpmnShape bpmnShape) {
    BpmnShape activity = findBpmnShape(element);
    Bounds boundaryBounds = bpmnShape.getBounds();

    double x = 0;
    double y = 0;

    if (activity != null) {
      Bounds activityBounds = activity.getBounds();
      double activityY = activityBounds.getY();
      double activityHeight = activityBounds.getHeight();
      double boundaryHeight = boundaryBounds.getHeight();
      x = calculateXCoordinate(boundaryBounds);
      y = activityY + activityHeight - boundaryHeight / 2;
    }

    boundaryBounds.setX(x);
    boundaryBounds.setY(y);
  }

}
