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
package org.operaton.bpm;

import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.ProcessEngine;


/**
 * <p>Provides access to the Operaton services.</p>
 *
 * @author Daniel Meyer
 *
 */
public final class BpmPlatform {

  public static final String JNDI_NAME_PREFIX = "java:global";
  public static final String APP_JNDI_NAME = "operaton-bpm-platform";
  public static final String MODULE_JNDI_NAME = "process-engine";

  public static final String PROCESS_ENGINE_SERVICE_NAME = "ProcessEngineService!org.operaton.bpm.ProcessEngineService";
  public static final String PROCESS_APPLICATION_SERVICE_NAME = "ProcessApplicationService!org.operaton.bpm.ProcessApplicationService";

  public static final String PROCESS_ENGINE_SERVICE_JNDI_NAME = "%s/%s/%s/%s".formatted(JNDI_NAME_PREFIX, APP_JNDI_NAME, MODULE_JNDI_NAME, PROCESS_ENGINE_SERVICE_NAME);
  public static final String PROCESS_APPLICATION_SERVICE_JNDI_NAME = "%s/%s/%s/%s".formatted(JNDI_NAME_PREFIX, APP_JNDI_NAME, MODULE_JNDI_NAME, PROCESS_APPLICATION_SERVICE_NAME);

  private BpmPlatform() {
  }

  public static ProcessEngineService getProcessEngineService() {
    return RuntimeContainerDelegate.INSTANCE.get().getProcessEngineService();
  }

  public static ProcessApplicationService getProcessApplicationService() {
    return RuntimeContainerDelegate.INSTANCE.get().getProcessApplicationService();
  }

  public static ProcessEngine getDefaultProcessEngine() {
    return getProcessEngineService().getDefaultProcessEngine();
  }
}
