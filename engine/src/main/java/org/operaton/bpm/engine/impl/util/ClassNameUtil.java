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
package org.operaton.bpm.engine.impl.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Tom Baeyens
 */
public final class ClassNameUtil {

  public static final String OPERATON_ENGINE_PACKAGE_PREFIX = "org.operaton.bpm";

  protected static final List<Map.Entry<String, String>> FORK_ENGINE_PACKAGE_PREFIX_MAPPINGS = List.of(
    Map.entry("org.camunda.bpm", OPERATON_ENGINE_PACKAGE_PREFIX),
    Map.entry("org.cibseven.bpm", OPERATON_ENGINE_PACKAGE_PREFIX),
    Map.entry("org.eximeebpms.bpm", OPERATON_ENGINE_PACKAGE_PREFIX),
    Map.entry("org.finos.fluxnova.bpm", OPERATON_ENGINE_PACKAGE_PREFIX)
  );

  protected static final Map<Class<?>, String> cachedNames = new ConcurrentHashMap<>();

  private ClassNameUtil() {
    // utility class
  }

  public static String getClassNameWithoutPackage(Object object) {
    return getClassNameWithoutPackage(object.getClass());
  }

  public static String getClassNameWithoutPackage(Class<?> clazz) {
    return cachedNames.computeIfAbsent(clazz, key -> {
      String fullyQualifiedClassName = key.getName();
      return fullyQualifiedClassName.substring(fullyQualifiedClassName.lastIndexOf('.') + 1);
    });
  }

  public static String mapKnownForkClassNameToOperaton(String className) {
    if (className == null) {
      return null;
    }

    for (var mapping : FORK_ENGINE_PACKAGE_PREFIX_MAPPINGS) {
      String sourcePrefix = mapping.getKey();
      if (className.equals(sourcePrefix) || className.startsWith(sourcePrefix + ".")) {
        return mapping.getValue() + className.substring(sourcePrefix.length());
      }
    }

    return className;
  }

  public static String mapKnownForkClassNamesInTextToOperaton(String text) {
    if (text == null) {
      return null;
    }

    String mappedText = text;
    for (var mapping : FORK_ENGINE_PACKAGE_PREFIX_MAPPINGS) {
      mappedText = mappedText.replace(mapping.getKey() + ".", mapping.getValue() + ".");
    }

    return mappedText;
  }
}
