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
package org.operaton.bpm.engine.rest.util.container;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import jakarta.ws.rs.core.Application;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.bpm.engine.rest.CustomJacksonDateFormatTest;
import org.operaton.bpm.engine.rest.ExceptionHandlerTest;
import org.operaton.bpm.engine.rest.application.TestCustomResourceApplication;
import org.operaton.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.operaton.bpm.engine.rest.standalone.NoServletAuthenticationFilterTest;
import org.operaton.bpm.engine.rest.standalone.NoServletEmptyBodyFilterTest;
import org.operaton.bpm.engine.rest.standalone.ServletAuthenticationFilterTest;
import org.operaton.bpm.engine.rest.standalone.ServletEmptyBodyFilterTest;

/**
 * @author Thorben Lindhauer
 */
public class ResteasySpecifics implements ContainerSpecifics {

  protected static final TestRuleFactory DEFAULT_RULE_FACTORY = new EmbeddedServerRuleFactory(new JaxrsApplication());

  protected static final Map<Class<?>, TestRuleFactory> TEST_RULE_FACTORIES = new HashMap<>();

  static {
    TEST_RULE_FACTORIES.put(ExceptionHandlerTest.class,
        new EmbeddedServerRuleFactory(new TestCustomResourceApplication()));

    TEST_RULE_FACTORIES.put(ServletAuthenticationFilterTest.class, new UndertowServletContainerRuleFactory(
        Servlets.deployment()
            .setDeploymentName("rest-test.war")
            .setContextPath("/rest-test/rest")
            .setClassLoader(ResteasyUndertowServerBootstrap.class.getClassLoader())
            .addListener(Servlets.listener(ResteasyBootstrap.class))
            .addFilter(Servlets.filter("operaton-auth", ProcessEngineAuthenticationFilter.class)
                .addInitParam("authentication-provider",
                    "org.operaton.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider"))
            .addFilterUrlMapping("operaton-auth", "/*", DispatcherType.REQUEST)
            .addServlet(Servlets.servlet("operaton-app", HttpServletDispatcher.class)
                .addMapping("/*")
                .addInitParam("jakarta.ws.rs.Application",
                    "org.operaton.bpm.engine.rest.util.container.JaxrsApplication"))));

    TEST_RULE_FACTORIES.put(NoServletAuthenticationFilterTest.class, new UndertowServletContainerRuleFactory(
        Servlets.deployment()
            .setDeploymentName("rest-test.war")
            .setContextPath("/rest-test/rest")
            .setClassLoader(ResteasyUndertowServerBootstrap.class.getClassLoader())
            .addListener(Servlets.listener(ResteasyBootstrap.class))
            .addFilter(Servlets.filter("operaton-auth", ProcessEngineAuthenticationFilter.class)
                .addInitParam("authentication-provider",
                    "org.operaton.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider")
                .addInitParam("rest-url-pattern-prefix", ""))
            .addFilterUrlMapping("operaton-auth", "/*", DispatcherType.REQUEST)
            .addFilter(Servlets.filter("Resteasy", FilterDispatcher.class)
                .addInitParam("jakarta.ws.rs.Application",
                    "org.operaton.bpm.engine.rest.util.container.JaxrsApplication"))
            .addFilterUrlMapping("Resteasy", "/*", DispatcherType.REQUEST)));

    TEST_RULE_FACTORIES.put(ServletEmptyBodyFilterTest.class, new UndertowServletContainerRuleFactory(
        Servlets.deployment()
            .setDeploymentName("rest-test.war")
            .setContextPath("/rest-test/rest")
            .setClassLoader(ResteasyUndertowServerBootstrap.class.getClassLoader())
            .addListener(Servlets.listener(ResteasyBootstrap.class))
            .addFilter(Servlets.filter("EmptyBodyFilter", org.operaton.bpm.engine.rest.filter.EmptyBodyFilter.class)
                .addInitParam("rest-url-pattern-prefix", ""))
            .addFilterUrlMapping("EmptyBodyFilter", "/*", DispatcherType.REQUEST)
            .addServlet(Servlets.servlet("operaton-app", HttpServletDispatcher.class)
                .addMapping("/*")
                .addInitParam("jakarta.ws.rs.Application",
                    "org.operaton.bpm.engine.rest.util.container.JaxrsApplication"))));

    TEST_RULE_FACTORIES.put(NoServletEmptyBodyFilterTest.class, new UndertowServletContainerRuleFactory(
        Servlets.deployment()
            .setDeploymentName("rest-test.war")
            .setContextPath("/rest-test/rest")
            .setClassLoader(ResteasyUndertowServerBootstrap.class.getClassLoader())
            .addListener(Servlets.listener(ResteasyBootstrap.class))
            .addFilter(Servlets.filter("EmptyBodyFilter", org.operaton.bpm.engine.rest.filter.EmptyBodyFilter.class)
                .addInitParam("rest-url-pattern-prefix", ""))
            .addFilterUrlMapping("EmptyBodyFilter", "/*", DispatcherType.REQUEST)
            .addFilter(Servlets.filter("Resteasy", FilterDispatcher.class)
                .addInitParam("jakarta.ws.rs.Application",
                    "org.operaton.bpm.engine.rest.util.container.JaxrsApplication"))
            .addFilterUrlMapping("Resteasy", "/*", DispatcherType.REQUEST)));

    TEST_RULE_FACTORIES.put(CustomJacksonDateFormatTest.class, new UndertowServletContainerRuleFactory(
        Servlets.deployment()
            .setDeploymentName("rest-test.war")
            .setContextPath("/rest-test")
            .setClassLoader(ResteasyUndertowServerBootstrap.class.getClassLoader())
            .addListener(Servlets.listener(ResteasyBootstrap.class))
            .addListener(Servlets.listener(org.operaton.bpm.engine.rest.CustomJacksonDateFormatListener.class))
            .addInitParameter("org.operaton.bpm.engine.rest.jackson.dateFormat", "yyyy-MM-dd'T'HH:mm:ss")
            .addFilter(Servlets.filter("Resteasy", FilterDispatcher.class)
                .addInitParam("jakarta.ws.rs.Application",
                    "org.operaton.bpm.engine.rest.util.container.JaxrsApplication"))
            .addFilterUrlMapping("Resteasy", "/*", DispatcherType.REQUEST)));
  }

