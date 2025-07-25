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
package org.operaton.bpm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.impl.digest.DatabasePrefixHandler;

class DatabasePrefixHandlerTest {

  DatabasePrefixHandler prefixHandler;

  @BeforeEach
  void inti() {
    prefixHandler = new DatabasePrefixHandler();
  }

  @Test
  void testGeneratePrefix() {

    // given
    String algorithmName = "test";

    // when
    String prefix = prefixHandler.generatePrefix(algorithmName);

    // then
    assertThat(prefix).isEqualTo("{test}");
  }

  @Test
  void testRetrieveAlgorithmName(){

    // given
    String encryptedPasswordWithPrefix = "{SHA}n3fE9/7XOmgD3BkeJlC+JLyb/Qg=";

    // when
    String algorithmName = prefixHandler.retrieveAlgorithmName(encryptedPasswordWithPrefix);

    // then
    assertThat(algorithmName).isEqualTo("SHA");
  }

  @ParameterizedTest(name = "{index} - {0}")
  @CsvSource({
      "Invalid Input, xxx{SHA}n3fE9/7XOmgD3BkeJlC+JLyb/Qg=",
      "Missing Algorithm Prefix, n3fE9/7XOmgD3BkeJlC+JLyb/Qg=",
      "Erroneous Algorithm Prefix, {SHAn3fE9/7XOmgD3BkeJlC+JLyb/Qg="
  })
  void retrieveAlgorithmNameForInvalidInput(@SuppressWarnings("unused") String testName, String encryptedPasswordWithPrefix) {

    // when
    String algorithmName = prefixHandler.retrieveAlgorithmName(encryptedPasswordWithPrefix);

    // then
    assertThat(algorithmName).isNull();
  }

  @Test
  void removePrefix() {

    // given
    String encryptedPasswordWithPrefix = "{SHA}n3fE9/7XOmgD3BkeJlC+JLyb/Qg=";

    // when
    String encryptedPassword = prefixHandler.removePrefix(encryptedPasswordWithPrefix);

    // then
    assertThat(encryptedPassword).isEqualTo("n3fE9/7XOmgD3BkeJlC+JLyb/Qg=");

  }

  @ParameterizedTest(name = "{index} - {0}")
  @CsvSource({
      "Invalid Input, xxx{SHA}n3fE9/7XOmgD3BkeJlC+JLyb/Qg=",
      "Missing Algorithm Prefix, n3fE9/7XOmgD3BkeJlC+JLyb/Qg=",
      "Erroneous Algorithm Prefix, {SHAn3fE9/7XOmgD3BkeJlC+JLyb/Qg="
  })
  void removePrefixForInvalidInput(@SuppressWarnings("unused") String testName, String encryptedPasswordWithPrefix) {
    // when
    String encryptedPassword = prefixHandler.removePrefix(encryptedPasswordWithPrefix);

    // then
    assertThat(encryptedPassword).isNull();

  }

  @Test
  void removePrefixWithMissingAlgorithmPrefix() {

    // given
    String encryptedPasswordWithPrefix = "n3fE9/7XOmgD3BkeJlC+JLyb/Qg=";

    // when
    String encryptedPassword = prefixHandler.removePrefix(encryptedPasswordWithPrefix);

    // then
    assertThat(encryptedPassword).isNull();

  }

  @Test
  void removePrefixWithErroneousAlgorithmPrefix() {

    // given
    String encryptedPasswordWithPrefix = "SHAn3fE9}/7XOmgD3BkeJlC+JLyb/Qg=";

    // when
    String encryptedPassword = prefixHandler.removePrefix(encryptedPasswordWithPrefix);

    // then
    assertThat(encryptedPassword).isNull();
  }


}
