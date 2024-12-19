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
package org.operaton.bpm.model.dmn.impl.instance;

import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.OPERATON_ATTRIBUTE_INPUT_VARIABLE;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.OPERATON_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static org.operaton.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_INPUT_CLAUSE;

import org.operaton.bpm.model.dmn.instance.DmnElement;
import org.operaton.bpm.model.dmn.instance.InputClause;
import org.operaton.bpm.model.dmn.instance.InputExpression;
import org.operaton.bpm.model.dmn.instance.InputValues;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;

public class InputClauseImpl extends DmnElementImpl implements InputClause {

  protected static ChildElement<InputExpression> inputExpressionChild;
  protected static ChildElement<InputValues> inputValuesChild;

  // operaton extensions
  protected static Attribute<String> operatonInputVariableAttribute;

  public InputClauseImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public InputExpression getInputExpression() {
    return inputExpressionChild.getChild(this);
  }

  @Override
  public void setInputExpression(InputExpression inputExpression) {
    inputExpressionChild.setChild(this, inputExpression);
  }

  @Override
  public InputValues getInputValues() {
    return inputValuesChild.getChild(this);
  }

  @Override
  public void setInputValues(InputValues inputValues) {
    inputValuesChild.setChild(this, inputValues);
  }

  // operaton extensions

  @Override
  public String getOperatonInputVariable() {
    return operatonInputVariableAttribute.getValue(this);
  }


  @Override
  public void setOperatonInputVariable(String inputVariable) {
    operatonInputVariableAttribute.setValue(this, inputVariable);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(InputClause.class, DMN_ELEMENT_INPUT_CLAUSE)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(DmnElement.class)
      .instanceProvider(new ModelTypeInstanceProvider<InputClause>() {
      @Override
      public InputClause newInstance(ModelTypeInstanceContext instanceContext) {
          return new InputClauseImpl(instanceContext);
        }
      });

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    inputExpressionChild = sequenceBuilder.element(InputExpression.class)
      .required()
      .build();

    inputValuesChild = sequenceBuilder.element(InputValues.class)
      .build();

    // operaton extensions

    operatonInputVariableAttribute = typeBuilder.stringAttribute(OPERATON_ATTRIBUTE_INPUT_VARIABLE)
      .namespace(OPERATON_NS)
      .build();

    typeBuilder.build();
  }

}
