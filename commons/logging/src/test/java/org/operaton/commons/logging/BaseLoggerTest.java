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
package org.operaton.commons.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import static org.operaton.commons.logging.ExampleLogger.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Daniel Meyer
 *
 */
public class BaseLoggerTest {

  public static final String ID = "01";
  public static final String SOME_MESSAGE = "Some message";

  @Test
  void shouldFormatMessage() {
    ExampleLogger logger = LOG;

    String messageTemplate = "Some message '{}'";

    String formattedMessage = logger.formatMessageTemplate(ID, messageTemplate);
    String expectedMessageTemplate = "%s-%s%s %s".formatted(PROJECT_CODE, COMPONENT_ID, ID, messageTemplate);

    assertThat(formattedMessage).isEqualTo(expectedMessageTemplate);
  }

  @Test
  void shouldFormatExceptionMessageWithParam() {
    ExampleLogger logger = LOG;

    String messageTemplate = "Some message '{}'";
    String parameter = "someParameter";

    String formattedMessage = logger.exceptionMessage(ID, messageTemplate, parameter);
    String expectedMessageTemplate = "%s-%s%s Some message 'someParameter'".formatted(PROJECT_CODE, COMPONENT_ID, ID);

    assertThat(formattedMessage).isEqualTo(expectedMessageTemplate);
  }

  @Test
  void shouldFormatExceptionMessageWithParams() {
    ExampleLogger logger = LOG;

    String messageTemplate = "Some message '{}' '{}'";
    String p1 = "someParameter";
    String p2 = "someOtherParameter";

    String formattedMessage = logger.exceptionMessage(ID, messageTemplate, p1, p2);
    String expectedMessageTemplate = "%s-%s%s Some message 'someParameter' 'someOtherParameter'".formatted(PROJECT_CODE, COMPONENT_ID, ID);

    assertThat(formattedMessage).isEqualTo(expectedMessageTemplate);
  }

  @Test
  void shouldFormatExceptionMessageWithoutParam() {
    ExampleLogger logger = LOG;

    String formattedMessage = logger.exceptionMessage(ID, SOME_MESSAGE);
    String expectedMessageTemplate = "%s-%s%s %s".formatted(PROJECT_CODE, COMPONENT_ID, ID, SOME_MESSAGE);

    assertThat(formattedMessage).isEqualTo(expectedMessageTemplate);
  }

  @Test
  void shouldCallLogTrace() {
    ExampleLogger logger = Mockito.spy(LOG);
    logger.log("TRACE", ID, SOME_MESSAGE);
    Mockito.verify(logger).logTrace(ID, SOME_MESSAGE);
  }

  @Test
  void shouldCallLogInfo() {
    ExampleLogger logger = Mockito.spy(LOG);
    logger.log("INFO", ID, SOME_MESSAGE);
    Mockito.verify(logger).logInfo(ID, SOME_MESSAGE);
  }

  @Test
  void shouldCallLogDebug() {
    ExampleLogger logger = Mockito.spy(LOG);
    logger.log("DEBUG", ID, SOME_MESSAGE);
    Mockito.verify(logger).logDebug(ID, SOME_MESSAGE);
  }

  @Test
  void shouldCallLogError() {
    ExampleLogger logger = Mockito.spy(LOG);
    logger.log("ERROR", ID, SOME_MESSAGE);
    Mockito.verify(logger).logError(ID, SOME_MESSAGE);
  }

  @Test
  void shouldCallLogWarn() {
    ExampleLogger logger = Mockito.spy(LOG);
    logger.log(" warn ", ID, SOME_MESSAGE);
    Mockito.verify(logger).logWarn(ID, SOME_MESSAGE);
  }

  @Test
  void shouldCallLogDebugWhenNotMatched() {
    ExampleLogger logger = Mockito.spy(LOG);
    logger.log("FATAL", ID, SOME_MESSAGE);
    Mockito.verify(logger).logDebug(ID, SOME_MESSAGE);
  }

  @Test
  void shouldCallLogWarnWhenNotMatched() {
    ExampleLogger logger = Mockito.spy(LOG);
    logger.log("FATAL", Level.WARN, ID, SOME_MESSAGE);
    Mockito.verify(logger).logWarn(ID, SOME_MESSAGE);
  }

  @ParameterizedTest
  @MethodSource("shouldSanitizeParameters_args")
  void shouldSanitizeParameters(Object[] givenParameters, Object[] expectedParameters) {
    ExampleLogger logger = Mockito.spy(LOG);
    Object[] sanitizedParameters = logger.sanitizeParameters(givenParameters);
    assertThat(sanitizedParameters).isEqualTo(expectedParameters);
  }

  static Stream<Arguments> shouldSanitizeParameters_args() {
    return Stream.of(
      arguments(null, null),
      arguments(new Object[0], new Object[0]),
      arguments(new Object[] { "someParameter" }, new Object[] {"someParameter"}),
      arguments(new Object[] { List.of("XYZ") }, new Object[] {List.of("XYZ")}),
      arguments(new Object[] { "some\nPara\nme\tter" }, new Object[] {"some_Para_me_ter"})
      );
  }
}
