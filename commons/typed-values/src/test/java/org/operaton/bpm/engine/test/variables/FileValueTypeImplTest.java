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
package org.operaton.bpm.engine.test.variables;

import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.type.FileValueTypeImpl;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.commons.utils.IoUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ronny Bräunlich
 *
 */
class FileValueTypeImplTest {

  private FileValueTypeImpl type;

  @BeforeEach
  void setUp() {
    type = new FileValueTypeImpl();
  }

  @Test
  void nameShouldBeFile() {
    assertThat(type.getName()).isEqualTo("file");
  }

  @Test
  void shouldNotHaveParent() {
    assertThat(type.getParent()).isNull();
  }

  @Test
  void isPrimitiveValue() {
    assertThat(type.isPrimitiveValueType()).isTrue();
  }

  @Test
  void isNotAnAbstractType() {
    assertThat(type.isAbstract()).isFalse();
  }

  @Test
  void canNotConvertFromAnyValue() {
    // we just use null to make sure false is always returned
    assertThat(type.canConvertFromTypedValue(null)).isFalse();
  }

  @Test
  void convertingThrowsException() {
    TypedValue untypedNullValue = Variables.untypedNullValue();
    assertThatThrownBy(() -> type.convertFromTypedValue(untypedNullValue))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createValueFromFile() throws URISyntaxException {
    File file = new File(this.getClass().getClassLoader().getResource("org/operaton/bpm/engine/test/variables/simpleFile.txt").toURI());
    TypedValue value = type.createValue(file, Collections.singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt"));
    assertThat(value).isInstanceOf(FileValue.class);
    assertThat(value.getType()).isInstanceOf(FileValueTypeImpl.class);
    checkStreamFromValue(value, "text");
  }

  @Test
  void createValueFromStream() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    TypedValue value = type.createValue(file, Collections.singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt"));
    assertThat(value).isInstanceOf(FileValue.class);
    assertThat(value.getType()).isInstanceOf(FileValueTypeImpl.class);
    checkStreamFromValue(value, "text");
  }

  @Test
  void createValueFromBytes() throws Exception {
    File file = new File(this.getClass().getClassLoader().getResource("org/operaton/bpm/engine/test/variables/simpleFile.txt").toURI());
    TypedValue value = type.createValue(Files.readAllBytes(file.toPath()), Collections.singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt"));
    assertThat(value).isInstanceOf(FileValue.class);
    assertThat(value.getType()).isInstanceOf(FileValueTypeImpl.class);
    checkStreamFromValue(value, "text");
  }

  @Test
  void createValueFromObject() {
    Object value = new Object();
    Map<String, Object> valueInfo = Collections.singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt");
    assertThrows(IllegalArgumentException.class, () -> type.createValue(value, valueInfo));
  }

  @Test
  void createValueWithProperties() {
    // given
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    Map<String, Object> properties = new HashMap<>();
    properties.put("filename", "someFileName");
    properties.put("mimeType", "someMimeType");
    properties.put("encoding", "someEncoding");

    TypedValue value = type.createValue(file, properties);

    assertThat(value).isInstanceOf(FileValue.class);
    FileValue fileValue = (FileValue) value;
    assertThat(fileValue.getFilename()).isEqualTo("someFileName");
    assertThat(fileValue.getMimeType()).isEqualTo("someMimeType");
    assertThat(fileValue.getEncoding()).isEqualTo("someEncoding");
  }


  @Test
  void createValueWithNullProperties() {
    // given
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    Map<String, Object> properties = new HashMap<>();
    properties.put("filename", "someFileName");
    properties.put("mimeType", null);
    properties.put("encoding", "someEncoding");

    // when
    try {
      type.createValue(file, properties);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // then
      assertThat(e.getMessage()).contains("The provided mime type is null. Set a non-null value info property with key 'filename'");
    }

    // given
    file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");

    properties.put("mimeType", "someMimetype");
    properties.put("encoding", null);

    // when
    try {
      type.createValue(file, properties);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // then
      assertThat(e.getMessage()).contains("The provided encoding is null. Set a non-null value info property with key 'encoding'");
    }
  }

  @Test
  void cannotCreateFileWithoutName() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    Map<String, Object> emptyValueInfo = emptyMap();

    assertThatThrownBy(() -> type.createValue(file, emptyValueInfo))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotCreateFileWithoutValueInfo() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    assertThatThrownBy(() -> type.createValue(file, null))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotCreateFileWithInvalidTransientFlag() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    Map<String, Object> info = new HashMap<>();
    info.put("filename", "bar");
    info.put("transient", "foo");
    assertThatThrownBy(() -> type.createValue(file, info))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("The property 'transient' should have a value of type 'boolean'.");
  }

  @Test
  void valueInfoContainsFileTypeNameTransientFlagAndCharsetEncoding() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    String fileName = "simpleFile.txt";
    String fileType = "text/plain";
    Charset encoding = UTF_8;
    FileValue fileValue = Variables.fileValue(fileName).file(file).mimeType(fileType).encoding(encoding).setTransient(true).create();
    Map<String, Object> info = type.getValueInfo(fileValue);

    assertThat(info)
            .containsEntry(FileValueTypeImpl.VALUE_INFO_FILE_NAME, fileName)
            .containsEntry(FileValueTypeImpl.VALUE_INFO_FILE_MIME_TYPE, fileType)
            .containsEntry(FileValueTypeImpl.VALUE_INFO_FILE_ENCODING, encoding.name())
            .containsEntry(FileValueTypeImpl.VALUE_INFO_TRANSIENT, true);
  }

  @Test
  void valueInfoContainsFileTypeNameAndStringEncoding() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    String fileName = "simpleFile.txt";
    String fileType = "text/plain";
    String encoding = UTF_8.name();
    FileValue fileValue = Variables.fileValue(fileName).file(file).mimeType(fileType).encoding(encoding).create();
    Map<String, Object> info = type.getValueInfo(fileValue);

    assertThat(info)
            .containsEntry(FileValueTypeImpl.VALUE_INFO_FILE_NAME, fileName)
            .containsEntry(FileValueTypeImpl.VALUE_INFO_FILE_MIME_TYPE, fileType)
            .containsEntry(FileValueTypeImpl.VALUE_INFO_FILE_ENCODING, encoding);
  }

  @Test
  void fileByteArrayIsEqualToFileValueContent() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    String fileName = "simpleFile.txt";

    FileValue fileValue = Variables.fileValue(fileName).file(file).create();
    file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    assertThat(IoUtil.inputStreamAsByteArray(fileValue.getValue())).isEqualTo(IoUtil.inputStreamAsByteArray(file));
  }

  @Test
  void fileByteArrayIsEqualToFileValueContentCase2() {

    byte[] bytes = new byte[]{ -16, -128, -128, -128 };
    InputStream byteStream = new ByteArrayInputStream(bytes);

    String fileName = "simpleFile.txt";

    FileValue fileValue = Variables.fileValue(fileName).file(byteStream).create();
    assertThat(IoUtil.inputStreamAsByteArray(fileValue.getValue())).isEqualTo(bytes);
  }

  @Test
  void doesNotHaveParent(){
    assertThat(type.getParent()).isNull();
  }


  private void checkStreamFromValue(TypedValue value, String expected) {
    InputStream stream = (InputStream) value.getValue();
    Scanner scanner = new Scanner(stream);
    assertThat(scanner.nextLine()).isEqualTo(expected);
  }
}
