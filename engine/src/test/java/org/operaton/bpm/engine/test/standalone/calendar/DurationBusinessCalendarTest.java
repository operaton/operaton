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
package org.operaton.bpm.engine.test.standalone.calendar;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.operaton.bpm.engine.impl.calendar.DurationBusinessCalendar;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.junit.After;
import org.junit.Test;

/**
 * @author Tom Baeyens
 */
public class DurationBusinessCalendarTest {

  @After
  public void tearDown() {
    ClockUtil.reset();
  }

  @Test
  public void testSimpleDuration() throws Exception {
    DurationBusinessCalendar businessCalendar = new DurationBusinessCalendar();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd - HH:mm");
    Date now = simpleDateFormat.parse("2010 06 11 - 17:23");
    ClockUtil.setCurrentTime(now);

    Date duedate = businessCalendar.resolveDuedate("P2DT5H70M");

    Date expectedDuedate = simpleDateFormat.parse("2010 06 13 - 23:33");

    assertEquals(expectedDuedate, duedate);
  }

  @Test
  public void testSimpleDurationWithStartDate() throws Exception {
    DurationBusinessCalendar businessCalendar = new DurationBusinessCalendar();

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd - HH:mm");
    Date now = simpleDateFormat.parse("2010 06 11 - 17:23");

    Date duedate = businessCalendar.resolveDuedate("P2DT5H70M", now);

    Date expectedDuedate = simpleDateFormat.parse("2010 06 13 - 23:33");

    assertEquals(expectedDuedate, duedate);
  }

}