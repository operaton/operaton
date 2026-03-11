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
package org.operaton.bpm.engine.impl.cmmn.listener;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.CaseExecutionListener;
import org.operaton.bpm.engine.delegate.DelegateCaseExecution;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.cmmn.delegate.CaseExecutionListenerInvocation;
import org.operaton.bpm.engine.impl.context.Context;

import static org.operaton.bpm.engine.impl.util.ClassDelegateUtil.applyFieldDeclaration;


/**
 * @author Roman Smirnov
 */
public class DelegateExpressionCaseExecutionListener implements CaseExecutionListener {

  protected Expression expression;
  private final List<FieldDeclaration> fieldDeclarations;

  public DelegateExpressionCaseExecutionListener(Expression expression, List<FieldDeclaration> fieldDeclarations) {
    this.expression = expression;
    this.fieldDeclarations = fieldDeclarations;
  }

  @Override
  public void notify(DelegateCaseExecution caseExecution) throws Exception {
    // Note: we can't cache the result of the expression, because the
    // caseExecution can change: eg. delegateExpression='${mySpringBeanFactory.randomSpringBean()}'
    Object delegate = expression.getValue(caseExecution);
    applyFieldDeclaration(fieldDeclarations, delegate);

    if (delegate instanceof CaseExecutionListener listenerInstance) {
      Context
        .getProcessEngineConfiguration()
        .getDelegateInterceptor()
        .handleInvocation(new CaseExecutionListenerInvocation(listenerInstance, caseExecution));
    } else {
      throw new ProcessEngineException("Delegate expression %s did not resolve to an implementation of ".formatted(expression) + CaseExecutionListener.class);
    }
  }

  /**
   * returns the expression text for this execution listener. Comes in handy if you want to
   * check which listeners you already have.
   */
  public String getExpressionText() {
    return expression.getExpressionText();
  }

  public List<FieldDeclaration> getFieldDeclarations() {
    return fieldDeclarations;
  }

}
