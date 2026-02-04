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
package org.operaton.connect.httpclient.impl;

import java.util.function.BiConsumer;

import org.apache.hc.client5.http.config.ConnectionConfig.Builder;
import org.apache.hc.core5.util.Timeout;

/**
 * @since 1.1
 */
public enum ConnectionConfigOption {

  CONNECTION_TIMEOUT("connection-timeout",
      (builder, value) -> builder.setConnectTimeout(toTimeout(value))),
  SOCKET_TIMEOUT("socket-timeout",
    (builder, value) -> builder.setSocketTimeout(toTimeout(value))),
  TIME_TO_LIVE("time-to-live",
      (builder, value) -> builder.setTimeToLive(toTimeout(value))),
  VALIDATE_AFTER_INACTIVITY("validate-after-inactivity",
      (builder, value) -> builder.setValidateAfterInactivity(toTimeout(value)));

  private final String name;
  private final BiConsumer<Builder, Object> consumer;

  ConnectionConfigOption(String name, BiConsumer<Builder, Object> consumer) {
    this.name = name;
    this.consumer = consumer;
  }

  public String getName() {
    return name;
  }

  public void apply(Builder configBuilder, Object value) {
    this.consumer.accept(configBuilder, value);
  }

  private static Timeout toTimeout(Object value) {
      if (value instanceof Timeout timeout) {
          return timeout;
      } else if (value instanceof Integer millis) {
          // Backward compatibility: convert integer milliseconds to Timeout
          if (millis > 0) {
              return Timeout.ofMilliseconds(millis);
          } else {
              return Timeout.DISABLED;
          }
      } else if (value instanceof String str) {
        return toTimeout(Integer.parseInt(str));
      } else {
          throw new IllegalArgumentException("Expected Timeout or Integer, got " + value.getClass().getSimpleName());
      }
  }

}
