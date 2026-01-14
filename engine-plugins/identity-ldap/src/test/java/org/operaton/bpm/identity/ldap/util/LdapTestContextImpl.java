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

import java.nio.charset.StandardCharsets;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import org.operaton.bpm.engine.impl.identity.IdentityProviderException;

/**
 * Implementation {@link LdapTestContext} general tests
 */
public class LdapTestContextImpl implements LdapTestContext {

    private static final String OFFICE_BERLIN = "office-berlin";
    private static final String OFFICE_LONDON = "office-london";
    private static final String OFFICE_HOME = "office-home";
    private static final String OFFICE_EXTERNAL = "office-external";
    private static final String OFFICE_BERKELEY = "office-berkeley";

    private final String host;
    private final int port;
    private final String baseDn;
    private final String password;

    private int numberOfAdditionalUsers;
    private int numberOfAdditionalGroups;
    private int numberOfAdditionalRoles;
    private int generatedUserCount;
    private int generatedGroupCount;
    private int generatedRoleCount;

    public LdapTestContextImpl(String host, int port, String baseDn, String password) {
        this.host = host;
        this.port = port;
        this.baseDn = baseDn;
        this.password = password;
        this.generatedUserCount = 0;
        this.generatedGroupCount = 0;
        this.generatedRoleCount = 0;
        this.numberOfAdditionalUsers = 0;
        this.numberOfAdditionalGroups = 0;
        this.numberOfAdditionalRoles = 0;
    }

    @Override
    public int numberOfGeneratedUsers() {
        return generatedUserCount;
    }

    @Override
    public int numberOfGeneratedGroups() {
        return generatedGroupCount;
    }

    @Override
    public int numberOfGeneratedRoles() {
        return generatedRoleCount;
    }

    @Override
    public LdapTestContext withAdditionalUsers(int numberOfAdditionalUsers) {
        this.numberOfAdditionalUsers = numberOfAdditionalUsers;
        return this;
    }

    @Override
    public LdapTestContext withAdditionalGroups(int numberOfAdditionalGroups) {
        this.numberOfAdditionalGroups = numberOfAdditionalGroups;
        return this;
    }

    @Override
    public LdapTestContext withAdditionalRoles(int numberOfAdditionalRoles) {
        this.numberOfAdditionalRoles = numberOfAdditionalRoles;
        return this;
    }

    @Override
    public LdapTestContext initialize() {
        try(var connection = new LDAPConnection(host,
                port,
                "cn=admin," + baseDn,
                password)) {

            addGroupEntry(connection, OFFICE_BERLIN);

            var kermitDn = addUserUidEntry(connection, "kermit", OFFICE_BERLIN, "Kermit", "The Frog", "kermit@operaton.org");
            var camillaDn = addUserUidEntry(connection, "camilla", OFFICE_BERLIN, "Camilla", "The Chicken", "camilla@operaton.org");
            var samDn = addUserUidEntry(connection, "sam", OFFICE_BERLIN, "Sam", "Eagle", "sam@operaton.org");
            var gonzoDn = addUserUidEntry(connection, "gonzo", OFFICE_BERLIN, "Gonzo", "The Great", "gonzo@operaton.org");
            var rowlfDn = addUserUidEntry(connection, "rowlf", OFFICE_BERLIN, "Rowlf", "The Dog", "rowlf@operaton.org");
            var pepeDn = addUserUidEntry(connection, "pepe", OFFICE_BERLIN, "Pepe", "The King Prawn", "pepe@operaton.org");
            var rizzoDn = addUserUidEntry(connection, "rizzo", OFFICE_BERLIN, "Rizzo", "The Rat", "rizzo@operaton.org");

            addGroupEntry(connection, OFFICE_LONDON);

            var oscarDn = addUserUidEntry(connection, "oscar", OFFICE_LONDON, "Oscar", "The Crouch", "oscar@operaton.org");
            var monsterDn = addUserUidEntry(connection, "monster", OFFICE_LONDON, "Monster", "Cookie", "monster@operaton.org");

            addGroupEntry(connection, OFFICE_HOME);

            var uncledeadlyItDn = addUserUidEntry(connection, "uncledeadly(IT)", OFFICE_HOME, "Uncle", "Deadly\\IT\\", "uncledeadly@operaton.org");
            var boboDn = addUserUidEntry(connection, "bobo", OFFICE_HOME, "Bobo", "The Bear", "bobo@operaton.org");

            addGroupEntry(connection, OFFICE_EXTERNAL);

            var fozzieDn = addUserCnEntry(connection, "fozzie", OFFICE_EXTERNAL, "Bear", "Fozzie", "fozzie@operaton.org");

            addRoleEntry(connection, "management", boboDn, camillaDn, samDn);
            addRoleEntry(connection, "development", kermitDn, samDn, oscarDn);
            addRoleEntry(connection, "consulting", boboDn);
            addRoleEntry(connection, "sales", boboDn, monsterDn, uncledeadlyItDn);
            addRoleEntry(connection, "external", fozzieDn);
            addRoleEntry(connection, "all", boboDn, camillaDn, samDn,
                    kermitDn, oscarDn, monsterDn,
                    uncledeadlyItDn, fozzieDn, gonzoDn, rowlfDn, pepeDn, rizzoDn);

            addGroupEntry(connection, OFFICE_BERKELEY);

            for (int i = 1; i <= numberOfAdditionalUsers; i++) {
                String lastName = "hogthrob" + "%04d".formatted(i);
                addUserUidEntry(connection, "link.hogthrob." + lastName,
                        OFFICE_BERKELEY,
                        "Link",
                        lastName,
                        "link.hogthrob" + lastName + "@operaton.org");
            }

            for (int i = 1; i <= numberOfAdditionalGroups; i++) {
                String groupName = "Paris" + "%04d".formatted(i);
                addGroupEntry(connection, groupName);
            }

            for (int i = 1; i <= numberOfAdditionalRoles; i++) {
                String roleName = "Support" + "%04d".formatted(i);
                addRoleEntry(connection, roleName, fozzieDn);
            }

            return this;
        } catch (Exception e) {
            throw new IdentityProviderException("Could not initialize LDAP Context",e);
        }
    }

