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
package org.operaton.bpm.dmn.engine.test.junit5;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessEngineLoggingExtensionTest {

    private static final Logger logger = LoggerFactory.getLogger(ProcessEngineLoggingExtensionTest.class);

    @RegisterExtension
    private final ProcessEngineLoggingExtension loggingExtension = new ProcessEngineLoggingExtension();

    @BeforeEach
    void setUp() {
        // Configure the extension to watch a specific logger at the DEBUG level.
        loggingExtension.watch(getClass().getName(), Level.DEBUG);
    }

    @Test
    void testLoggingCapture() {
        // Log a message at DEBUG level
        logger.debug("This is a debug message for testing.");
        
        // Retrieve logs from the watched logger
        List<ILoggingEvent> logEvents = loggingExtension.getLog(getClass().getName());
        
        // Assert that the log list contains the expected message
        assertThat(logEvents)
                .isNotEmpty()
                .anyMatch(event -> event.getFormattedMessage().contains("This is a debug message"));
    }

    @Test
    void testFilteredLoggingCapture() {
        // Log messages at different levels
        logger.info("An informational message.");
        logger.error("An error message for testing.");

        // Retrieve only error logs from the watched logger
        List<ILoggingEvent> errorLogs = loggingExtension.getFilteredLog(getClass().getName(), "error");

        // Assert that only the error message is captured in filtered logs
        assertThat(errorLogs)
                .isNotEmpty()
                .allMatch(event -> event.getFormattedMessage().contains("error"));
    }

    @Test
    void testNoLoggerWatchedError() {
        // Attempt to retrieve logs for a non-watched logger to trigger an error
        assertThatThrownBy(() -> loggingExtension.getLog("NonExistentLogger"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(ProcessEngineLoggingExtension.NOT_WATCHING_ERROR);
    }
}
