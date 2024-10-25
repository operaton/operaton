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
package org.operaton.bpm.engine.test.variables;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.impl.type.FileValueTypeImpl;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.commons.utils.IoUtil;

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
    assertThat(type.getName(), is("file"));
  }

  @Test
  void shouldNotHaveParent() {
    assertThat(type.getParent(), is(nullValue()));
  }

  @Test
  void isPrimitiveValue() {
    assertThat(type.isPrimitiveValueType(), is(true));
  }

  @Test
  void isNotAnAbstractType() {
    assertThat(type.isAbstract(), is(false));
  }

  @Test
  void canNotConvertFromAnyValue() {
    // we just use null to make sure false is always returned
    assertThat(type.canConvertFromTypedValue(null), is(false));
  }

  @Test
  void convertingThrowsException() {
    assertThrows(IllegalArgumentException.class, () ->
      type.convertFromTypedValue(Variables.untypedNullValue()));
  }

  @Test
  void createValueFromFile() throws URISyntaxException {
    File file = new File(this.getClass().getClassLoader().getResource("org/operaton/bpm/engine/test/variables/simpleFile.txt").toURI());
    TypedValue value = type.createValue(file, Collections.<String, Object> singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt"));
    assertThat(value, is(instanceOf(FileValue.class)));
    assertThat(value.getType(), is(instanceOf(FileValueTypeImpl.class)));
    checkStreamFromValue(value, "text");
  }

  @Test
  void createValueFromStream() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    TypedValue value = type.createValue(file, Collections.<String, Object> singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt"));
    assertThat(value, is(instanceOf(FileValue.class)));
    assertThat(value.getType(), is(instanceOf(FileValueTypeImpl.class)));
    checkStreamFromValue(value, "text");
  }

  @Test
  void createValueFromBytes() throws IOException, URISyntaxException {
    File file = new File(this.getClass().getClassLoader().getResource("org/operaton/bpm/engine/test/variables/simpleFile.txt").toURI());
    TypedValue value = type.createValue(file, Collections.<String, Object> singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt"));
    assertThat(value, is(instanceOf(FileValue.class)));
    assertThat(value.getType(), is(instanceOf(FileValueTypeImpl.class)));
    checkStreamFromValue(value, "text");
  }

  @Test
  void createValueFromObject() {
    assertThrows(IllegalArgumentException.class, () ->
      type.createValue(new Object(), Collections.<String, Object>singletonMap(FileValueTypeImpl.VALUE_INFO_FILE_NAME, "simpleFile.txt")));
  }

  @Test
  void createValueWithProperties() {
    // given
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("filename", "someFileName");
    properties.put("mimeType", "someMimeType");
    properties.put("encoding", "someEncoding");

    TypedValue value = type.createValue(file, properties);

    assertThat(value, is(instanceOf(FileValue.class)));
    FileValue fileValue = (FileValue) value;
    assertThat(fileValue.getFilename(), is("someFileName"));
    assertThat(fileValue.getMimeType(), is("someMimeType"));
    assertThat(fileValue.getEncoding(), is("someEncoding"));
  }


  @Test
  void createValueWithNullProperties() {
    // given
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("filename", "someFileName");
    properties.put("mimeType", null);
    properties.put("encoding", "someEncoding");

    // when
    try {
      type.createValue(file, properties);
      fail("expected exception");
    } catch (IllegalArgumentException e) {
      // then
      assertThat(e.getMessage(), containsString("The provided mime type is null. Set a non-null value info property with key 'filename'"));
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
      assertThat(e.getMessage(), containsString("The provided encoding is null. Set a non-null value info property with key 'encoding'"));
    }
  }

  @Test
  void cannotCreateFileWithoutName() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    assertThrows(IllegalArgumentException.class, () ->
      type.createValue(file, Collections.<String, Object>emptyMap()));
  }

  @Test
  void cannotCreateFileWithoutValueInfo() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    assertThrows(IllegalArgumentException.class, () ->
      type.createValue(file, null));
  }

  @Test
  void cannotCreateFileWithInvalidTransientFlag() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    Map<String, Object> info = new HashMap<String, Object>();
    info.put("filename", "bar");
    info.put("transient", "foo");
    try {
      type.createValue(file, info);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("The property 'transient' should have a value of type 'boolean'."));
    }
    
  }

  @Test
  void valueInfoContainsFileTypeNameTransientFlagAndCharsetEncoding() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    String fileName = "simpleFile.txt";
    String fileType = "text/plain";
    Charset encoding = Charset.forName("UTF-8");
    FileValue fileValue = Variables.fileValue(fileName).file(file).mimeType(fileType).encoding(encoding).setTransient(true).create();
    Map<String, Object> info = type.getValueInfo(fileValue);

    assertThat(info, hasEntry(FileValueTypeImpl.VALUE_INFO_FILE_NAME, (Object) fileName));
    assertThat(info, hasEntry(FileValueTypeImpl.VALUE_INFO_FILE_MIME_TYPE, (Object) fileType));
    assertThat(info, hasEntry(FileValueTypeImpl.VALUE_INFO_FILE_ENCODING, (Object) encoding.name()));
    assertThat(info, hasEntry(FileValueTypeImpl.VALUE_INFO_TRANSIENT, (Object) true));
  }

  @Test
  void valueInfoContainsFileTypeNameAndStringEncoding() {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    String fileName = "simpleFile.txt";
    String fileType = "text/plain";
    String encoding = "UTF-8";
    FileValue fileValue = Variables.fileValue(fileName).file(file).mimeType(fileType).encoding(encoding).create();
    Map<String, Object> info = type.getValueInfo(fileValue);

    assertThat(info, hasEntry(FileValueTypeImpl.VALUE_INFO_FILE_NAME, (Object) fileName));
    assertThat(info, hasEntry(FileValueTypeImpl.VALUE_INFO_FILE_MIME_TYPE, (Object) fileType));
    assertThat(info, hasEntry(FileValueTypeImpl.VALUE_INFO_FILE_ENCODING, (Object) encoding));
  }

  @Test
  void fileByteArrayIsEqualToFileValueContent() throws IOException {
    InputStream file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    String fileName = "simpleFile.txt";

    FileValue fileValue = Variables.fileValue(fileName).file(file).create();
    file = this.getClass().getClassLoader().getResourceAsStream("org/operaton/bpm/engine/test/variables/simpleFile.txt");
    assertThat(IoUtil.inputStreamAsByteArray(fileValue.getValue()), equalTo(IoUtil.inputStreamAsByteArray(file)));
  }

  @Test
  void fileByteArrayIsEqualToFileValueContentCase2() throws IOException {

    byte[] bytes = new byte[]{ -16, -128, -128, -128 };
    InputStream byteStream = new ByteArrayInputStream(bytes);

    String fileName = "simpleFile.txt";

    FileValue fileValue = Variables.fileValue(fileName).file(byteStream).create();
    assertThat(IoUtil.inputStreamAsByteArray(fileValue.getValue()), equalTo(bytes));
  }

  @Test
  void doesNotHaveParent(){
    assertThat(type.getParent(), is(nullValue()));
  }


  private void checkStreamFromValue(TypedValue value, String expected) {
    InputStream stream = (InputStream) value.getValue();
    Scanner scanner = new Scanner(stream);
    assertThat(scanner.nextLine(), is(expected));
  }
}
