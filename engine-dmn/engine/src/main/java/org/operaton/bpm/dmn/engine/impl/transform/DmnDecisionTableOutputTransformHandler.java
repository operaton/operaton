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
package org.operaton.bpm.dmn.engine.impl.transform;

import java.util.List;
import java.util.stream.Stream;
import org.operaton.bpm.dmn.engine.impl.DmnDecisionTableOutputImpl;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformContext;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnDataTypeTransformer;
import org.operaton.bpm.dmn.engine.impl.spi.type.DmnTypeDefinition;
import org.operaton.bpm.dmn.engine.impl.type.DefaultTypeDefinition;
import org.operaton.bpm.dmn.engine.impl.type.DmnTypeDefinitionImpl;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.dmn.instance.Output;
import org.operaton.bpm.model.dmn.instance.OutputValues;

public class DmnDecisionTableOutputTransformHandler implements DmnElementTransformHandler<Output, DmnDecisionTableOutputImpl> {

  @Override
  public DmnDecisionTableOutputImpl handleElement(DmnElementTransformContext context, Output output) {
    return createFromOutput(context, output);
  }

  protected DmnDecisionTableOutputImpl createFromOutput(DmnElementTransformContext context, Output output) {
    DmnDecisionTableOutputImpl decisionTableOutput = createDmnElement(context, output);

    decisionTableOutput.setId(output.getId());
    decisionTableOutput.setName(output.getLabel());
    decisionTableOutput.setOutputName(output.getName());
    decisionTableOutput.setTypeDefinition(getTypeDefinition(context, output));
    decisionTableOutput.setOutputValues(getOutputValues(output, decisionTableOutput.getTypeDefinition()));

    return decisionTableOutput;
  }

  protected List<TypedValue> getOutputValues(Output output, DmnTypeDefinition typeDefinition) {
    OutputValues outputValues = output.getOutputValues();
    if (outputValues == null) {
      return List.of();
    }
    String textContent = output.getOutputValues().getTextContent();
    if (textContent != null && !textContent.isEmpty()) {
      return Stream.of(textContent.split(","))
        .map(String::trim)
        // Unquote the DMN string literals
        // https://github.com/camunda/dmn-scala/blob/0a988f285de2221c4d0bdf3bc2b20e5e1dd6f361/src/main/scala/org/camunda/dmn/evaluation/DecisionTableEvaluator.scala#L264C35-L264C62
        .map(DmnDecisionTableOutputTransformHandler::unquoteDMN)
        .map(typeDefinition::transform)
        .toList();
    }
    else {
      return List.of();
    }
  }

  @SuppressWarnings("unused")
  protected DmnDecisionTableOutputImpl createDmnElement(DmnElementTransformContext context, Output output) {
    return new DmnDecisionTableOutputImpl();
  }

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

  /**
   * Unquotes a DMN/FEEL string literal:
   * - Removes the first and last quote if they match (single or double).
   * - Replaces doubled quote characters inside with a single quote.
   * - Leaves unquoted strings unchanged.
   */
  protected static String unquoteDMN(String str) {
    if (str == null || str.length() < 2) {
      return str;
    }
    char first = str.charAt(0);
    char last = str.charAt(str.length() - 1);

    if ((first == last) && (first == '"' || first == '\'')) {
      String inner = str.substring(1, str.length() - 1);
      String doubled = String.valueOf(first) + first;
      return inner.replace(doubled, String.valueOf(first));
    } else {
      return str;
    }
  }

}
