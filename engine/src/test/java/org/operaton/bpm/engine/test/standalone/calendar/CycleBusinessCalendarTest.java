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
package org.operaton.bpm.engine.test.standalone.calendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.calendar.CycleBusinessCalendar;
import org.operaton.bpm.engine.impl.util.ClockUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CycleBusinessCalendarTest {

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
  }

  @Test
  void testSimpleCron() throws Exception {
    CycleBusinessCalendar businessCalendar = new CycleBusinessCalendar();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd - HH:mm");
    Date now = simpleDateFormat.parse("2011 03 11 - 17:23");
    ClockUtil.setCurrentTime(now);

    Date duedate = businessCalendar.resolveDuedate("0 0 0 1 * ?");

    Date expectedDuedate = simpleDateFormat.parse("2011 04 1 - 00:00");

    assertThat(duedate).isEqualTo(expectedDuedate);
  }

  @Test
  void testSimpleDuration() throws Exception {
    CycleBusinessCalendar businessCalendar = new CycleBusinessCalendar();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd - HH:mm");
    Date now = simpleDateFormat.parse("2010 06 11 - 17:23");
    ClockUtil.setCurrentTime(now);

    Date duedate = businessCalendar.resolveDuedate("R/P2DT5H70M");

    Date expectedDuedate = simpleDateFormat.parse("2010 06 13 - 23:33");

    assertThat(duedate).isEqualTo(expectedDuedate);
  }

  @Test
  void testSimpleCronWithStartDate() throws Exception {
    CycleBusinessCalendar businessCalendar = new CycleBusinessCalendar();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd - HH:mm");
    Date now = simpleDateFormat.parse("2011 03 11 - 17:23");

    Date duedate = businessCalendar.resolveDuedate("0 0 0 1 * ?", now);

    Date expectedDuedate = simpleDateFormat.parse("2011 04 1 - 00:00");

    assertThat(duedate).isEqualTo(expectedDuedate);
  }

  @Test
  void testSimpleDurationWithStartDate() throws Exception {
    CycleBusinessCalendar businessCalendar = new CycleBusinessCalendar();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd - HH:mm");
    Date now = simpleDateFormat.parse("2010 06 11 - 17:23");

    Date duedate = businessCalendar.resolveDuedate("R/P2DT5H70M", now);

    Date expectedDuedate = simpleDateFormat.parse("2010 06 13 - 23:33");

    assertThat(duedate).isEqualTo(expectedDuedate);
  }

  @ParameterizedTest
  @CsvSource({
    "'0 0 * * * ?'      , '2010 02 11 18:00'",
    "'*/10 * * * 2 ?'   , '2010 02 11 17:23'",
    "'0 * * * 2 ?'      , '2010 02 11 17:24'",
    "'0 0 * * 2 ?'      , '2010 02 11 18:00'",
    "'0 0 8-10 * * ?'   , '2010 02 12 08:00'",
    "'0 0/30 8-10 * * ?', '2010 02 12 08:00'",
    "'0 0 9-17 * * ?'   , '2010 02 12 09:00'",
    "'0 0 0 25 12 ?'    , '2010 12 25 00:00'",
    "'0 0 0 L 12 ?'     , '2010 12 31 00:00'",
    "'0 0 * 1|2 * ?'    , '2010 03 01 00:00'",
    "'0 0 * 1,2 * ?'    , '2010 03 01 00:00'",
    "'0 0 6,19 * * ?'   , '2010 02 11 19:00'"
  })
  void testResolveDueDate(String cronExpression, String expectedDueDate) throws Exception {
    CycleBusinessCalendar cbc = new CycleBusinessCalendar();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH:mm");
    Date startDate = sdf.parse("2010 02 11 17:23");
    assertThat(sdf.format(cbc.resolveDuedate(cronExpression, startDate))).isEqualTo(expectedDueDate);
  }

  @ParameterizedTest
  @CsvSource({
    "'0 0 0 * * THUL', '2010 02 25 00:00'",
    "'0 0 0 * * THU' , '2010 02 18 00:00'",
    "'0 0 0 1W * *'  , '2010 03 01 00:00'",
    "'0 0 0 ? * 5#2' , '2010 02 12 00:00'",
    "'@monthly'      , '2010 03 01 00:00'",
    "'@annually'     , '2011 01 01 00:00'",
    "'@yearly'       , '2011 01 01 00:00'",
    "'@weekly'       , '2010 02 14 00:00'",
    "'@daily'        , '2010 02 12 00:00'",
    "'@midnight'     , '2010 02 12 00:00'",
    "'@hourly'       , '2010 02 11 18:00'"
  })
  void testSpecialCharactersResolveDueDate(String cronExpression, String expectedDueDate) throws Exception {
    CycleBusinessCalendar cbc = new CycleBusinessCalendar();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH:mm");
    Date startDate = sdf.parse("2010 02 11 17:23");
    assertThat(sdf.format(cbc.resolveDuedate(cronExpression, startDate))).isEqualTo(expectedDueDate);
  }

  @Test
  public void testEndOfMonthRelativeExpressions() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH:mm");
    CycleBusinessCalendar cbc = new CycleBusinessCalendar();

    Date startDate = sdf.parse("2025 02 14 12:00");

    // All of these assertions should pass
    assertThat(sdf.format(cbc.resolveDuedate("0 37 14 L-22 * ?", startDate))).isEqualTo("2025 03 09 14:37");
    assertThat(sdf.format(cbc.resolveDuedate("0 23 8 L-2 * ?", startDate))).isEqualTo("2025 02 26 08:23");
    assertThat(sdf.format(cbc.resolveDuedate("0 37 8 L-1 * ?", startDate))).isEqualTo("2025 02 27 08:37");
    assertThat(sdf.format(cbc.resolveDuedate("0 0 12 L-15 * ?", startDate))).isEqualTo("2025 03 16 12:00");
    assertThat(sdf.format(cbc.resolveDuedate("0 0 12 L-27 * ?", startDate))).isEqualTo("2025 03 04 12:00");

    // leap year
    startDate = sdf.parse("2000 02 26 10:00");
    assertThat(sdf.format(cbc.resolveDuedate("0 15 10 L-3 2 ?", startDate))).isEqualTo("2000 02 26 10:15");
    startDate = sdf.parse("2000 02 27 10:00");
    assertThat(sdf.format(cbc.resolveDuedate("0 15 10 L-3 2 ?", startDate))).isEqualTo("2001 02 25 10:15");
  }

  @Test
  public void testTooManyArgumentExpressions() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH:mm");
    CycleBusinessCalendar cbc = new CycleBusinessCalendar();

    Date startDate = sdf.parse("2025 02 14 12:00");

    assertThatThrownBy(() -> cbc.resolveDuedate("0 15 10 * * ? 2025 *", startDate))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Exception while parsing cycle expression");

  }
}
