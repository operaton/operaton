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
package org.operaton.bpm.model.bpmn.impl.instance.operaton;

import java.util.List;

import org.operaton.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.operaton.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonGenericValueElement;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.impl.util.ModelUtil;
import org.operaton.bpm.model.xml.instance.DomElement;

/**
 * A helper interface for operaton extension elements which
 * hold a generic child element like operaton:inputParameter,
 * operaton:outputParameter and operaton:entry.
 *
 * @author Sebastian Menski
 */
public class OperatonGenericValueElementImpl extends BpmnModelElementInstanceImpl implements OperatonGenericValueElement {

  public OperatonGenericValueElementImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BpmnModelElementInstance> T getValue() {
    List<DomElement> childElements = getDomElement().getChildElements();
    if (childElements.isEmpty()) {
      return null;
    }
    else {
      return (T) ModelUtil.getModelElement(childElements.get(0), modelInstance);
    }
  }

  @Override
  public void removeValue() {
    DomElement domElement = getDomElement();
    List<DomElement> childElements = domElement.getChildElements();
    for (DomElement childElement : childElements) {
      domElement.removeChild(childElement);
    }
  }

  @Override
  public <T extends BpmnModelElementInstance> void setValue(T value) {
    removeValue();
    getDomElement().appendChild(value.getDomElement());
  }

}
