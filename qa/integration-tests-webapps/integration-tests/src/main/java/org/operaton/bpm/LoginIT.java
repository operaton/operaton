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
package org.operaton.bpm;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

public class LoginIT extends AbstractWebappUiIntegrationTest {

  protected WebDriverWait wait;

  public void login(String appName) {
    driver.manage().deleteAllCookies();

    driver.get("%sapp/%s/default/".formatted(appUrl, appName));

    wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    WebElement userNameInput = wait.until(visibilityOfElementLocated(By.cssSelector("input[type=\"text\"]")));
    sendKeys(userNameInput, "demo");

    WebElement passwordInput = wait.until(visibilityOfElementLocated(By.cssSelector("input[type=\"password\"]")));
    sendKeys(passwordInput, "demo");

    wait.until(visibilityOfElementLocated(By.cssSelector("button[type=\"submit\"]")))
        .submit();
  }

  public void sendKeys(WebElement element, String keys)  {
    // fix for CAM-13548
    Arrays.stream(keys.split("")).forEach(element::sendKeys);
  }

  @Test
  void shouldLoginToCockpit() {
    assertThatCode(() -> {
      try {
        loginToCockpit();
      } catch (WebDriverException e) {
        loginToCockpit();
      }
    }).doesNotThrowAnyException();
  }

  public void loginToCockpit() {
    String appName = "cockpit";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".deployed .processes .stats-label"),
        "Process Definitions"));

    wait.until(currentURIIs(URI.create("%sapp/%s/default/#/dashboard".formatted(appUrl, appName))));
  }

  @Test
  void shouldLoginToTasklist() {
    assertThatCode(() -> {
      try {
        loginToTasklist();
      } catch (WebDriverException e) {
        loginToTasklist();
      }
    }).doesNotThrowAnyException();
  }

  public void loginToTasklist() {
    String appName = "tasklist";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".start-process-action view a"),
        "Start process"));

    wait.until(containsCurrentUrl(appUrl + "app/"
        + appName + "/default/#/?searchQuery="));
  }

  @Test
  void shouldLoginToAdmin() {
    assertThatCode(() -> {
      try {
        loginToAdmin();
      } catch (WebDriverException e) {
        loginToAdmin();
      }
    }).doesNotThrowAnyException();
  }

  public void loginToAdmin() {
    String appName = "admin";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector("[ng-class=\"activeClass('#/authorization')\"] a"),
        "Authorizations"));

    wait.until(currentURIIs(URI.create("%sapp/%s/default/#/".formatted(appUrl, appName))));
  }

  @Test
  void shouldLoginToWelcome() {
    assertThatCode(() -> {
      try {
        loginToWelcome();
      } catch (WebDriverException e) {
        loginToWelcome();
      }
    }).doesNotThrowAnyException();
  }

  public void loginToWelcome() {
    String appName = "welcome";
    login(appName);
    wait.until(textToBePresentInElementLocated(
        By.cssSelector(".webapps .section-title"),
        "Applications"));

    wait.until(currentURIIs(URI.create("%sapp/%s/default/#!/welcome".formatted(appUrl, appName))));
  }

}
