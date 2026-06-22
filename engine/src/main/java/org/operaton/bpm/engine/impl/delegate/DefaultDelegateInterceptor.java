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

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.context.CoreExecutionContext;
import org.operaton.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.DelegateInterceptor;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.repository.ResourceDefinitionEntity;

/**
 * The default implementation of the DelegateInterceptor.
 *<p/>
 * This implementation has the following features:
 * <ul>
 * <li>it performs context switch into the target process application (if applicable)</li>
 * <li>it checks autorizations if {@link ProcessEngineConfigurationImpl#isAuthorizationEnabledForCustomCode()} is true</li>
 * </ul>
 *
 * @author Daniel Meyer
 * @author Roman Smirnov
 */
public class DefaultDelegateInterceptor implements DelegateInterceptor {

  @Override
  public void handleInvocation(final DelegateInvocation invocation) throws Exception {

    final ProcessApplicationReference processApplication = getProcessApplicationForInvocation(invocation);

    if (processApplication != null && ProcessApplicationContextUtil.requiresContextSwitch(processApplication)) {
      Context.executeWithinProcessApplication(() -> {
        handleInvocation(invocation);
        return null;
      }, processApplication, new InvocationContext(invocation.getContextExecution()));
    }
    else {
      handleInvocationInContext(invocation);
    }

  }

  protected void handleInvocationInContext(final DelegateInvocation invocation) throws Exception {
    CommandContext commandContext = Context.getCommandContext();
    boolean wasAuthorizationCheckEnabled = commandContext.isAuthorizationCheckEnabled();
    boolean wasUserOperationLogEnabled = commandContext.isUserOperationLogEnabled();
    BaseDelegateExecution contextExecution = invocation.getContextExecution();

    ProcessEngineConfigurationImpl configuration = Context.getProcessEngineConfiguration();

    boolean popExecutionContext = false;

    try {
      if (!configuration.isAuthorizationEnabledForCustomCode()) {
        // the custom code should be executed without authorization
        commandContext.disableAuthorizationCheck();
      }

      try {
        commandContext.disableUserOperationLog();

        try {
          if (contextExecution != null && !isCurrentContextExecution(contextExecution)) {
            popExecutionContext = setExecutionContext(contextExecution);
          }

          invocation.proceed();
        }
        finally {
          if (popExecutionContext) {
            Context.removeExecutionContext();
          }
        }
      }
      finally {
        if (wasUserOperationLogEnabled) {
          commandContext.enableUserOperationLog();
        }
      }
    }
    finally {
      if (wasAuthorizationCheckEnabled) {
        commandContext.enableAuthorizationCheck();
      }
    }

  }

  /**
   * @return true if the execution context is modified by this invocation
   */
  protected boolean setExecutionContext(BaseDelegateExecution execution) {
    if (execution instanceof ExecutionEntity executionEntity) {
      Context.setExecutionContext(executionEntity);
      return true;
    }
    else if (execution instanceof CaseExecutionEntity caseExecutionEntity) {
      Context.setExecutionContext(caseExecutionEntity);
      return true;
    }
    return false;
  }

  protected boolean isCurrentContextExecution(BaseDelegateExecution execution) {
    CoreExecutionContext<?> coreExecutionContext = Context.getCoreExecutionContext();
    return coreExecutionContext != null && coreExecutionContext.getExecution() == execution;
  }

  protected ProcessApplicationReference getProcessApplicationForInvocation(final DelegateInvocation invocation) {

    BaseDelegateExecution contextExecution = invocation.getContextExecution();
    ResourceDefinitionEntity<?> contextResource = invocation.getContextResource();

    if (contextExecution != null) {
      return ProcessApplicationContextUtil.getTargetProcessApplication((CoreExecution) contextExecution);
    }
    else if (contextResource != null) {
      return ProcessApplicationContextUtil.getTargetProcessApplication(contextResource);
    }
    else {
      return null;
    }
  }

}
