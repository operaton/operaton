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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.digest.PasswordEncryptionException;
import org.operaton.bpm.engine.impl.digest.PasswordEncryptor;
import org.operaton.bpm.engine.impl.digest.PasswordManager;
import org.operaton.bpm.engine.impl.digest.SaltGenerator;
import org.operaton.bpm.engine.impl.digest.ShaHashDigest;
import org.operaton.bpm.engine.test.api.identity.util.MyConstantSaltGenerator;
import org.operaton.bpm.engine.test.api.identity.util.MyCustomPasswordEncryptor;
import org.operaton.bpm.engine.test.api.identity.util.MyCustomPasswordEncryptorCreatingPrefixThatCannotBeResolved;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(ProcessEngineExtension.class)
class PasswordHashingTest {

  protected static final String PASSWORD = "password";
  protected static final String USER_NAME = "johndoe";
  protected static final String ALGORITHM_NAME = "awesome";

  protected IdentityService identityService;
  protected RuntimeService runtimeService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  protected PasswordEncryptor operatonDefaultEncryptor;
  protected List<PasswordEncryptor> operatonDefaultPasswordChecker;
  protected SaltGenerator operatonDefaultSaltGenerator;


  @BeforeEach
  void initialize() {
    operatonDefaultEncryptor = processEngineConfiguration.getPasswordEncryptor();
    operatonDefaultPasswordChecker = processEngineConfiguration.getCustomPasswordChecker();
    operatonDefaultSaltGenerator = processEngineConfiguration.getSaltGenerator();
  }

  @AfterEach
  void cleanUp() {
    removeAllUser();
    resetEngineConfiguration();
  }

  protected void removeAllUser() {
    List<User> list = identityService.createUserQuery().list();
    for (User user : list) {
      identityService.deleteUser(user.getId());
    }
  }

  protected void resetEngineConfiguration() {
    setEncryptors(operatonDefaultEncryptor, operatonDefaultPasswordChecker);
    processEngineConfiguration.setSaltGenerator(operatonDefaultSaltGenerator);
  }

  @Test
  void saltHashingOnHashedPasswordWithoutSaltThrowsNoError() {
    // given
    processEngineConfiguration.setSaltGenerator(new MyConstantSaltGenerator(null));
    User user = identityService.newUser(USER_NAME);
    user.setPassword(PASSWORD);

    // when
    identityService.saveUser(user);

    // then
    assertThat(identityService.checkPassword(USER_NAME, PASSWORD)).isTrue();
  }

  @Test
  void enteringTheSamePasswordShouldProduceTwoDifferentEncryptedPassword() {
    // given
    User user1 = identityService.newUser(USER_NAME);
    user1.setPassword(PASSWORD);
    identityService.saveUser(user1);

    // when
    User user2 = identityService.newUser("kermit");
    user2.setPassword(PASSWORD);
    identityService.saveUser(user2);

    // then
    assertThat(user1.getPassword()).isNotEqualTo(user2.getPassword());
  }

  @Test
  void ensurePasswordIsCorrectlyHashedWithSHA1() {
    // given
    setDefaultEncryptor(new ShaHashDigest());
    processEngineConfiguration.setSaltGenerator(new MyConstantSaltGenerator("12345678910"));
    User user = identityService.newUser(USER_NAME);
    user.setPassword(PASSWORD);
    identityService.saveUser(user);

    // when
    user = identityService.createUserQuery().userId(USER_NAME).singleResult();

    // then
    // obtain the expected value on the command line like so: echo -n password12345678910 | openssl dgst -binary -sha1 | openssl base64
    assertThat(user.getPassword()).isEqualTo("{SHA}n3fE9/7XOmgD3BkeJlC+JLyb/Qg=");
  }

  @Test
  void ensurePasswordIsCorrectlyHashedWithSHA512() {
    // given
    processEngineConfiguration.setSaltGenerator(new MyConstantSaltGenerator("12345678910"));
    User user = identityService.newUser(USER_NAME);
    user.setPassword(PASSWORD);
    identityService.saveUser(user);

    // when
    user = identityService.createUserQuery().userId(USER_NAME).singleResult();

    // then
    // obtain the expected value on the command line like so: echo -n password12345678910 | openssl dgst -binary -sha512 | openssl base64
    assertThat(user.getPassword()).isEqualTo("{SHA-512}sM1U4nCzoDbdUugvJ7dJ6rLc7t1ZPPsnAbUpTqi5nXCYp7PTZCHExuzjoxLLYoUK" +
        "Gd637jKqT8d9tpsZs3K5+g==");
  }

