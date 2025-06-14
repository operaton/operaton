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
package org.operaton.bpm.engine.variable.impl.value.builder;

import org.operaton.bpm.engine.variable.impl.value.FileValueImpl;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.engine.variable.value.builder.FileValueBuilder;
import org.operaton.commons.utils.EnsureUtil;
import org.operaton.commons.utils.IoUtil;
import org.operaton.commons.utils.IoUtilException;
import static org.operaton.bpm.engine.variable.type.ValueType.FILE;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author Ronny Bräunlich
 *
 */
public class FileValueBuilderImpl implements FileValueBuilder {

  protected FileValueImpl fileValue;

  public FileValueBuilderImpl(String filename) {
    EnsureUtil.ensureNotNull("filename", filename);
    fileValue = new FileValueImpl(FILE, filename);
  }

  @Override
  public FileValue create() {
    return fileValue;
  }

  @Override
  public FileValueBuilder mimeType(String mimeType) {
    fileValue.setMimeType(mimeType);
    return this;
  }

  @Override
  public FileValueBuilder file(File file) {
    try {
      return file(IoUtil.fileAsByteArray(file));
    } catch(IoUtilException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public FileValueBuilder file(InputStream stream) {
      try {
        return file(IoUtil.inputStreamAsByteArray(stream));
	  } catch(IoUtilException e) {
	  	throw new IllegalArgumentException(e);
	  }
  }

  @Override
  public FileValueBuilder file(byte[] bytes) {
    fileValue.setValue(bytes);
    return this;
  }

  @Override
  public FileValueBuilder encoding(Charset encoding) {
    fileValue.setEncoding(encoding);
    return this;
  }

  @Override
  public FileValueBuilder encoding(String encoding) {
    fileValue.setEncoding(encoding);
    return this;
  }

  @Override
  public FileValueBuilder setTransient(boolean isTransient) {
    fileValue.setTransient(isTransient);
    return this;
  }

}
