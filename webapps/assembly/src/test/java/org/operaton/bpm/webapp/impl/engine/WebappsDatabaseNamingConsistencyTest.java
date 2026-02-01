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
package org.operaton.bpm.webapp.impl.engine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class WebappsDatabaseNamingConsistencyTest {

  public static final String COLUMN_NAME_REGEX = "([a-zA-Z_]*(?=[a-z]+)[a-zA-Z_]+_)[,\\s]";
  public static final String[] SCANNED_FOLDERS = { "org/operaton/bpm/cockpit/plugin/base/queries",
      "org/operaton/bpm/admin/plugin/base/queries" };

  @Test
  void shouldNotFindLowercaseDbColumnNamesInMappings() {
    // given the rule that all DB column names are created in uppercase

    // when scanning all mapping files for lowercase column names
    var errorMessageBuilder = new StringBuilder();
    var pattern = Pattern.compile(COLUMN_NAME_REGEX);
    
    assertThatCode(() -> {
      for (String scannedFolder : SCANNED_FOLDERS) {
        URL resource = getClass().getClassLoader().getResource(scannedFolder);
        if (resource == null) {
          throw new IOException("Could not find path: " + scannedFolder);
        }
        File folder = new File(resource.getFile());
        File[] filesInFolder = folder.listFiles();
        for (File file : filesInFolder) {
          AtomicInteger lineNumber = new AtomicInteger(0);
          try (Stream<String> lines = Files.lines(file.toPath())) {
            lines.forEach(line -> {
              lineNumber.getAndIncrement();
              Matcher matcher = pattern.matcher(line);
              while (matcher.find()) {
                errorMessageBuilder.append("Found illegal lowercase column name ")
                  .append(matcher.group(1))
                  .append(" in SQL ")
                  .append(file)
                  .append(" at line ")
                  .append(lineNumber)
                  .append(". All SQL column names should be uppercase.\n");
              }
            });
          }
        }
      }
    }).as("Unable to find test resource for test " + getClass().getName())
      .doesNotThrowAnyException();
    
    // then don't expect any results
    var errorMessage = errorMessageBuilder.toString();
    assertThat(errorMessage).isEmpty();
  }
}
