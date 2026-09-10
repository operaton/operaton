/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.identity.impl.scim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for SCIM Response Cache.
 */
public class ScimResponseCacheTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testPutAndGet() throws Exception {
    ScimSimpleCache<JsonNode> cache = new ScimSimpleCache<JsonNode>(10, 5);
    JsonNode node = mapper.readTree("{\"id\":\"123\"}");
    cache.put("http://example.com/Users?filter=test", node);

    JsonNode result = cache.get("http://example.com/Users?filter=test");
    assertThat(result).isNotNull();
    assertThat(result.get("id").asText()).isEqualTo("123");
  }

  @Test
  public void testGetMissingKey() {
    ScimSimpleCache<JsonNode> cache = new ScimSimpleCache<JsonNode>(10, 5);

    JsonNode result = cache.get("http://example.com/Users?filter=test");
    assertThat(result).isNull();
  }

  @Test
  public void testNullValueNotCached() {
    ScimSimpleCache<JsonNode> cache = new ScimSimpleCache<JsonNode>(10, 5);
    cache.put("http://example.com/Users?filter=test", null);

    assertThat(cache.size()).isEqualTo(0);
  }

  @Test
  public void testMaxSizeEviction() throws Exception {
    ScimSimpleCache<JsonNode> cache = new ScimSimpleCache<JsonNode>(3, 5);

    cache.put("key1", mapper.readTree("{\"id\":\"1\"}"));
    cache.put("key2", mapper.readTree("{\"id\":\"2\"}"));
    cache.put("key3", mapper.readTree("{\"id\":\"3\"}"));
    assertThat(cache.size()).isEqualTo(3);

    // Adding a 4th entry should evict the oldest
    cache.put("key4", mapper.readTree("{\"id\":\"4\"}"));
    assertThat(cache.size()).isEqualTo(3);
    assertThat(cache.get("key1")).isNull();
    assertThat(cache.get("key4")).isNotNull();
  }

  @Test
  public void testInvalidateAll() throws Exception {
    ScimSimpleCache<JsonNode> cache = new ScimSimpleCache<JsonNode>(10, 5);

    cache.put("key1", mapper.readTree("{\"id\":\"1\"}"));
    cache.put("key2", mapper.readTree("{\"id\":\"2\"}"));

    cache.invalidateAll();
    assertThat(cache.size()).isEqualTo(0);
  }

  @Test
  public void testExpirationEviction() throws Exception {
    // Cache with 0 minute expiration (immediate expiry)
    ScimSimpleCache<JsonNode> cache = new ScimSimpleCache<JsonNode>(10, 0);

    cache.put("key1", mapper.readTree("{\"id\":\"1\"}"));

    // Wait briefly to ensure the entry expires (timeout is 0ms)
    Thread.sleep(10);

    // Entry should be expired
    JsonNode result = cache.get("key1");
    assertThat(result).isNull();
  }
}
