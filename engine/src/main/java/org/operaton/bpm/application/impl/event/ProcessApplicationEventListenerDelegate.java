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
package org.operaton.bpm.application.impl.event;

import org.operaton.bpm.application.InvocationContext;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.ProcessApplicationUnavailableException;
import org.operaton.bpm.application.impl.ProcessApplicationLogger;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.context.ProcessApplicationContextUtil;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

import java.util.concurrent.Callable;

/**
 * <p>{@link ExecutionListener} and {@link TaskListener} implementation delegating to
 * the {@link ExecutionListener} and {@link TaskListener} provided by a
 * {@link ProcessApplicationInterface ProcessApplication}.</p>
 *
 * <p>If the process application does not provide an execution listener (ie.
 * {@link ProcessApplicationInterface#getExecutionListener()} returns null), the
 * request is silently ignored.</p>
 *
 * <p>If the process application does not provide a task listener (ie.
 * {@link ProcessApplicationInterface#getTaskListener()} returns null), the
 * request is silently ignored.</p>
 *
 *
 * @author Daniel Meyer
 * @see ProcessApplicationInterface#getExecutionListener()
 * @see ProcessApplicationInterface#getTaskListener()
 *
 */
public class ProcessApplicationEventListenerDelegate implements ExecutionListener, TaskListener {

  private static final ProcessApplicationLogger LOG = ProcessEngineLogger.PROCESS_APPLICATION_LOGGER;

  @Override
  public void notify(final DelegateExecution execution) throws Exception {
    Callable<Void> notification = () -> {
      notifyExecutionListener(execution);
      return null;
    };
    performNotification(execution, notification);
  }

  @Override
  public void notify(final DelegateTask delegateTask) {
    if(delegateTask.getExecution() == null) {
      LOG.taskNotRelatedToExecution(delegateTask);
    } else {
      final DelegateExecution execution = delegateTask.getExecution();
      Callable<Void> notification = () -> {
        notifyTaskListener(delegateTask);
        return null;
      };
      try {
        performNotification(execution, notification);
      } catch(Exception e) {
        throw LOG.exceptionWhileNotifyingPaTaskListener(e);
      }
    }
  }

  protected void performNotification(final DelegateExecution execution, Callable<Void> notification) throws Exception {
    final ProcessApplicationReference processApp = ProcessApplicationContextUtil.getTargetProcessApplication((ExecutionEntity) execution);
    if (processApp == null) {
      // ignore silently
      LOG.noTargetProcessApplicationForExecution(execution);

    } else {
      if (ProcessApplicationContextUtil.requiresContextSwitch(processApp)) {
        // this should not be necessary since context switch is already performed by OperationContext and / or DelegateInterceptor
        Context.executeWithinProcessApplication(notification, processApp, new InvocationContext(execution));

      } else {
        // context switch already performed
        notification.call();

      }
    }
  }

  protected void notifyExecutionListener(DelegateExecution execution) throws Exception {
    ProcessApplicationReference processApp = Context.getCurrentProcessApplication();
    try {
      ProcessApplicationInterface processApplication = processApp.getProcessApplication();
      ExecutionListener executionListener = processApplication.getExecutionListener();
      if(executionListener != null) {
        executionListener.notify(execution);

      } else {
        LOG.paDoesNotProvideExecutionListener(processApp.getName());

      }
    } catch (ProcessApplicationUnavailableException e) {
      // Process Application unavailable => ignore silently
      LOG.cannotInvokeListenerPaUnavailable(processApp.getName(), e);
    }
  }

  protected void notifyTaskListener(DelegateTask task) {
    ProcessApplicationReference processApp = Context.getCurrentProcessApplication();
    try {
      ProcessApplicationInterface processApplication = processApp.getProcessApplication();
      TaskListener taskListener = processApplication.getTaskListener();
      if(taskListener != null) {
        taskListener.notify(task);

      } else {
        LOG.paDoesNotProvideTaskListener(processApp.getName());

      }
    } catch (ProcessApplicationUnavailableException e) {
      // Process Application unavailable => ignore silently
      LOG.cannotInvokeListenerPaUnavailable(processApp.getName(), e);
    }
  }

}
