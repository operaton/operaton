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
package org.operaton.bpm.integrationtest.functional.spin.dataformat;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

import org.operaton.spin.DeserializationTypeValidator;
import org.operaton.spin.spi.DataFormat;
import org.operaton.spin.spi.DataFormatMapper;
import org.operaton.spin.spi.DataFormatReader;
import org.operaton.spin.spi.DataFormatWriter;
import org.operaton.spin.spi.TextBasedDataFormatReader;

/**
 * @author Thorben Lindhauer
 *
 */
public class FooDataFormat implements DataFormat<FooSpin> {

  public static final String TYPE_NAME = "FooType";
  public static final String NAME = "application/foo";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Class<? extends FooSpin> getWrapperType() {
    return FooSpin.class;
  }

  @Override
  public FooSpin createWrapperInstance(Object parameter) {
    return new FooSpin();
  }

  @Override
  public DataFormatReader getReader() {
    return new TextBasedDataFormatReader() {

      @Override
      public Object readInput(Reader reader) {
        return null;
      }

      @Override
      protected Pattern getInputDetectionPattern() {
        return Pattern.compile("foo");
      }

    };
  }

  @Override
  public DataFormatWriter getWriter() {
    return new DataFormatWriter() {

      @Override
      public void writeToWriter(Writer writer, Object input) {
        try {
          writer.write("foo");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  @Override
  public DataFormatMapper getMapper() {
    return new DataFormatMapper() {

      @Override
      public Object mapJavaToInternal(Object parameter) {
        return null;
      }

      @Override
      public <T> T mapInternalToJava(Object parameter, String typeIdentifier) {
        return mapInternalToJava(parameter, typeIdentifier, null);
      }

      @Override
      public <T> T mapInternalToJava(Object parameter, String typeIdentifier, DeserializationTypeValidator validator) {
        return (T) new Foo();
      }

      @Override
      public <T> T mapInternalToJava(Object parameter, Class<T> type) {
        return null;
      }

      @Override
      public <T> T mapInternalToJava(Object parameter, Class<T> type, DeserializationTypeValidator validator) {
        return null;
      }

      @Override
      public String getCanonicalTypeName(Object object) {
        return TYPE_NAME;
      }

      @Override
      public boolean canMap(Object parameter) {
        return parameter instanceof Foo;
      }

    };
  }

}
