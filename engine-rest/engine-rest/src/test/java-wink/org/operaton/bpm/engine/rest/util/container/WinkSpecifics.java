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
package org.operaton.bpm.engine.rest.util.container;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.rest.CustomJacksonDateFormatTest;
import org.operaton.bpm.engine.rest.ExceptionHandlerTest;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;

/**
 * @author Thorben Lindhauer
 *
 */
public class WinkSpecifics implements ContainerSpecifics {

  protected static final TestRuleFactory DEFAULT_RULE_FACTORY =
      new ServletContainerRuleFactory("default-application-web.xml");

  protected static final Map<Class<?>, TestRuleFactory> TEST_RULE_FACTORIES =
      new HashMap<Class<?>, TestRuleFactory>();

  static {
    TEST_RULE_FACTORIES.put(ExceptionHandlerTest.class, new ServletContainerRuleFactory("custom-application-web.xml"));
    TEST_RULE_FACTORIES.put(CustomJacksonDateFormatTest.class, new ServletContainerRuleFactory("custom-date-format-web.xml"));
  }

  public Extension getExtension(Class<?> testClass) {
    TestRuleFactory ruleFactory = DEFAULT_RULE_FACTORY;

    if (TEST_RULE_FACTORIES.containsKey(testClass)) {
      ruleFactory = TEST_RULE_FACTORIES.get(testClass);
    }

    return ruleFactory.createExtension();
  }

  public static class ServletContainerRuleFactory implements TestRuleFactory {

    protected String webXmlResource;

    public ServletContainerRuleFactory(String webXmlResource) {
      this.webXmlResource = webXmlResource;
    }

    public Extension createExtension() {
      return new TomcatExtension(new WinkTomcatServerBootstrap(webXmlResource));
    }

  }

}
