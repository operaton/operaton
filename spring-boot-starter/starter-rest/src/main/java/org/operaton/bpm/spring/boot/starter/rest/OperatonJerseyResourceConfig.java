/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.spring.boot.starter.rest;

import jakarta.ws.rs.ApplicationPath;
import org.operaton.bpm.engine.rest.impl.OperatonRestResources;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

@ApplicationPath("/engine-rest")
public class OperatonJerseyResourceConfig extends ResourceConfig implements InitializingBean {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(OperatonJerseyResourceConfig.class);

  @Override
  public void afterPropertiesSet() throws Exception {
    registerOperatonRestResources();
    registerAdditionalResources();
  }

  protected void registerOperatonRestResources() {
    log.info("Configuring operaton rest api.");

    this.registerClasses(OperatonRestResources.getResourceClasses());
    this.registerClasses(OperatonRestResources.getConfigurationClasses());
    this.register(JacksonFeature.class);

    log.info("Finished configuring operaton rest api.");
  }

  protected void registerAdditionalResources() {

  }

}
