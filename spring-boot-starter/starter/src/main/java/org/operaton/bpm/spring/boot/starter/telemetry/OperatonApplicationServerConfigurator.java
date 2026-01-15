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
package org.operaton.bpm.spring.boot.starter.telemetry;

import jakarta.servlet.ServletContext;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ManagementServiceImpl;


public class OperatonApplicationServerConfigurator implements InitializingBean {

  protected ProcessEngine processEngine;

  protected ApplicationContext applicationContext;

  public OperatonApplicationServerConfigurator(ProcessEngine processEngine, ApplicationContext applicationContext) {
    this.processEngine = processEngine;
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() {
    try {
      ServletContext servletContext = (ServletContext) applicationContext.getBean("servletContext");
      String serverInfo = servletContext.getServerInfo();
      if (serverInfo != null) {
        ((ManagementServiceImpl) processEngine.getManagementService()).addApplicationServerInfoToTelemetry(serverInfo);
      }
    } catch (Exception ignored) {
      // ignore
    }
  }
}
