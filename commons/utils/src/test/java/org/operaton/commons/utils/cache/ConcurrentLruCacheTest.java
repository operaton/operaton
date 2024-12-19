/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
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
package org.operaton.commons.utils.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentLruCacheTest {

  private ConcurrentLruCache<String, String> cache;

  @BeforeEach
  void createCache() {
    cache = new ConcurrentLruCache<>(3);
  }

  @Test
  void getEntryWithNotExistingKey() {
    assertThat(cache.get("not existing")).isNull();
  }

  @Test
  void getEntry() {
    cache.put("a", "1");

    assertThat(cache.size()).isEqualTo(1);
    assertThat(cache.get("a")).isEqualTo("1");
  }

  @Test
  void overrideEntry() {
    cache.put("a", "1");
    cache.put("a", "2");

    assertThat(cache.size()).isEqualTo(1);
    assertThat(cache.get("a")).isEqualTo("2");
  }

  @Test
  void removeLeastRecentlyInsertedEntry() {
    cache.put("a", "1");
    cache.put("b", "2");
    cache.put("c", "3");
    cache.put("d", "4");

    assertThat(cache.size()).isEqualTo(3);
    assertThat(cache.get("a")).isNull();
    assertThat(cache.get("b")).isEqualTo("2");
    assertThat(cache.get("c")).isEqualTo("3");
    assertThat(cache.get("d")).isEqualTo("4");
  }

  @Test
  void removeLeastRecentlyUsedEntry() {
    cache.put("a", "1");
    cache.put("b", "2");
    cache.put("c", "3");

    cache.get("a");
    cache.get("b");

    cache.put("d", "4");

    assertThat(cache.size()).isEqualTo(3);
    assertThat(cache.get("c")).isNull();
    assertThat(cache.get("a")).isEqualTo("1");
    assertThat(cache.get("b")).isEqualTo("2");
    assertThat(cache.get("d")).isEqualTo("4");
  }

  @Test
  void clearCache() {
    cache.put("a", "1");

    cache.clear();
    assertThat(cache.size()).isZero();
    assertThat(cache.get("a")).isNull();
  }

  @Test
  void failToInsertInvalidKey() {
    assertThrows(NullPointerException.class, () ->

      cache.put(null, "1"));
  }

  @Test
  void failToInsertInvalidValue() {
    assertThrows(NullPointerException.class, () ->

      cache.put("a", null));
  }

  @Test
  void failToCreateCacheWithInvalidCapacity() {
    assertThrows(IllegalArgumentException.class, () -> {

      new ConcurrentLruCache<String, String>(-1);
    });
  }

  @Test
  void removeElementInEmptyCache() {

    // given
    cache.clear();

    // when
    cache.remove("123");

    // then
    assertThat(cache.isEmpty()).isTrue();
  }

  @Test
  void removeNoneExistingKeyInCache(){
    //given
    cache.put("a", "1");
    cache.put("b", "2");
    cache.put("c", "3");

    // when
    cache.remove("d");

    // then
    assertThat(cache.get("a")).isEqualTo("1");
    assertThat(cache.get("b")).isEqualTo("2");
    assertThat(cache.get("c")).isEqualTo("3");
  }

  @Test
  void removeAllElements() {
    // given
    cache.put("a", "1");
    cache.put("b", "2");
    cache.put("c", "3");

    // when
    cache.remove("a");
    cache.remove("b");
    cache.remove("c");

    // then
    assertThat(cache.isEmpty()).isTrue();
  }


}
