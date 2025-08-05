package org.operaton.bpm.identity.ldap.util;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import java.nio.charset.StandardCharsets;

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

            var romanDn = addUserUidEntry(connection, "roman", OFFICE_BERLIN, "Roman", "Smirnow", "roman@operaton.org");
            var robertDn = addUserUidEntry(connection, "robert", OFFICE_BERLIN, "Robert", "Gimbel", "robert@operaton.org");
            var danielDn = addUserUidEntry(connection, "daniel", OFFICE_BERLIN, "Daniel", "Meyer", "daniel@operaton.org");
            var gonzoDn = addUserUidEntry(connection, "gonzo", OFFICE_BERLIN, "Gonzo", "The Great", "gonzo@operaton.org");
            var rowlfDn = addUserUidEntry(connection, "rowlf", OFFICE_BERLIN, "Rowlf", "The Dog", "rowlf@operaton.org");
            var pepeDn = addUserUidEntry(connection, "pepe", OFFICE_BERLIN, "Pepe", "The King Prawn", "pepe@operaton.org");
            var rizzoDn = addUserUidEntry(connection, "rizzo", OFFICE_BERLIN, "Rizzo", "The Rat", "rizzo@operaton.org");

            addGroupEntry(connection, OFFICE_LONDON);

            var oscarDn = addUserUidEntry(connection, "oscar", OFFICE_LONDON, "Oscar", "The Crouch", "oscar@operaton.org");
            var monsterDn = addUserUidEntry(connection, "monster", OFFICE_LONDON, "Monster", "Cookie", "monster@operaton.org");

            addGroupEntry(connection, OFFICE_HOME);

            var davidItDn = addUserUidEntry(connection, "david(IT)", OFFICE_HOME, "David", "Howe\\IT\\", "david@operaton.org");
            var rueckerDn = addUserUidEntry(connection, "ruecker", OFFICE_HOME, "Bernd", "Ruecker", "ruecker@operaton.org");

            addGroupEntry(connection, OFFICE_EXTERNAL);

            var fozzieDn = addUserCnEntry(connection, "fozzie", OFFICE_EXTERNAL, "Bear", "Fozzie", "fozzie@operaton.org");

            addRoleEntry(connection, "management", rueckerDn, robertDn, danielDn);
            addRoleEntry(connection, "development", romanDn, danielDn, oscarDn);
            addRoleEntry(connection, "consulting", rueckerDn);
            addRoleEntry(connection, "sales", rueckerDn, monsterDn, davidItDn);
            addRoleEntry(connection, "external", fozzieDn);
            addRoleEntry(connection, "all", rueckerDn, robertDn, danielDn,
                    romanDn, oscarDn, monsterDn,
                    davidItDn, fozzieDn, gonzoDn, rowlfDn, pepeDn, rizzoDn);

            addGroupEntry(connection, OFFICE_BERKELEY);

            for (int i = 1; i <= numberOfAdditionalUsers; i++) {
                String lastName = "fisher" + "%04d".formatted(i);
                addUserUidEntry(connection, "jan.fisher." + lastName,
                        OFFICE_BERKELEY,
                        "jan",
                        lastName,
                        "jan.fisher" + lastName + "@operaton.org");
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
            throw new RuntimeException("Could not initialize LDAP Context",e);
        }
    }

    private String addGroupEntry(LDAPConnection connection, String groupName) throws LDAPException {
        String orgDN = "ou=" + groupName + "," + baseDn;
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
        String userDN = "uid=" + uid +",ou=" + groupName + ",dc=operaton,dc=org";
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
        String userDN = "cn=" + lastname + "\\," + firstname + ",ou=" + groupName + ",dc=operaton,dc=org";
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
        String orgDN = "ou=" + roleName + "," + baseDn;
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
