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
package org.operaton.bpm.engine.impl.delegate;

import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.scripting.ExecutableScript;

/**
 * @author Roman Smirnov
 *
 */
public class ScriptInvocation extends DelegateInvocation {

  protected ExecutableScript script;
  protected VariableScope scope;

  public ScriptInvocation(ExecutableScript script, VariableScope scope) {
    this(script, scope, null);
  }

  public ScriptInvocation(ExecutableScript script, VariableScope scope, BaseDelegateExecution contextExecution) {
    super(contextExecution, null);
    this.script = script;
    this.scope = scope;
  }

  @Override
  protected void invoke() throws Exception {
    invocationResult = Context
      .getProcessEngineConfiguration()
      .getScriptingEnvironment()
      .execute(script, scope);
  }

}
