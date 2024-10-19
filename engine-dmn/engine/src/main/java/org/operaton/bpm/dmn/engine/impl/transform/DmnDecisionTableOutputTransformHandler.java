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

import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;
import org.operaton.bpm.dmn.engine.impl.type.DefaultTypeDefinition;
import org.operaton.bpm.dmn.engine.impl.type.DmnTypeDefinitionImpl;
import org.operaton.bpm.model.dmn.instance.Output;

public class DmnDecisionTableOutputTransformHandler implements DmnElementTransformHandler<Output, DmnDecisionTableOutputImpl> {

    /**
   * Handles the given output element by creating a DmnDecisionTableOutputImpl object from it.
   *
   * @param context the DmnElementTransformContext object
   * @param output the Output object to be transformed
   * @return the DmnDecisionTableOutputImpl created from the given output
   */
  public DmnDecisionTableOutputImpl handleElement(DmnElementTransformContext context, Output output) {
    return createFromOutput(context, output);
  }

    /**
   * Creates a DmnDecisionTableOutputImpl object from the given Output object.
   * 
   * @param context the DmnElementTransformContext
   * @param output the Output object to create from
   * @return the created DmnDecisionTableOutputImpl object
   */
  protected DmnDecisionTableOutputImpl createFromOutput(DmnElementTransformContext context, Output output) {
    DmnDecisionTableOutputImpl decisionTableOutput = createDmnElement(context, output);

    decisionTableOutput.setId(output.getId());
    decisionTableOutput.setName(output.getLabel());
    decisionTableOutput.setOutputName(output.getName());
    decisionTableOutput.setTypeDefinition(getTypeDefinition(context, output));

    return decisionTableOutput;
  }

    /**
   * Creates a new DmnDecisionTableOutputImpl object.
   * 
   * @param context the transformation context
   * @param output the output to create the decision table output from
   * @return the created DmnDecisionTableOutputImpl object
   */
  protected DmnDecisionTableOutputImpl createDmnElement(DmnElementTransformContext context, Output output) {
    return new DmnDecisionTableOutputImpl();
  }

    /**
   * Returns the type definition for the given output element based on its type reference.
   * 
   * @param context the element transform context
   * @param output the output element
   * @return the type definition for the output element
   */
  protected DmnTypeDefinition getTypeDefinition(DmnElementTransformContext context, Output output) {
    String typeRef = output.getTypeRef();
    if (typeRef != null) {
      DmnDataTypeTransformer transformer = context.getDataTypeTransformerRegistry().getTransformer(typeRef);
      return new DmnTypeDefinitionImpl(typeRef, transformer);
    }
    else {
      return new DefaultTypeDefinition();
    }
  }

}
