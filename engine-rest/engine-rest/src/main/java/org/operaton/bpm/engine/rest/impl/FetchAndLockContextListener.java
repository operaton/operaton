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

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.ws.rs.core.Response;

import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.spi.FetchAndLockHandler;

/**
 * @author Tassilo Weidner
 */
public class FetchAndLockContextListener implements ServletContextListener {

  private static final AtomicReference<FetchAndLockHandler> fetchAndLockHandler = new AtomicReference<>();

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    if (fetchAndLockHandler.get() == null) {
      var handler = lookupFetchAndLockHandler();
      handler.contextInitialized(sce);
      handler.start();
      fetchAndLockHandler.set(handler);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    fetchAndLockHandler.getAndSet(null).shutdown();
  }

  public static FetchAndLockHandler getFetchAndLockHandler() {
    return fetchAndLockHandler.get();
  }

  private static FetchAndLockHandler lookupFetchAndLockHandler() {
    ServiceLoader<FetchAndLockHandler> serviceLoader = ServiceLoader.load(FetchAndLockHandler.class);
    Iterator<FetchAndLockHandler> iterator = serviceLoader.iterator();
    if(iterator.hasNext()) {
      return iterator.next();
    } else {
      throw new RestException(Response.Status.INTERNAL_SERVER_ERROR,
        "Could not find an implementation of the %s - SPI".formatted(FetchAndLockHandler.class.getSimpleName()));
    }
  }

}
