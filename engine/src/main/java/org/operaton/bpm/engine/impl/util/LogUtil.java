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
package org.operaton.bpm.engine.impl.util;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.operaton.bpm.engine.impl.pvm.PvmException;

/**
 * @author Tom Baeyens
 *
 * @deprecated Use slf4j instead.
 */
@Deprecated(forRemoval = true, since = "1.0")
public final class LogUtil {


  public enum ThreadLogMode {
    NONE, INDENT, PRINT_ID

  }
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  private static Map<Long, String> threadIndents = new HashMap<>();
  private static ThreadLogMode threadLogMode = ThreadLogMode.NONE;

  private LogUtil() {
  }

  public static ThreadLogMode getThreadLogMode() {
    return threadLogMode;
  }

  public static ThreadLogMode setThreadLogMode(ThreadLogMode threadLogMode) {
    ThreadLogMode old = LogUtil.threadLogMode;
    LogUtil.threadLogMode = threadLogMode;
    return old;
  }

  public static void readJavaUtilLoggingConfigFromClasspath() {
    InputStream inputStream = ReflectUtil.getResourceAsStream("logging.properties");
    try {
      if (inputStream != null) {
        LogManager.getLogManager().readConfiguration(inputStream);

        String redirectCommons = LogManager.getLogManager().getProperty("redirect.commons.logging");
        if ((redirectCommons != null) && (!"false".equalsIgnoreCase(redirectCommons))) {
          System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        }
      }
    } catch (Exception e) {
      throw new PvmException("couldn't initialize logging properly", e);
    } finally {
      IoUtil.closeSilently(inputStream);
    }
  }

  public static class LogFormatter extends Formatter {
    private final Format dateFormat = new SimpleDateFormat("HH:mm:ss,SSS");

    @Override
    public String format(LogRecord logRecord) {
      StringBuilder line = new StringBuilder();
      line.append(dateFormat.format(new Date()));
      if (Level.FINE.equals(logRecord.getLevel())) {
        line.append(" FIN ");
      } else if (Level.FINEST.equals(logRecord.getLevel())) {
        line.append(" FST ");
      } else if (Level.INFO.equals(logRecord.getLevel())) {
        line.append(" INF ");
      } else if (Level.SEVERE.equals(logRecord.getLevel())) {
        line.append(" SEV ");
      } else if (Level.WARNING.equals(logRecord.getLevel())) {
        line.append(" WRN ");
      } else if (Level.FINER.equals(logRecord.getLevel())) {
        line.append(" FNR ");
      } else if (Level.CONFIG.equals(logRecord.getLevel())) {
        line.append(" CFG ");
      }

      long threadId = logRecord.getLongThreadID();
      String threadIndent = getThreadIndent(threadId);

      line.append(threadIndent);
      line.append(" | ");
      line.append(logRecord.getMessage());

      if (logRecord.getThrown() != null) {
        line.append(LINE_SEPARATOR);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        logRecord.getThrown().printStackTrace(printWriter);
        line.append(stringWriter);
      }

      line.append("  [");
      line.append(logRecord.getLoggerName());
      line.append("]");

      line.append(LINE_SEPARATOR);

      return line.toString();
    }

    protected static String getThreadIndent(long threadId) {
      Long threadIdInteger = threadId;
      if (threadLogMode==ThreadLogMode.NONE) {
        return "";
      }
      if (threadLogMode==ThreadLogMode.PRINT_ID) {
        return String.valueOf(threadId);
      }
      return threadIndents.computeIfAbsent(threadIdInteger, i -> "  ".repeat(threadIndents.size()));
    }

  }

  public static void resetThreadIndents() {
    threadIndents = new HashMap<>();
  }
}
