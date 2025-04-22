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
package org.operaton.bpm.model.bpmn.builder;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonFormField;

/**
 * @author Kristin Polenz
 *
 */
public class AbstractOperatonFormFieldBuilder<P, B extends AbstractOperatonFormFieldBuilder<P, B>> 
  extends AbstractBpmnModelElementBuilder<B, OperatonFormField> {

  protected BaseElement parent;

  protected AbstractOperatonFormFieldBuilder(BpmnModelInstance modelInstance, BaseElement parent, OperatonFormField element, Class<?> selfType) {
    super(modelInstance, element, selfType);
    this.parent = parent;
  }
  

  /**
   * Sets the form field id.
   *
   * @param id the form field id
   * @return  the builder object
   */
  public B operatonId(String id) {
    element.setOperatonId(id);
    return myself;
  }

  /**
   * Sets form field label.
   *
   * @param label the form field label
   * @return  the builder object
   */
  public B operatonLabel(String label) {
    element.setOperatonLabel(label);
    return myself;
  }

  /**
   * Sets the form field type.
   *
   * @param type the form field type
   * @return the builder object
   */
  public B operatonType(String type) {
    element.setOperatonType(type);
    return myself;
  }

  /**
   * Sets the form field default value.
   *
   * @param defaultValue the form field default value
   * @return the builder object
   */
  public B operatonDefaultValue(String defaultValue) {
    element.setOperatonDefaultValue(defaultValue);
    return myself;
  }

  /**
   * Finishes the building of a form field.
   *
   * @return the parent activity builder
   */
  @SuppressWarnings({ "unchecked" })
  public P operatonFormFieldDone() {
    return (P) parent.builder();
  }
}
