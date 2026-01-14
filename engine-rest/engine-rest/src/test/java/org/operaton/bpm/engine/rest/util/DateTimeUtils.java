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
package org.operaton.bpm.engine.rest.util;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

public abstract class DateTimeUtils {
  public static final DateFormat DATE_FORMAT_WITHOUT_TIMEZONE = new DateFormat("yyyy-MM-dd'T'HH:mm:ss");
  public static final DateFormat DATE_FORMAT_WITH_TIMEZONE = new DateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /**
   * A thread-safe wrapper around DateTimeFormatter that provides a SimpleDateFormat-like API
   * for formatting and parsing dates.
   */
  public static class DateFormat {
    private final DateTimeFormatter formatter;
    private final boolean hasTimezone;

    public DateFormat(String pattern) {
      this.formatter = DateTimeFormatter.ofPattern(pattern);
      this.hasTimezone = pattern.contains("Z");
    }

    public String format(Date date) {
      if (hasTimezone) {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .format(formatter);
      } else {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(formatter);
      }
    }

    public Date parse(String dateString) throws ParseException {
      try {
        if (hasTimezone) {
          ZonedDateTime parsedDateTime = ZonedDateTime.parse(dateString, formatter);
          return Date.from(parsedDateTime.toInstant());
        } else {
          LocalDateTime parsedDateTime = LocalDateTime.parse(dateString, formatter);
          return Date.from(parsedDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
      } catch (DateTimeParseException e) {
        throw new ParseException(e.getMessage(), e.getErrorIndex());
      }
    }
  }

  /**
   * Converts date string without timezone to the one with timezone.
   * @param dateString
   * @return
   */
  public static String withTimezone(String dateString) {
    try {
      Date date = DATE_FORMAT_WITHOUT_TIMEZONE.parse(dateString);
      return withTimezone(date);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Formats a date with timezone information using the system default timezone.
   * @param date the date to format
   * @return formatted date string with timezone
   */
  public static String withTimezone(Date date) {
    return DATE_FORMAT_WITH_TIMEZONE.format(date);
  }

  public static Date updateTime(Date now, Date newTime) {
    Calendar c = Calendar.getInstance();
    c.setTime(now);
    Calendar newTimeCalendar = Calendar.getInstance();
    newTimeCalendar.setTime(newTime);
    c.set(Calendar.ZONE_OFFSET, newTimeCalendar.get(Calendar.ZONE_OFFSET));
    c.set(Calendar.DST_OFFSET, newTimeCalendar.get(Calendar.DST_OFFSET));
    c.set(Calendar.HOUR_OF_DAY, newTimeCalendar.get(Calendar.HOUR_OF_DAY));
    c.set(Calendar.MINUTE, newTimeCalendar.get(Calendar.MINUTE));
    c.set(Calendar.SECOND, newTimeCalendar.get(Calendar.SECOND));
    c.set(Calendar.MILLISECOND, newTimeCalendar.get(Calendar.MILLISECOND));
    return c.getTime();
  }

  public static Date addDays(Date date, int amount) {
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    c.add(Calendar.DATE, amount);
    return c.getTime();
  }
}
