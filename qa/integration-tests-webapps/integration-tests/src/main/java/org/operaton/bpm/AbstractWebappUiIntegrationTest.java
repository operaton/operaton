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
import java.net.URISyntaxException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;

import org.operaton.bpm.util.SeleniumScreenshotExtension;

public class AbstractWebappUiIntegrationTest extends AbstractWebIntegrationTest {

  protected static WebDriver driver;

  @RegisterExtension
  public SeleniumScreenshotExtension screenshotRule = new SeleniumScreenshotExtension(driver);

  @BeforeAll
  static void createDriver() {

    ChromeDriverService chromeDriverService = new ChromeDriverService.Builder()
            .withVerbose(true)
            .usingAnyFreePort()
            .build();

    ChromeOptions chromeOptions = new ChromeOptions()
            .addArguments("--headless=new")
            .addArguments("--window-size=1920,1200")
            .addArguments("--disable-gpu")
            .addArguments("--no-sandbox")
            .addArguments("--disable-dev-shm-usage")
            .addArguments("--remote-allow-origins=*");

    driver = new ChromeDriver(chromeDriverService, chromeOptions);
  }

  public static ExpectedCondition<Boolean> currentURIIs(final URI pageURI) {

    return webDriver -> {
      try {
        return URI.create(webDriver.getCurrentUrl()).equals(pageURI);
      } catch (IllegalArgumentException e) {
        return false;
      }
    };

  }

  public static ExpectedCondition<Boolean> containsCurrentUrl(final String url) {

    return webDriver -> webDriver.getCurrentUrl().contains(url);

  }

  @BeforeEach
  void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
    appUrl = testProperties.getApplicationPath("/" + getWebappCtxPath());
  }

  @AfterAll
  static void quitDriver() {
    driver.quit();
  }

}
