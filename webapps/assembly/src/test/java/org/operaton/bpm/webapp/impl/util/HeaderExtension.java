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
package org.operaton.bpm.webapp.impl.util;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/**
 * JUnit 5 extension for managing a Jetty server during tests.
 * 
 * <p>Replaces the JUnit 4 rule-based approach with the JUnit 5 extension mechanism.</p>
 * 
 * <p>Usage:</p>
 * <pre>
 * &#64;ExtendWith(HeaderExtension.class)
 * class MyTest {
 *     // Tests that use the server
 * }
 * </pre>
 * 
 * @author Tassilo Weidner
 */
public class HeaderExtension implements BeforeEachCallback, AfterEachCallback {

    private static final int SERVER_PORT = 8085;
    private static final int RETRIES = 3;

    private final Server server = new Server(SERVER_PORT);
    private final WebAppContext webAppContext = new WebAppContext();
    private HttpURLConnection connection = null;

    @Override
    public void beforeEach(ExtensionContext context) {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startServer(String webDescriptor, String scope) {
        startServer(webDescriptor, scope, "/operaton");
    }

    public void startServer(String webDescriptor, String scope, String contextPath) {
        startServer(webDescriptor, scope, contextPath, RETRIES);
    }

    private void startServer(String webDescriptor, String scope, String contextPath, int startUpRetries) {
        webAppContext.setContextPath(contextPath);
        webAppContext.setResourceBase("/");
        webAppContext.setDescriptor("src/test/resources/WEB-INF/" + scope + "/" + webDescriptor);

        server.setHandler(webAppContext);

        try {
            server.start();
        } catch (Exception e) {
            if (e.getCause() instanceof BindException && startUpRetries > 0) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                startServer(webDescriptor, scope, contextPath, --startUpRetries);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public void performRequest() {
        performRequestWithHeader(null, null, "", null);
    }

    public void performPostRequest(String path) {
        performRequestWithHeader(null, null, path, "POST");
    }

    public void performRequestWithHeader(String name, String value) {
        performRequestWithHeader(name, value, "", null);
    }

    public void performRequestWithHeader(String name, String value, String path, String method) {
        try {
            connection = (HttpURLConnection) new URL("http://localhost:" + SERVER_PORT + "/operaton" + path)
                    .openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if ("POST".equals(method)) {
            try {
                connection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            }
        }

        if (name != null && value != null) {
            connection.setRequestProperty(name, value);
        }

        try {
            connection.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getHeader(String headerName) {
        return connection.getHeaderField(headerName);
    }

    public String getCookieHeader() {
        return connection.getHeaderField("Set-Cookie");
    }

    public Throwable getException() {
        return webAppContext.getUnavailableException();
    }

    public boolean headerExists(String name) {
        return connection.getHeaderFields().containsKey(name);
    }

    public String getResponseBody() {
        try {
            return connection.getResponseMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getResponseCode() {
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSessionCookieRegex(String path, String sameSite, boolean secure) {
        return getSessionCookieRegex(path, "JSESSIONID", sameSite, secure);
    }

    public String getSessionCookieRegex(String path, String cookieName, String sameSite, boolean secure) {
        StringBuilder regex = new StringBuilder(cookieName + "=.*;\\W*Path=/");
        if (path != null) {
            regex.append(path);
        }
        if (sameSite != null) {
            regex.append(";\\W*SameSite=").append(sameSite);
        }
        if (secure) {
            regex.append(";\\W*Secure");
        }
        return regex.toString();
    }
}
