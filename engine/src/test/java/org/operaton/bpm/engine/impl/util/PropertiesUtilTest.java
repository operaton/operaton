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
package org.operaton.bpm.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;

import ch.qos.logback.classic.Level;

public class PropertiesUtilTest {

  @RegisterExtension
  public ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension()
                                                           .watch("org.operaton.bpm.engine.util")
                                                           .level(Level.DEBUG);

  @Test
  void shouldLogMissingFile() {
    // given
    String invalidFile = "/missingProps.properties";

    // when
    PropertiesUtil.getProperties(invalidFile);

    // then
    String logMessage = String.format("Could not find the '%s' file on the classpath. " +
        "If you have removed it, please restore it.", invalidFile);
    assertThat(loggingRule.getFilteredLog(logMessage)).hasSize(1);
  }
}
