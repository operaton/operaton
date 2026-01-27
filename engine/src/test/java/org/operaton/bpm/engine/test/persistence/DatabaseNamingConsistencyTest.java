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
package org.operaton.bpm.engine.test.persistence;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

public class DatabaseNamingConsistencyTest {

  public static final String COLUMN_NAME_REGEX = "([a-zA-Z_]*(?=[a-z]+)[a-zA-Z_]+_)[,\\s]";
  public static final String[] SCANNED_FOLDERS = {
      "org/operaton/bpm/engine/impl/mapping/entity/",
      "org/operaton/bpm/engine/db/create", "org/operaton/bpm/engine/db/drop",
      "org/operaton/bpm/engine/db/liquibase/baseline" };

  @Test
  void shouldNotFindLowercaseDbColumnNamesInMappings() {
    // given
    Pattern pattern = Pattern.compile(COLUMN_NAME_REGEX);
    StringBuilder errorMessageBuilder = new StringBuilder();

    // when
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
                errorMessageBuilder
                  .append("Found illegal lowercase column name ")
                  .append(matcher.group(1))
                  .append(" in SQL ")
                  .append(file)
                  .append(" at line ")
                  .append(lineNumber.get())
                  .append(". All SQL column names should be uppercase.\n");
              }
            });
          }
        }
      }
    }).doesNotThrowAnyException();

    // then
    String errorMessage = errorMessageBuilder.toString();
    if (!errorMessage.isEmpty()) {
      fail(errorMessage);
    }
  }

  @Test
  void shouldFindLowercaseDbColumnNamesInTestStrings() {
    // given
    String[] testStringsIncorrect = { "alter table ACT_RU_TASK add column TASK_sTATE_ varchar(64);",
        "${queryType} E.BUSINESS_kEY_ = #{query.processInstanceBusinessKey}", "and RES.AssIGNEE_ is null" };
    String[] testStringsCorrect = { "alter table ACT_RU_TASK add column TASK_STATE_ varchar(64);",
        "${queryType} E.BUSINESS_KEY_ = #{query.processInstanceBusinessKey}", "and RES.ASSIGNEE_ is null" };
    String[] testStrings = Stream.concat(Arrays.stream(testStringsIncorrect), Arrays.stream(testStringsCorrect))
        .toArray(String[]::new);

    Pattern pattern = Pattern.compile(COLUMN_NAME_REGEX);
    List<String> errors = new ArrayList<>();

    // when
    for (String testString : testStrings) {
      Matcher matcher = pattern.matcher(testString);
      if (matcher.find()) {
        errors.add(testString);
      }
    }

    // then
    assertThat(errors)
      .containsAll(List.of(testStringsIncorrect))
      .doesNotContainAnyElementsOf(List.of(testStringsCorrect));
  }
}
