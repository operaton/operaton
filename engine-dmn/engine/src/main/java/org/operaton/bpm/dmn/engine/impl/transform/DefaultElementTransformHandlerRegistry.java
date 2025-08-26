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

import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandler;
import org.operaton.bpm.dmn.engine.impl.spi.transform.DmnElementTransformHandlerRegistry;
import org.operaton.bpm.model.dmn.instance.*;

public class DefaultElementTransformHandlerRegistry implements DmnElementTransformHandlerRegistry {

  protected final Map<Class<? extends DmnModelElementInstance>, DmnElementTransformHandler<? extends DmnModelElementInstance, ?>> handlers = getDefaultElementTransformHandlers();

  protected static Map<Class<? extends DmnModelElementInstance>, DmnElementTransformHandler<? extends DmnModelElementInstance, ?>> getDefaultElementTransformHandlers() {
    Map<Class<? extends DmnModelElementInstance>, DmnElementTransformHandler<? extends DmnModelElementInstance, ?>> handlers = new HashMap<>();

    handlers.put(Definitions.class, new DmnDecisionRequirementsGraphTransformHandler());
    handlers.put(Decision.class, new DmnDecisionTransformHandler());

    handlers.put(DecisionTable.class, new DmnDecisionTableTransformHandler());
    handlers.put(Input.class, new DmnDecisionTableInputTransformHandler());
    handlers.put(InputExpression.class, new DmnDecisionTableInputExpressionTransformHandler());
    handlers.put(Output.class, new DmnDecisionTableOutputTransformHandler());
    handlers.put(Rule.class, new DmnDecisionTableRuleTransformHandler());
    handlers.put(InputEntry.class, new DmnDecisionTableConditionTransformHandler());
    handlers.put(OutputEntry.class, new DmnLiternalExpressionTransformHandler());

    handlers.put(LiteralExpression.class, new DmnLiternalExpressionTransformHandler());
    handlers.put(Variable.class, new DmnVariableTransformHandler());

    return handlers;
  }

  @Override
  public <Source extends DmnModelElementInstance, Target> void addHandler(Class<Source> sourceClass, DmnElementTransformHandler<Source, Target> handler) {
    handlers.put(sourceClass, handler);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <Source extends DmnModelElementInstance, Target> DmnElementTransformHandler<Source, Target> getHandler(Class<Source> sourceClass) {
    return (DmnElementTransformHandler<Source, Target>) handlers.get(sourceClass);
  }

}
