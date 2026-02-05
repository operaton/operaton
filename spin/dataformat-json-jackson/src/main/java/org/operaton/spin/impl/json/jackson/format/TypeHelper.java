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
package org.operaton.spin.impl.json.jackson.format;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Collection of helper methods to construct types.
 */
public final class TypeHelper {

  private TypeHelper() {
  }

  /**
   * Checks if the erased type has the correct number of type bindings.
   *
   * @param erasedType                  class of the type.
   * @param expectedTypeParametersCount expected number of bindings.
   * @return true if the number of type binding matches expected value.
   */
  static boolean bindingsArePresent(Class<?> erasedType, int expectedTypeParametersCount) {
    if (erasedType == null) {
      return false;
    }
    var typeParameters = erasedType.getTypeParameters();
    if (typeParameters.length == 0) {
      return false;
    }
    if (typeParameters.length != expectedTypeParametersCount) {
      throw new IllegalArgumentException(
          "Cannot create TypeBindings for class %s with %d type parameter: class expects %d type parameters.".formatted(
              erasedType.getName(), expectedTypeParametersCount, typeParameters.length));
    }
    return true;
  }

  /**
   * Constructs Java type based on the content values.
   *
   * @param value value with values.
   * @return Java type.
   */
  static JavaType constructType(Object value) {
    TypeFactory typeFactory = TypeFactory.defaultInstance();
    if (value instanceof Collection<?> collection) {
      JavaType collectionType = constructCollectionType(collection);
      if (collectionType != null) {
        return collectionType;
      }
    } else if (value instanceof Map<?, ?> map) {
      JavaType mapType = constructMapType(map);
      if (mapType != null) {
        return mapType;
      }
    }

    if (value != null) {
      return typeFactory.constructType(value.getClass());
    } else {
      return TypeFactory.unknownType();
    }
  }

  private static JavaType constructCollectionType (Collection<?> collection) {
    TypeFactory typeFactory = TypeFactory.defaultInstance();
    if (collection.isEmpty()) {
      return null;
    }

    Iterator<?> iterator = collection.iterator();
    Object element;
    do {
      element = iterator.next();

      if (bindingsArePresent(collection.getClass(), 1) && (element != null || collection.size() == 1)) {
        JavaType elementType = constructType(element);
        return typeFactory.constructCollectionType(guessCollectionType(collection), elementType);

      }
    } while (iterator.hasNext() && element == null);
    return null;
  }

  private static JavaType constructMapType (Map<?,?> map) {
    TypeFactory typeFactory = TypeFactory.defaultInstance();
    if (map.isEmpty()) {
      return null;
    }
    Set<? extends Map.Entry<?, ?>> entries = map.entrySet();
    Iterator<? extends Map.Entry<?, ?>> iterator = entries.iterator();

    Map.Entry<?, ?> entry;
    do {
      entry = iterator.next();

      if (bindingsArePresent(map.getClass(), 2) && (entry.getValue() != null || map.size() == 1)) {
        JavaType keyType = constructType(entry.getKey());
        JavaType valueType = constructType(entry.getValue());
        return typeFactory.constructMapType(Map.class, keyType, valueType);

      }
    } while (iterator.hasNext() && entry.getValue() == null);

    JavaType keyType = constructType(entry.getKey());
    return typeFactory.constructMapType(Map.class, keyType, TypeFactory.unknownType());
  }

  /**
   * Guess collection class.
   *
   * @param value collection.
   * @return class of th collection implementation.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  static Class<? extends Collection> guessCollectionType(Object value) {
    if (value instanceof Collection<?>) {
      return (Class<? extends Collection>) value.getClass();
    } else {
      throw new IllegalArgumentException(
          "Could not detect class for %s of type %s".formatted(value, value.getClass().getName()));
    }
  }
}
