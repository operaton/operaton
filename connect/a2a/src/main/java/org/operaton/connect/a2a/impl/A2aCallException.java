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

/**
 * A failed A2A call, classified as either worth retrying or permanent.
 *
 * <p>
 * The distinction decides what the process sees: a retryable failure is turned into a
 * {@link org.operaton.connect.ConnectorRequestException} so the job executor retries it, while a permanent
 * failure becomes a {@code BpmnError} with code {@code a2a-protocol-error} that an error boundary event can
 * catch. Classification happens in {@link SdkA2aAgent}, the only class that sees SDK exception types.
 * </p>
 */
public class A2aCallException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final boolean retryable;

  public A2aCallException(String message, Throwable cause, boolean retryable) {
    super(message, cause);
    this.retryable = retryable;
  }

  /**
   * @return {@code true} if the call failed for a reason that may go away on its own, such as a connection
   *         reset, a timeout, an HTTP 429 or an HTTP 5xx
   */
  public boolean isRetryable() {
    return retryable;
  }

}
