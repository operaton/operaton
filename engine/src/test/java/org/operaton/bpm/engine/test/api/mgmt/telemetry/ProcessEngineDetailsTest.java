/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
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
package org.operaton.bpm.engine.test.api.mgmt.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.impl.util.ParseUtil.parseProcessEngineVersion;
import static org.operaton.bpm.engine.impl.util.ProcessEngineDetails.EDITION_COMMUNITY;
import static org.operaton.bpm.engine.impl.util.ProcessEngineDetails.EDITION_ENTERPRISE;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.impl.util.ProcessEngineDetails;
import org.operaton.bpm.engine.test.util.TestconfigProperties;

class ProcessEngineDetailsTest {

  // process engine version and edition ////////////////////////////////////////////////////////////

  @Test
  void shouldAssertProcessEngineVersionSnapshotTrimSuffix() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-SNAPSHOT", true);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0-SNAPSHOT");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_COMMUNITY);
  }

  @Test
  void shouldAssertProcessEngineVersionAlphaTrimSuffix() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-alpha1", true);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0-alpha1");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_COMMUNITY);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotAlphaEETrimSuffix() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-alpha1-ee", true);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0-alpha1");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_ENTERPRISE);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotMinorTrimSuffix() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0", true);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_COMMUNITY);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotMinorEETrimSuffix() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-ee", true);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_ENTERPRISE);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotPatchTrimSuffix() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.1-ee", true);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.1");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_ENTERPRISE);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshot() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-SNAPSHOT", false);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0-SNAPSHOT");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_COMMUNITY);
  }

  @Test
  void shouldAssertProcessEngineVersionAlpha() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-alpha1", false);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0-alpha1");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_COMMUNITY);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotAlphaEE() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-alpha1-ee", false);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0-alpha1-ee");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_ENTERPRISE);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotMinor() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0", false);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_COMMUNITY);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotMinorEE() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.0-ee", false);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.0-ee");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_ENTERPRISE);
  }

  @Test
  void shouldAssertProcessEngineVersionSnapshotPatch() {
    // when
    ProcessEngineDetails engineInfo = parseProcessEngineVersion("7.14.1-ee", false);

    // then
    assertThat(engineInfo.getVersion()).isEqualTo("7.14.1-ee");
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_ENTERPRISE);
  }

  @Test
  void shouldAssertCurrentProcessEngineVersionFromPropertiesFile() {
    // when
    // the version is not available from the package
    ProcessEngineDetails engineInfo = parseProcessEngineVersion(false);

    // then
    // the version is read from the product-info.properties file
    assertThat(engineInfo.getVersion()).isEqualTo(TestconfigProperties.getEngineVersion());
    assertThat(engineInfo.getEdition()).isEqualTo(EDITION_COMMUNITY);
  }
}
