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
package org.operaton.bpm.engine.impl.core.variable.mapping;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.core.CoreLogger;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.operaton.bpm.engine.impl.core.variable.scope.AbstractVariableScope;

/**
 *
 * <pre>
 *               +-----------------+
 *               |                 |
 *  outer scope-----> inner scope  |
 *               |                 |
 *               +-----------------+
 * </pre>
 *
 * @author Daniel Meyer
 */
public class InputParameter extends IoParameter {

  private static final CoreLogger LOG = ProcessEngineLogger.CORE_LOGGER;

  public InputParameter(String name, ParameterValueProvider valueProvider) {
    super(name, valueProvider);
  }

  @Override
  protected void execute(AbstractVariableScope innerScope, AbstractVariableScope outerScope) {

    // get value from outer scope
    Object value = valueProvider.getValue(outerScope);

    LOG.debugMappingValueFromOuterScopeToInnerScope(value,outerScope, name, innerScope);

    // set variable in inner scope
    innerScope.setVariableLocal(name, value);
  }

}
