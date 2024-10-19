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
package org.operaton.bpm.dmn.engine.impl.el;

import org.operaton.bpm.impl.juel.jakarta.el.ELContext;
import org.operaton.bpm.impl.juel.jakarta.el.ELResolver;

import org.operaton.bpm.engine.variable.context.VariableContext;

import org.operaton.bpm.impl.juel.SimpleContext;

/**
 * @author Daniel Meyer
 *
 */
public class JuelElContextFactory {

  protected final ELResolver resolver;

  public JuelElContextFactory(ELResolver resolver) {
    this.resolver = resolver;
  }

    /**
   * Creates and returns a new ELContext object with the given VariableContext.
   * 
   * @param variableContext the VariableContext to be associated with the ELContext
   * @return a new ELContext object with the given VariableContext
   */
  public ELContext createElContext(VariableContext variableContext) {
    SimpleContext elContext = new SimpleContext(resolver);
    elContext.putContext(VariableContext.class, variableContext);
    return elContext;
  }

}
