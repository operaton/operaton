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
package org.operaton.bpm.engine.rest.hal.cache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.rest.cache.Cache;
import org.operaton.bpm.engine.rest.hal.Hal;

public class HalRelationCacheBootstrap implements ServletContextListener {

  public static final String CONTEXT_PARAM_NAME = "org.operaton.bpm.engine.rest.hal.cache.config";

  protected ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    String contextParameter = sce.getServletContext().getInitParameter(CONTEXT_PARAM_NAME);
    if (contextParameter != null) {
      configureCaches(contextParameter);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    Hal.getInstance().destroyHalRelationCaches();
  }

  public void configureCaches(String contextParameter) {
    HalRelationCacheConfiguration configuration = new HalRelationCacheConfiguration(contextParameter);
    configureCaches(configuration);
  }

  public void configureCaches(HalRelationCacheConfiguration configuration) {
    Class<? extends Cache> cacheClass = configuration.getCacheImplementationClass();
    for (Map.Entry<Class<?>, Map<String, Object>> cacheConfiguration : configuration.getCacheConfigurations().entrySet()) {
      Cache cache = createCache(cacheClass, cacheConfiguration.getValue());
      registerCache(cacheConfiguration.getKey(), cache);
    }
  }

  protected Cache createCache(Class<? extends Cache> cacheClass, Map<String, Object> cacheConfiguration) {
    Cache cache = createCacheInstance(cacheClass);
    configureCache(cache, cacheConfiguration);
    return cache;
  }

  protected void configureCache(Cache cache, Map<String, Object> cacheConfiguration) {
    for (Map.Entry<String, Object> configuration : cacheConfiguration.entrySet()) {
      configureCache(cache, configuration.getKey(), configuration.getValue());
    }
  }

  protected Cache createCacheInstance(Class<? extends Cache> cacheClass) {
    try {
      return ReflectUtil.instantiate(cacheClass);
    }
    catch (ProcessEngineException e) {
      throw new HalRelationCacheConfigurationException("Unable to instantiate cache class %s".formatted(cacheClass.getName()), e);
    }
  }

  protected void configureCache(Cache cache, String property, Object value) {
    Method setter;
    try {
      setter = ReflectUtil.getSingleSetter(property, cache.getClass());
    }
    catch (ProcessEngineException e) {
      throw new HalRelationCacheConfigurationException("Unable to find setter for property %s".formatted(property), e);
    }

    if (setter == null) {
      throw new HalRelationCacheConfigurationException("Unable to find setter for property %s".formatted(property));
    }

    try {
      setter.invoke(cache, value);
    } catch (IllegalAccessException e) {
      throw new HalRelationCacheConfigurationException("Unable to access setter for property %s".formatted(property));
    } catch (InvocationTargetException e) {
      throw new HalRelationCacheConfigurationException("Unable to invoke setter for property %s".formatted(property));
    }
  }

  protected void registerCache(Class<?> halResourceClass, Cache cache) {
    Hal.getInstance().registerHalRelationCache(halResourceClass, cache);
  }

}
