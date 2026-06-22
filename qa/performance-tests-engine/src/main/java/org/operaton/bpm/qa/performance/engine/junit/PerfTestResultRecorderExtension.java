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
package org.operaton.bpm.qa.performance.engine.junit;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import org.operaton.bpm.qa.performance.engine.framework.PerfTestException;
import org.operaton.bpm.qa.performance.engine.framework.PerfTestResults;
import org.operaton.bpm.qa.performance.engine.util.JsonUtil;

/**
 * JUnit 5 extension recording the performance test result.
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;RegisterExtension
 * static PerfTestResultRecorderExtension resultRecorderExtension = new PerfTestResultRecorderExtension();
 * </pre>
 *
 * <p>Author: Daniel Meyer</p>
 */
public class PerfTestResultRecorderExtension implements TestWatcher {

  public static final Logger LOG = Logger.getLogger(PerfTestResultRecorderExtension.class.getName());

  protected PerfTestResults results;

  @Override
  public void testSuccessful(ExtensionContext context) {
    if (results != null) {
      String testName = context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName();
      results.setTestName(testName);
      LOG.log(Level.INFO, () -> results.toString());

      String resultFileName = formatResultFileName(context);

      try {
        // create file:
        File directory = new File(formatResultFileDirName());
        if (!directory.exists()) {
          Files.createDirectories(directory.toPath());
        }

        JsonUtil.writeObjectToFile(resultFileName, results);

      } catch (Exception e) {
        throw new PerfTestException("Could not record results to file " + resultFileName, e);
      }
    }
  }

  protected String formatResultFileDirName() {
    return "target" + File.separatorChar + "results";
  }

  protected String formatResultFileName(ExtensionContext context) {
    return "%s%c%s.%s.json".formatted(formatResultFileDirName(), File.separatorChar, context.getRequiredTestClass().getSimpleName(), context.getRequiredTestMethod().getName());
  }

  public void setResults(PerfTestResults results) {
    this.results = results;
  }
}
