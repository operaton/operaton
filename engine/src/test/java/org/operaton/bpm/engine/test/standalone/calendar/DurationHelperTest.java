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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.calendar.DurationHelper;
import org.operaton.bpm.engine.impl.util.ClockUtil;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

class DurationHelperTest {

  @AfterEach
  void tearDown() {
    ClockUtil.reset();
  }

  @Test
  void shouldNotExceedNumber() {
    ClockUtil.setCurrentTime(new Date(0));
    DurationHelper dh = new DurationHelper("R2/PT10S");

    ClockUtil.setCurrentTime(new Date(15000));
    assertEquals(20000, dh.getDateAfter().getTime());


    ClockUtil.setCurrentTime(new Date(30000));
    assertThat(dh.getDateAfter()).isNull();
  }

  @Test
  void shouldNotExceedNumberPeriods() throws Exception {
    ClockUtil.setCurrentTime(parse("19700101-00:00:00"));
    DurationHelper dh = new DurationHelper("R2/1970-01-01T00:00:00/1970-01-01T00:00:10");

    ClockUtil.setCurrentTime(parse("19700101-00:00:15"));
    assertEquals(parse("19700101-00:00:20"), dh.getDateAfter());


    ClockUtil.setCurrentTime(parse("19700101-00:00:30"));
    assertThat(dh.getDateAfter()).isNull();
  }

  @Test
  void shouldNotExceedNumberNegative() throws Exception {
    ClockUtil.setCurrentTime(parse("19700101-00:00:00"));
    DurationHelper dh = new DurationHelper("R2/PT10S/1970-01-01T00:00:50");

    ClockUtil.setCurrentTime(parse("19700101-00:00:20"));
    assertEquals(parse("19700101-00:00:30"), dh.getDateAfter());


    ClockUtil.setCurrentTime(parse("19700101-00:00:35"));

    assertEquals(parse("19700101-00:00:40"), dh.getDateAfter());
  }

  @Test
  void shouldNotExceedNumberWithStartDate() {
    DurationHelper dh = new DurationHelper("R2/PT10S", new Date(0));
    assertEquals(20000, dh.getDateAfter(new Date(15000)).getTime());
    assertThat(dh.getDateAfter(new Date(30000))).isNull();
  }

  @Test
  void shouldNotExceedNumberPeriodsWithStartDate() throws Exception {
    DurationHelper dh = new DurationHelper("R2/1970-01-01T00:00:00/1970-01-01T00:00:10", parse("19700101-00:00:00"));

    assertEquals(parse("19700101-00:00:20"), dh.getDateAfter(parse("19700101-00:00:15")));
    assertThat(dh.getDateAfter(parse("19700101-00:00:30"))).isNull();
  }

  @Test
  void shouldNotExceedNumberNegativeWithStartDate() throws Exception {
    DurationHelper dh = new DurationHelper("R2/PT10S/1970-01-01T00:00:50", parse("19700101-00:00:00"));

    assertEquals(parse("19700101-00:00:30"), dh.getDateAfter(parse("19700101-00:00:20")));

    assertEquals(parse("19700101-00:00:40"), dh.getDateAfter(parse("19700101-00:00:35")));
  }

  @Test
  void shouldParseAllSupportedISO8601DurationPatterns() throws Exception {
    // given
    // when
    DurationHelper dh = new DurationHelper("P1Y5M21DT19H47M55S", parse("19700101-00:00:00"));

    // then
    assertThat(dh.getDateAfter()).isEqualTo(parse("19710622-19:47:55"));
  }

  @Test
  void shouldParseP2W() throws Exception {
    // given

    // when
    DurationHelper dh = new DurationHelper("P2W", parse("19700101-00:00:00"));

    // then
    assertThat(dh.getDateAfter()).isEqualTo(parse("19700115-00:00:00"));
  }

  @Test
  void shouldParseP4W() throws Exception {
    // given

    // when
    DurationHelper dh = new DurationHelper("P4W", parse("19700101-00:00:00"));

    // then
    assertThat(dh.getDateAfter()).isEqualTo(parse("19700129-00:00:00"));
  }

  @Test
  void shouldParseP5W() throws Exception {
    // given

    // when
    DurationHelper dh = new DurationHelper("P5W", parse("19700101-00:00:00"));

    // then
    assertThat(dh.getDateAfter()).isEqualTo(parse("19700205-00:00:00"));
  }

  @Test
  void shouldParseP22W() throws Exception {
    // given

    // when
    DurationHelper dh = new DurationHelper("P22W", parse("19700101-00:00:00"));

    // then
    assertThat(dh.getDateAfter()).isEqualTo(parse("19700604-00:00:00"));
  }

  private Date parse(String str) throws Exception {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
    return simpleDateFormat.parse(str);
  }
}
