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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import static org.operaton.bpm.engine.impl.util.ClassDelegateUtil.applyFieldDeclaration;

import java.util.List;
import java.util.concurrent.Callable;

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.bpmn.delegate.ActivityBehaviorInvocation;
import org.operaton.bpm.engine.impl.bpmn.delegate.JavaDelegateInvocation;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SignallableActivityBehavior;


/**
 * {@link ActivityBehavior} used when 'delegateExpression' is used
 * for a serviceTask.
 *
 * @author Joram Barrez
 * @author Josh Long
 * @author Slawomir Wojtasiak (Patch for ACT-1159)
 * @author Falko Menge
 */
public class ServiceTaskDelegateExpressionActivityBehavior extends TaskActivityBehavior {

  protected static final BpmnBehaviorLogger LOG = ProcessEngineLogger.BPMN_BEHAVIOR_LOGGER;

  protected Expression expression;
  private final List<FieldDeclaration> fieldDeclarations;

  public ServiceTaskDelegateExpressionActivityBehavior(Expression expression, List<FieldDeclaration> fieldDeclarations) {
    this.expression = expression;
    this.fieldDeclarations = fieldDeclarations;
  }

  @Override
  public void signal(final ActivityExecution execution, final String signalName, final Object signalData) throws Exception {
    ProcessApplicationReference targetProcessApplication = ProcessApplicationContextUtil.getTargetProcessApplication((ExecutionEntity) execution);
    if(ProcessApplicationContextUtil.requiresContextSwitch(targetProcessApplication)) {
      Context.executeWithinProcessApplication(() -> {
        signal(execution, signalName, signalData);
        return null;
      }, targetProcessApplication, new InvocationContext(execution));
    }
    else {
      doSignal(execution, signalName, signalData);
    }
  }

  public void doSignal(final ActivityExecution execution, final String signalName, final Object signalData) throws Exception {
    Object delegate = expression.getValue(execution);
    applyFieldDeclaration(fieldDeclarations, delegate);
    final ActivityBehavior activityBehaviorInstance = getActivityBehaviorInstance(execution, delegate);

    if (activityBehaviorInstance instanceof CustomActivityBehavior behavior) {
      ActivityBehavior delegateActivityBehavior = behavior.getDelegateActivityBehavior();

      if (!(delegateActivityBehavior instanceof SignallableActivityBehavior)) {
        // legacy behavior: do nothing when it is not a signallable activity behavior
        return;
      }
    }
    executeWithErrorPropagation(execution, () -> {
      ((SignallableActivityBehavior) activityBehaviorInstance).signal(execution, signalName, signalData);
      return null;
    });
  }

	@Override
  public void performExecution(final ActivityExecution execution) throws Exception {
	  Callable<Void> callable = () -> {
      // Note: we can't cache the result of the expression, because the
      // execution can change: eg. delegateExpression='${mySpringBeanFactory.randomSpringBean()}'
      Object delegate = expression.getValue(execution);
      applyFieldDeclaration(fieldDeclarations, delegate);

      if (delegate instanceof ActivityBehavior activityBehavior) {
        Context.getProcessEngineConfiguration()
            .getDelegateInterceptor()
            .handleInvocation(new ActivityBehaviorInvocation(activityBehavior, execution));

      } else if (delegate instanceof JavaDelegate javaDelegate) {
        Context.getProcessEngineConfiguration()
            .getDelegateInterceptor()
            .handleInvocation(new JavaDelegateInvocation(javaDelegate, execution));
        leave(execution);

      } else {
        throw LOG.resolveDelegateExpressionException(expression, ActivityBehavior.class, JavaDelegate.class);
      }
      return null;
    };
    executeWithErrorPropagation(execution, callable);
  }

  @SuppressWarnings("unused")
  protected ActivityBehavior getActivityBehaviorInstance(ActivityExecution execution, Object delegateInstance) {

    if (delegateInstance instanceof ActivityBehavior activityBehavior) {
      return new CustomActivityBehavior(activityBehavior);
    } else if (delegateInstance instanceof JavaDelegate javaDelegate) {
      return new ServiceTaskJavaDelegateActivityBehavior(javaDelegate);
    } else {
      throw LOG.missingDelegateParentClassException(delegateInstance.getClass().getName(),
        JavaDelegate.class.getName(), ActivityBehavior.class.getName());
    }
  }

}
