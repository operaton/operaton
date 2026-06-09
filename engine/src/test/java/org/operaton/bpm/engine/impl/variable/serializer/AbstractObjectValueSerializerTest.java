/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.variable.serializer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractObjectValueSerializerTest {

  @Test
  void shouldRetryDeserializationWithMappedForkClassNameWhenCompatibilityMappingIsEnabled() throws Exception {
    Context.setProcessEngineConfiguration(new StandaloneInMemProcessEngineConfiguration()
      .setEnableForkClassNameCompatibilityMapping(true));

    try {
      RecordingObjectValueSerializer serializer = new RecordingObjectValueSerializer();
      TestValueFields valueFields = new TestValueFields("org.cibseven.bpm.engine.delegate.BpmnError");

      Object result = serializer.deserializeFromByteArray(new byte[0], valueFields);

      assertThat(result).isEqualTo("mapped");
      assertThat(serializer.attemptedObjectTypeNames)
        .containsExactly("org.cibseven.bpm.engine.delegate.BpmnError", BpmnError.class.getName());
    } finally {
      Context.removeProcessEngineConfiguration();
    }
  }

  @Test
  void shouldNotRetryDeserializationWhenCompatibilityMappingIsDisabled() {
    RecordingObjectValueSerializer serializer = new RecordingObjectValueSerializer();
    TestValueFields valueFields = new TestValueFields("org.cibseven.bpm.engine.delegate.BpmnError");

    assertThatThrownBy(() -> serializer.deserializeFromByteArray(new byte[0], valueFields))
      .isInstanceOf(ClassNotFoundException.class);

    assertThat(serializer.attemptedObjectTypeNames)
      .containsExactly("org.cibseven.bpm.engine.delegate.BpmnError");
  }

  private static class RecordingObjectValueSerializer extends AbstractObjectValueSerializer {

    private final List<String> attemptedObjectTypeNames = new ArrayList<>();

    private RecordingObjectValueSerializer() {
      super("test");
    }

    @Override
    public String getName() {
      return "test";
    }

    @Override
    protected String getTypeNameForDeserialized(Object deserializedObject) {
      return deserializedObject.getClass().getName();
    }

    @Override
    protected byte[] serializeToByteArray(Object deserializedObject) {
      return new byte[0];
    }

    @Override
    protected Object deserializeFromByteArray(byte[] object, String objectTypeName) throws Exception {
      attemptedObjectTypeNames.add(objectTypeName);
      if (BpmnError.class.getName().equals(objectTypeName)) {
        return "mapped";
      }
      throw new ClassNotFoundException(objectTypeName);
    }

    @Override
    protected boolean isSerializationTextBased() {
      return false;
    }

    @Override
    protected boolean canSerializeValue(Object value) {
      return true;
    }
  }

  private static class TestValueFields implements ValueFields {

    private final String textValue2;

    private TestValueFields(String textValue2) {
      this.textValue2 = textValue2;
    }

    @Override
    public String getName() {
      return "variable";
    }

    @Override
    public String getTextValue() {
      return null;
    }

    @Override
    public void setTextValue(String textValue) {
      // test stub
    }

    @Override
    public String getTextValue2() {
      return textValue2;
    }

    @Override
    public void setTextValue2(String textValue2) {
      // test stub
    }

    @Override
    public Long getLongValue() {
      return null;
    }

    @Override
    public void setLongValue(Long longValue) {
      // test stub
    }

    @Override
    public Double getDoubleValue() {
      return null;
    }

    @Override
    public void setDoubleValue(Double doubleValue) {
      // test stub
    }

    @Override
    public byte[] getByteArrayValue() {
      return new byte[0];
    }

    @Override
    public void setByteArrayValue(byte[] bytes) {
      // test stub
    }
  }
}
