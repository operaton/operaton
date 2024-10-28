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
package org.operaton.spin.json.tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON;
import static org.operaton.spin.json.JsonTestConstants.EXAMPLE_JSON_COLLECTION;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.spin.DataFormats;
import org.operaton.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.operaton.spin.impl.json.jackson.format.JacksonJsonDataFormatReader;

public class JsonJacksonTreeDataFormatReaderTest {

  private JacksonJsonDataFormatReader reader;
  private Reader inputReader;

  private static final int REWINDING_LIMIT = 256;

  @BeforeEach
  void setUp() {
    reader = new JacksonJsonDataFormatReader(new JacksonJsonDataFormat(DataFormats.JSON_DATAFORMAT_NAME));
  }

  @Test
  void shouldMatchJsonInput() throws IOException {
    inputReader = stringToReader(EXAMPLE_JSON);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isTrue();
    inputReader.close();

    inputReader = stringToReader(EXAMPLE_JSON_COLLECTION);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isTrue();
  }

  @Test
  void shouldMatchJsonInputWithWhitespace() throws IOException {
    inputReader = stringToReader("   " + EXAMPLE_JSON);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isTrue();
    inputReader.close();

    inputReader = stringToReader("\r\n\t   " + EXAMPLE_JSON);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isTrue();
  }

  @Test
  void shouldNotMatchInvalidJson() {
    inputReader = stringToReader("prefix " + EXAMPLE_JSON);
    assertThat(reader.canRead(inputReader, REWINDING_LIMIT)).isFalse();
  }

  public Reader stringToReader(String input) {
    return new StringReader(input);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (inputReader != null) {
      inputReader.close();
    }
  }
}
