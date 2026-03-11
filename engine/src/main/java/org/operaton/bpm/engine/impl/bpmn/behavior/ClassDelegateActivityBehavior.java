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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.util.List;

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.SignallableActivityBehavior;

import static org.operaton.bpm.engine.impl.util.ClassDelegateUtil.instantiateDelegate;


/**
 * Helper class for bpmn constructs that allow class delegation.
 *
 * <p>
 * This class will lazily instantiate the referenced classes when needed at runtime.
 * </p>
 *
 * @author Joram Barrez
 * @author Falko Menge
 * @author Roman Smirnov
 */
public class ClassDelegateActivityBehavior extends AbstractBpmnActivityBehavior {

  protected static final BpmnBehaviorLogger LOG = ProcessEngineLogger.BPMN_BEHAVIOR_LOGGER;

  protected String className;
  protected List<FieldDeclaration> fieldDeclarations;

  public ClassDelegateActivityBehavior(String className, List<FieldDeclaration> fieldDeclarations) {
    this.className = className;
    this.fieldDeclarations = fieldDeclarations;
  }

  public ClassDelegateActivityBehavior(Class<?> clazz, List<FieldDeclaration> fieldDeclarations) {
    this(clazz.getName(), fieldDeclarations);
  }

  // Activity Behavior
  @Override
  public void execute(final ActivityExecution execution) throws Exception {
    this.executeWithErrorPropagation(execution, () -> {
      getActivityBehaviorInstance(execution).execute(execution);
      return null;
    });
  }

  // Signallable activity behavior
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

  protected void doSignal(final ActivityExecution execution, final String signalName, final Object signalData) throws Exception {
    final ActivityBehavior activityBehaviorInstance = getActivityBehaviorInstance(execution);

    if (activityBehaviorInstance instanceof CustomActivityBehavior behavior) {
      ActivityBehavior delegate = behavior.getDelegateActivityBehavior();

      if (!(delegate instanceof SignallableActivityBehavior)) {
        throw LOG.incorrectlyUsedSignalException(SignallableActivityBehavior.class.getName() );
      }
    }
    executeWithErrorPropagation(execution, () -> {
      ((SignallableActivityBehavior) activityBehaviorInstance).signal(execution, signalName, signalData);
      return null;
    });
  }

  @SuppressWarnings("unused")
  protected ActivityBehavior getActivityBehaviorInstance(ActivityExecution execution) {
    Object delegateInstance = instantiateDelegate(className, fieldDeclarations);

    if (delegateInstance instanceof ActivityBehavior activityBehavior) {
      return new CustomActivityBehavior(activityBehavior);
    } else if (delegateInstance instanceof JavaDelegate javaDelegate) {
      return new ServiceTaskJavaDelegateActivityBehavior(javaDelegate);
    } else {
      throw LOG.missingDelegateParentClassException(
        delegateInstance.getClass().getName(),
        JavaDelegate.class.getName(),
        ActivityBehavior.class.getName()
      );
    }
  }

}
