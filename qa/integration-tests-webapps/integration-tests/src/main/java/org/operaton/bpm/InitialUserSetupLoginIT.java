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
package org.operaton.bpm;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

class InitialUserSetupLoginIT extends AbstractWebappUiIntegrationTest {

  private static final String SETUP_ADMIN = "setupadmin";

  protected WebDriverWait wait;

  @Test
  void shouldAllowLoginAfterInitialUserSetup() {
    driver.manage().deleteAllCookies();
    driver.get("%sapp/admin/default/setup/#setup".formatted(appUrl));

    wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    sendKeys(wait.until(visibilityOfElementLocated(By.id("inputUserId"))), SETUP_ADMIN);
    sendKeys(wait.until(visibilityOfElementLocated(By.id("inputPassword"))), SETUP_ADMIN);
    sendKeys(wait.until(visibilityOfElementLocated(By.id("inputPasswordRepeat"))), SETUP_ADMIN);
    sendKeys(wait.until(visibilityOfElementLocated(By.id("inputFirstname"))), "Setup");
    sendKeys(wait.until(visibilityOfElementLocated(By.id("inputLastname"))), "Admin");
    sendKeys(wait.until(visibilityOfElementLocated(By.id("inputEmail"))), "setup.admin@example.com");

    wait.until(elementToBeClickable(By.cssSelector("button[type=\"submit\"]"))).click();
    wait.until(visibilityOfElementLocated(By.cssSelector(".alert.alert-success")));
    wait.until(elementToBeClickable(By.cssSelector("a[href=\"./#/login\"]"))).click();

    sendKeys(wait.until(visibilityOfElementLocated(By.cssSelector("input[type=\"text\"]"))), SETUP_ADMIN);
    sendKeys(wait.until(visibilityOfElementLocated(By.cssSelector("input[type=\"password\"]"))), SETUP_ADMIN);
    wait.until(elementToBeClickable(By.cssSelector("button[type=\"submit\"]"))).click();

    wait.until(textToBePresentInElementLocated(
        By.cssSelector("[ng-class=\"activeClass('#/authorization')\"] a"),
        "Authorizations"));

    wait.until(currentURIIs(URI.create("%sapp/admin/default/#/".formatted(appUrl))));
  }

  public void sendKeys(WebElement element, String keys) {
    // Type character-by-character because Angular-bound fields can intermittently drop
    // batched input in headless Selenium runs.
    Arrays.stream(keys.split("")).forEach(element::sendKeys);
  }
}
