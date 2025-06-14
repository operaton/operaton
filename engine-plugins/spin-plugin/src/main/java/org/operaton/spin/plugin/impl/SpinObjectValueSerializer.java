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
package org.operaton.spin.plugin.impl;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.variable.serializer.AbstractObjectValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.spin.DeserializationTypeValidator;
import org.operaton.spin.spi.DataFormat;
import org.operaton.spin.spi.DataFormatMapper;
import org.operaton.spin.spi.DataFormatReader;
import org.operaton.spin.spi.DataFormatWriter;

import java.io.*;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a {@link TypedValueSerializer} for {@link ObjectValue ObjectValues} using a
 * Spin-provided {@link DataFormat} to serialize and deserialize java objects.
 *
 * @author Daniel Meyer
 *
 */
public class SpinObjectValueSerializer extends AbstractObjectValueSerializer {

  protected String name;
  protected DataFormat<?> dataFormat;
  protected DeserializationTypeValidator validator;

  public SpinObjectValueSerializer(String name, DataFormat<?> dataFormat) {
    super(dataFormat.getName());
    this.name = name;
    this.dataFormat = dataFormat;
  }

  public String getName() {
    return name;
  }

  protected boolean isSerializationTextBased() {
    // for the moment we assume that all spin data formats are text based.
    return true;
  }

  protected String getTypeNameForDeserialized(Object deserializedObject) {
    return dataFormat.getMapper().getCanonicalTypeName(deserializedObject);
  }

  protected byte[] serializeToByteArray(Object deserializedObject) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    requireNonNull(processEngineConfiguration);

    DataFormatMapper mapper = dataFormat.getMapper();
    DataFormatWriter writer = dataFormat.getWriter();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OutputStreamWriter outWriter = new OutputStreamWriter(out, processEngineConfiguration.getDefaultCharset());
    BufferedWriter bufferedWriter = new BufferedWriter(outWriter);

    try {
      Object mappedObject = mapper.mapJavaToInternal(deserializedObject);
      writer.writeToWriter(bufferedWriter, mappedObject);
      return out.toByteArray();
    }
    finally {
      IoUtil.closeSilently(out);
      IoUtil.closeSilently(outWriter);
      IoUtil.closeSilently(bufferedWriter);
    }
  }

  protected Object deserializeFromByteArray(byte[] bytes, String objectTypeName) {
    ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
    requireNonNull(processEngineConfiguration);

    DataFormatMapper mapper = dataFormat.getMapper();
    DataFormatReader reader = dataFormat.getReader();

    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    InputStreamReader inReader = new InputStreamReader(bais, processEngineConfiguration.getDefaultCharset());
    BufferedReader bufferedReader = new BufferedReader(inReader);

    try {
      Object mappedObject = reader.readInput(bufferedReader);
      return mapper.mapInternalToJava(mappedObject, objectTypeName, getValidator(processEngineConfiguration));
    }
    finally{
      IoUtil.closeSilently(bais);
      IoUtil.closeSilently(inReader);
      IoUtil.closeSilently(bufferedReader);
    }
  }

  protected boolean canSerializeValue(Object value) {
    return dataFormat.getMapper().canMap(value);
  }

  protected DeserializationTypeValidator getValidator(final ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (validator == null && processEngineConfiguration.isDeserializationTypeValidationEnabled()) {
      validator = type -> processEngineConfiguration.getDeserializationTypeValidator().validate(type);
    }
    return validator;
  }

}
