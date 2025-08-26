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

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.DefaultTimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DefaultTimeZone("UTC")
class CronExpressionTest {
    private static Date dateFromString(String date) {
        return Date.from(Instant.parse(date));
    }

    @ParameterizedTest
    @CsvSource({
            // cron, start, expected
            "'1 * * * * ?', 2025-01-01T00:00:00Z, 2025-01-01T00:00:01Z",
            "'* 1 * * * ?', 2025-01-01T00:00:00Z, 2025-01-01T00:01:00Z",
            "'* * 1 * * ?', 2025-01-01T00:00:00Z, 2025-01-01T01:00:00Z",
            "'* * * 2 * ?', 2025-01-01T00:00:00Z, 2025-01-02T00:00:00Z",
            "'* * * L * ?', 2025-01-01T00:00:00Z, 2025-01-31T00:00:00Z",
            "'* * * * 2 ?', 2025-01-01T00:00:00Z, 2025-02-01T00:00:00Z",
            "'* * * ? * 1', 2025-01-01T00:00:00Z, 2025-01-05T00:00:00Z",
            "'* * * ? * * 2026', 2025-01-01T00:00:00Z, 2026-01-01T00:00:00Z",
            "'0 0 12 * * ?', 2025-01-01T11:00:00Z, 2025-01-01T12:00:00Z",
            "'0 15 10 ? * MON-FRI', 2025-01-03T10:00:00Z, 2025-01-03T10:15:00Z",
            "'0 0 0 1 1 ?', 2025-01-01T00:00:00Z, 2026-01-01T00:00:00Z",
            "'0 0 0 29 2 ?', 2023-01-01T00:00:00Z, 2024-02-29T00:00:00Z",
            // Month names
            "'0 0 12 * JAN ? 2025', 2025-01-01T00:00:00Z, 2025-01-01T12:00:00Z",
            "'0 0 12 * FEB ? 2025', 2025-01-01T00:00:00Z, 2025-02-01T12:00:00Z",
            // Weekday names
            "'0 0 12 ? * MON 2025', 2025-01-01T00:00:00Z, 2025-01-06T12:00:00Z",
            "'0 0 12 ? * SUN 2025', 2025-01-01T00:00:00Z, 2025-01-05T12:00:00Z",
            // Ranges
            "'0 0 0 28-2 * ?', 2025-01-01T00:00:00Z, 2025-01-02T00:00:00Z",
            // Steps
            "'0 0/15 12 * * ?', 2025-01-01T12:00:00Z, 2025-01-01T12:15:00Z",
            "'0 0 0 1/2 * ?', 2025-01-01T00:00:00Z, 2025-01-03T00:00:00Z",
            // Lists
            "'0 0 12 * 1,3,5 ?', 2025-01-01T00:00:00Z, 2025-01-01T12:00:00Z",
            // L (last day of the month)
            "'0 0 0 L * ?', 2025-01-01T00:00:00Z, 2025-01-31T00:00:00Z",
            // L weekday (last friday of the month)
            "'0 0 0 ? * 6L', 2025-01-01T00:00:00Z, 2025-01-31T00:00:00Z",
            // W (next weekday)
            "'0 0 0 15W * ?', 2025-02-01T00:00:00Z, 2025-02-14T00:00:00Z", // 15.2.2025 is Saturday, next weekday is 14.2. (Friday)
            // LW (last weekday of the month)
            "'0 0 0 LW * ?', 2025-02-01T00:00:00Z, 2025-02-28T00:00:00Z",
            // L-3 (third last day of the month)
            "'0 0 0 L-3 * ?', 2025-01-01T00:00:00Z, 2025-01-28T00:00:00Z",
            // L-3W (third last weekday of the month)
            "'0 0 0 L-3W * ?', 2025-01-01T00:00:00Z, 2025-01-28T00:00:00Z",
            // # (third Friday of the month)
            "'0 0 0 ? * 6#3', 2025-01-01T00:00:00Z, 2025-01-17T00:00:00Z",
            // ?
            "'0 0 12 ? * 2', 2025-01-01T00:00:00Z, 2025-01-06T12:00:00Z"
    })
    void shouldParseBasicCronExpression(String expression, String startDate, String expectedDate) throws Exception {
        // given
        CronExpression cronExpression = new CronExpression(expression);
        assertThat(cronExpression.toString()).isEqualTo(expression);

        // when
        Date result = cronExpression.getTimeAfter(CronExpressionTest.dateFromString(startDate));

        // then
        assertThat(result).isEqualTo(CronExpressionTest.dateFromString(expectedDate));
        assertThat(cronExpression.getTimeZone()).isEqualTo(TimeZone.getTimeZone("UTC"));
    }

    @ParameterizedTest
    @CsvSource({
            // Invalid cron expressions
            "'', 'Unexpected end of expression'",
            "'* * * * *', 'Unexpected end of expression'",
            "'0 0 0 1 1 1', 'Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.'"
    })
    void shouldNotParseInvalidCronExpression(String expression, String expectedMessage) {
        assertThatThrownBy(() -> new CronExpression(expression))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining(expectedMessage);
    }
}
