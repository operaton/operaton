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
import org.operaton.bpm.engine.impl.interceptor.DelegateInterceptor;
import org.operaton.bpm.engine.impl.repository.ResourceDefinitionEntity;
import org.operaton.bpm.engine.repository.ResourceDefinition;

/**
 * Provides context about the invocation of usercode and handles the actual
 * invocation
 *
 * @author Daniel Meyer
 * @see DelegateInterceptor
 */
public abstract class DelegateInvocation {

  protected Object invocationResult;
  protected BaseDelegateExecution contextExecution;
  protected ResourceDefinitionEntity<? extends ResourceDefinition> contextResource;

  /**
   * Provide a context execution or resource definition in which context the invocation
   *   should be performed. If both parameters are null, the invocation is performed in the
   *   current context.
   *
   * @param contextExecution set to an execution
   */
  protected DelegateInvocation(BaseDelegateExecution contextExecution, ResourceDefinitionEntity<? extends ResourceDefinition> contextResource) {
    // This constructor forces sub classes to call it, thereby making it more visible
    // whether a context switch is going to be performed for them.
    this.contextExecution = contextExecution;
    this.contextResource = contextResource;
  }

  /**
   * make the invocation proceed, performing the actual invocation of the user
   * code.
   *
   * @throws Exception
   *           the exception thrown by the user code
   */
  public void proceed() throws Exception {
    invoke();
  }

  protected abstract void invoke() throws Exception;

  /**
   * @return the result of the invocation (can be null if the invocation does
   *         not return a result)
   */
  public Object getInvocationResult() {
    return invocationResult;
  }

  /**
   * returns the execution in which context this delegate is invoked. may be null
   */
  public BaseDelegateExecution getContextExecution() {
    return contextExecution;
  }

  public ResourceDefinitionEntity<? extends ResourceDefinition> getContextResource() {
    return contextResource;
  }
}
