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
package org.operaton.bpm.application.impl.web;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.Objects;
import org.operaton.bpm.application.ProcessApplicationInfo;
import org.operaton.bpm.application.ProcessApplicationInterface;

/**
 * <p>Sets the ProcessApplicationInfo.PROP_SERVLET_CONTEXT_PATH property if this is
 * deployed as part of a WebApplication.</p>
 *
 * @author Daniel Meyer
 *
 */
public class ProcessArchiveServletContextListener implements ServletContextListener {

  @EJB
  private ProcessApplicationInterface defaultEjbProcessApplication;

  @Override
  public void contextInitialized(ServletContextEvent contextEvent) {
    String contextPath = contextEvent.getServletContext().getContextPath();

    Objects.requireNonNull(defaultEjbProcessApplication, "Cannot inject ProcessApplicationInterface EJB. Make sure the ProcessApplication is deployed as EJB.");
    defaultEjbProcessApplication.getProperties().put(ProcessApplicationInfo.PROP_SERVLET_CONTEXT_PATH, contextPath);
  }

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
    // nothing to do
  }

}
