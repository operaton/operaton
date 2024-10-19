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
import org.operaton.bpm.model.dmn.instance.InputEntry;

public class DmnDecisionTableConditionTransformHandler implements DmnElementTransformHandler<InputEntry, DmnExpressionImpl> {

    /**
   * Handles the given input entry by creating a DmnExpressionImpl object from it.
   * 
   * @param context the DmnElementTransformContext
   * @param inputEntry the InputEntry to create the DmnExpressionImpl from
   * @return the DmnExpressionImpl created from the input entry
   */
  public DmnExpressionImpl handleElement(DmnElementTransformContext context, InputEntry inputEntry) {
    return createFromInputEntry(context, inputEntry);
  }

    /**
   * Creates a DmnExpressionImpl object from an InputEntry object
   * 
   * @param context the DmnElementTransformContext object
   * @param inputEntry the InputEntry object to create the DmnExpressionImpl from
   * @return the created DmnExpressionImpl object
   */
  protected DmnExpressionImpl createFromInputEntry(DmnElementTransformContext context, InputEntry inputEntry) {
    DmnExpressionImpl condition = createDmnElement(context, inputEntry);

    condition.setId(inputEntry.getId());
    condition.setName(inputEntry.getLabel());
    condition.setExpressionLanguage(getExpressionLanguage(context, inputEntry));
    condition.setExpression(getExpression(inputEntry));

    return condition;
  }

    /**
   * Creates a new DmnExpressionImpl element based on the provided context and inputEntry.
   *
   * @param context the DmnElementTransformContext to use
   * @param inputEntry the InputEntry to use
   * @return a new DmnExpressionImpl element
   */
  protected DmnExpressionImpl createDmnElement(DmnElementTransformContext context, InputEntry inputEntry) {
    return new DmnExpressionImpl();
  }

}
