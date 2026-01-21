/*
 * Copyright 2025 the Operaton contributors.
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
 * 
 */
package org.operaton.bpm.engine.test.junit5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 Extension to monitor and capture log events for specified loggers during test execution.
 * <p>
 * This extension provides the capability to watch specific loggers at a customizable log level.
 * By default, all watched loggers are set to {@code DEBUG} level unless specified otherwise.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>
 * <code>@RegisterExtension</code>
 * ProcessEngineLoggingExtension loggingExtension = new ProcessEngineLoggingExtension()
 *    .watch("org.operaton.bpm.engine")
 *    .level(Level.INFO);
 * </pre>
 *
 * <p>
 * During test execution, this extension intercepts log events for specified loggers and stores them,
 * allowing the logs to be accessed and filtered based on test-specific criteria.
 * For example, calling {@link #getLog(String)} provides a list of log events for a specific logger.
 * You may also filter log entries containing a particular substring by using
 * {@link #getFilteredLog(String, String)}.
 * </p>
 *
 * <h3>Methods:</h3>
 * <ul>
 * <li>{@link #watch(String...)} - Starts watching specified loggers with the default or specified log level.</li>
 * <li>{@link #level(Level)} - Sets the global log level for all watched loggers.</li>
 * <li>{@link #getLog(String)} - Retrieves the log entries recorded for a specified logger.</li>
 * <li>{@link #getFilteredLog(String, String)} - Retrieves log entries containing a specified substring.</li>
 * </ul>
 *
 * <p>
 * <h3>Logging Flow:</h3>
 * <p>
 * Before each test execution, the extension attaches an in-memory appender to each watched logger.
 * After the test execution, the extension detaches all appenders, restoring original log configurations.
 * This ensures no residual configuration persists beyond the test's scope.
 * </p>
 *
 * <p>
 * Example usage with filtered logs:
 * </p>
 * <pre>
 * <code>List&lt;ILoggingEvent&gt; filteredLogs = loggingExtension.getFilteredLog("org.operaton.bpm.engine", "error message");</code>
 * </pre>
 *
 * <p>
 * The extension is especially useful in scenarios where log messages play a key role in testing,
 * such as verifying that certain events are logged at specific levels.
 * </p>
 */
public class ProcessEngineLoggingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  public static final String LOGGER_NOT_FOUND_ERROR = "no logger found with name ";
  public static final String NOT_WATCHING_ERROR = "not watching any logger with name: ";
  private static final String APPENDER_NAME = "defaultAppender";

  private final Map<String, Logger> globallyWatched = new HashMap<>();
  private Level globalLevel = Level.DEBUG;

  private final Map<String, Logger> allWatched = new HashMap<>();

  public ProcessEngineLoggingExtension watch(String... loggerName) {
    for (String logger : loggerName) {
      watch(logger, null);
    }
    return this;
  }

  public ProcessEngineLoggingExtension watch(String loggerName, Level level) {
    Logger logger = getLogger(loggerName);
    logger.setLevel(level);
    globallyWatched.put(logger.getName(), logger);
    return this;
  }

  public ProcessEngineLoggingExtension level(Level level) {
    globalLevel = level;
    return this;
  }

  private Logger getLogger(String loggerName) {
    Logger logger;
    try {
      logger = (Logger) LoggerFactory.getLogger(loggerName);
      if (logger.getLevel() == null || globalLevel.isGreaterOrEqual(logger.getLevel())) {
        logger.setLevel(globalLevel);
      }
    } catch (ClassCastException e) {
      throw new RuntimeException(LOGGER_NOT_FOUND_ERROR + loggerName);
    }
    return logger;
  }

  public List<ILoggingEvent> getLog(String loggerName) {
    Logger logger = allWatched.get(loggerName);
    if (logger == null) {
      throw new RuntimeException(NOT_WATCHING_ERROR + loggerName);
    }
    return ((ListAppender<ILoggingEvent>) logger.getAppender(APPENDER_NAME)).list;
  }

  public List<ILoggingEvent> getLog() {
    List<ILoggingEvent> allLogs = new ArrayList<>();
    for (String loggerName : allWatched.keySet()) {
      allLogs.addAll(getLog(loggerName));
    }
    allLogs.sort((event1, event2) -> Long.valueOf(event1.getTimeStamp() - event2.getTimeStamp()).intValue());
    return allLogs;
  }

  public List<ILoggingEvent> getFilteredLog(String subString) {
    List<ILoggingEvent> log = getLog();
    return filterLog(log, subString);
  }

  public List<ILoggingEvent> getFilteredLog(String loggerName, String subString) {
    List<ILoggingEvent> log = getLog(loggerName);
    return filterLog(log, subString);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    Map<String, Logger> toWatch = new HashMap<>(globallyWatched);
    WatchLogger watchLoggerAnnotation = context.getRequiredTestMethod().getAnnotation(WatchLogger.class);
    if (watchLoggerAnnotation != null) {
      Level level = Level.toLevel(watchLoggerAnnotation.level());
      if (level == null) {
        level = globalLevel;
      }
      for (String loggerName : watchLoggerAnnotation.loggerNames()) {
        Logger logger = getLogger(loggerName);
        logger.setLevel(level);
        toWatch.put(loggerName, logger);
      }
    }
    watchLoggers(toWatch);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    // Reset logback configuration
    for (Logger logger : allWatched.values()) {
      logger.detachAppender(APPENDER_NAME);
      logger.setLevel(null);
    }
    allWatched.clear();
  }

  private void watchLoggers(Map<String, Logger> loggers) {
    for (Entry<String, Logger> loggerEntry : loggers.entrySet()) {
      ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
      listAppender.setName(APPENDER_NAME);
      listAppender.start();
      Logger logger = loggerEntry.getValue();
      if (logger.getLevel() == null) {
        logger.setLevel(globalLevel);
      }
      logger.addAppender(listAppender);
      allWatched.put(loggerEntry.getKey(), logger);
    }
  }

  private List<ILoggingEvent> filterLog(List<ILoggingEvent> log, String subString) {
    List<ILoggingEvent> filteredLog = new ArrayList<>();
    for (ILoggingEvent logEntry : log) {
      if (logEntry.getFormattedMessage().contains(subString)) {
        filteredLog.add(logEntry);
      }
    }
    return filteredLog;
  }

}
