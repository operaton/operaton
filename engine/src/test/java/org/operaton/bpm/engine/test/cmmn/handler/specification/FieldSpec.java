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
package org.operaton.bpm.engine.test.cmmn.handler.specification;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonCaseExecutionListener;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonExpression;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonField;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonString;

public class FieldSpec {

  protected String fieldName;
  protected String expression;
  protected String childExpression;
  protected String stringValue;
  protected String childStringValue;

  public FieldSpec(String fieldName, String expression, String childExpression,
      String stringValue, String childStringValue) {
    this.fieldName = fieldName;
    this.expression = expression;
    this.childExpression = childExpression;
    this.stringValue = stringValue;
    this.childStringValue = childStringValue;
  }

  public void verify(FieldDeclaration field) {
    assertThat(field.getName()).isEqualTo(fieldName);

    Object fieldValue = field.getValue();
    assertThat(fieldValue).isNotNull();

    assertThat(fieldValue).isInstanceOf(Expression.class);
    Expression expressionValue = (Expression) fieldValue;
    assertThat(expressionValue.getExpressionText()).isEqualTo(getExpectedExpression());
  }

  public void addFieldToListenerElement(CmmnModelInstance modelInstance, OperatonCaseExecutionListener listenerElement) {
    OperatonField field = SpecUtil.createElement(modelInstance, listenerElement, null, OperatonField.class);
    field.setOperatonName(fieldName);

    if (expression != null) {
      field.setOperatonExpression(expression);

    } else if (childExpression != null) {
      OperatonExpression fieldExpressionChild = SpecUtil.createElement(modelInstance, field, null, OperatonExpression.class);
      fieldExpressionChild.setTextContent(childExpression);

    } else if (stringValue != null) {
      field.setOperatonStringValue(stringValue);

    } else if (childStringValue != null) {
      OperatonString fieldExpressionChild = SpecUtil.createElement(modelInstance, field, null, OperatonString.class);
      fieldExpressionChild.setTextContent(childStringValue);
    }
  }

  protected String getExpectedExpression() {
    if (expression != null) {
      return expression;
    } else if (childExpression != null) {
      return childExpression;
    } else if (stringValue != null) {
      return stringValue;
    } else {
      return childStringValue;
    }
  }

}
