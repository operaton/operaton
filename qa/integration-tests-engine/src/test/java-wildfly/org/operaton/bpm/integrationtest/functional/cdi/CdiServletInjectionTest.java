/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.integrationtest.functional.cdi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.integrationtest.functional.cdi.beans.IdentityServiceInjectingProcessApplication;
import org.operaton.bpm.integrationtest.functional.cdi.beans.IdentityServiceInjectingServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Regression test for WELD-001408 when injecting IdentityService into a Servlet
 * in a Process Application on WildFly.
 */
@ExtendWith(ArquillianExtension.class)
public class CdiServletInjectionTest {

  private static final String DEPLOYMENT_NAME = "cdi-identity-service-servlet-injection";
  private static final String JBOSS_HTTP_PORT_PROPERTY = "jboss.http.port";
  private static final Duration SERVLET_RESPONSE_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration SERVLET_RESPONSE_POLL_INTERVAL = Duration.ofMillis(250);

  @ArquillianResource
  private Deployer deployer;

  @Deployment(name = DEPLOYMENT_NAME, managed = false)
  public static WebArchive createDeployment() {
    return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war")
      .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
      .addAsManifestResource("jboss-deployment-structure.xml")
      .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
      .addClass(IdentityServiceInjectingServlet.class)
      .addClass(IdentityServiceInjectingProcessApplication.class);
  }

  @Test
  @RunAsClient
  void shouldDeployWithIdentityServiceServletInjectionUsingServerModule() throws Exception {
    assertDeploymentSucceeds(DEPLOYMENT_NAME,
      "Deployment should be successful with operaton-engine-cdi provided by WildFly module");
  }

  private void assertDeploymentSucceeds(String deploymentName, String failureMessage) throws Exception {
    try {
      try {
        deployer.deploy(deploymentName);
      } catch (Exception exception) {
        throw new AssertionError(failureMessage
          + System.lineSeparator()
          + formatExceptionChain(exception), exception);
      }

      assertIdentityServiceServletResponse(deploymentName);
    } finally {
      try {
        deployer.undeploy(deploymentName);
      } catch (Exception ignored) {
        // deployment may fail before being installed
      }
    }
  }

  private int resolveHttpPort() {
    String httpPortProperty = System.getProperty(JBOSS_HTTP_PORT_PROPERTY);
    if (httpPortProperty == null || httpPortProperty.isBlank()) {
      throw new IllegalStateException("Missing system property '" + JBOSS_HTTP_PORT_PROPERTY + "'");
    }

    try {
      return Integer.parseInt(httpPortProperty);
    } catch (NumberFormatException exception) {
      throw new IllegalStateException(
        "System property '" + JBOSS_HTTP_PORT_PROPERTY + "' is not a valid integer: " + httpPortProperty,
        exception);
    }
  }

  private void assertIdentityServiceServletResponse(String deploymentName) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    int httpPort = resolveHttpPort();
    URI servletUrl = URI.create("http://127.0.0.1:" + httpPort + "/" + deploymentName + "/identity-service-check");
    HttpRequest request = HttpRequest.newBuilder(servletUrl).GET().build();
    AtomicReference<HttpResponse<String>> responseReference = new AtomicReference<>();

    try {
      await()
        .atMost(SERVLET_RESPONSE_TIMEOUT)
        .pollInterval(SERVLET_RESPONSE_POLL_INTERVAL)
        .ignoreExceptions()
        .until(() -> {
          HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
          responseReference.set(response);
          return response.statusCode() == 200 && "identityService=ok".equals(response.body());
        });
    } catch (Exception exception) {
      throw new AssertionError("Servlet check failed for URL " + servletUrl
        + " within timeout " + SERVLET_RESPONSE_TIMEOUT
        + ". Last observed response: " + formatResponse(responseReference.get())
        + System.lineSeparator()
        + formatExceptionChain(exception), exception);
    }

    HttpResponse<String> response = responseReference.get();
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("identityService=ok");
  }

  private String formatResponse(HttpResponse<String> response) {
    if (response == null) {
      return "<no response>";
    }

    return "status=" + response.statusCode() + ", body=\"" + response.body() + "\"";
  }

  private String formatExceptionChain(Throwable throwable) {
    StringBuilder builder = new StringBuilder("Exception chain:");
    Throwable current = throwable;
    int depth = 0;

    while (current != null && depth < 20) {
      builder.append(System.lineSeparator())
        .append("  [").append(depth).append("] ")
        .append(current.getClass().getName())
        .append(": ")
        .append(current.getMessage() == null ? "<no message>" : current.getMessage());

      current = current.getCause();
      depth++;
    }

    return builder.toString();
  }
}
