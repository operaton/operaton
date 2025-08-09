/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.identity.ldap.util;

/**
 * LDAP Testdata Context
 */
public interface LdapTestContext {

    /**
     * Returns the total number of users generated and added to the currently active LDAP test instance
     * @return total number of generated users
     */
    int numberOfGeneratedUsers();

    /**
     * Returns the total number of groups generated and added to the currently active LDAP test instance
     * @return total number of generated groups
     */
    int numberOfGeneratedGroups();

    /**
     * Returns the total number of roles generated and added to the currently active LDAP test instance
     * @return total number of generated roles
     */
    int numberOfGeneratedRoles();

    /**
     * Modifies the context to add a number of additional users when populating the LDAP test instance.
     * This method must be called before {@link #initialize()}.
     * @param numberOfAdditionalUsers Number of additional users to be added to the currently active LDAP test instance
     */
    LdapTestContext withAdditionalUsers(int numberOfAdditionalUsers);

    /**
     * Modifies the context to add a number of additional groups when populating the LDAP test instance.
     * This method must be called before {@link #initialize()}.
     * @param numberOfAdditionalGroups Number of additional groups to be added to the currently active LDAP test instance
     */
    LdapTestContext withAdditionalGroups(int numberOfAdditionalGroups);

    /**
     * Modifies the context to add a number of additional roles when populating the LDAP test instance.
     * This method must be called before {@link #initialize()}.
     * @param numberOfAdditionalRoles Number of additional roles to be added to the currently active LDAP test instance
     */
    LdapTestContext withAdditionalRoles(int numberOfAdditionalRoles);

    /**
     * Connects to an LDAP test instance and populates it with test data
     */
    LdapTestContext initialize();
}
