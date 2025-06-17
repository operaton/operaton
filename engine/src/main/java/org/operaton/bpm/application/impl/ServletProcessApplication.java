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
package org.operaton.bpm.application.impl;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.operaton.bpm.application.AbstractProcessApplication;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.util.ClassLoaderUtil;

import java.lang.ref.WeakReference;

/**
 * <p>
 * An {@link AbstractProcessApplication} implementation to be used in a Servlet
 * container environment.
 * </p>
 *
 * <p>
 * This class implements the {@link ServletContextListener} interface and can
 * thus participate in the deployment lifecycle of your web application.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * It is sufficient adding a custom
 * subclass of {@link ServletProcessApplication} annotated with
 * <code>{@literal @}ProcessApplication</code> and <code>{@literal @}WebListener</code>
 * to your application:
 *
 * <pre>
 * {@literal @}ProcessApplication("Loan Approval App")
 * {@literal @}WebListener
 * public class LoanApprovalApplication extends ServletProcessApplication {
 * // empty implementation
 * }
 * </pre>
 *
 * This, in combination with a <code>META-INF/processes.xml</code> file is
 * sufficient for making sure that the process application class is picked up at
 * runtime.
 * </p>
 *
 * <h2>Invocation Semantics</h2>
 * <p>
 * When the {@link #execute(java.util.concurrent.Callable)} method is invoked,
 * the servlet process application modifies the context classloader of the
 * current Thread to the classloader that loaded the application-provided
 * subclass of this class. This allows
 * <ul>
 * <li>the process engine to resolve {@link JavaDelegate} implementations using
 * the classloader of the process application</li>
 * <li>In apache tomcat this allows you to resolve Naming Resources (JNDI) form
 * the naming context of the process application. JNDI name resolution is based
 * on the TCCL in Apache Tomcat.</li>
 * </ul>
 * </p>
 *
 *
 * <pre>
 *                        Set TCCL of Process Application
 *                                     |
 *                                     |  +--------------------+
 *                                     |  |Process Application |
 *                       invoke        v  |                    |
 *      ProcessEngine -----------------O--|--> Java Delegate   |
 *                                        |                    |
 *                                        |                    |
 *                                        +--------------------+
 *
 * </pre>
 *
 * <h2>Process Application Reference</h2>
 * <p>
 * The process engine holds a {@link WeakReference} to the
 * {@link ServletProcessApplication} and does not cache any classes loaded using
 * the Process Application classloader.
 * </p>
 *
 *
 * @author Daniel Meyer
 * @author Thorben Lindhauer
 *
 */
public class ServletProcessApplication extends AbstractServletProcessApplication implements ServletContextListener {

  protected ServletContext servletContext;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    servletContext = sce.getServletContext();
    servletContextPath = servletContext.getContextPath();
    servletContextName = sce.getServletContext().getServletContextName();

    processApplicationClassloader = initProcessApplicationClassloader(sce);

    // perform lifecycle start
    deploy();
  }

  protected ClassLoader initProcessApplicationClassloader(ServletContextEvent sce) {
    if (isServlet30ApiPresent(sce) && getClass().equals(ServletProcessApplication.class)) {
      return ClassLoaderUtil.getServletContextClassloader(sce);
    } else {
      return ClassLoaderUtil.getClassloader(getClass());
    }
  }

  private boolean isServlet30ApiPresent(ServletContextEvent sce) {
    return sce.getServletContext().getMajorVersion() >= 3;
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // perform lifecycle stop
    undeploy();

    // clear the reference
    if (reference != null) {
      reference.clear();
    }
    reference = null;
  }

  public ServletContext getServletContext() {
    return servletContext;
  }
}
