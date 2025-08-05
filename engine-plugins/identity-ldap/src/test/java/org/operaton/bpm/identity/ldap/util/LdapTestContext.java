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
