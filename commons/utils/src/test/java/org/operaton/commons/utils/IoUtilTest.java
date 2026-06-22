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
package org.operaton.commons.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sebastian Menski
 */
public class IoUtilTest {

  public static final String TEST_FILE_NAME = "org/operaton/commons/utils/testFile.txt";

  @Test
  void shouldTransformBetweenInputStreamAndString() {
    InputStream inputStream = IoUtil.stringAsInputStream("test");
    String string = IoUtil.inputStreamAsString(inputStream);
    assertThat(string).isEqualTo("test");
  }

  @Test
  void shouldTransformFromInputStreamToByteArray() {
    String testString = "Test String";
    InputStream inputStream = IoUtil.stringAsInputStream(testString);
    assertThat(IoUtil.inputStreamAsByteArray(inputStream)).isEqualTo(testString.getBytes(IoUtil.ENCODING_CHARSET));
  }

  @Test
  void shouldTransformFromStringToInputStreamToByteArray() {
    String testString = "Test String";
    InputStream inputStream = IoUtil.stringAsInputStream(testString);

    String newString = IoUtil.inputStreamAsString(inputStream);
    assertThat(testString).isEqualTo(newString);

    inputStream = IoUtil.stringAsInputStream(testString);
    byte[] newBytes = newString.getBytes(IoUtil.ENCODING_CHARSET);
    assertThat(IoUtil.inputStreamAsByteArray(inputStream)).isEqualTo(newBytes);
  }


  @Test
  void getFileContentAsString() {
    assertThat(IoUtil.fileAsString(TEST_FILE_NAME)).isEqualTo("This is a Test!");
  }

  @Test
  void shouldFailGetFileContentAsStringWithGarbageAsFilename() {
    assertThatThrownBy(() -> IoUtil.fileAsString("asd123"))
        .isInstanceOf(IoUtilException.class);
  }

  @Test
  void getFileContentAsStream() {
    // given
    var stream = IoUtil.fileAsStream(TEST_FILE_NAME);
    var reader = new BufferedReader(new InputStreamReader(stream));
    var output = new StringBuilder();

    // when/then
    assertThatCode(() -> {
      String line;
      while((line = reader.readLine()) != null) {
        output.append(line);
      }
      assertThat(output).hasToString("This is a Test!");
    })
      .withFailMessage("Something went wrong while reading the input stream")
      .doesNotThrowAnyException();
  }

  @Test
  void shouldFailGetFileContentAsStreamWithGarbageAsFilename() {
    assertThatThrownBy(() -> IoUtil.fileAsStream("asd123"))
        .isInstanceOf(IoUtilException.class);
  }

  @Test
  void getFileFromClassPath() {
    File file = IoUtil.getClasspathFile(TEST_FILE_NAME);

    assertThat(file)
      .isNotNull()
      .hasName("testFile.txt");
  }

  @Test
  void shouldFailGetFileFromClassPathWithGarbage() {
    assertThatThrownBy(() -> IoUtil.getClasspathFile("asd123"))
        .isInstanceOf(IoUtilException.class);
  }

  @Test
  void shouldFailGetFileFromClassPathWithNull() {
    assertThatThrownBy(() -> IoUtil.getClasspathFile(null))
        .isInstanceOf(IoUtilException.class);
  }

  @Test
  void shouldUseFallBackWhenCustomClassLoaderIsWrong() {
    File file = IoUtil.getClasspathFile(TEST_FILE_NAME, new ClassLoader() {
      @Override
      public URL getResource(String name) {
        return null;
      }
    });
    assertThat(file)
      .isNotNull()
      .hasName("testFile.txt");
  }

  @Test
  void shouldUseFallBackWhenCustomClassLoaderIsNull() {
    File file = IoUtil.getClasspathFile(TEST_FILE_NAME, null);
    assertThat(file)
      .isNotNull()
      .hasName("testFile.txt");
  }
}
