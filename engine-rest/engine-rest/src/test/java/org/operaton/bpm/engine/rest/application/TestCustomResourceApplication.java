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
package org.operaton.bpm.engine.rest.application;

import com.fasterxml.jackson.jakarta.rs.base.JsonMappingExceptionMapper;
import com.fasterxml.jackson.jakarta.rs.base.JsonParseExceptionMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.operaton.bpm.engine.rest.exception.ExceptionHandler;
import org.operaton.bpm.engine.rest.exception.ProcessEngineExceptionHandler;
import org.operaton.bpm.engine.rest.exception.RestExceptionHandler;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
public class TestCustomResourceApplication extends Application {

  private static final Set<Class<?>> RESOURCES = new HashSet<>();
  private static final Set<Class<?>> PROVIDERS = new HashSet<>();

  static {
    // RESOURCES
    RESOURCES.add(UnannotatedResource.class);

    // PROVIDERS
    PROVIDERS.add(ConflictingProvider.class);

    PROVIDERS.add(JacksonConfigurator.class);

    PROVIDERS.add(JacksonJsonProvider.class);
    PROVIDERS.add(JsonMappingExceptionMapper.class);
    PROVIDERS.add(JsonParseExceptionMapper.class);

    PROVIDERS.add(ProcessEngineExceptionHandler.class);
    PROVIDERS.add(RestExceptionHandler.class);
    PROVIDERS.add(ExceptionHandler.class);
  }

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();
    classes.addAll(RESOURCES);
    classes.addAll(PROVIDERS);

    return classes;
  }

  public static Set<Class<?>> getResourceClasses() {
    return RESOURCES;
  }

  public static Set<Class<?>> getProviderClasses() {
    return PROVIDERS;
  }
}
