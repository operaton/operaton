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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.rest.cache.Cache;

public class HalRelationCacheConfiguration {

  public static final String CONFIG_CACHE_IMPLEMENTATION = "cacheImplementation";
  public static final String CONFIG_CACHES = "caches";

  protected ObjectMapper objectMapper = new ObjectMapper();
  protected Class<? extends Cache> cacheImplementationClass;
  protected Map<Class<?>, Map<String, Object>> cacheConfigurations;

  public HalRelationCacheConfiguration() {
    cacheConfigurations = new HashMap<>();
  }

  public HalRelationCacheConfiguration(String configuration) {
    this();
    parseConfiguration(configuration);
  }

  public Class<? extends Cache> getCacheImplementationClass() {
    return cacheImplementationClass;
  }

  @SuppressWarnings("unchecked")
  public void setCacheImplementationClass(Class<?> cacheImplementationClass) {
    if (Cache.class.isAssignableFrom(cacheImplementationClass)) {
      this.cacheImplementationClass = (Class<? extends Cache>) cacheImplementationClass;
    }
    else {
      throw new HalRelationCacheConfigurationException("Cache implementation class %s does not implement the interface %s".formatted(cacheImplementationClass.getName(), Cache.class.getName()));
    }
  }

  public Map<Class<?>, Map<String, Object>> getCacheConfigurations() {
    return cacheConfigurations;
  }

  public void setCacheConfigurations(Map<Class<?>, Map<String, Object>> cacheConfigurations) {
    this.cacheConfigurations = cacheConfigurations;
  }

  public void addCacheConfiguration(Class<?> halResourceClass, Map<String, Object> cacheConfiguration) {
    this.cacheConfigurations.put(halResourceClass, cacheConfiguration);
  }

  protected void parseConfiguration(String configuration) {
    try {
      JsonNode jsonConfiguration = objectMapper.readTree(configuration);
      parseConfiguration(jsonConfiguration);
    } catch (IOException e) {
      throw new HalRelationCacheConfigurationException("Unable to parse cache configuration", e);
    }
  }

  protected void parseConfiguration(JsonNode jsonConfiguration) {
    parseCacheImplementationClass(jsonConfiguration);
    parseCacheConfigurations(jsonConfiguration);
  }

  protected void parseCacheImplementationClass(JsonNode jsonConfiguration) {
    JsonNode jsonNode = jsonConfiguration.get(CONFIG_CACHE_IMPLEMENTATION);
    if (jsonNode != null) {
      String cacheImplementationClassName = jsonNode.textValue();
      Class<?> cacheImplClass = loadClass(cacheImplementationClassName);
      setCacheImplementationClass(cacheImplClass);
    }
    else {
      throw new HalRelationCacheConfigurationException("Unable to find the %s parameter".formatted(CONFIG_CACHE_IMPLEMENTATION));
    }
  }

  protected void parseCacheConfigurations(JsonNode jsonConfiguration) {
    JsonNode jsonNode = jsonConfiguration.get(CONFIG_CACHES);
    if (jsonNode != null) {
      jsonNode.properties().stream().forEach(entry -> parseCacheConfiguration(entry.getKey(), entry.getValue()));
    }
  }

  @SuppressWarnings("unchecked")
  protected void parseCacheConfiguration(String halResourceClassName, JsonNode jsonConfiguration) {
    try {
      Class<?> halResourceClass = loadClass(halResourceClassName);
      Map<String, Object> configuration = objectMapper.treeToValue(jsonConfiguration, Map.class);
      addCacheConfiguration(halResourceClass, configuration);
    } catch (IOException e) {
      throw new HalRelationCacheConfigurationException("Unable to parse cache configuration for HAL resource %s".formatted(halResourceClassName));
    }
  }

  protected Class<?> loadClass(String className) {
    try {
      // use classloader which loaded the REST api classes
      return Class.forName(className, true, HalRelationCacheConfiguration.class.getClassLoader());
    }
    catch (ClassNotFoundException e) {
      throw new HalRelationCacheConfigurationException("Unable to load class of cache configuration %s".formatted(className), e);
    }
  }

}
