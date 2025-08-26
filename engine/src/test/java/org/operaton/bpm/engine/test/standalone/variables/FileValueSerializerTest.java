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
package org.operaton.bpm.engine.test.standalone.variables;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.variable.serializer.FileValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFields;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.type.FileValueTypeImpl;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Ronny Bräunlich
 *
 */
class FileValueSerializerTest {

  private static final String SEPARATOR = "#";
  private FileValueSerializer serializer;

  @BeforeEach
  void setUp() {
    serializer = new FileValueSerializer();
  }

  @Test
  void testTypeIsFileValueType() {
    assertThat(serializer.getType()).isInstanceOf(FileValueTypeImpl.class);
  }

  @Test
  void testWriteFilenameOnlyValue() {
    String filename = "test.txt";
    FileValue fileValue = Variables.fileValue(filename).create();
    ValueFields valueFields = new MockValueFields();

    serializer.writeValue(fileValue, valueFields);

    assertThat(valueFields.getByteArrayValue()).isNull();
    assertThat(valueFields.getTextValue()).isEqualTo(filename);
    assertThat(valueFields.getTextValue2()).isNull();
  }

  @Test
  void testWriteEmptyFilenameOnlyValue() {
    String filename = "";
    FileValue fileValue = Variables.fileValue(filename).create();
    ValueFields valueFields = new MockValueFields();

    serializer.writeValue(fileValue, valueFields);

    assertThat(valueFields.getByteArrayValue()).isNull();
    assertThat(valueFields.getTextValue()).isEqualTo(filename);
    assertThat(valueFields.getTextValue2()).isNull();
  }

  @Test
  void testWriteMimetypeAndFilenameValue() {
    String filename = "test.txt";
    String mimeType = "text/json";
    FileValue fileValue = Variables.fileValue(filename).mimeType(mimeType).create();
    ValueFields valueFields = new MockValueFields();

    serializer.writeValue(fileValue, valueFields);

    assertThat(valueFields.getByteArrayValue()).isNull();
    assertThat(valueFields.getTextValue()).isEqualTo(filename);
    assertThat(valueFields.getTextValue2()).isEqualTo(mimeType + SEPARATOR);
  }

  @Test
  void testWriteMimetypeFilenameAndBytesValue() {
    String filename = "test.txt";
    String mimeType = "text/json";
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt");
    FileValue fileValue = Variables.fileValue(filename).mimeType(mimeType).file(is).create();
    ValueFields valueFields = new MockValueFields();

    serializer.writeValue(fileValue, valueFields);

    assertThat(new String(valueFields.getByteArrayValue(), UTF_8)).isEqualTo("text");
    assertThat(valueFields.getTextValue()).isEqualTo(filename);
    assertThat(valueFields.getTextValue2()).isEqualTo(mimeType + SEPARATOR);
  }

  @Test
  void testWriteMimetypeFilenameBytesValueAndEncoding() {
    String filename = "test.txt";
    String mimeType = "text/json";
    Charset encoding = UTF_8;
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt");
    FileValue fileValue = Variables.fileValue(filename).mimeType(mimeType).encoding(encoding).file(is).create();
    ValueFields valueFields = new MockValueFields();

    serializer.writeValue(fileValue, valueFields);

    assertThat(new String(valueFields.getByteArrayValue(), UTF_8)).isEqualTo("text");
    assertThat(valueFields.getTextValue()).isEqualTo(filename);
    assertThat(valueFields.getTextValue2()).isEqualTo(mimeType + SEPARATOR + encoding.name());
  }

  @Test
  void testWriteMimetypeFilenameAndBytesValueWithShortcutMethod() throws Exception {
    File file = new File(this.getClass().getClassLoader().getResource("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt").toURI());
    FileValue fileValue = Variables.fileValue(file);
    ValueFields valueFields = new MockValueFields();

    serializer.writeValue(fileValue, valueFields);

    assertThat(new String(valueFields.getByteArrayValue(), UTF_8)).isEqualTo("text");
    assertThat(valueFields.getTextValue()).isEqualTo("simpleFile.txt");
    assertThat(valueFields.getTextValue2()).isEqualTo("text/plain" + SEPARATOR);
  }

  @Test
  void testThrowsExceptionWhenConvertingUnknownUntypedValueToTypedValue() {
    UntypedValueImpl untypedValue = (UntypedValueImpl) Variables.untypedValue(new Object());
    assertThrows(UnsupportedOperationException.class, () -> serializer.convertToTypedValue(untypedValue));
  }

  @Test
  void testReadFileNameMimeTypeAndByteArray() throws Exception {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt");
    byte[] data = new byte[is.available()];
    DataInputStream dataInputStream = new DataInputStream(is);
    dataInputStream.readFully(data);
    dataInputStream.close();
    MockValueFields valueFields = new MockValueFields();
    String filename = "file.txt";
    valueFields.setTextValue(filename);
    valueFields.setByteArrayValue(data);
    String mimeType = "text/plain";
    valueFields.setTextValue2(mimeType + SEPARATOR);

    FileValue fileValue = serializer.readValue(valueFields, true, false);

    assertThat(fileValue.getFilename()).isEqualTo(filename);
    assertThat(fileValue.getMimeType()).isEqualTo(mimeType);
    checkStreamFromValue(fileValue, "text");
  }

