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
package org.operaton.bpm.container.impl.jboss.util;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Provides method abstractions to make our subsystem compatible with different JBoss versions.
 * This affects mainly EAP versions.
 */
public class JBossCompatibilityExtension {

  /**
   * The service name of the root application server service.
   * Copied from org.jboss.as.server.Services - JBoss 7.2.0.Final
   */
  public static final ServiceName JBOSS_AS = ServiceName.JBOSS.append("as");

  /**
   * The service corresponding to the {@link java.util.concurrent.ExecutorService} for this instance.
   * Copied from org.jboss.as.server.Services - JBoss 7.2.0.Final
   */
  static final ServiceName JBOSS_SERVER_EXECUTOR = JBOSS_AS.append("server-executor");

  private JBossCompatibilityExtension() {
  }

  /**
   * Adds the JBoss server executor as a dependency to the given service.
   * Copied from org.jboss.as.server.Services - JBoss 7.2.0.Final
   */
  public static Supplier<ExecutorService> addServerExecutorDependency(ServiceBuilder<?> serviceBuilder) {
    return serviceBuilder.requires(JBOSS_SERVER_EXECUTOR);
  }
}
