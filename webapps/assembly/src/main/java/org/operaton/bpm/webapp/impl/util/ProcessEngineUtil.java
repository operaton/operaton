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
package org.operaton.bpm.webapp.impl.util;

import java.util.Iterator;
import java.util.ServiceLoader;
import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.spi.ProcessEngineProvider;

public final class ProcessEngineUtil {

  private ProcessEngineUtil() {
  }

  public static ProcessEngine lookupProcessEngine(String engineName) {
    ServiceLoader<ProcessEngineProvider> serviceLoader = ServiceLoader.load(ProcessEngineProvider.class);
    Iterator<ProcessEngineProvider> iterator = serviceLoader.iterator();

    if(iterator.hasNext()) {
      ProcessEngineProvider provider = iterator.next();
      return provider.getProcessEngine(engineName);
    } else {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, "Could not find an implementation of the %s - SPI".formatted(ProcessEngineProvider.class));
    }
  }

}
