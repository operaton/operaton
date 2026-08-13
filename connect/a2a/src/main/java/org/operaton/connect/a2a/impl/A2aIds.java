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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Derives the ids the connector sends to an agent.
 *
 * <p>
 * A2A does not let a client choose the task id: the agent assigns it when it creates the task. What a client
 * does own is the message id and, optionally, the context id. Both are derived here from a value that is
 * stable across job retries of the same activity, which is what makes deduplication and reattachment possible
 * at all. See the module README for the limits of that.
 * </p>
 */
final class A2aIds {

  /** Enough hex to make a collision between two activity instances not worth worrying about. */
  private static final int HASH_LENGTH = 32;

  private A2aIds() {
  }

  /**
   * @param idempotencyKey a value stable across retries of one activity instance, or {@code null}
   * @return a deterministic message id, or a random one if no key was given
   */
  static String messageId(String idempotencyKey) {
    return idempotencyKey == null ? random("msg") : "msg-" + hash(idempotencyKey);
  }

  /**
   * @param idempotencyKey a value stable across retries of one activity instance, or {@code null}
   * @return a deterministic context id, or a random one if no key was given
   */
  static String contextId(String idempotencyKey) {
    return idempotencyKey == null ? random("ctx") : "ctx-" + hash(idempotencyKey);
  }

  private static String random(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }

  private static String hash(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is required of every JVM, so this cannot happen.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

}