  @Override
  public Extension getExtension(Class<?> testClass) {
    TestRuleFactory ruleFactory = DEFAULT_RULE_FACTORY;

    if (TEST_RULE_FACTORIES.containsKey(testClass)) {
      ruleFactory = TEST_RULE_FACTORIES.get(testClass);
    }

    return ruleFactory.createExtension();
  }

  public static class EmbeddedServerRuleFactory implements TestRuleFactory {

    protected Application jaxRsApplication;

    public EmbeddedServerRuleFactory(Application jaxRsApplication) {
      this.jaxRsApplication = jaxRsApplication;
    }

    @Override
    public Extension createExtension() {
      return new BeforeAfterExtension(new ResteasyServerBootstrap(jaxRsApplication));
    }
  }

  public static class UndertowServletContainerRuleFactory implements TestRuleFactory {

    protected DeploymentInfo deploymentInfo;

    public UndertowServletContainerRuleFactory(DeploymentInfo deploymentInfo) {
      this.deploymentInfo = deploymentInfo;
    }

    @Override
    public Extension createExtension() {
      return new BeforeAfterExtension(new ResteasyUndertowServerBootstrap(deploymentInfo));
    }

  }

  protected static class BeforeAfterExtension implements BeforeAllCallback, AfterAllCallback, Extension {

    protected final AbstractServerBootstrap bootstrap;

    protected BeforeAfterExtension(AbstractServerBootstrap bootstrap) {
      this.bootstrap = bootstrap;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
      bootstrap.start();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
      bootstrap.stop();
    }
  }

}
