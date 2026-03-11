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
package org.operaton.connect.httpclient.impl.util;

import java.util.Map;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;

import org.operaton.connect.httpclient.impl.ConnectionConfigOption;
import org.operaton.connect.httpclient.impl.HttpConnectorLogger;
import org.operaton.connect.httpclient.impl.HttpLogger;
import org.operaton.connect.httpclient.impl.RequestConfigOption;

public final class ParseUtil {

  private static final HttpConnectorLogger LOG = HttpLogger.HTTP_LOGGER;

  private ParseUtil() {
  }

  public static void parseConfigOptions(Map<String, Object> configOptions, ConnectionConfig.Builder configBuilder) {
    for (ConnectionConfigOption option : ConnectionConfigOption.values()) {
      try {
        if (configOptions.containsKey(option.getName())) {
          option.apply(configBuilder, configOptions.get(option.getName()));
        }
      } catch (ClassCastException e) {
        throw LOG.invalidConfigurationOption(option.getName(), e);
      }
    }
  }

  public static void parseConfigOptions(Map<String, Object> configOptions, RequestConfig.Builder configBuilder) {
    for (RequestConfigOption option : RequestConfigOption.values()) {
      try {
        if (configOptions.containsKey(option.getName())) {
          option.apply(configBuilder, configOptions.get(option.getName()));
        }
      } catch (ClassCastException e) {
        throw LOG.invalidConfigurationOption(option.getName(), e);
      }
    }
  }

}
