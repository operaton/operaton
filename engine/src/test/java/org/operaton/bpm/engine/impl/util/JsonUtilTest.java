package org.operaton.bpm.engine.impl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    Map<String, Object> result = JsonUtil.createGsonMapper().<Map<String, Object>>fromJson(json, Map.class);

    assertNotNull(result, "Result map should not be null");
    assertEquals(4, result.size(), "Result map should have exactly 4 entries");
    assertEquals("hello", result.get("keyString"));
    assertInstanceOf(String.class, result.get("keyString"));
    assertEquals(123, ((Number) result.get("keyInt")).intValue());
    assertInstanceOf(Number.class, result.get("keyInt"));
    assertEquals(45.67, ((Number) result.get("keyDouble")).doubleValue());
    assertInstanceOf(Number.class, result.get("keyDouble"));
    assertEquals(true, result.get("keyBoolean"));
    assertInstanceOf(Boolean.class, result.get("keyBoolean"));
  }

  @Test
  void createGsonMapper_nullValue() {
    String json = """
            {
              "notNullKey": "present",
              "nullKey": null
            }
            """;

    Map<String, Object> result = JsonUtil.createGsonMapper().<Map<String, Object>>fromJson(json, Map.class);

    assertTrue(result.containsKey("nullKey"), "Map should contain the 'nullKey'");
    assertNull(result.get("nullKey"), "Value for 'nullKey' must be Java null");
    assertEquals("present", result.get("notNullKey"));
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

    Map<String, Object> result = JsonUtil.createGsonMapper().<Map<String, Object>>fromJson(json, Map.class);

    assertEquals(2, result.size(), "Only primitive and null fields should be kept");
    assertTrue(result.containsKey("primitiveKey"), "Primitive key should be kept");
    assertTrue(result.containsKey("nullKey"), "Null key should be kept");
    assertFalse(result.containsKey("objectKey"), "Nested object key should be ignored/not added");
    assertFalse(result.containsKey("arrayKey"), "Array key should be ignored/not added");
  }

  @Test
  void createGsonMapper_emptyObject() {
    String json = "{}";

    Map<String, Object> result = JsonUtil.createGsonMapper().<Map<String, Object>>fromJson(json, Map.class);

    assertNotNull(result, "Result map should not be null");
    assertTrue(result.isEmpty(), "Result map should be empty for an empty JSON object");
  }

  @Test
  void createGsonMapper_invalidInputNonObject() {
    String json = "[1, 2, 3]";

    Map<String, Object> result = JsonUtil.createGsonMapper().<Map<String, Object>>fromJson(json, Map.class);

    assertNotNull(result, "Result map should not be null");
    assertTrue(result.isEmpty(), "Result map should be empty for an empty JSON object");
  }
}
