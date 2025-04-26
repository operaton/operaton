package org.operaton.bpm.engine.impl.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CronExpressionTest {

  @BeforeAll
  static void beforeAll() {
    // The third test fails without this line
     TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  private static Date dateFromString(String date) {
    return Date.from(Instant.parse(date));
  }

  private static Stream<Arguments> validCronArguments() {
    return Stream.of(
        Arguments.of("1 * * * * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-01T00:00:01Z")),
        Arguments.of("* 1 * * * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-01T00:01:00Z")),
        Arguments.of("* * 1 * * ?", dateFromString("2025-01-01T00:00:00Z"), dateFromString("2025-01-01T01:00:00Z")));
  }

  @ParameterizedTest
  @MethodSource("validCronArguments")
  void shouldParseBasicCronExpression(String expression, Date startDate, Date expectedDate) throws Exception {
    CronExpression cronExpression = new CronExpression(expression);
    assertTrue(cronExpression.expressionParsed);
    assertEquals(expectedDate, cronExpression.getTimeAfter(startDate));
  }
}
