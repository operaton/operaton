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
package org.operaton.bpm.engine.impl.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.telemetry.dto.JdkImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ParseUtilTest {

  // --- parseHistoryTimeToLive ---

  @ParameterizedTest
  @MethodSource("historyTimeToLiveValidArgs")
  void parseHistoryTimeToLive_shouldParseValidInput(String input, Integer expected) {
    assertThat(ParseUtil.parseHistoryTimeToLive(input)).isEqualTo(expected);
  }

  static Stream<Arguments> historyTimeToLiveValidArgs() {
    return Stream.of(
      arguments(null,    null),
      arguments("",     null),
      arguments("0",    0),
      arguments("5",    5),
      arguments("365",  365),
      arguments("P0D",  0),
      arguments("P5D",  5),
      arguments("P365D", 365)
    );
  }

  @Test
  void parseHistoryTimeToLive_shouldThrowOnNegativeValue() {
    assertThatThrownBy(() -> ParseUtil.parseHistoryTimeToLive("-1"))
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("negative value is not allowed");
  }

  @Test
  void parseHistoryTimeToLive_shouldThrowOnNonNumericValue() {
    assertThatThrownBy(() -> ParseUtil.parseHistoryTimeToLive("notANumber"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot parse historyTimeToLive");
  }

  // --- parseProcessEngineVersion ---

  @ParameterizedTest
  @MethodSource("processEngineVersionArgs")
  void parseProcessEngineVersion_shouldParseEditionAndVersion(
      String version, boolean trim, String expectedVersion, String expectedEdition) {
    ProcessEngineDetails details = ParseUtil.parseProcessEngineVersion(version, trim);
    assertThat(details.getVersion()).isEqualTo(expectedVersion);
    assertThat(details.getEdition()).isEqualTo(expectedEdition);
  }

  static Stream<Arguments> processEngineVersionArgs() {
    return Stream.of(
      arguments("1.0.0",     false, "1.0.0",     ProcessEngineDetails.EDITION_COMMUNITY),
      arguments("1.0.0-ee",  false, "1.0.0-ee",  ProcessEngineDetails.EDITION_ENTERPRISE),
      arguments("1.0.0-ee",  true,  "1.0.0",     ProcessEngineDetails.EDITION_ENTERPRISE)
    );
  }

  // --- parseServerVendor ---

  @ParameterizedTest
  @MethodSource("serverVendorArgs")
  void parseServerVendor_shouldExtractVendorName(String serverInfo, String expectedVendor) {
    assertThat(ParseUtil.parseServerVendor(serverInfo)).isEqualTo(expectedVendor);
  }

  static Stream<Arguments> serverVendorArgs() {
    return Stream.of(
      arguments("WildFly Full 26.0.0.Final (WildFly Core 18.0.1.Final)", "WildFly"),
      arguments("Apache Tomcat/9.0.50",                                  "Apache Tomcat"),
      arguments("",                                                       "")
    );
  }

  @Test
  void parseServerVendor_shouldThrowOnNullInput() {
    assertThatThrownBy(() -> ParseUtil.parseServerVendor(null))
      .isInstanceOf(NullPointerException.class);
  }

  // --- parseJdkDetails ---

  @Test
  void parseJdkDetails_shouldReturnJvmVersionAndVendor() {
    JdkImpl jdk = ParseUtil.parseJdkDetails();
    assertThat(jdk.getVersion()).isEqualTo(System.getProperty("java.version"));
    assertThat(jdk.getVendor()).isNotNull();
  }
}
