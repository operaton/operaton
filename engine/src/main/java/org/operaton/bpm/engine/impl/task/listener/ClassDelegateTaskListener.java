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

import static org.operaton.bpm.engine.impl.util.ClassDelegateUtil.instantiateDelegate;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.delegate.ClassDelegate;
import org.operaton.bpm.engine.impl.task.delegate.TaskListenerInvocation;

/**
 * @author Roman Smirnov
 *
 */
public class ClassDelegateTaskListener extends ClassDelegate implements TaskListener {

  public ClassDelegateTaskListener(String className, List<FieldDeclaration> fieldDeclarations) {
    super(className, fieldDeclarations);
  }

  public ClassDelegateTaskListener(Class<?> clazz, List<FieldDeclaration> fieldDeclarations) {
    super(clazz, fieldDeclarations);
  }

  @Override
  public void notify(DelegateTask delegateTask) {
    TaskListener taskListenerInstance = getTaskListenerInstance();
    try {
      Context.getProcessEngineConfiguration()
        .getDelegateInterceptor()
        .handleInvocation(new TaskListenerInvocation(taskListenerInstance, delegateTask));

    }catch (Exception e) {
      throw new ProcessEngineException("Exception while invoking TaskListener: "+e.getMessage(), e);
    }
  }

  protected TaskListener getTaskListenerInstance() {
    Object delegateInstance = instantiateDelegate(className, fieldDeclarations);

    if (delegateInstance instanceof TaskListener taskListener) {
      return taskListener;

    } else {
      throw new ProcessEngineException(delegateInstance.getClass().getName()+" doesn't implement "+TaskListener.class);
    }
  }

}
