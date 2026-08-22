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
package org.operaton.connect.a2a.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.operaton.connect.spi.ConnectorRequest;

import static org.operaton.connect.a2a.impl.A2aConnectorLogger.LOG;

/**
 * Reads connector input parameters and coerces them into the types the connector needs.
 *
 * <p>
 * This exists because a literal {@code operaton:inputParameter} in a BPMN diagram is always a {@code String},
 * while the same parameter written as an expression can produce an {@code Integer}, a {@code Boolean}, a
 * {@code Map} or a {@code List}. Both forms have to work, otherwise a modeler has to know which one the
 * connector wants.
 * </p>
 */
final class A2aParams {

  private A2aParams() {
  }

  /**
   * @return the parameter as a string with surrounding whitespace removed, or {@code null} if unset or blank
   */
  static String string(ConnectorRequest<?> request, String name) {
    Object value = request.getRequestParameter(name);
    if (value == null) {
      return null;
    }
    String text = value.toString().trim();
    return text.isEmpty() ? null : text;
  }

  /**
   * @return the parameter as an int, or {@code fallback} if unset
   * @throws org.operaton.connect.ConnectorRequestException if the value is not a number
   */
  static int integer(ConnectorRequest<?> request, String name, int fallback) {
    Integer value = integerOrNull(request, name);
    return value == null ? fallback : value;
  }

  /**
   * @return the parameter as an Integer, or {@code null} if unset
   * @throws org.operaton.connect.ConnectorRequestException if the value is not a number
   */
  static Integer integerOrNull(ConnectorRequest<?> request, String name) {
    Object value = request.getRequestParameter(name);
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.valueOf(value.toString().trim());
    } catch (NumberFormatException e) {
      throw LOG.invalidNumber(name, value);
    }
  }

  /**
   * @return the parameter as a boolean, or {@code fallback} if unset. Anything other than {@code true},
   *         ignoring case, counts as {@code false}.
   */
  static boolean bool(ConnectorRequest<?> request, String name, boolean fallback) {
    Object value = request.getRequestParameter(name);
    if (value == null) {
      return fallback;
    }
    if (value instanceof Boolean flag) {
      return flag;
    }
    return Boolean.parseBoolean(value.toString().trim());
  }

  /**
   * @return the parameter as a map of strings to strings, never {@code null}. Entries with a null value are
   *         dropped, since an HTTP header without a value is not meaningful.
   */
  static Map<String, String> stringMap(ConnectorRequest<?> request, String name) {
    Object value = request.getRequestParameter(name);
    if (value == null) {
      return Collections.emptyMap();
    }
    if (!(value instanceof Map<?, ?> map)) {
      throw LOG.invalidMap(name, value);
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getKey() != null && entry.getValue() != null) {
        result.put(entry.getKey().toString(), entry.getValue().toString());
      }
    }
    return result;
  }

  /**
   * @return the parameter as a map of strings to arbitrary values, never {@code null}
   */
  static Map<String, Object> objectMap(ConnectorRequest<?> request, String name) {
    Object value = request.getRequestParameter(name);
    if (value == null) {
      return Collections.emptyMap();
    }
    if (!(value instanceof Map<?, ?> map)) {
      throw LOG.invalidMap(name, value);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getKey() != null) {
        result.put(entry.getKey().toString(), entry.getValue());
      }
    }
    return result;
  }

  /**
   * @return the parameter as a list of strings, never {@code null}. A plain string is split on commas, so that
   *         {@code text/plain, application/json} works as a literal in the diagram.
   */
  static List<String> stringList(ConnectorRequest<?> request, String name) {
    Object value = request.getRequestParameter(name);
    if (value == null) {
      return Collections.emptyList();
    }
    if (value instanceof String text) {
      List<String> result = new ArrayList<>();
      for (String item : text.split(",")) {
        String trimmed = item.trim();
        if (!trimmed.isEmpty()) {
          result.add(trimmed);
        }
      }
      return result;
    }
    if (!(value instanceof List<?> list)) {
      throw LOG.invalidList(name, value);
    }
    List<String> result = new ArrayList<>();
    for (Object item : list) {
      if (item != null) {
        result.add(item.toString());
      }
    }
    return result;
  }

  /**
   * @return the parameter as a list of maps, never {@code null}. Used for message parts.
   */
  @SuppressWarnings("unchecked")
  static List<Map<String, Object>> mapList(ConnectorRequest<?> request, String name) {
    Object value = request.getRequestParameter(name);
    if (value == null) {
      return Collections.emptyList();
    }
    if (!(value instanceof List<?> list)) {
      throw LOG.invalidList(name, value);
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object item : list) {
      if (item == null) {
        continue;
      }
      if (!(item instanceof Map<?, ?>)) {
        throw LOG.invalidMap(name, item);
      }
      result.add((Map<String, Object>) item);
    }
    return result;
  }

}
