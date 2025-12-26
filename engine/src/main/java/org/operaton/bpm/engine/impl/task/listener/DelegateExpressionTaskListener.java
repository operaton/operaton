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
package org.operaton.bpm.engine.impl.task.listener;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.task.delegate.TaskListenerInvocation;

import static org.operaton.bpm.engine.impl.util.ClassDelegateUtil.applyFieldDeclaration;


/**
 * @author Joram Barrez
 */
public class DelegateExpressionTaskListener implements TaskListener {

  protected Expression expression;
  private final List<FieldDeclaration> fieldDeclarations;

  public DelegateExpressionTaskListener(Expression expression, List<FieldDeclaration> fieldDeclarations) {
    this.expression = expression;
    this.fieldDeclarations = fieldDeclarations;
  }

  @Override
  public void notify(DelegateTask delegateTask) {
    // Note: we can't cache the result of the expression, because the
    // execution can change: eg. delegateExpression='${mySpringBeanFactory.randomSpringBean()}'

    VariableScope variableScope = delegateTask.getExecution();
    if (variableScope == null) {
      variableScope = delegateTask.getCaseExecution();
    }

    Object delegate = expression.getValue(variableScope);
    applyFieldDeclaration(fieldDeclarations, delegate);

    if (delegate instanceof TaskListener taskListener) {
      try {
        Context.getProcessEngineConfiguration()
          .getDelegateInterceptor()
          .handleInvocation(new TaskListenerInvocation(taskListener, delegateTask));
      } catch (Exception e) {
        throw new ProcessEngineException("Exception while invoking TaskListener: "+e.getMessage(), e);
      }
    } else {
      throw new ProcessEngineException("Delegate expression %s did not resolve to an implementation of %s".formatted(expression, TaskListener.class));
    }
  }

  /**
   * returns the expression text for this task listener. Comes in handy if you want to
   * check which listeners you already have.
   */
  public String getExpressionText() {
    return expression.getExpressionText();
  }

  public List<FieldDeclaration> getFieldDeclarations() {
    return fieldDeclarations;
  }

}
