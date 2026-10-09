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
package org.operaton.bpm.engine.impl.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jspecify.annotations.NonNull;
import org.operaton.bpm.application.InvocationContext;

import org.jspecify.annotations.Nullable;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.ProcessApplicationUnavailableException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandInvocationContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutorContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

/**
 * Holds the context of the current command being executed.
 *
 * @author Tom Baeyens
 * @author Daniel Meyer
 * @author Thorben Lindhauer
 */
public final class Context {
  private static final ThreadLocal<Deque<CommandContext>> commandContextThreadLocal = new ThreadLocal<>();

  private static final ThreadLocal<Deque<CommandInvocationContext>> commandInvocationContextThreadLocal = new ThreadLocal<>();

  private static final ThreadLocal<Deque<ProcessEngineConfigurationImpl>> processEngineConfigurationStackThreadLocal = new ThreadLocal<>();
  private static final ThreadLocal<Deque<CoreExecutionContext<? extends CoreExecution>>> executionContextStackThreadLocal = new ThreadLocal<>();
  private static final ThreadLocal<JobExecutorContext> jobExecutorContextThreadLocal = new ThreadLocal<>();
  private static final ThreadLocal<Deque<ProcessApplicationReference>> processApplicationContext = new ThreadLocal<>();

  private Context() {
  }

  /**
   * @return {@code true} if a command context is active on the current thread
   * @since 2.2
   */
  public static boolean hasActiveCommandContext() {
    return findCommandContext().isPresent();
  }

