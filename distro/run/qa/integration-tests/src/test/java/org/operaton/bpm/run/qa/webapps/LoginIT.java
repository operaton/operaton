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
package org.operaton.bpm.run.qa.webapps;

import org.operaton.bpm.run.qa.util.SpringBootManagedContainer;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

/**
 * NOTE:
 * copied from
 * <a href="https://github.com/operaton/operaton/blob/main/qa/integration-tests-webapps/integration-tests/src/main/java/org/operaton/bpm/LoginIT.java">platform</a>
 * then added <code>@BeforeParam</code> and <code>@AfterParam</code> methods for container setup
 * and <code>@Parameters</code> for different setups, might be removed with https://jira.camunda.com/browse/CAM-11379
 */
public class LoginIT extends AbstractWebappUiIT {
  public String[] commands;

  public static Collection<Object[]> commands() {
    return Arrays.asList(new Object[][] {
      { new String[0] },
      { new String[]{"--rest", "--webapps"} },
      { new String[]{"--webapps"} }
    });
  }

  
  public String name;

  protected SpringBootManagedContainer container;

  protected WebDriverWait wait;

  public void startContainer(String[] commands) {
    container = new SpringBootManagedContainer(commands);
    try {
      container.start();
    } catch (Exception e) {
      throw new RuntimeException("Cannot start managed Spring Boot application!", e);
    }
  }

  @AfterEach
  public void stopContainer() {
    try {
      if (container != null) {
        container.stop();
      }
    } catch (Exception e) {
      throw new RuntimeException("Cannot stop managed Spring Boot application!", e);
    } finally {
      container = null;
    }
  }

  void login(String appName) {
    driver.manage().deleteAllCookies();

    driver.get(appUrl + "app/" + appName + "/default/");

    wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    WebElement userNameInput = wait.until(visibilityOfElementLocated(By.cssSelector("input[type=\"text\"]")));
    sendKeys(userNameInput, "demo");

    WebElement passwordInput = wait.until(visibilityOfElementLocated(By.cssSelector("input[type=\"password\"]")));
    sendKeys(passwordInput, "demo");

    wait.until(visibilityOfElementLocated(By.cssSelector("button[type=\"submit\"]")))
        .submit();
  }

  void sendKeys(WebElement element, String keys)  {
    // fix for CAM-13548
    Arrays.stream(keys.split("")).forEach(element::sendKeys);
  }

  @MethodSource("commands")
  @ParameterizedTest
  public void shouldLoginToCockpit(String[] commands) throws URISyntaxException {
    startContainer(commands);
    initLoginIT(commands);
    try {
      loginToCockpit();
    } catch (WebDriverException e) {
      loginToCockpit();
    }
  }

  public void loginToCockpit() throws URISyntaxException {
    String appName = "cockpit";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".deployed .processes .stats-label"),
        "Process Definitions"));

    wait.until(currentURIIs(new URI(appUrl + "app/"
        + appName + "/default/#/dashboard")));
  }

  @MethodSource("commands")
  @ParameterizedTest
  public void shouldLoginToTasklist(String[] commands) {
    startContainer(commands);
    initLoginIT(commands);
    try {
      loginToTasklist();
    } catch (WebDriverException e) {
      loginToTasklist();
    }
  }

  void loginToTasklist() {
    String appName = "tasklist";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".start-process-action view a"),
        "Start process"));

    wait.until(containsCurrentUrl(appUrl + "app/"
        + appName + "/default/#/?searchQuery="));
  }

  @MethodSource("commands")
  @ParameterizedTest
  public void shouldLoginToAdmin(String[] commands) throws URISyntaxException {
    initLoginIT(commands);
    try {
      loginToAdmin();
    } catch (WebDriverException e) {
      loginToAdmin();
    }
  }

  void loginToAdmin() throws URISyntaxException {
    String appName = "admin";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector("[ng-class=\"activeClass('#/authorization')\"] a"),
        "Authorizations"));

    wait.until(currentURIIs(new URI(appUrl
        + "app/" + appName + "/default/#/")));
  }

  @MethodSource("commands")
  @ParameterizedTest
  public void shouldLoginToWelcome(String[] commands) throws URISyntaxException {
    initLoginIT(commands);
    try {
      loginToWelcome();
    } catch (WebDriverException e) {
      loginToWelcome();
    }
  }

  public void loginToWelcome() throws URISyntaxException {
    String appName = "welcome";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".webapps .section-title"),
        "Applications"));

    wait.until(currentURIIs(new URI(appUrl
        + "app/" + appName + "/default/#!/welcome")));
  }

  @BeforeEach
  void setup(TestInfo testInfo) {
    Optional<Method> testMethod = testInfo.getTestMethod();
    testMethod.ifPresent(method -> this.name = method.getName());
  }

  public void initLoginIT(String[] commands) {
    this.commands = commands;
  }

}
