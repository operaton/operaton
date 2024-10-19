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

import org.operaton.bpm.dmn.engine.impl.DmnVariableImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;
import org.operaton.bpm.model.dmn.instance.Variable;

public class DmnVariableTransformHandler implements DmnElementTransformHandler<Variable, DmnVariableImpl> {

    /**
   * Handles a DmnElementTransformContext and a Variable to create a DmnVariableImpl.
   * 
   * @param context the DmnElementTransformContext
   * @param variable the Variable to create the DmnVariableImpl from
   * @return the DmnVariableImpl created from the Variable
   */
  public DmnVariableImpl handleElement(DmnElementTransformContext context, Variable variable) {
    return createFromVariable(context, variable);
  }

    /**
   * Creates a DmnVariableImpl object from a Variable object using the provided context
   * 
   * @param context the DmnElementTransformContext used for transformation
   * @param variable the Variable object to be transformed
   * @return the created DmnVariableImpl object
   */
  protected DmnVariableImpl createFromVariable(DmnElementTransformContext context, Variable variable) {
    DmnVariableImpl dmnVariable = createDmnElement(context, variable);

    dmnVariable.setId(variable.getId());
    dmnVariable.setName(variable.getName());

    DmnTypeDefinition typeDefinition = createTypeDefinition(context, variable);
    dmnVariable.setTypeDefinition(typeDefinition);

    return dmnVariable;
  }

    /**
   * Creates a new DmnVariableImpl using the provided DmnElementTransformContext and Variable objects.
   * 
   * @param context the DmnElementTransformContext object to use
   * @param variable the Variable object to use
   * @return a new DmnVariableImpl object
   */
  protected DmnVariableImpl createDmnElement(DmnElementTransformContext context, Variable variable) {
    return new DmnVariableImpl();
  }

}