  /**
   * Returns the current command context, if any is active on the current thread.
   * Use this over {@link #getCommandContext()} when the absence of a command context
   * is a valid case to be handled rather than an error.
   *
   * @return the current command context, or {@link Optional#empty()} if none is active
   * @since 2.2
   */
  public static Optional<CommandContext> findCommandContext() {
    Deque<CommandContext> stack = getStack(commandContextThreadLocal);
    if (stack.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(stack.peek());
  }

  /**
   * Returns the current command context. If no command context is active, an exception is thrown.
   * This is a null-safe alternative to reading the top of the command context stack directly,
   * useful in cases where the command context is required to be present. It supports the IDE
   * in detecting potential null pointer exceptions at compile time. Use {@link #findCommandContext()}
   * instead if the absence of a command context is expected and should be handled explicitly.
   *
   * @return the current command context
   * @throws IllegalStateException if no command context is active
   */
  public static @NonNull CommandContext getCommandContext() {
    return EnsureUtil.ensureActiveCommandContext(findCommandContext().orElse(null));
  }


  public static void setCommandContext(@NonNull CommandContext commandContext) {
    getStack(commandContextThreadLocal).push(commandContext);
  }

  public static void removeCommandContext() {
    getStack(commandContextThreadLocal).pop();
  }

  public static @Nullable CommandInvocationContext getCommandInvocationContext() {
    Deque<CommandInvocationContext> stack = getStack(commandInvocationContextThreadLocal);
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  public static void setCommandInvocationContext(CommandInvocationContext commandInvocationContext) {
    getStack(commandInvocationContextThreadLocal).push(commandInvocationContext);
  }

  public static void removeCommandInvocationContext() {
    Deque<CommandInvocationContext> stack = getStack(commandInvocationContextThreadLocal);
    CommandInvocationContext currentContext = stack.pop();
    if (stack.isEmpty()) {
      // do not clear when called from JobExecutor, will be cleared there after logging
      if (getJobExecutorContext() == null) {
        // outer command remove flow
        currentContext.getProcessDataContext().clearMdc();
        currentContext.getProcessDataContext().restoreExternalMDCProperties();
      }
    } else {
      // reset the MDC to the logging context of the outer command invocation
      // inner command remove flow
      stack.peek().getProcessDataContext().updateMdcFromCurrentValues();
    }
  }

  /**
   * @return {@code true} if a process engine configuration is active on the current thread
   * @since 2.2
   */
  public static boolean hasActiveProcessEngineConfiguration() {
    return findProcessEngineConfiguration().isPresent();
  }

  /**
   * Returns the current process engine configuration, if any is active on the current thread.
   * Use this over {@link #getProcessEngineConfiguration()} when the absence of a process engine
   * configuration is a valid case to be handled rather than an error.
   *
   * @return the current process engine configuration, or {@link Optional#empty()} if none is active
   * @since 2.2
   */
  public static Optional<ProcessEngineConfigurationImpl> findProcessEngineConfiguration() {
    Deque<ProcessEngineConfigurationImpl> stack = getStack(processEngineConfigurationStackThreadLocal);
    if (stack.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(stack.peek());
  }

  /**
   * Returns the current process engine configuration. If no process engine configuration is active, an exception is thrown.
   * This is a null-safe alternative to reading the top of the process engine configuration stack directly,
   * useful in cases where the process engine configuration is required to be present. It supports the IDE
   * in detecting potential null pointer exceptions at compile time. Use {@link #findProcessEngineConfiguration()}
   * instead if the absence of a process engine configuration is expected and should be handled explicitly.
   *
   * @return the current process engine configuration
   * @throws IllegalStateException if no process engine configuration is active
   */
  public static @NonNull ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    Optional<ProcessEngineConfigurationImpl> processEngineConfiguration = findProcessEngineConfiguration();
    if (processEngineConfiguration.isEmpty()) {
      throw new IllegalStateException("No process engine configuration active on thread " + Thread.currentThread());
    }
    return processEngineConfiguration.get();
  }

  public static void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
    getStack(processEngineConfigurationStackThreadLocal).push(processEngineConfiguration);
  }

  public static void removeProcessEngineConfiguration() {
    getStack(processEngineConfigurationStackThreadLocal).pop();
  }

  /**
   * @deprecated Use {@link #getBpmnExecutionContext()} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  public static @Nullable ExecutionContext getExecutionContext() {
    return getBpmnExecutionContext();
  }

  public static @Nullable BpmnExecutionContext getBpmnExecutionContext() {
    return (BpmnExecutionContext) getCoreExecutionContext();
  }

  public static @Nullable CaseExecutionContext getCaseExecutionContext() {
    return (CaseExecutionContext) getCoreExecutionContext();
  }

  public static @Nullable CoreExecutionContext<? extends CoreExecution> getCoreExecutionContext() {
    var stack = getStack(executionContextStackThreadLocal);
    if(stack.isEmpty()) {
      return null;
    } else {
      return stack.peek();
    }
  }

  public static void setExecutionContext(ExecutionEntity execution) {
    getStack(executionContextStackThreadLocal).push(new BpmnExecutionContext(execution));
  }

  public static void setExecutionContext(CaseExecutionEntity execution) {
    getStack(executionContextStackThreadLocal).push(new CaseExecutionContext(execution));
  }

  public static void removeExecutionContext() {
    getStack(executionContextStackThreadLocal).pop();
  }

  private static <T> Deque<T> getStack(ThreadLocal<Deque<T>> threadLocal) {
    Deque<T> stack = threadLocal.get();
    if (stack==null) {
      stack = new ArrayDeque<>();
      threadLocal.set(stack);
    }
    return stack;
  }

  public static @Nullable JobExecutorContext getJobExecutorContext() {
    return jobExecutorContextThreadLocal.get();
  }

  public static void setJobExecutorContext(JobExecutorContext jobExecutorContext) {
    jobExecutorContextThreadLocal.set(jobExecutorContext);
  }

  public static void removeJobExecutorContext() {
    jobExecutorContextThreadLocal.remove();
  }

  public static @Nullable ProcessApplicationReference getCurrentProcessApplication() {
    Deque<ProcessApplicationReference> stack = getStack(processApplicationContext);
    if(stack.isEmpty()) {
      return null;
    } else {
      return stack.peek();
    }
  }

  public static void setCurrentProcessApplication(ProcessApplicationReference reference) {
    Deque<ProcessApplicationReference> stack = getStack(processApplicationContext);
    stack.push(reference);
  }

  public static void removeCurrentProcessApplication() {
    Deque<ProcessApplicationReference> stack = getStack(processApplicationContext);
    stack.pop();
  }

  /**
   * Use {@link #executeWithinProcessApplication(Callable, ProcessApplicationReference, InvocationContext)}
   * instead if an {@link InvocationContext} is available.
   */
  public static <T> @Nullable T executeWithinProcessApplication(Callable<T> callback, ProcessApplicationReference processApplicationReference) {
    return executeWithinProcessApplication(callback, processApplicationReference, null);
  }

  public static <T> @Nullable T executeWithinProcessApplication(Callable<T> callback, ProcessApplicationReference processApplicationReference, InvocationContext invocationContext) {
    String paName = processApplicationReference.getName();
    try {
      ProcessApplicationInterface processApplication = processApplicationReference.getProcessApplication();
      setCurrentProcessApplication(processApplicationReference);

      return executeWrappedCallback(callback, invocationContext, processApplication);
    } catch (ProcessApplicationUnavailableException e) {
      throw new ProcessEngineException("Cannot switch to process application '%s' for execution: %s".formatted(paName, e.getMessage()), e);
    }
  }

  private static <T> @Nullable T executeWrappedCallback(Callable<T> callback, InvocationContext invocationContext,
      ProcessApplicationInterface processApplication) {
    try {
      // wrap callback
      ProcessApplicationClassloaderInterceptor<T> wrappedCallback = new ProcessApplicationClassloaderInterceptor<>(callback);
      // execute wrapped callback
      return processApplication.execute(wrappedCallback, invocationContext);
    } catch (Exception e) {
      // unwrap exception
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      } else {
        throw new ProcessEngineException("Unexpected exception while executing within process application ", e);
      }
    } finally {
      removeCurrentProcessApplication();
    }
  }
}
