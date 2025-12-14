/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

  @Test
  void createGsonMapper_primitiveTypes() {
    String json = """
            {
              "keyString": "hello",
              "keyInt": 123,
              "keyDouble": 45.67,
              "keyBoolean": true
            }
            """;

    Map<String, Object> result = JsonUtil.createGsonMapper().fromJson(json, Map.class);

    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(4);
    assertThat(result.get("keyString")).isEqualTo("hello");
    assertThat(((Number) result.get("keyInt")).intValue()).isEqualTo(123);
    assertThat(((Number) result.get("keyDouble")).doubleValue()).isEqualTo(45.67);
    assertThat(result.get("keyBoolean")).isEqualTo(true);
  }

  @Test
  void createGsonMapper_nullValue() {
    String json = """
            {
              "notNullKey": "present",
              "nullKey": null
            }
            """;

    Map<String, Object> result = JsonUtil.createGsonMapper().fromJson(json, Map.class);

    assertThat(result).containsKey("nullKey");
    assertThat(result.get("nullKey")).isNull();
    assertThat(result.get("notNullKey")).isEqualTo("present");
  }

  @Test
  void createGsonMapper_complexTypesIgnored() {
    String json = """
            {
              "primitiveKey": "keep me",
              "objectKey": { "nested": 1 },
              "arrayKey": [1, 2, 3],
              "nullKey": null
            }
            """;

    Map<String, Object> result = JsonUtil.createGsonMapper().fromJson(json, Map.class);

    assertThat(result).hasSize(2);
    assertThat(result).containsKey("primitiveKey");
    assertThat(result).containsKey("nullKey");
    assertThat(result).doesNotContainKey("objectKey");
    assertThat(result).doesNotContainKey("arrayKey");
  }

  @Test
  void createGsonMapper_emptyObject() {
    String json = "{}";

    Map<String, Object> result = JsonUtil.createGsonMapper().fromJson(json, Map.class);

    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void createGsonMapper_invalidInputNonObject() {
    String json = "[1, 2, 3]";

    Map<String, Object> result = JsonUtil.createGsonMapper().fromJson(json, Map.class);

    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }
}