/*
 *  Copyright 2025 the Operaton contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.operaton.bpm.identity.ldap.util;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class LdapTestEnvironmentExtension implements BeforeEachCallback, AfterEachCallback {

  protected LdapTestEnvironment ldapTestEnvironment;

  protected int additionalNumberOfUsers = 0;
  protected int additionnalNumberOfGroups = 0;
  protected int additionalNumberOfRoles = 0;
  protected boolean posix = false;

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    if (posix) {
      setupPosix();
    } else {
      setupLdap();
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    if (ldapTestEnvironment != null) {
      ldapTestEnvironment.shutdown();
      ldapTestEnvironment = null;
    }
  }

  protected void setupLdap() throws Exception {
    ldapTestEnvironment = new LdapTestEnvironment();
    ldapTestEnvironment.init(additionalNumberOfUsers, additionnalNumberOfGroups, additionalNumberOfRoles);
  }

  public void setupPosix() throws Exception {
    ldapTestEnvironment = new LdapPosixTestEnvironment();
    ldapTestEnvironment.init();
  }

  public LdapTestEnvironmentExtension additionalNumberOfUsers(int additionalNumberOfUsers) {
    this.additionalNumberOfUsers = additionalNumberOfUsers;
    return this;
  }

  public LdapTestEnvironmentExtension additionnalNumberOfGroups(int additionnalNumberOfGroups) {
    this.additionnalNumberOfGroups = additionnalNumberOfGroups;
    return this;
  }

  public LdapTestEnvironmentExtension additionalNumberOfRoles(int additionalNumberOfRoles) {
    this.additionalNumberOfRoles = additionalNumberOfRoles;
    return this;
  }

  public LdapTestEnvironmentExtension posix(boolean posix) {
    this.posix = posix;
    return this;
  }

  public LdapTestEnvironment getLdapTestEnvironment() {
    return ldapTestEnvironment;
  }
}
