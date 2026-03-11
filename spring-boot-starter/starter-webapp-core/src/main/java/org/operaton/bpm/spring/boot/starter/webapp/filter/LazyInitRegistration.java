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
package org.operaton.bpm.spring.boot.starter.webapp.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.servlet.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import org.operaton.bpm.spring.boot.starter.webapp.filter.LazyDelegateFilter.InitHook;

public class LazyInitRegistration implements ApplicationContextAware {

  protected static final String RESOURCE_LOADER_DEPENDING_INIT_HOOK = "resourceLoaderDependingInitHook";

  protected static final Set<LazyDelegateFilter<? extends Filter>> REGISTRATION = new HashSet<>();

  protected static ApplicationContext applicationContext;

  private static final Logger LOGGER = LoggerFactory.getLogger(LazyInitRegistration.class);

  static void register(LazyDelegateFilter<? extends Filter> lazyDelegateFilter) {
    REGISTRATION.add(lazyDelegateFilter);
  }

  @SuppressWarnings("unchecked")
  protected static <T extends Filter> InitHook<T> getInitHook() {
    if (applicationContext != null && applicationContext.containsBean(RESOURCE_LOADER_DEPENDING_INIT_HOOK)) {
      return applicationContext.getBean(RESOURCE_LOADER_DEPENDING_INIT_HOOK, InitHook.class);
    }
    return null;
  }

  static boolean isRegistered(LazyDelegateFilter<? extends Filter> lazyDelegateFilter) {
    return REGISTRATION.contains(lazyDelegateFilter);
  }

  static <T extends Filter> boolean lazyInit(LazyDelegateFilter<T> lazyDelegateFilter) {
    if (applicationContext != null) {
      if (isRegistered(lazyDelegateFilter)) {
        lazyDelegateFilter.setInitHook(LazyInitRegistration.<T> getInitHook());
        lazyDelegateFilter.lazyInit();
        REGISTRATION.remove(lazyDelegateFilter);
        LOGGER.info("lazy initialized {}", lazyDelegateFilter);
        return true;
      } else {
        LOGGER.debug("skipping lazy init for {} because of no init hook registration", lazyDelegateFilter);
      }
    } else {
      LOGGER.debug("skipping lazy init for {} because application context not initialized yet", lazyDelegateFilter);
    }

    return false;
  }

  @Override
  @SuppressWarnings({"java:S2696"}) // cannot add static to overridden method
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    LazyInitRegistration.applicationContext = applicationContext;
    for (LazyDelegateFilter<? extends Filter> lazyDelegateFilter : getRegistrations()) {
      lazyInit(lazyDelegateFilter);
    }
  }

  @EventListener
  @SuppressWarnings({"java:S2696"}) // EventListener method cannot be static
  protected void onContextClosed(ContextClosedEvent ev) {
    applicationContext = null;
  }

  static Set<LazyDelegateFilter<? extends Filter>> getRegistrations() {
    return Collections.unmodifiableSet(new HashSet<LazyDelegateFilter<? extends Filter>>(REGISTRATION));
  }
}
