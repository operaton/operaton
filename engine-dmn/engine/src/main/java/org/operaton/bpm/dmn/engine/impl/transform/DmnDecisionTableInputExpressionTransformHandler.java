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
package org.operaton.bpm.dmn.engine.impl.transform;

import static org.operaton.bpm.dmn.engine.impl.transform.DmnExpressionTransformHelper.createTypeDefinition;
import static org.operaton.bpm.dmn.engine.impl.transform.DmnExpressionTransformHelper.getExpression;
import static org.operaton.bpm.dmn.engine.impl.transform.DmnExpressionTransformHelper.getExpressionLanguage;

import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.model.dmn.instance.InputExpression;

public class DmnDecisionTableInputExpressionTransformHandler implements DmnElementTransformHandler<InputExpression, DmnExpressionImpl> {

    /**
   * Handles a DmnElement by creating a DmnExpressionImpl from the given InputExpression
   * @param context the DmnElementTransformContext
   * @param inputExpression the InputExpression to create DmnExpressionImpl from
   * @return the created DmnExpressionImpl
   */
  public DmnExpressionImpl handleElement(DmnElementTransformContext context, InputExpression inputExpression) {
    return createFromInputExpression(context, inputExpression);
  }

    /**
   * Creates a DmnExpressionImpl object from the given InputExpression object
   * 
   * @param context the context in which the transformation is being performed
   * @param inputExpression the input expression to be transformed
   * @return the DmnExpressionImpl object created from the input expression
   */
  protected DmnExpressionImpl createFromInputExpression(DmnElementTransformContext context, InputExpression inputExpression) {
    DmnExpressionImpl dmnExpression = createDmnElement(context, inputExpression);

    dmnExpression.setId(inputExpression.getId());
    dmnExpression.setName(inputExpression.getLabel());
    dmnExpression.setTypeDefinition(createTypeDefinition(context, inputExpression));
    dmnExpression.setExpressionLanguage(getExpressionLanguage(context, inputExpression));
    dmnExpression.setExpression(getExpression(inputExpression));

    return dmnExpression;
  }

    /**
   * Creates a new DmnExpressionImpl element using the provided context and input expression.
   *
   * @param context the DmnElementTransformContext used for creating the DmnExpressionImpl
   * @param inputExpression the InputExpression used for creating the DmnExpressionImpl
   * @return a new DmnExpressionImpl element
   */
  protected DmnExpressionImpl createDmnElement(DmnElementTransformContext context, InputExpression inputExpression) {
    return new DmnExpressionImpl();
  }

}
