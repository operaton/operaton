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
package org.operaton.bpm.run.qa.webapps;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.operaton.bpm.run.qa.util.SpringBootManagedContainer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

/**
 * NOTE:
 * copied from
 * <a href="https://github.com/operaton/operaton/blob/main/qa/integration-tests-webapps/integration-tests/src/main/java/org/operaton/bpm/LoginIT.java">platform</a>
 * then added <code>@BeforeParam</code> and <code>@AfterParam</code> methods for container setup
 * and <code>@Parameters</code> for different setups, might be removed with
 * <a href="https://jira.camunda.com/browse/CAM-11379">CAM-11379</a>
 */
@Disabled("Fix Chrome driver download: Chrome driver is outdated and must be downloaded in the version installed on the local machine")
class LoginIT extends AbstractWebappUiIT {
  String[] commands;

  static Collection<Object[]> commands() {
    return Arrays.asList(new Object[][] {
      { new String[0] },
      { new String[]{"--rest", "--webapps"} },
      { new String[]{"--webapps"} }
    });
  }


  String name;

  protected SpringBootManagedContainer container;

  protected WebDriverWait wait;

  void startContainer(String[] commands) {
    container = new SpringBootManagedContainer(commands);
    try {
      container.start();
    } catch (Exception e) {
      throw new RuntimeException("Cannot start managed Spring Boot application!", e);
    }
  }

  @AfterEach
  void stopContainer() {
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
  void shouldLoginToCockpit(String[] commands) {
    startContainer(commands);
    initLoginIT(commands);
    assertThatCode(() -> {
      try {
        loginToCockpit();
      } catch (WebDriverException e) {
        loginToCockpit();
      }
    }).doesNotThrowAnyException();
  }

  void loginToCockpit() {
    String appName = "cockpit";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".deployed .processes .stats-label"),
        "Process Definitions"));

    wait.until(currentURIIs(URI.create(appUrl + "app/" + appName + "/default/#/dashboard")));
  }

  @MethodSource("commands")
  @ParameterizedTest
  void shouldLoginToTasklist(String[] commands) {
    startContainer(commands);
    initLoginIT(commands);
    assertThatCode(() -> {
      try {
        loginToTasklist();
      } catch (WebDriverException e) {
        loginToTasklist();
      }
    }).doesNotThrowAnyException();
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
  void shouldLoginToAdmin(String[] commands) {
    initLoginIT(commands);
    assertThatCode(() -> {
      try {
        loginToAdmin();
      } catch (WebDriverException e) {
        loginToAdmin();
      }
    }).doesNotThrowAnyException();
  }

  void loginToAdmin() {
    String appName = "admin";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector("[ng-class=\"activeClass('#/authorization')\"] a"),
        "Authorizations"));

    wait.until(currentURIIs(URI.create(appUrl + "app/" + appName + "/default/#/")));
  }

  @MethodSource("commands")
  @ParameterizedTest
  void shouldLoginToWelcome(String[] commands) {
    initLoginIT(commands);
    assertThatCode(() -> {
      try {
        loginToWelcome();
      } catch (WebDriverException e) {
        loginToWelcome();
      }
    }).doesNotThrowAnyException();
  }

  void loginToWelcome() {
    String appName = "welcome";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".webapps .section-title"),
        "Applications"));

    wait.until(currentURIIs(URI.create(appUrl + "app/" + appName + "/default/#!/welcome")));
  }

  @BeforeEach
  void setup(TestInfo testInfo) {
    Optional<Method> testMethod = testInfo.getTestMethod();
    testMethod.ifPresent(method -> this.name = method.getName());
  }

  void initLoginIT(String[] commands) {
    this.commands = commands;
  }

}
