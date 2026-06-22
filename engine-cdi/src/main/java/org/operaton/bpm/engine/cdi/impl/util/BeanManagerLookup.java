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
package org.operaton.bpm.engine.cdi.impl.util;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.operaton.bpm.engine.ProcessEngineException;

public final class BeanManagerLookup {

  /** holds a local beanManager if no jndi is available */
  private static BeanManager localInstance;

  /** provide a custom jndi lookup name */
  private static String jndiName;

  private BeanManagerLookup() {
  }

  public static void setLocalInstance(BeanManager beanManager) {
    localInstance = beanManager;
  }

  public static BeanManager getLocalInstance() {
    return localInstance;
  }

  public static BeanManager getBeanManager() {

    BeanManager beanManager = lookupBeanManager();

    if (beanManager != null) {
      return beanManager;
    }

    if (localInstance != null) {
      return localInstance;
    }

    throw new ProcessEngineException(
        "Could not lookup BeanManager. If no CDI container is available, set the BeanManager via the 'localInstance' property of this class.");
  }

  private static BeanManager lookupBeanManager() {

    // custom JNDI name takes precedence
    if (jndiName != null) {
      try {
        return (BeanManager) InitialContext.doLookup(jndiName);
      } catch (NamingException e) {
        throw new ProcessEngineException("Could not lookup BeanManager in JNDI using name: '%s'.".formatted(jndiName), e);
      }
    }

    // JNDI lookup for application servers — returns the deployment-local BeanManager
    // (java:comp is component-scoped and resolves to the correct WAR/EJB BeanManager)
    try {
      return (BeanManager) InitialContext.doLookup("java:comp/BeanManager");
    } catch (NamingException e) {
      // silently ignore — not available from server module classloader context
    }

    // JNDI fallback for servlet containers
    try {
      return (BeanManager) InitialContext.doLookup("java:comp/env/BeanManager");
    } catch (NamingException e) {
      // silently ignore
    }

    // CDI 4.1 standard programmatic lookup — used when java:comp/BeanManager is
    // not available (e.g. when engine-cdi is loaded as a WildFly server module)
    try {
      return CDI.current().getBeanManager();
    } catch (RuntimeException e) {
      // no CDI container available or accessible from this classloader
    }

    return null;

  }
}
