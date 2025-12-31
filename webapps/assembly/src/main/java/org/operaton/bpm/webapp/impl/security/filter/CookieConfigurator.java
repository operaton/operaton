/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.webapp.impl.security.filter;

import java.util.Arrays;
import jakarta.servlet.FilterConfig;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.webapp.impl.security.filter.util.CookieConstants;

import static org.operaton.bpm.engine.impl.util.StringUtil.hasText;

public class CookieConfigurator {

  protected static final String ENABLE_SECURE_PARAM = "enableSecureCookie";
  protected static final String ENABLE_SAME_SITE_PARAM = "enableSameSiteCookie";
  protected static final String SAME_SITE_OPTION_PARAM = "sameSiteCookieOption";
  protected static final String SAME_SITE_VALUE_PARAM = "sameSiteCookieValue";

  protected boolean isSecureCookieEnabled;
  protected boolean isSameSiteCookieEnabled;
  protected String sameSiteCookieValue;
  protected String cookieName;

  public void parseParams(FilterConfig filterConfig) {

    String enableSecureCookieInitParam = filterConfig.getInitParameter(ENABLE_SECURE_PARAM);
    if (hasText(enableSecureCookieInitParam)) {
      isSecureCookieEnabled = Boolean.parseBoolean(enableSecureCookieInitParam);
    }

    String sessionCookieName = filterConfig.getServletContext().getSessionCookieConfig().getName();
    if (hasText(sessionCookieName) && !CookieConstants.JSESSION_ID.equals(sessionCookieName)) {
      cookieName = sessionCookieName;
    }

    String enableSameSiteCookieInitParam = filterConfig.getInitParameter(ENABLE_SAME_SITE_PARAM);
    if (hasText(enableSameSiteCookieInitParam)) {
      isSameSiteCookieEnabled = Boolean.parseBoolean(enableSameSiteCookieInitParam);
    } else {
      isSameSiteCookieEnabled = true; // default
    }

    String sameSiteCookieValueInitParam = filterConfig.getInitParameter(SAME_SITE_VALUE_PARAM);
    String sameSiteCookieOptionInitParam = filterConfig.getInitParameter(SAME_SITE_OPTION_PARAM);

    this.sameSiteCookieValue = getSameSiteCookieValueInitValue(sameSiteCookieValueInitParam, sameSiteCookieOptionInitParam);
  }

  public String getConfig() {
    return getConfig(null);
  }

  public String getConfig(String currentHeader) {
    StringBuilder stringBuilder = new StringBuilder(currentHeader == null ? "" : currentHeader);

    if (isSameSiteCookieEnabled
            && (currentHeader == null || !CookieConstants.SAME_SITE_FIELD_NAME_REGEX.matcher(currentHeader).find())) {
      stringBuilder
        .append(CookieConstants.SAME_SITE_FIELD_NAME)
        .append(sameSiteCookieValue);
    }

    if (isSecureCookieEnabled
            && (currentHeader == null || !CookieConstants.SECURE_FLAG_NAME_REGEX.matcher(currentHeader).find())) {
      stringBuilder.append(CookieConstants.SECURE_FLAG_NAME);
    }

    return stringBuilder.toString();
  }

  public String getCookieName(String defaultName) {
    return hasText(cookieName) ? cookieName : defaultName;
  }

  static String getSameSiteCookieValueInitValue(String sameSiteCookieValueInitParam, String sameSiteCookieOptionInitParam) {
    if (hasText(sameSiteCookieValueInitParam) && hasText(sameSiteCookieOptionInitParam)) {
      throw new ProcessEngineException("Please either configure " + SAME_SITE_OPTION_PARAM +
              " or " + SAME_SITE_VALUE_PARAM + ".");
    }

    if (hasText(sameSiteCookieValueInitParam)) {
      return sameSiteCookieValueInitParam;
    } else if (hasText(sameSiteCookieOptionInitParam)) {
      if (sameSiteCookieOptionInitParam.equalsIgnoreCase(SameSiteOption.LAX.name())) {
        return SameSiteOption.LAX.getValue();
      } else if (sameSiteCookieOptionInitParam.equalsIgnoreCase(SameSiteOption.STRICT.name())) {
        return SameSiteOption.STRICT.getValue();
      } else {
        throw new ProcessEngineException("For %s param, please configure one of the following options: %s".formatted(
                SAME_SITE_OPTION_PARAM,
                Arrays.toString(Arrays.stream(SameSiteOption.values()).map(SameSiteOption::getValue).toArray(String[]::new))));
      }
    } else { // default
      return SameSiteOption.LAX.getValue();
    }
  }

  public enum SameSiteOption {

    LAX("Lax"),
    STRICT("Strict");

    private final String value;

    SameSiteOption(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

}
