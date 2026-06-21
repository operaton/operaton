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
package org.operaton.bpm.engine.impl.calendar;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.util.EngineUtilLogger;

/**
 * A cron timer implementation that uses cronutils library for parsing and evaluation.
 */
public class CronTimer {

  private static final EngineUtilLogger LOG = ProcessEngineLogger.UTIL_LOGGER;

  protected final Cron cron;

  public CronTimer(final Cron cron) {
    this.cron = cron;
  }

  public Date getDueDate(final Date afterTime) {
    long fromEpochMilli = afterTime.getTime();
    final var next = ExecutionTime.forCron(cron)
      .nextExecution(ZonedDateTime.ofInstant(Instant.ofEpochMilli(fromEpochMilli), ZoneId.systemDefault()))
      .map(ZonedDateTime::toInstant)
      .map(Instant::toEpochMilli);

    return new Date(next.orElse(fromEpochMilli));
  }

  public static CronTimer parse(final String text) throws ParseException {
    return parse(text, CronType.SPRING53, true);
  }

  public static CronTimer parse(
      final String text,
      final CronType cronType,
      final boolean supportLegacyQuartzSyntax) throws ParseException {
    try {
      String expression = text;
      if (cronType == CronType.QUARTZ && supportLegacyQuartzSyntax) {
        String patchedExpression = patchLegacyCronExpression(expression);
        if (!expression.equals(patchedExpression)) {
          LOG.warnLegacyCronExpressionPatched(expression, patchedExpression);
        }
        expression = patchedExpression;
      }

      final var cron =
        new CronParser(CronDefinitionBuilder.instanceDefinitionFor(cronType))
          .parse(expression);
      return new CronTimer(cron);
    } catch (final IllegalArgumentException | NullPointerException ex) {
      throw new ParseException(ex.getMessage(), 0);
    }
  }

  private static String patchLegacyCronExpression(final String expression) {
    final String[] parts = expression.split("\\s+");
    if (parts.length < 6) {
      return expression;
    }

    final String dayOfMonth = parts[3];
    final String dayOfWeek = parts[5];

    boolean dayOfMonthSet = !"?".equals(dayOfMonth) && !"*".equals(dayOfMonth);
    boolean dayOfWeekSet = !"?".equals(dayOfWeek) && !"*".equals(dayOfWeek);

    if (dayOfMonthSet && dayOfWeekSet) {
      if (dayOfMonth.contains("W")) {
        parts[5] = "?";
      } else {
        parts[3] = "?";
      }
    } else if ("*".equals(dayOfMonth) && dayOfWeekSet) {
      parts[3] = "?";
    } else if ("*".equals(dayOfWeek) && dayOfMonthSet) {
      parts[5] = "?";
    } else if ("*".equals(dayOfMonth) && "*".equals(dayOfWeek)) {
      parts[5] = "?";
    } else if ("?".equals(dayOfMonth) && "?".equals(dayOfWeek)) {
      parts[3] = "*";
    }

    return String.join(" ", parts);
  }
}