  @Test
  void twoEncryptorsWithSamePrefixThrowError() {

    // given two algorithms with the same prefix
    List<PasswordEncryptor> additionalEncryptorsForPasswordChecking = new LinkedList<>();
    additionalEncryptorsForPasswordChecking.add(new ShaHashDigest());
    PasswordEncryptor defaultEncryptor = new ShaHashDigest();

    // when/then
    assertThatThrownBy(() -> setEncryptors(defaultEncryptor, additionalEncryptorsForPasswordChecking))
      .isInstanceOf(PasswordEncryptionException.class)
      .hasMessageContaining("Hash algorithm with the name 'SHA' was already added");
  }

  @Test
  void prefixThatCannotBeResolvedThrowsError() {
    // given
    setDefaultEncryptor(new MyCustomPasswordEncryptorCreatingPrefixThatCannotBeResolved());
    User user = identityService.newUser(USER_NAME);
    user.setPassword(PASSWORD);
    identityService.saveUser(user);
    String userId = identityService.createUserQuery().userId(USER_NAME).singleResult().getId();

    // when/then
    assertThatThrownBy(() -> identityService.checkPassword(userId, PASSWORD))
      .isInstanceOf(PasswordEncryptionException.class)
      .hasMessageContaining("Could not resolve hash algorithm name of a hashed password");
  }

  @Test
  void plugInCustomPasswordEncryptor() {
    // given
    setEncryptors(new MyCustomPasswordEncryptor(PASSWORD, ALGORITHM_NAME), Collections.emptyList());
    User user = identityService.newUser(USER_NAME);
    user.setPassword(PASSWORD);
    identityService.saveUser(user);

    // when
    user = identityService.createUserQuery().userId(USER_NAME).singleResult();

    // then
    assertThat(user.getPassword()).isEqualTo("{%s}xxx".formatted(ALGORITHM_NAME));
  }

  @Test
  void useSeveralCustomEncryptors() {

    // given three users with different hashed passwords
    processEngineConfiguration.setSaltGenerator(new MyConstantSaltGenerator("12345678910"));

    String userName1 = "Kermit";
    createUserWithEncryptor(userName1, new MyCustomPasswordEncryptor(PASSWORD, ALGORITHM_NAME));

    String userName2 = "Fozzie";
    String anotherAlgorithmName = "marvelousAlgorithm";
    createUserWithEncryptor(userName2, new MyCustomPasswordEncryptor(PASSWORD, anotherAlgorithmName));

    String userName3 = "Gonzo";
    createUserWithEncryptor(userName3, new ShaHashDigest());

    List<PasswordEncryptor> additionalEncryptorsForPasswordChecking = new LinkedList<>();
    additionalEncryptorsForPasswordChecking.add(new MyCustomPasswordEncryptor(PASSWORD, ALGORITHM_NAME));
    additionalEncryptorsForPasswordChecking.add(new MyCustomPasswordEncryptor(PASSWORD, anotherAlgorithmName));
    PasswordEncryptor defaultEncryptor = new ShaHashDigest();
    setEncryptors(defaultEncryptor, additionalEncryptorsForPasswordChecking);

    // when
    User user1 = identityService.createUserQuery().userId(userName1).singleResult();
    User user2 = identityService.createUserQuery().userId(userName2).singleResult();
    User user3 = identityService.createUserQuery().userId(userName3).singleResult();

    // then
    assertThat(user1.getPassword()).isEqualTo("{%s}xxx".formatted(ALGORITHM_NAME));
    assertThat(user2.getPassword()).isEqualTo("{%s}xxx".formatted(anotherAlgorithmName));
    assertThat(user3.getPassword()).isEqualTo("{SHA}n3fE9/7XOmgD3BkeJlC+JLyb/Qg=");
  }

  protected void createUserWithEncryptor(String userName, PasswordEncryptor encryptor) {
    setEncryptors(encryptor, Collections.emptyList());
    User user = identityService.newUser(userName);
    user.setPassword(PASSWORD);
    identityService.saveUser(user);
  }

  protected void setDefaultEncryptor(PasswordEncryptor defaultEncryptor) {
    setEncryptors(defaultEncryptor, Collections.emptyList());
  }

  protected void setEncryptors(PasswordEncryptor defaultEncryptor, List<PasswordEncryptor> additionalEncryptorsForPasswordChecking) {
    processEngineConfiguration.setPasswordManager(new PasswordManager(defaultEncryptor, additionalEncryptorsForPasswordChecking));
  }

}
