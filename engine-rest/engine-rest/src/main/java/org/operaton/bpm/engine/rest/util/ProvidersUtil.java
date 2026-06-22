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
package org.operaton.bpm.engine.rest.util;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.operaton.bpm.engine.rest.exception.RestException;

/**
 * @author Thorben Lindhauer
 *
 */
public final class ProvidersUtil {

  private ProvidersUtil() {
  }

  public static <T> T resolveFromContext(Providers providers, Class<T> clazz) {
    return resolveFromContext(providers, clazz, null);
  }

  public static <T> T resolveFromContext(Providers providers, Class<T> clazz, Class<?> type) {
    return resolveFromContext(providers, clazz, null, type);
  }

  public static <T> T resolveFromContext(Providers providers, Class<T> clazz, MediaType mediaType, Class<?> type) {
    ContextResolver<T> contextResolver = providers.getContextResolver(clazz, mediaType);

    if (contextResolver == null) {
      throw new RestException("No context resolver found for class %s".formatted(clazz.getName()));
    }

    return contextResolver.getContext(type);
  }
}
