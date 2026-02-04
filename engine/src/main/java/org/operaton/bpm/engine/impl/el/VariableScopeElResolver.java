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
package org.operaton.bpm.engine.impl.el;

import java.util.List;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.bpmn.behavior.ExternalTaskActivityBehavior;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;


/**
 * Implementation of an {@link ELResolver} that resolves expressions
 * with the process variables of a given {@link VariableScope} as context.
 * <br>
 * Also exposes the currently logged in username to be used in expressions (if any)
 *
 * @author Joram Barrez
 * @author Frederik Heremans
 */
public class VariableScopeElResolver extends ELResolver {

  public static final String EXECUTION_KEY = "execution";
  public static final String CASE_EXECUTION_KEY = "caseExecution";
  public static final String TASK_KEY = "task";
  public static final String EXTERNAL_TASK_KEY = "externalTask";
  public static final String LOGGED_IN_USER_KEY = "authenticatedUserId";

  @Override
  public Object getValue(ELContext context, Object base, Object property) {
    if (base != null) {
      return null;
    }

    VariableScope variableScope = (VariableScope) context.getContext(VariableScope.class);
    if (variableScope == null) {
      return null;
    }

    String variable = (String) property;

    if (isScopeReference(variableScope, variable)) {
      context.setPropertyResolved(true);
      return variableScope;
    }

    if (isExternalTaskReference(variableScope, variable)) {
      context.setPropertyResolved(true);
      return getExternalTask(variableScope);
    }

    if (EXECUTION_KEY.equals(variable) && variableScope instanceof TaskEntity taskEntity) {
      context.setPropertyResolved(true);
      return taskEntity.getExecution();
    }

    if (LOGGED_IN_USER_KEY.equals(variable)) {
      context.setPropertyResolved(true);
      return Context.getCommandContext().getAuthenticatedUserId();
    }

    if (variableScope.hasVariable(variable)) {
      context.setPropertyResolved(true); // if not set, the next elResolver in the CompositeElResolver will be called
      return variableScope.getVariable(variable);
    }

    // property resolution (eg. bean.value) will be done by the BeanElResolver (part of the CompositeElResolver)
    // It will use the bean resolved in this resolver as base.

    return null;
  }

  private boolean isScopeReference(VariableScope variableScope, String property) {
    return (EXECUTION_KEY.equals(property) && variableScope instanceof ExecutionEntity)
        || (TASK_KEY.equals(property) && variableScope instanceof TaskEntity)
        || (variableScope instanceof CaseExecutionEntity
            && (CASE_EXECUTION_KEY.equals(property) || EXECUTION_KEY.equals(property)));
  }

  private boolean isExternalTaskReference(VariableScope variableScope, String property) {
    return EXTERNAL_TASK_KEY.equals(property)
        && variableScope instanceof ExecutionEntity executionEntity
        && executionEntity.getActivity() != null
        && executionEntity.getActivity().getActivityBehavior() instanceof ExternalTaskActivityBehavior;
  }

  private ExternalTaskEntity getExternalTask(VariableScope variableScope) {
    ExecutionEntity executionEntity = (ExecutionEntity) variableScope;
    List<ExternalTaskEntity> externalTasks = executionEntity.getExternalTasks();
    if (externalTasks.size() != 1) {
      throw new ProcessEngineException("Could not resolve expression to single external task entity.");
    }
    return externalTasks.get(0);
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property) {
    if (base == null) {
      String variable = (String) property;
      Object object = context.getContext(VariableScope.class);
      return object != null && !((VariableScope)object).hasVariable(variable);
    }
    return true;
  }

  @Override
  public void setValue(ELContext context, Object base, Object property, Object value) {
    if (base == null) {
      String variable = (String) property;
      Object object = context.getContext(VariableScope.class);
      if (object != null) {
        VariableScope variableScope = (VariableScope) object;
        if (variableScope.hasVariable(variable)) {
          variableScope.setVariable(variable, value);
          context.setPropertyResolved(true);
        }
      }
    }
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext arg0, Object arg1) {
    return Object.class;
  }

  @Override
  public Class<?> getType(ELContext arg0, Object arg1, Object arg2) {
    return Object.class;
  }

}