    private String addGroupEntry(LDAPConnection connection, String groupName) throws LDAPException {
        String orgDN = "ou=%s,%s".formatted(groupName, baseDn);
        Entry orgEntry = new Entry(orgDN);
        orgEntry.addAttribute("objectClass",  "organizationalUnit", "top", "operatonGroup");
        orgEntry.addAttribute("ou", groupName);
        orgEntry.addAttribute("sortableGroupIdAlias", groupName);
        orgEntry.addAttribute("sortableGroupNameAlias", groupName);
        connection.add(orgEntry);
        generatedGroupCount++;
        return orgDN;

    }

    private String addUserUidEntry(LDAPConnection connection, String uid, String groupName, String firstname, String lastname, String mail) throws LDAPException {
        String userDN = "uid=%s,ou=%s,dc=operaton,dc=org".formatted(uid, groupName);
        Entry userEntry = new Entry(userDN);
        userEntry.addAttribute("objectClass",  "top", "person", "inetOrgPerson", "operatonPerson");
        userEntry.addAttribute("uid", uid);
        userEntry.addAttribute("cn", firstname);
        userEntry.addAttribute("sn", lastname);
        userEntry.addAttribute("mail", mail);
        userEntry.addAttribute("sortableUserIdAlias", uid);
        userEntry.addAttribute("firstnameAlias", firstname);
        userEntry.addAttribute("lastnameAlias", lastname);
        userEntry.addAttribute("sortableMailAlias", mail);
        userEntry.addAttribute("userPassword", uid.getBytes(StandardCharsets.UTF_8));
        generatedUserCount++;
        connection.add(userEntry);
        return userDN;
    }

    private String addUserCnEntry(LDAPConnection connection, String uid, String groupName, String firstname, String lastname, String mail) throws LDAPException {
        String userDN = "cn=%s\\,%s,ou=%s,dc=operaton,dc=org".formatted(lastname, firstname, groupName);
        Entry userEntry = new Entry(userDN);
        userEntry.addAttribute("objectClass", "top", "person", "inetOrgPerson", "operatonPerson");
        userEntry.addAttribute("uid", uid);
        userEntry.addAttribute("cn", firstname);
        userEntry.addAttribute("sn", lastname);
        userEntry.addAttribute("mail", mail);
        userEntry.addAttribute("sortableUserIdAlias", uid);
        userEntry.addAttribute("firstnameAlias", firstname);
        userEntry.addAttribute("lastnameAlias", lastname);
        userEntry.addAttribute("sortableMailAlias", mail);
        userEntry.addAttribute("userPassword", uid.getBytes(StandardCharsets.UTF_8));
        connection.add(userEntry);
        generatedUserCount++;
        return userDN;
    }

    private String addRoleEntry(LDAPConnection connection, String roleName, String... userDns) throws LDAPException {
        String orgDN = "ou=%s,%s".formatted(roleName, baseDn);
        Entry roleEntry = new Entry(orgDN);
        roleEntry.addAttribute("objectClass", "top", "groupOfNames");
        roleEntry.addAttribute("cn", roleName);
        for (String user : userDns) {
            roleEntry.addAttribute("member", user);
        }
        connection.add(roleEntry);
        generatedRoleCount++;
        return orgDN;
    }
}
