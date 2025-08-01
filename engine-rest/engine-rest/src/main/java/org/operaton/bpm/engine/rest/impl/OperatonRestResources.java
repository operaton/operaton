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
package org.operaton.bpm.engine.rest.impl;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.operaton.bpm.engine.rest.exception.ExceptionHandler;
import org.operaton.bpm.engine.rest.exception.JsonMappingExceptionHandler;
import org.operaton.bpm.engine.rest.exception.JsonParseExceptionHandler;
import org.operaton.bpm.engine.rest.exception.ProcessEngineExceptionHandler;
import org.operaton.bpm.engine.rest.exception.RestExceptionHandler;
import org.operaton.bpm.engine.rest.hal.JacksonHalJsonProvider;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;
import org.operaton.bpm.engine.rest.mapper.MultipartPayloadProvider;

/**
 * <p>Class providing static methods returning all the resource classes provided by Operaton.</p>
 *
 * @author Daniel Meyer
 *
 */
public final class OperatonRestResources {

  private static final Set<Class<?>> RESOURCE_CLASSES = new HashSet<>();

  private static final Set<Class<?>> CONFIGURATION_CLASSES = new HashSet<>();

  static {
    RESOURCE_CLASSES.add(NamedProcessEngineRestServiceImpl.class);
    RESOURCE_CLASSES.add(DefaultProcessEngineRestServiceImpl.class);

    CONFIGURATION_CLASSES.add(JacksonConfigurator.class);
    CONFIGURATION_CLASSES.add(JacksonJsonProvider.class);
    CONFIGURATION_CLASSES.add(JsonMappingExceptionHandler.class);
    CONFIGURATION_CLASSES.add(JsonParseExceptionHandler.class);
    CONFIGURATION_CLASSES.add(ProcessEngineExceptionHandler.class);
    CONFIGURATION_CLASSES.add(RestExceptionHandler.class);
    CONFIGURATION_CLASSES.add(MultipartPayloadProvider.class);
    CONFIGURATION_CLASSES.add(JacksonHalJsonProvider.class);
    CONFIGURATION_CLASSES.add(ExceptionHandler.class);
  }

  private OperatonRestResources() {
  }

  /**
   * Returns a set containing all resource classes provided by Operaton.
   * @return a set of resource classes.
   */
  public static Set<Class<?>> getResourceClasses() {
    return RESOURCE_CLASSES;
  }

  /**
   * Returns a set containing all provider / mapper / config classes used in the
   * default setup of the operaton REST api.
   * @return a set of provider / mapper / config classes.
   */
  public static Set<Class<?>> getConfigurationClasses() {
    return CONFIGURATION_CLASSES;
  }
}
