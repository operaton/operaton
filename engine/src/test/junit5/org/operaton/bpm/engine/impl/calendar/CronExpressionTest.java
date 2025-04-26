/*
 *  Copyright 2025 the Operaton contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.operaton.bpm.engine.impl.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.DefaultTimeZone;

@DefaultTimeZone("UTC")
class CronExpressionTest {
  private static Date dateFromString(String date) {
    return Date.from(Instant.parse(date));
  }

  private static Stream<Arguments> validCronArguments() {
    return Stream.of(
        Arguments.of("1 * * * * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-01T00:00:01Z")),
        Arguments.of("* 1 * * * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-01T00:01:00Z")),
        Arguments.of("* * 1 * * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-01T01:00:00Z")),
        Arguments.of("* * * 2 * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-02T00:00:00Z")),
        Arguments.of("* * * L * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-31T00:00:00Z")),
        Arguments.of("* * * L * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-31T00:00:00Z")),
        Arguments.of("* * * * 2 ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-02-01T00:00:00Z")),
        Arguments.of("* * * ? * 1", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-05T00:00:00Z")),
        Arguments.of("* * * ? * * 2026", dateFromString("2025-01-01T00:00:00Z"),
            dateFromString("2026-01-01T00:00:00Z")));
  }

  @ParameterizedTest
  @MethodSource("validCronArguments")
  void shouldParseBasicCronExpression(String expression, Date startDate, Date expectedDate) throws Exception {
    CronExpression cronExpression = new CronExpression(expression);
    assertTrue(cronExpression.expressionParsed);
    assertEquals(expectedDate, cronExpression.getTimeAfter(startDate));
    assertEquals(TimeZone.getTimeZone("UTC"), cronExpression.getTimeZone());
  }
}
