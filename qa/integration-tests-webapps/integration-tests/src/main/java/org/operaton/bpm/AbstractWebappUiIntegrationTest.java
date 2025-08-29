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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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
  public static void createDriver() {
    String chromeDriverExecutable = "chromedriver";
    if (System.getProperty("os.name").toLowerCase(Locale.US).contains("windows")) {
      chromeDriverExecutable += ".exe";
    }

    File chromeDriver = new File("target/chromedriver/" + chromeDriverExecutable);
    if (!chromeDriver.exists()) {
      throw new RuntimeException("chromedriver could not be located!");
    }

    ChromeDriverService chromeDriverService = new ChromeDriverService.Builder()
        .withVerbose(true)
        .usingAnyFreePort()
        .usingDriverExecutable(chromeDriver)
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
        return new URI(webDriver.getCurrentUrl()).equals(pageURI);
      } catch (URISyntaxException e) {
        return false;
      }
    };

  }

  public static ExpectedCondition<Boolean> containsCurrentUrl(final String url) {

    return webDriver -> webDriver.getCurrentUrl().contains(url);

  }

  @BeforeEach
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getWebappCtxPath());
    appUrl = testProperties.getApplicationPath("/" + getWebappCtxPath());
  }

  @AfterEach
  public void after() {
    testUtil.destroy();
  }

  @AfterAll
  public static void quitDriver() {
    driver.quit();
  }

}
