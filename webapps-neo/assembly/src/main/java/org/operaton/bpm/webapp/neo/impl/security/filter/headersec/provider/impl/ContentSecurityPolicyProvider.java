/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements.
 * Modifications Copyright the Operaton contributors.
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
package org.operaton.bpm.webapp.neo.impl.security.filter.headersec.provider.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import jakarta.servlet.ServletRequest;

import org.operaton.bpm.webapp.neo.impl.security.filter.headersec.provider.HeaderSecurityProvider;
import org.operaton.bpm.webapp.neo.impl.util.ServletFilterUtil;

public class ContentSecurityPolicyProvider extends HeaderSecurityProvider {

  public static final String HEADER_NAME = "Content-Security-Policy";
  public static final String HEADER_NONCE_PLACEHOLDER = "$NONCE";
  /**
   * Deliberately different from the legacy webapp's policy.
   *
   * <p>Legacy templates its {@code index.html} through {@code ProcessEnginesFilter} and stamps the
   * nonce onto its inline scripts, so a nonce plus {@code 'strict-dynamic'} works there. The neo
   * SPA is a static document served straight off the classpath: its one {@code <script src>} tag
   * carries no nonce and nothing substitutes one. Since {@code 'strict-dynamic'} makes browsers
   * ignore {@code 'self'}, {@code https:} and {@code 'unsafe-inline'} whenever it appears, that
   * policy blocked the SPA's own bundle and the page rendered blank.</p>
   *
   * <p>So: no nonce and no {@code 'strict-dynamic'}, and in exchange the host allowlist is narrowed
   * to {@code 'self'}. The bundle is same-origin, there are no inline scripts to permit, and remote
   * plugins are opt-in and origin-checked separately — so dropping {@code https:} and
   * {@code 'unsafe-inline'} costs nothing and leaves a stricter policy than the legacy one.
   * {@code 'unsafe-eval'} stays: the bundled BPMN/DMN and FEEL viewers need it.</p>
   *
   * <p>{@code $NONCE} still works if an operator configures a custom value containing it.</p>
   */
  public static final String HEADER_DEFAULT_VALUE = ""
    + "base-uri 'self';"
    + "script-src 'self' 'unsafe-eval';"
    + "style-src 'unsafe-inline' 'self';"
    + "default-src 'self';"
    + "img-src 'self' data:;"
    + "block-all-mixed-content;"
    + "form-action 'self';"
    + "frame-ancestors 'none';"
    + "object-src 'none';"
    + "sandbox allow-forms allow-scripts allow-same-origin allow-popups allow-downloads";

  public static final String DISABLED_PARAM = "contentSecurityPolicyDisabled";
  public static final String VALUE_PARAM = "contentSecurityPolicyValue";
  public static final String ATTR_CSP_FILTER_NONCE = "org.operaton.bpm.csp.nonce";
  public static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  /**
   * The nonce gates {@code script-src}, so it must be unpredictable to an attacker.
   * {@link SecureRandom} is thread-safe, hence a single shared instance.
   */
  protected static final SecureRandom NONCE_RANDOM = new SecureRandom();

  @Override
  public Map<String, String> initParams() {
    initParams.put(VALUE_PARAM, null);
    initParams.put(DISABLED_PARAM, null);

    return initParams;
  }

  @Override
  public void parseParams() {
    String disabled = initParams.get(DISABLED_PARAM);

    if (ServletFilterUtil.isEmpty(disabled)) {
      setDisabled(false);

    } else {
      setDisabled(Boolean.parseBoolean(disabled));

    }

    String value = initParams.get(VALUE_PARAM);
    if (!ServletFilterUtil.isEmpty(value)) {
      value = normalizeString(value);
      setValue(value);

    } else {
      setValue(HEADER_DEFAULT_VALUE);

    }
  }

  protected String normalizeString(String value) {
    return value
      .trim()
      .replaceAll("\\s+", " "); // replaces [\t\n\x0B\f\r]
  }

  @Override
  public String getHeaderName() {
    return HEADER_NAME;
  }

  @Override
  public String getHeaderValue(final ServletRequest request) {
    // Only mint a nonce when the configured policy actually has somewhere to put it.
    if (!value.contains(HEADER_NONCE_PLACEHOLDER)) {
      return value;
    }

    final String nonce = generateNonce();
    request.setAttribute(ATTR_CSP_FILTER_NONCE, nonce);
    return value.replaceAll("\\" + HEADER_NONCE_PLACEHOLDER, "'nonce-%s'".formatted(nonce));
  }

  protected String generateNonce() {
    final byte[] bytes = new byte[20];
    NONCE_RANDOM.nextBytes(bytes);
    return ENCODER.encodeToString(bytes);
  }
}
