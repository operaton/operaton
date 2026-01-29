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
package org.operaton.bpm.engine.spring;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.UrlResource;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;

/**
 * @author Tom Baeyens
 */
public final class SpringConfigurationHelper {

  private static final Logger log = Logger.getLogger(SpringConfigurationHelper.class.getName());

  private SpringConfigurationHelper() {
  }

  public static ProcessEngine buildProcessEngine(URI resource) {
    try {
      return buildProcessEngine(resource.toURL());
    } catch (java.io.IOException e) {
      throw new org.operaton.bpm.engine.ProcessEngineException(
          "couldn't open resource stream: %s".formatted(e.getMessage()), e);
    }
  }

  public static ProcessEngine buildProcessEngine(URL resource) {
    log.fine("==== BUILDING SPRING APPLICATION CONTEXT AND PROCESS ENGINE =========================================");

    ApplicationContext applicationContext = new GenericXmlApplicationContext(new UrlResource(resource));
    Map<String, ProcessEngine> beansOfType = applicationContext.getBeansOfType(ProcessEngine.class);
    if (beansOfType.isEmpty()) {
      throw new ProcessEngineException("no " + ProcessEngine.class.getName() + " defined in the application context " + resource.toString());
    }

    ProcessEngine processEngine = beansOfType.values().iterator().next();

    log.fine("==== SPRING PROCESS ENGINE CREATED ==================================================================");
    return processEngine;
  }
}
