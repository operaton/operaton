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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.util.ProductPropertiesUtil;

import static org.operaton.bpm.spring.boot.starter.util.OperatonBpmVersion.key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatonBpmVersionTest {

  protected static String currentVersion;

  public static OperatonBpmVersion operatonBpmVersion(final String version) {
    final Package pkg = mock(Package.class);
    when(pkg.getImplementationVersion()).thenReturn(version);
    return new OperatonBpmVersion(pkg);
  }

  @BeforeAll
  static void setUp() {
    currentVersion = ProcessEngine.class.getPackage().getImplementationVersion();
    if (currentVersion == null) {
      currentVersion = ProductPropertiesUtil.getProductVersion();
    }
  }

  @Test
  void currentVersion() {
    final OperatonBpmVersion version =  new OperatonBpmVersion();
    assertThat(version.isEnterprise()).isFalse();
    assertThat(version.get()).startsWith(currentVersion);

    final PropertiesPropertySource source = version.getPropertiesPropertySource();
    assertThat(source.getName()).isEqualTo("OperatonBpmVersion");
    final String versionFromPropertiesSource = (String) source.getProperty(key(OperatonBpmVersion.VERSION));
    assertThat(versionFromPropertiesSource).startsWith(currentVersion);
    assertThat(source.getProperty(key(OperatonBpmVersion.FORMATTED_VERSION))).isEqualTo("(v" + versionFromPropertiesSource + ")");
    assertThat(source.getProperty(key(OperatonBpmVersion.IS_ENTERPRISE))).isEqualTo(Boolean.FALSE);
  }

  @Test
  void isEnterprise_true() {
    assertThat(operatonBpmVersion("7.6.0-alpha3-ee").isEnterprise()).isTrue();
  }

  @Test
  void isEnterprise_false() {
    assertThat(operatonBpmVersion("7.6.0-alpha3").isEnterprise()).isFalse();
  }
}
