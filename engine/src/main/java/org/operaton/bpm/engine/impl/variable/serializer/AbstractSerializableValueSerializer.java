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
package org.operaton.bpm.engine.impl.variable.serializer;

import java.util.Base64;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.variable.impl.value.UntypedValueImpl;
import org.operaton.bpm.engine.variable.type.SerializableValueType;
import org.operaton.bpm.engine.variable.value.SerializableValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Abstract base class for serializers that handle serializable values.
 * Provides common functionality for serialization and deserialization of objects.
 *
 * @param <T> the type of the serializable value
 * @author Roman Smirnov
 */
public abstract class AbstractSerializableValueSerializer<T extends SerializableValue> extends AbstractTypedValueSerializer<T> {

  // the serialization data format used by this serializer
  protected String serializationDataFormat;

  /**
   * Constructor to initialize the serializer with a specific value type and data format.
   *
   * @param type the type of the serializable value
   * @param serializationDataFormat the data format used for serialization
   */
  protected AbstractSerializableValueSerializer(SerializableValueType type, String serializationDataFormat) {
    super(type);
    this.serializationDataFormat = serializationDataFormat;
  }

  /**
   * Returns the serialization data format used by this serializer.
   *
   * @return the serialization data format
   */
  @Override
  public String getSerializationDataformat() {
    return serializationDataFormat;
  }

  /**
   * Serializes the given value and writes it to the provided value fields.
   *
   * @param value the value to serialize
   * @param valueFields the fields to write the serialized value to
   */
  public void writeValue(T value, ValueFields valueFields) {

    String serializedStringValue = value.getValueSerialized();
    byte[] serializedByteValue = null;

    if(value.isDeserialized()) {
      Object objectToSerialize = value.getValue();
      if(objectToSerialize != null) {
        // serialize to byte array
        try {
          serializedByteValue = serializeToByteArray(objectToSerialize);
          serializedStringValue = getSerializedStringValue(serializedByteValue);
        } catch(Exception e) {
          throw new ProcessEngineException("Cannot serialize object in variable '%s': ".formatted(valueFields.getName())+e.getMessage(), e);
        }
      }
    }
    else {
      if (serializedStringValue != null) {
        serializedByteValue = getSerializedBytesValue(serializedStringValue);
      }
    }

    // write value and type to fields.
    writeToValueFields(value, valueFields, serializedByteValue);

    // update the ObjectValue to keep it consistent with value fields.
    updateTypedValue(value, serializedStringValue);
  }

  /**
   * Reads and deserializes a value from the provided value fields.
   *
   * @param valueFields the fields containing the serialized value
   * @param deserializeObjectValue whether to deserialize the object value
   * @param asTransientValue whether the value is transient
   * @return the deserialized value
   */
  @Override
  public T readValue(ValueFields valueFields, boolean deserializeObjectValue, boolean asTransientValue) {

    byte[] serializedByteValue = readSerializedValueFromFields(valueFields);
    String serializedStringValue = getSerializedStringValue(serializedByteValue);

    if(deserializeObjectValue) {
      Object deserializedObject = null;
      if(serializedByteValue != null) {
        try {
          deserializedObject = deserializeFromByteArray(serializedByteValue, valueFields);
        } catch (Exception e) {
          throw new ProcessEngineException("Cannot deserialize object in variable '%s': ".formatted(valueFields.getName())+e.getMessage(), e);
        }
      }
      return createDeserializedValue(deserializedObject, serializedStringValue, valueFields, asTransientValue);
    }
    else {
      return createSerializedValue(serializedStringValue, valueFields, asTransientValue);
    }
  }

  /**
   * Creates a deserialized value from the given parameters.
   *
   * @param deserializedObject the deserialized object
   * @param serializedStringValue the serialized string representation
   * @param valueFields the value fields
   * @param asTransientValue whether the value is transient
   * @return the deserialized value
   */
  protected abstract T createDeserializedValue(Object deserializedObject, String serializedStringValue, ValueFields valueFields, boolean asTransientValue);

