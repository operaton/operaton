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

import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.impl.ProcessApplicationLogger;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.application.ProcessApplicationManager;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.repository.CaseDefinitionEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.core.instance.CoreExecution;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.repository.ResourceDefinitionEntity;
import org.operaton.bpm.engine.impl.util.ClassLoaderUtil;

public final class ProcessApplicationContextUtil {

  private static final ProcessApplicationLogger LOG = ProcessEngineLogger.PROCESS_APPLICATION_LOGGER;

  private ProcessApplicationContextUtil() {
  }

  public static ProcessApplicationReference getTargetProcessApplication(CoreExecution execution) {
    if (execution instanceof ExecutionEntity executionEntity) {
      return getTargetProcessApplication(executionEntity);
    } else {
      return getTargetProcessApplication((CaseExecutionEntity) execution);
    }
  }

  public static ProcessApplicationReference getTargetProcessApplication(ExecutionEntity execution) {
    if (execution == null) {
      return null;
    }

    ProcessApplicationReference processApplicationForDeployment = getTargetProcessApplication(execution.getProcessDefinition());

    // logg application context switch details
    if(LOG.isContextSwitchLoggable() && processApplicationForDeployment == null) {
      loggContextSwitchDetails(execution);
    }

    return processApplicationForDeployment;
  }

  public static ProcessApplicationReference getTargetProcessApplication(CaseExecutionEntity execution) {
    if (execution == null) {
      return null;
    }

    ProcessApplicationReference processApplicationForDeployment = getTargetProcessApplication((CaseDefinitionEntity) execution.getCaseDefinition());

    // logg application context switch details
    if(LOG.isContextSwitchLoggable() && processApplicationForDeployment == null) {
      loggContextSwitchDetails(execution);
    }

    return processApplicationForDeployment;
  }

  public static ProcessApplicationReference getTargetProcessApplication(TaskEntity task) {
    if (task.getProcessDefinition() != null) {
      return getTargetProcessApplication(task.getProcessDefinition());
    }
    else if (task.getCaseDefinition() != null) {
      return getTargetProcessApplication(task.getCaseDefinition());
    }
    else {
      return null;
    }
  }

  public static ProcessApplicationReference getTargetProcessApplication(ResourceDefinitionEntity<?> definition) {
    ProcessApplicationReference reference = getTargetProcessApplication(definition.getDeploymentId());

    if (reference == null && areProcessApplicationsRegistered()) {
      ResourceDefinitionEntity<?> previous = definition.getPreviousDefinition();

      // do it in an iterative way instead of recursive to avoid
      // a possible StackOverflowException in cases with a lot
      // of versions of a definition
      while (previous != null) {
        reference = getTargetProcessApplication(previous.getDeploymentId());

        if (reference == null) {
          previous = previous.getPreviousDefinition();
        }
        else {
          return reference;
        }
      }
    }

    return reference;
  }

  public static ProcessApplicationReference getTargetProcessApplication(String deploymentId) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    ProcessApplicationManager processApplicationManager = processEngineConfiguration.getProcessApplicationManager();

    return processApplicationManager.getProcessApplicationForDeployment(deploymentId);
  }

  public static boolean areProcessApplicationsRegistered() {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    ProcessApplicationManager processApplicationManager = processEngineConfiguration.getProcessApplicationManager();

    return processApplicationManager.hasRegistrations();
  }

  private static void loggContextSwitchDetails(ExecutionEntity execution) {

    final CoreExecutionContext<? extends CoreExecution> executionContext = Context.getCoreExecutionContext();
    // only log for first atomic op:
    if(executionContext == null ||( executionContext.getExecution() != execution) ) {
      ProcessApplicationManager processApplicationManager = Context.getProcessEngineConfiguration().getProcessApplicationManager();
      LOG.debugNoTargetProcessApplicationFound(execution, processApplicationManager);
    }

  }

  private static void loggContextSwitchDetails(CaseExecutionEntity execution) {

    final CoreExecutionContext<? extends CoreExecution> executionContext = Context.getCoreExecutionContext();
    // only log for first atomic op:
    if(executionContext == null ||( executionContext.getExecution() != execution) ) {
      ProcessApplicationManager processApplicationManager = Context.getProcessEngineConfiguration().getProcessApplicationManager();
      LOG.debugNoTargetProcessApplicationFoundForCaseExecution(execution, processApplicationManager);
    }

  }

  public static boolean requiresContextSwitch(ProcessApplicationReference processApplicationReference) {

    final ProcessApplicationReference currentProcessApplication = Context.getCurrentProcessApplication();

    if(processApplicationReference == null) {
      return false;
    }

    if(currentProcessApplication == null) {
      return true;
    }
    else {
      if(!processApplicationReference.getName().equals(currentProcessApplication.getName())) {
        return true;
      }
      else {
        // check whether the thread context has been manipulated since last context switch. This can happen as a result of
        // an operation causing the container to switch to a different application.
        // Example: JavaDelegate implementation (inside PA) invokes an EJB from different application which in turn interacts with the Process engine.
        ClassLoader processApplicationClassLoader = ProcessApplicationClassloaderInterceptor.getProcessApplicationClassLoader();
        ClassLoader currentClassloader = ClassLoaderUtil.getContextClassloader();
        return currentClassloader != processApplicationClassLoader;
      }
    }
  }

  public static void doContextSwitch(final Runnable runnable, ProcessDefinitionEntity contextDefinition) {
    ProcessApplicationReference processApplication = getTargetProcessApplication(contextDefinition);
    if (requiresContextSwitch(processApplication)) {
      Context.executeWithinProcessApplication(() -> {
        runnable.run();
        return null;
      }, processApplication);
    }
    else {
      runnable.run();
    }
  }
}
