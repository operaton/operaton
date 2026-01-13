/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Test utility class for date parsing operations.
 */
public final class DateTestUtil {

  /**
   * ISO-like date-time format: yyyy-MM-dd'T'HH:mm:ss
   * Example: 2023-12-31T23:59:59
   */
  public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private DateTestUtil() {
    // Utility class - no instantiation
  }

  /**
   * Parses a date string in ISO-like format (yyyy-MM-dd'T'HH:mm:ss) to a Date object.
   *
   * @param dateString the date string to parse
   * @return the parsed Date object
   */
  public static Date parseDate(String dateString) {
    LocalDateTime parsedDateTime = LocalDateTime.parse(dateString, ISO_DATE_TIME_FORMATTER);
    return Date.from(parsedDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  /**
   * Parses a date string with a custom format to a Date object.
   *
   * @param dateString the date string to parse
   * @param formatter the DateTimeFormatter to use
   * @return the parsed Date object
   */
  public static Date parseDate(String dateString, DateTimeFormatter formatter) {
    LocalDateTime parsedDateTime = LocalDateTime.parse(dateString, formatter);
    return Date.from(parsedDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
