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
package org.operaton.bpm.dmn.engine.test;

import java.io.InputStream;
import java.util.List;

import org.operaton.bpm.dmn.engine.DmnDecision;
import org.operaton.bpm.dmn.engine.DmnEngineConfiguration;
import org.operaton.commons.utils.IoUtil;
import org.junit.runner.Description;

/**
 * JUnit test rule for internal unit tests. Uses The
 * {@link DecisionResource} annotation to load decisions
 * before tests.
 */
public class DmnEngineTestRule extends DmnEngineRule {

  public static final String DMN_SUFFIX = "dmn";

  protected DmnDecision decision;

  public DmnEngineTestRule() {
    super();
  }

  public DmnEngineTestRule(DmnEngineConfiguration dmnEngineConfiguration) {
    super(dmnEngineConfiguration);
  }

    /**
   * Returns the DmnDecision object.
   *
   * @return the DmnDecision object
   */
  public DmnDecision getDecision() {
    return decision;
  }

    /**
   * Calls the super class's starting method with the provided description and then loads a decision based on the description.
   * 
   * @param description the description used to load the decision
   */
  @Override
  protected void starting(Description description) {
    super.starting(description);

    decision = loadDecision(description);
  }

    /**
   * Loads a DMN decision based on the provided Description.
   *
   * @param description the Description containing information about the decision
   * @return the loaded DmnDecision, or null if the decision cannot be loaded
   */
  protected DmnDecision loadDecision(Description description) {
    DecisionResource decisionResource = description.getAnnotation(DecisionResource.class);

    if(decisionResource != null) {

      String resourcePath = decisionResource.resource();

      resourcePath = expandResourcePath(description, resourcePath);

      InputStream inputStream = IoUtil.fileAsStream(resourcePath);

      String decisionKey = decisionResource.decisionKey();

      if (decisionKey == null || decisionKey.isEmpty()) {
        List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
        if (!decisions.isEmpty()) {
          return decisions.get(0);
        }
        else {
          return null;
        }
      } else {
        return dmnEngine.parseDecision(decisionKey, inputStream);
      }
    }
    else {
      return null;
    }
  }

    /**
   * Expands the given resource path based on the description and test class.
   * If the resource path already contains "/", it is considered expanded.
   * If the resource path is empty, it uses the test class and method name as the resource file name.
   * If the resource path is not empty, it uses the test class location as the resource location.
   * 
   * @param description the description of the test
   * @param resourcePath the path of the resource file
   * @return the expanded resource path
   */
  protected String expandResourcePath(Description description, String resourcePath) {
    if (resourcePath.contains("/")) {
      // already expanded path
      return resourcePath;
    }
    else {
      Class<?> testClass = description.getTestClass();
      if (resourcePath.isEmpty()) {
        // use test class and method name as resource file name
        return testClass.getName().replace(".", "/") + "." + description.getMethodName() + "." + DMN_SUFFIX;
      }
      else {
        // use test class location as resource location
        return testClass.getPackage().getName().replace(".", "/") + "/" + resourcePath;
      }
    }
  }

}