  @Test
  void testReadFileNameEncodingAndByteArray() throws Exception {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt");
    byte[] data = new byte[is.available()];
    DataInputStream dataInputStream = new DataInputStream(is);
    dataInputStream.readFully(data);
    dataInputStream.close();
    MockValueFields valueFields = new MockValueFields();
    String filename = "file.txt";
    valueFields.setTextValue(filename);
    valueFields.setByteArrayValue(data);
    String encoding = SEPARATOR + UTF_8;
    valueFields.setTextValue2(encoding);

    FileValue fileValue = serializer.readValue(valueFields, true, false);

    assertThat(fileValue.getFilename()).isEqualTo(filename);
    assertThat(fileValue.getEncoding()).isEqualTo(UTF_8.name());
    assertThat(fileValue.getEncodingAsCharset()).isEqualTo(UTF_8);
    checkStreamFromValue(fileValue, "text");
  }

  @Test
  void testReadFullValue() throws Exception {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt");
    byte[] data = new byte[is.available()];
    DataInputStream dataInputStream = new DataInputStream(is);
    dataInputStream.readFully(data);
    dataInputStream.close();
    MockValueFields valueFields = new MockValueFields();
    String filename = "file.txt";
    valueFields.setTextValue(filename);
    valueFields.setByteArrayValue(data);
    String mimeType = "text/plain";
    String encoding = "UTF-16";
    valueFields.setTextValue2(mimeType + SEPARATOR + encoding);

    FileValue fileValue = serializer.readValue(valueFields, true, false);

    assertThat(fileValue.getFilename()).isEqualTo(filename);
    assertThat(fileValue.getMimeType()).isEqualTo(mimeType);
    assertThat(fileValue.getEncoding()).isEqualTo("UTF-16");
    assertThat(fileValue.getEncodingAsCharset()).isEqualTo(StandardCharsets.UTF_16);
    checkStreamFromValue(fileValue, "text");
  }

  @Test
  void testReadFilenameAndByteArrayValue() throws Exception {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/standalone/variables/simpleFile.txt");
    byte[] data = new byte[is.available()];
    DataInputStream dataInputStream = new DataInputStream(is);
    dataInputStream.readFully(data);
    dataInputStream.close();
    MockValueFields valueFields = new MockValueFields();
    String filename = "file.txt";
    valueFields.setTextValue(filename);
    valueFields.setByteArrayValue(data);

    FileValue fileValue = serializer.readValue(valueFields, true, false);

    assertThat(fileValue.getFilename()).isEqualTo(filename);
    assertThat(fileValue.getMimeType()).isNull();
    checkStreamFromValue(fileValue, "text");
  }

  @Test
  void testReadFilenameValue() {
    MockValueFields valueFields = new MockValueFields();
    String filename = "file.txt";
    valueFields.setTextValue(filename);

    FileValue fileValue = serializer.readValue(valueFields, true, false);

    assertThat(fileValue.getFilename()).isEqualTo(filename);
    assertThat(fileValue.getMimeType()).isNull();
    assertThat(fileValue.getValue()).isNull();
  }

  @Test
  void testReadEmptyFilenameValue() {
    MockValueFields valueFields = new MockValueFields();
    String filename = "";
    valueFields.setTextValue(filename);

    FileValue fileValue = serializer.readValue(valueFields, true, false);

    assertThat(fileValue.getFilename()).isEmpty();
    assertThat(fileValue.getMimeType()).isNull();
    assertThat(fileValue.getValue()).isNull();
  }

  @Test
  void testReadNullFilenameValue() {
    MockValueFields valueFields = new MockValueFields();
    String filename = null;
    valueFields.setTextValue(filename);

    FileValue fileValue = serializer.readValue(valueFields, true, false);

    assertThat(fileValue.getFilename()).isEmpty();
    assertThat(fileValue.getMimeType()).isNull();
    assertThat(fileValue.getValue()).isNull();
  }

  @Test
  void testNameIsFile() {
    assertThat(serializer.getName()).isEqualTo("file");
  }

  @Test
  void testWriteFilenameAndEncodingValue() {
    String filename = "test.txt";
    String encoding = UTF_8.name();
    FileValue fileValue = Variables.fileValue(filename).encoding(encoding).create();
    ValueFields valueFields = new MockValueFields();

    serializer.writeValue(fileValue, valueFields);

    assertThat(valueFields.getByteArrayValue()).isNull();
    assertThat(valueFields.getTextValue()).isEqualTo(filename);
    assertThat(valueFields.getTextValue2()).isEqualTo(SEPARATOR + encoding);
  }

  @Test
  void testSerializeFileValueWithoutName() {
    assertThatThrownBy(() -> Variables.fileValue((String) null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UTILS-02001 Parameter 'filename' is null");
  }

  private void checkStreamFromValue(TypedValue value, String expected) {
    InputStream stream = (InputStream) value.getValue();
    try (Scanner scanner = new Scanner(stream)) {
      assertThat(scanner.nextLine()).isEqualTo(expected);
    }
  }

  private static class MockValueFields implements ValueFields {

    private String name;
    private String textValue;
    private String textValue2;
    private Long longValue;
    private Double doubleValue;
    private byte[] bytes;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getTextValue() {
      return textValue;
    }

    @Override
    public void setTextValue(String textValue) {
      this.textValue = textValue;
    }

    @Override
    public String getTextValue2() {
      return textValue2;
    }

    @Override
    public void setTextValue2(String textValue2) {
      this.textValue2 = textValue2;
    }

    @Override
    public Long getLongValue() {
      return longValue;
    }

    @Override
    public void setLongValue(Long longValue) {
      this.longValue = longValue;
    }

    @Override
    public Double getDoubleValue() {
      return doubleValue;
    }

    @Override
    public void setDoubleValue(Double doubleValue) {
      this.doubleValue = doubleValue;
    }

    @Override
    public byte[] getByteArrayValue() {
      return bytes;
    }

    @Override
    public void setByteArrayValue(byte[] bytes) {
      this.bytes = bytes;
    }

  }
}