  /**
   * Creates a serialized value from the given parameters.
   *
   * @param serializedStringValue the serialized string representation
   * @param valueFields the value fields
   * @param asTransientValue whether the value is transient
   * @return the serialized value
   */
  protected abstract T createSerializedValue(String serializedStringValue, ValueFields valueFields, boolean asTransientValue);

  /**
   * Writes the serialized value to the provided value fields.
   *
   * @param value the value to write
   * @param valueFields the fields to write to
   * @param serializedValue the serialized value as a byte array
   */
  protected abstract void writeToValueFields(T value, ValueFields valueFields, byte[] serializedValue);

  /**
   * Updates the typed value with the serialized string representation.
   *
   * @param value the typed value to update
   * @param serializedStringValue the serialized string representation
   */
  protected abstract void updateTypedValue(T value, String serializedStringValue);

  /**
   * Reads the serialized value from the provided value fields as a byte array.
   *
   * @param valueFields the fields containing the serialized value
   * @return the serialized value as a byte array
   */
  protected byte[] readSerializedValueFromFields(ValueFields valueFields) {
    return valueFields.getByteArrayValue();
  }

  /**
   * Converts a byte array to a serialized string representation.
   *
   * @param serializedByteValue the byte array to convert
   * @return the serialized string representation
   */
  protected String getSerializedStringValue(byte[] serializedByteValue) {
    if(serializedByteValue != null) {
      if(!isSerializationTextBased()) {
        serializedByteValue = Base64.getEncoder().encode(serializedByteValue);
      }
      return StringUtil.fromBytes(serializedByteValue);
    }
    else {
      return null;
    }
  }

  /**
   * Converts a serialized string representation to a byte array.
   *
   * @param serializedStringValue the string to convert
   * @return the byte array representation
   */
  protected byte[] getSerializedBytesValue(String serializedStringValue) {
    if(serializedStringValue != null) {
      byte[] serializedByteValue = StringUtil.toByteArray(serializedStringValue);
      if (!isSerializationTextBased()) {
        serializedByteValue = Base64.getDecoder().decode(serializedByteValue);
      }
      return serializedByteValue;
    }
    else {
      return null;
    }
  }

  /**
   * Checks if the serializer can write the given typed value.
   *
   * @param typedValue the value to check
   * @return true if the serializer can write the value, false otherwise
   */
  @Override
  protected boolean canWriteValue(TypedValue typedValue) {

    if (!(typedValue instanceof SerializableValue) && !(typedValue instanceof UntypedValueImpl)) {
      return false;
    }

    if (typedValue instanceof SerializableValue serializableValue) {
      String requestedDataFormat = serializableValue.getSerializationDataFormat();
      if (!serializableValue.isDeserialized()) {
        // serialized object => dataformat must match
        return serializationDataFormat.equals(requestedDataFormat);
      } else {
        final boolean canSerialize = typedValue.getValue() == null || canSerializeValue(typedValue.getValue());
        return canSerialize && (requestedDataFormat == null || serializationDataFormat.equals(requestedDataFormat));
      }
    } else {
      return typedValue.getValue() == null || canSerializeValue(typedValue.getValue());
    }

  }


  /**
   * return true if this serializer is able to serialize the provided object.
   *
   * @param value the object to test (guaranteed to be a non-null value)
   * @return true if the serializer can handle the object.
   */
  protected abstract boolean canSerializeValue(Object value);

  // methods to be implemented by subclasses ////////////

  /**
   * Implementations must return a byte[] representation of the provided object.
   * The object is guaranteed not to be null.
   *
   * @param deserializedObject the object to serialize
   * @return the byte array value of the object
   * @throws Exception in case the object cannot be serialized
   */
  protected abstract byte[] serializeToByteArray(Object deserializedObject) throws Exception;

  /**
   * Deserialize the object from a byte array.
   *
   * @param object the object to deserialize
   * @param valueFields the value fields
   * @return the deserialized object
   * @throws Exception in case the object cannot be deserialized
   */
  protected abstract Object deserializeFromByteArray(byte[] object, ValueFields valueFields) throws Exception;

  /**
   * Return true if the serialization is text based. Return false otherwise
   *
   */
  protected abstract boolean isSerializationTextBased();

}
