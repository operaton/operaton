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

import static org.operaton.bpm.dmn.engine.impl.transform.DmnExpressionTransformHelper.getExpression;
import static org.operaton.bpm.dmn.engine.impl.transform.DmnExpressionTransformHelper.getExpressionLanguage;

import org.operaton.bpm.dmn.engine.impl.DmnExpressionImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.model.dmn.instance.LiteralExpression;

public class DmnLiternalExpressionTransformHandler implements DmnElementTransformHandler<LiteralExpression, DmnExpressionImpl> {

    /**
   * Handles a DMN element by creating a DmnExpressionImpl from a given LiteralExpression.
   *
   * @param context the context of the DMN element transformation
   * @param literalExpression the LiteralExpression to create the DmnExpressionImpl from
   * @return the DmnExpressionImpl created from the LiteralExpression
   */
  public DmnExpressionImpl handleElement(DmnElementTransformContext context, LiteralExpression literalExpression) {
    return createFromLiteralExpressionEntry(context, literalExpression);
  }

    /**
   * Creates a DmnExpressionImpl object from a LiteralExpression object.
   * 
   * @param context the transformation context
   * @param literalExpression the LiteralExpression object to transform
   * @return the created DmnExpressionImpl object
   */
  protected DmnExpressionImpl createFromLiteralExpressionEntry(DmnElementTransformContext context, LiteralExpression literalExpression) {
    DmnExpressionImpl dmnExpression = createDmnElement(context, literalExpression);

    dmnExpression.setId(literalExpression.getId());
    dmnExpression.setName(literalExpression.getLabel());
    dmnExpression.setExpressionLanguage(getExpressionLanguage(context, literalExpression));
    dmnExpression.setExpression(getExpression(literalExpression));

    return dmnExpression;
  }

    /**
   * Creates a new DmnExpressionImpl element.
   *
   * @param context the context for transforming the DmnElement
   * @param inputEntry the LiteralExpression input entry
   * @return a new DmnExpressionImpl element
   */
  protected DmnExpressionImpl createDmnElement(DmnElementTransformContext context, LiteralExpression inputEntry) {
    return new DmnExpressionImpl();
  }

}
