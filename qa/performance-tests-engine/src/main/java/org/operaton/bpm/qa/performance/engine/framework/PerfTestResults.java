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
package org.operaton.bpm.qa.performance.engine.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Meyer
 *
 */
public class PerfTestResults {

  /** the name of the test */
  protected String testName;

  /** the configuration used */
  protected PerfTestConfiguration configuration;

  /** the individual result entries **/
  protected List<PerfTestResult> passResults = new ArrayList<>();

  public PerfTestResults(PerfTestConfiguration configuration) {
    this.configuration = configuration;
  }

  public PerfTestResults() {
  }

  // getter / setters ////////////////////////////

  public String getTestName() {
    return testName;
  }

  public void setTestName(String testName) {
    this.testName = testName;
  }

  public PerfTestConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(PerfTestConfiguration configuration) {
    this.configuration = configuration;
  }

  public List<PerfTestResult> getPassResults() {
    return passResults;
  }

  public void setPassResults(List<PerfTestResult> results) {
    this.passResults = results;
  }

}
