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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache for SCIM GET responses with TTL-based expiration and max size eviction.
 */
public class ScimSimpleCache<T> {

  protected final ConcurrentHashMap<String, CacheEntry<T>> cache = new ConcurrentHashMap<>();
  protected final int maxSize;
  protected final long expirationTimeoutMs;

  protected static class CacheEntry<T> {
    final T value;
    final long createdAt;

    CacheEntry(T value) {
      this.value = value;
      this.createdAt = System.currentTimeMillis();
    }

    boolean isExpired(long timeoutMs) {
      return System.currentTimeMillis() - createdAt > timeoutMs;
    }
  }

  public ScimSimpleCache(int maxSize, long expirationTimeoutMin) {
    this.maxSize = maxSize;
    this.expirationTimeoutMs = expirationTimeoutMin * 60 * 1000;
  }

  /**
   * Get a cached value by key. Returns null if not found or expired.
   */
  public T get(String key) {
    CacheEntry<T> entry = cache.get(key);
    if (entry != null) {
      if (!entry.isExpired(expirationTimeoutMs)) {
        return entry.value;
      }
      cache.remove(key);
    }
    return null;
  }

  /**
   * Put a value in cache.
   */
  public void put(String key, T value) {
    if (value == null) {
      return;
    }
    evictExpired();
    if (cache.size() >= maxSize) {
      evictOldest(cache.size() - maxSize + 1);
    }
    cache.put(key, new CacheEntry<>(value));
  }

  /**
   * Invalidate all cache entries.
   */
  public void invalidateAll() {
    cache.clear();
  }

  /**
   * Get the current number of cached entries.
   */
  public int size() {
    return cache.size();
  }

  protected void evictExpired() {
    cache.entrySet().removeIf(entry -> entry.getValue().isExpired(expirationTimeoutMs));
  }

  protected void evictOldest(int count) {
    for (int i = 0; i < count; ++i) {
      cache.entrySet().stream()
        .min((a, b) -> Long.compare(a.getValue().createdAt, b.getValue().createdAt))
        .ifPresent(oldest -> cache.remove(oldest.getKey(), oldest.getValue()));
    }
  }
}
