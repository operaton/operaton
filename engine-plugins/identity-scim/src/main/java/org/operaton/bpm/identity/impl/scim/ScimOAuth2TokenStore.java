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

/**
 * Thread-safe store for OAuth2 token shared across ScimClient instances.
 * This prevents token loss when new ScimClient objects are created per session.
 */
public class ScimOAuth2TokenStore {

  private volatile String token;
  private volatile long expiryTime;

  public synchronized String getToken() {
    return token;
  }

  public synchronized void setToken(String token) {
    this.token = token;
  }

  public synchronized long getExpiryTime() {
    return expiryTime;
  }

  public synchronized void setExpiryTime(long expiryTime) {
    this.expiryTime = expiryTime;
  }

  public synchronized boolean isTokenValid() {
    return token != null && System.currentTimeMillis() < expiryTime;
  }
}
