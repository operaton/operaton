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
package org.operaton.bpm.spring.boot.starter.util;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import org.springframework.core.env.PropertiesPropertySource;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.util.ProductPropertiesUtil;

import static org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties.PREFIX;

/**
 * Return the full version string of the present Operaton codebase, or
 * {@code null} if it cannot be determined.
 * <p/>
 * return the version of Operaton or {@code null}
 *
 * @see Package#getImplementationVersion()
 */
public class OperatonBpmVersion implements Supplier<String> {

  private static final String VERSION_FORMAT = "(v%s)";
  public static final String VERSION = "version";
  public static final String IS_ENTERPRISE = "is-enterprise";
  public static final String FORMATTED_VERSION = "formatted-version";

  public static String key(String name) {
    return PREFIX + "." + name;
  }

  private final String version;
  private final boolean isEnterprise;
  private final String formattedVersion;

  public OperatonBpmVersion() {
    this(ProcessEngine.class.getPackage());
  }

  OperatonBpmVersion(final Package pkg) {
    this.version = Optional.ofNullable(pkg.getImplementationVersion())
      .map(String::trim)
      .filter(v -> !v.isEmpty())
      .or(() -> Optional.ofNullable(ProductPropertiesUtil.getProductVersion()))
      .orElse("");
    this.isEnterprise = version.endsWith("-ee");
    this.formattedVersion = VERSION_FORMAT.formatted(version);
  }

  @Override
  public String get() {
    return version;
  }

  public boolean isEnterprise() {
    return isEnterprise;
  }

  public PropertiesPropertySource getPropertiesPropertySource() {
    final Properties props = new Properties();
    props.put(key(VERSION), version);
    props.put(key(IS_ENTERPRISE), isEnterprise);
    props.put(key(FORMATTED_VERSION), formattedVersion);

    return new PropertiesPropertySource(this.getClass().getSimpleName(), props);
  }


}
