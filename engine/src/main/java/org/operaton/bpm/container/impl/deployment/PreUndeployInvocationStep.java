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
package org.operaton.bpm.container.impl.deployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.application.PreUndeploy;
import org.operaton.bpm.container.impl.ContainerIntegrationLogger;
import org.operaton.bpm.container.impl.deployment.util.InjectionUtil;
import org.operaton.bpm.container.impl.spi.DeploymentOperation;
import org.operaton.bpm.container.impl.spi.DeploymentOperationStep;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;

/**
 * <p>Operation step responsible for invoking the {@literal @}{@link PreUndeploy} method of a
 * ProcessApplication class.</p>
 *
 * @author Daniel Meyer
 *
 */
public class PreUndeployInvocationStep extends DeploymentOperationStep {

  private static final String CALLBACK_NAME = "@PreUndeploy";

  private static final ContainerIntegrationLogger LOG = ProcessEngineLogger.CONTAINER_INTEGRATION_LOGGER;

  @Override
  public String getName() {
    return "Invoking @PreUndeploy";
  }

  @Override
  public void performOperationStep(DeploymentOperation operationContext) {

    final AbstractProcessApplication processApplication = operationContext.getAttachment(Attachments.PROCESS_APPLICATION);
    final String paName = processApplication.getName();

    Class<? extends AbstractProcessApplication> paClass = processApplication.getClass();
    Method preUndeployMethod = InjectionUtil.detectAnnotatedMethod(paClass, PreUndeploy.class);

    if(preUndeployMethod == null) {
      LOG.debugPaLifecycleMethodNotFound(CALLBACK_NAME, paName);
      return;
    }

    LOG.debugFoundPaLifecycleCallbackMethod(CALLBACK_NAME, paName);

    // resolve injections
    Object[] injections = InjectionUtil.resolveInjections(operationContext, preUndeployMethod);

    try {
      // perform the actual invocation
      preUndeployMethod.invoke(processApplication, injections);
    }
    catch (IllegalArgumentException | IllegalAccessException e) {
      throw LOG.exceptionWhileInvokingPaLifecycleCallback(CALLBACK_NAME, paName, e);
    }
    catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if(cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      else {
        throw LOG.exceptionWhileInvokingPaLifecycleCallback(CALLBACK_NAME, paName, e);
      }
    }

  }

}
