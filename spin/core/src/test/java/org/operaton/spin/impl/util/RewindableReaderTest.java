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
package org.operaton.spin.impl.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RewindableReaderTest {

  private static final String EXAMPLE_INPUT_STRING = "a long string with content";
  private static final int DEFAULT_BUFFER_SIZE = 10;

  protected RewindableReader reader;

  @Test
  void shouldRead() throws IOException {
    // read(char[])
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);
    assertThat(reader.getRewindBufferSize()).isEqualTo(DEFAULT_BUFFER_SIZE);
    assertThat(reader.getCurrentRewindableCapacity()).isEqualTo(DEFAULT_BUFFER_SIZE);

    char[] buffer = new char[5];
    int charactersRead = reader.read(buffer);

    String bufferAsString = new String(buffer);

    assertThat(charactersRead).isEqualTo(5);
    assertThat(reader.getCurrentRewindableCapacity()).isEqualTo(DEFAULT_BUFFER_SIZE - 5);

    assertThat(bufferAsString).isEqualTo(EXAMPLE_INPUT_STRING.substring(0, 5));

    reader.close();

    // read(char[], off, len)
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    buffer = new char[5];
    charactersRead = reader.read(buffer, 2, 3);

    assertThat(charactersRead).isEqualTo(3);

    assertThat(buffer[0]).isEqualTo((char) 0);
    assertThat(buffer[1]).isEqualTo((char) 0);

    bufferAsString = new String(Arrays.copyOfRange(buffer, 2, 4));
    assertThat(bufferAsString).isEqualTo(EXAMPLE_INPUT_STRING.substring(0, 2));

    // read()
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    char charRead = (char) reader.read();
    assertThat(charRead).isEqualTo('a');

    reader.close();
  }

  @Test
  void shouldRewind() throws IOException {
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    char[] buffer = new char[5];
    int read = reader.read(buffer);
    assertThat(read).isEqualTo(5);

    reader.rewind();

    assertThat(SpinIoUtil.getStringFromReader(reader)).isEqualTo(EXAMPLE_INPUT_STRING);
  }

  @Test
  void shouldRewindAfterRepeatedRead() throws IOException {
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    char[] buffer = new char[5];
    reader.read(buffer);
    reader.read(buffer);

    reader.rewind();

    assertThat(SpinIoUtil.getStringFromReader(reader)).isEqualTo(EXAMPLE_INPUT_STRING);
  }

  @Test
  void shouldReadAndRewindWhenEndOfInputIsReached() throws IOException {
    String input = EXAMPLE_INPUT_STRING.substring(0, 5);

    reader = newReaderInstance(input, DEFAULT_BUFFER_SIZE);

    char[] buffer = new char[10];
    int charactersRead = reader.read(buffer);
    String bufferAsString =
        new String(Arrays.copyOfRange(buffer, 0, charactersRead));

    assertThat(charactersRead).isEqualTo(5);
    assertThat(bufferAsString).isEqualTo(input);

    charactersRead = reader.read(buffer);
    assertThat(charactersRead).isEqualTo(-1);

    reader.rewind();

    charactersRead = reader.read(buffer);
    bufferAsString =
        new String(Arrays.copyOfRange(buffer, 0, charactersRead));

    assertThat(charactersRead).isEqualTo(5);
    assertThat(bufferAsString).isEqualTo(input);
  }

  @Test
  void shouldReadRemainder() throws IOException {
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    char[] buffer = new char[5];
    int read = reader.read(buffer);

    assertThat(read).isEqualTo(5);
    assertThat(SpinIoUtil.getStringFromReader(reader)).isEqualTo(EXAMPLE_INPUT_STRING.substring(5));
  }

  /**
   * When reading more characters than fits into the reader's buffer
   */
  @Test
  void shouldFailWhenRewindLimitExceeded() throws IOException {
    // exceeding with read(char[])
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    char[] buffer = new char[DEFAULT_BUFFER_SIZE + 5];
    reader.read(buffer);

    assertThat(SpinIoUtil.getStringFromReader(reader))
      .isEqualTo(EXAMPLE_INPUT_STRING.substring(DEFAULT_BUFFER_SIZE + 5));

    assertThatThrownBy(reader::rewind).isInstanceOf(IOException.class);

    // exceeding with read()
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    buffer = new char[DEFAULT_BUFFER_SIZE];
    reader.read(buffer);
    reader.read();

    assertThatThrownBy(reader::rewind).isInstanceOf(IOException.class);

    // repeated read(char[])
    reader = newReaderInstance(EXAMPLE_INPUT_STRING, DEFAULT_BUFFER_SIZE);

    buffer = new char[DEFAULT_BUFFER_SIZE];
    reader.read(buffer);
    reader.read(buffer);

    assertThatThrownBy(reader::rewind).isInstanceOf(IOException.class);
  }

  @Test
  void shouldRewindWhenNothingWasRead() throws IOException {
    reader = newReaderInstance("", DEFAULT_BUFFER_SIZE);

    int charRead = reader.read();
    assertThat(charRead).isEqualTo(-1);

    reader.rewind();

    charRead = reader.read();
    assertThat(charRead).isEqualTo(-1);
  }

  @AfterEach
  void closeReader() {
    if (reader != null) {
      SpinIoUtil.closeSilently(reader);
    }
  }

  protected RewindableReader newReaderInstance(String input, int bufferSize) {
    return new RewindableReader(new StringReader(input), bufferSize);
  }
}
