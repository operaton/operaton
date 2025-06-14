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
package org.operaton.bpm.engine.spring.test.components.scope;

import org.operaton.bpm.engine.spring.components.scope.ProcessScope;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tobias Metzke
 *
 */
class ProcessScopeTest {

  @Test
  void shouldLogExceptionStacktrace() throws IOException {
    Logger logger = Logger.getLogger(ProcessScope.class.getName());
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Handler handler = new StreamHandler(out, new SimpleFormatter());
      logger.addHandler(handler);
      try {
        ProcessScope scope = new ProcessScope();
        Object variable = scope.get("testObject", null);
        assertThat(variable).isNull();
      } finally {
        handler.flush();
        handler.close();
        logger.removeHandler(handler);
      }
      // test for logged exception
      String message = out.toString(StandardCharsets.UTF_8);
      assertThat(message).isNotEmpty();
      assertThat(message).contains("org.operaton.bpm.engine.spring.components.scope.ProcessScope get")
          .contains("couldn't return value from process scope! java.lang.NullPointerException")
          .contains("at org.operaton.bpm.engine.spring.components.scope.ProcessScope.getExecutionId(ProcessScope.java:")
          .contains("at org.operaton.bpm.engine.spring.components.scope.ProcessScope.getConversationId(ProcessScope.java:")
          .contains("at org.operaton.bpm.engine.spring.components.scope.ProcessScope.get(ProcessScope.java:")
          .contains("at org.operaton.bpm.engine.spring.test.components.scope.ProcessScopeTest.shouldLogExceptionStacktrace(ProcessScopeTest.java:");
    }
  }
}
