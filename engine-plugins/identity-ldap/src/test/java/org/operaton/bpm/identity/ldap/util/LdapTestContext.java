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
     * Returns total number of generated users added to currently active LDAP test instance
     * @return <code>int</code> total number of generated users
     */
    int numberOfGeneratedUsers();

    /**
     * Returns total number of generated groups added to currently active LDAP test instance
     * @return <code>int</code> total number of generated groups
     */
    int numberOfGeneratedGroups();

    /**
     * Returns total number of generated roles added to currently active LDAP test instance
     * @return <code>int</code> total number of generated roles
     */
    int numberOfGeneratedRoles();

    /**
     * Modifies context to add number of additional users when populating LDAP test instance.
     * Must be called before <b>initialize</b> Method
     * @param numberOfAdditionalUsers <code>int</code> number of additional users to add to currently active LDAP test instance
     * @return <code>this</code> itself
     */
    LdapTestContext withAdditionalUsers(int numberOfAdditionalUsers);

    /**
     * Modifies context to add number of additional groups when populating LDAP test instance.
     * Must be called before <b>initialize</b> Method
     * @param numberOfAdditionalGroups <code>int</code> number of additional groups to add to currently active LDAP test instance
     * @return <code>this</code> itself
     */
    LdapTestContext withAdditionalGroups(int numberOfAdditionalGroups);

    /**
     * Modifies context to add number of additional roles when populating LDAP test instance.
     * Must be called before <b>initialize</b> Method
     * @param numberOfAdditionalRoles <code>int</code> number of additional roles to add to currently active LDAP test instance
     * @return <code>this</code> itself
     */
    LdapTestContext withAdditionalRoles(int numberOfAdditionalRoles);

    /**
     * Attempts to connect to LDAP test instance and populate it with test data
     * @return <code>this</code> itself
     */
    LdapTestContext initialize();
}
