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
package org.operaton.bpm.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Allows to take screenshots in case of a Selenium test error.
 */
public class SeleniumScreenshotExtension implements AfterTestExecutionCallback, TestExecutionExceptionHandler {

  private static final String OUTPUT_DIR_PROPERTY_NAME = "selenium.screenshot.directory";
  private static final Logger log = Logger.getAnonymousLogger();
  protected WebDriver webDriver;

  public SeleniumScreenshotExtension(WebDriver driver) {
    if (driver instanceof RemoteWebDriver) {
      webDriver = new Augmenter().augment(driver);
    } else {
      webDriver = driver;
    }
  }

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    captureScreenShot(context);
    throw throwable;
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    if (context.getExecutionException().isPresent()) {
      captureScreenShot(context);
    }
  }

  private void captureScreenShot(ExtensionContext context) {
    String screenshotDirectory = System.getProperty(OUTPUT_DIR_PROPERTY_NAME);

    if (screenshotDirectory == null) {
      log.info("No screenshot created, because no output directory is specified. "
          + "Set the system property " + OUTPUT_DIR_PROPERTY_NAME + " to resolve this.");
      return;
    }

    File scrFile = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
    String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    String scrFilename = context.getRequiredTestClass().getSimpleName() + "-" + context.getRequiredTestMethod().getName() + "-" + now + ".png";
    File outputFile = new File(screenshotDirectory, scrFilename);

    try {
      FileUtils.copyFile(scrFile, outputFile);
    } catch (IOException ioe) {
      log.severe("No screenshot created due to an error on copying: " + ioe.getMessage());
      return;
    }

    log.info("Screenshot created at: " + outputFile.getPath());
  }
}
