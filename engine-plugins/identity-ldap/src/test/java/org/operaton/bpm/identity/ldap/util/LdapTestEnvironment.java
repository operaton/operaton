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
package org.operaton.bpm.identity.ldap.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.assertj.core.util.Files;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * LDAP test setup using apache directory</p>
 *
 * @author Bernd Ruecker
 * @author Daniel Meyer
 */
public class LdapTestEnvironment {

  private static final Logger LOG = LoggerFactory.getLogger(LdapTestEnvironment.class.getName());

  private static final String BASE_DN = "o=operaton,c=org";
  public static final String OFFICE_BERKELEY = "office-berkeley";

  protected DirectoryService service;
  protected LdapServer ldapService;
  protected String configFilePath = "ldap.properties";
  protected File workingDirectory = Files.newTemporaryFolder();

  private int numberOfUsersCreated = 0;
  private int numberOfGroupsCreated = 0;
  private int numberOfRolesCreated = 0;

  private final int numberOfUsersCreatedInBerkeleyOffice = 0;

  /**
   * initialize the schema manager and add the schema partition to directory
   * service
   *
   * @throws Exception if the schema LDIF files are not found on the classpath
   */
  protected void initSchemaPartition() throws Exception {
    InstanceLayout instanceLayout = service.getInstanceLayout();

    File schemaPartitionDirectory = new File(instanceLayout.getPartitionsDirectory(), "schema");

    // Extract the schema on disk (a brand new one) and load the registries
    if (schemaPartitionDirectory.exists()) {
      LOG.info("schema partition already exists, skipping schema extraction");
    } else {
      SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(instanceLayout.getPartitionsDirectory());
      extractor.extractOrCopy();
    }

    SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
    SchemaManager schemaManager = new DefaultSchemaManager(loader);

    // We have to load the schema now, otherwise we won't be able
    // to initialize the Partitions, as we won't be able to parse
    // and normalize their suffix Dn
    schemaManager.loadAllEnabled();

    List<Throwable> errors = schemaManager.getErrors();

    if (!errors.isEmpty()) {
      throw new Exception(I18n.err(I18n.ERR_317, Exceptions.printErrors(errors)));
    }

    service.setSchemaManager(schemaManager);

    // Init the LdifPartition with schema
    LdifPartition schemaLdifPartition = new LdifPartition(schemaManager, service.getDnFactory());
    schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());

    // The schema partition
    SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
    schemaPartition.setWrappedPartition(schemaLdifPartition);
    service.setSchemaPartition(schemaPartition);
  }

  /**
   * Initialize the server. It creates the partition, adds the index, and
   * injects the context entries for the created partitions.
   *
   * @throws Exception if there were some problems while initializing the system
   */
  protected void initializeDirectory() throws Exception {

    workingDirectory.mkdirs();
    LOG.debug("Working directory for LDAP server: {}", workingDirectory.getAbsolutePath());

    service = new DefaultDirectoryService();
    InstanceLayout il = new InstanceLayout(workingDirectory);
    service.setInstanceLayout(il);

    initSchemaPartition();

    // then the system partition
    // this is a MANDATORY partition
    // DO NOT add this via addPartition() method, trunk code complains about duplicate partition
    // while initializing
    JdbmPartition systemPartition = new JdbmPartition(service.getSchemaManager(), service.getDnFactory());
    systemPartition.setId("system");
    systemPartition.setPartitionPath(new File(service.getInstanceLayout().getPartitionsDirectory(), systemPartition.getId()).toURI());
    systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
    systemPartition.setSchemaManager(service.getSchemaManager());

    // mandatory to call this method to set the system partition
    // Note: this system partition might be removed from trunk
    service.setSystemPartition(systemPartition);

    // Disable the ChangeLog system
    service.getChangeLog().setEnabled(false);
    service.setDenormalizeOpAttrsEnabled(true);

    Partition operatonPartition = addPartition("operaton", BASE_DN, service.getDnFactory());
    addIndex(operatonPartition, "objectClass", "ou", "uid");

    service.startup();

    // Create the root entry
    if (!service.getAdminSession().exists(operatonPartition.getSuffixDn())) {
      Dn dn = new Dn(BASE_DN);
      Entry entry = service.newEntry(dn);
      entry.add("objectClass", "top", "domain", "extensibleObject");
      entry.add("dc", "operaton");
      service.getAdminSession().add(entry);
    }
  }

  /**
   * starts the LdapServer
   *
   * @throws Exception
   */
  public void startServer() throws Exception {
    ldapService = new LdapServer();
    Properties properties = loadTestProperties();
    String port = properties.getProperty("ldap.server.port");
    ldapService.setTransports(new TcpTransport(Integer.parseInt(port)));
    ldapService.setDirectoryService(service);
    ldapService.start();
  }

  public void init() throws Exception {
    // create a simple test
    init(0,0,0);
  }

  /**
   * create a specific test case. Test creates a minimum users/groups/roles but additional users can be added to reach this number.
   * @param additionalNumberOfUsers add this number of user.
   * @param additionalNumberOfGroups add this number of groups
   * @param additionalNumberOfRoles add this number of roles
   * @throws Exception
   */
  public void init(int additionalNumberOfUsers, int additionalNumberOfGroups, int additionalNumberOfRoles) throws Exception {
    initializeDirectory();
    startServer();

    createGroup("office-berlin");

    String dnRoman = createUserUid("roman", "office-berlin", "Roman", "Smirnov", "roman@operaton.org");
    String dnRobert = createUserUid("robert", "office-berlin", "Robert", "Gimbel", "robert@operaton.org");
    String dnDaniel = createUserUid("daniel", "office-berlin", "Daniel", "Meyer", "daniel@operaton.org");
    String dnGonzo = createUserUid("gonzo", "office-berlin", "Gonzo", "The Great", "gonzo@operaton.org");
    String dnRowlf = createUserUid("rowlf", "office-berlin", "Rowlf", "The Dog", "rowlf@operaton.org");
    String dnPepe = createUserUid("pepe", "office-berlin", "Pepe", "The King Prawn", "pepe@operaton.org");
    String dnRizzo = createUserUid("rizzo", "office-berlin", "Rizzo", "The Rat", "rizzo@operaton.org");

    createGroup("office-london");

    String dnOscar = createUserUid("oscar", "office-london", "Oscar", "The Crouch", "oscar@operaton.org");
    String dnMonster = createUserUid("monster", "office-london", "Cookie", "Monster", "monster@operaton.org");

    createGroup("office-home");

    // Doesn't work using backslashes, end up with two uid attributes
    // See https://issues.apache.org/jira/browse/DIRSERVER-1442
    String dnDavid = createUserUid("david(IT)", "office-home", "David", "Howe\\IT\\", "david@operaton.org");

    String dnRuecker = createUserUid("ruecker", "office-home", "Bernd", "Ruecker", "ruecker@operaton.org");

    createGroup("office-external");

    String dnFozzie = createUserCN("fozzie", "office-external", "Bear", "Fozzie", "fozzie@operaton.org");

    createRole("management", dnRuecker, dnRobert, dnDaniel);
    createRole("development", dnRoman, dnDaniel, dnOscar);
    createRole("consulting", dnRuecker);
    createRole("sales", dnRuecker, dnMonster, dnDavid);
    createRole("external", dnFozzie);
    createRole("all", dnRuecker, dnRobert, dnDaniel, dnRoman, dnOscar, dnMonster, dnDavid, dnFozzie, dnGonzo, dnRowlf, dnPepe, dnRizzo);

    // create a large number of users
    createGroup(OFFICE_BERKELEY);

    // record more than a page in this group
    for (int i = 1; i <= additionalNumberOfUsers; i++) {
      String lastName = "fisher" + String.format("%04d", i);
      createUserUid("jan.fisher." + lastName,
              OFFICE_BERKELEY,
              "jan",
              lastName,
              "jan.fisher" + lastName + "@operaton.org");
    }

    // Create a lot of groups
    for (int i = 1; i <= additionalNumberOfGroups; i++) {
      String groupName = "Paris" + String.format("%04d", i);
      createGroup(groupName);
    }

    // Create a lot of roles
    for (int i = 1; i <= additionalNumberOfRoles; i++) {
      String roleName = "Support" + String.format("%04d", i);
      createRole(roleName, dnFozzie);
    }

  }

  public int getTotalNumberOfUsersCreated() {
    return numberOfUsersCreated;
  }

  public int getTotalNumberOfGroupsCreated() {
    return numberOfGroupsCreated;
  }

  public int getTotalNumberOfRolesCreated() {
    return numberOfRolesCreated;
  }

  public int getTotalNumberOfUserInOfficeBerkeley() {
    return numberOfUsersCreatedInBerkeleyOffice;
  }

  protected String createUserUid(String user, String group, String firstname, String lastname, String email) throws Exception {
    Dn dn = new Dn("uid=" + user + ",ou=" + group + ",o=operaton,c=org");
    createUser(user, firstname, lastname, email, dn);
    return dn.getNormName();
  }

  protected String createUserCN(String user, String group, String firstname, String lastname, String email) throws Exception {
    String upRdns = "cn=" + lastname + "\\," + firstname + ",ou=" + group + ",o=operaton,c=org";
    Dn dn = new Dn(upRdns);
    createUser(user, firstname, lastname, email, dn);
    return upRdns;
  }

  protected void createUser(String user, String firstname, String lastname,
                            String email, Dn dn) throws Exception {
    if (!service.getAdminSession().exists(dn)) {
      Entry entry = service.newEntry(dn);
      entry.add("objectClass", "top", "person", "inetOrgPerson"); //, "extensibleObject"); //make extensible to allow for the "memberOf" field
      entry.add("uid", user);
      entry.add("cn", firstname);
      entry.add("sn", lastname);
      entry.add("mail", email);
      entry.add("userPassword", user.getBytes(StandardCharsets.UTF_8));
      service.getAdminSession().add(entry);
      System.out.println("created entry: " + dn.getNormName());
      numberOfUsersCreated++;
    }
  }

  /**
   * A role is implemented by a LDAP organizationalUnit
   *
   * @param name group to create
   * @throws Exception in case of error
   */
  public void createGroup(String name) throws Exception {
    Dn dn = new Dn("ou=" + name + ",o=operaton,c=org");
    if (!service.getAdminSession().exists(dn)) {
      Entry entry = service.newEntry(dn);
      entry.add("objectClass", "top", "organizationalUnit");
      entry.add("ou", name);
      service.getAdminSession().add(entry);
      System.out.println("created entry: " + dn.getNormName());
      numberOfGroupsCreated++;
    }
  }

  /**
   * A role is implemented by a LDAP groupOfNames
   *
   * @param roleName role to create (cn)
   * @param users users members of this role
   * @throws Exception in case of error
   */
  protected void createRole(String roleName, String... users) throws Exception {
    Dn dn = new Dn("ou=" + roleName + ",o=operaton,c=org");
    if (!service.getAdminSession().exists(dn)) {
      Entry entry = service.newEntry(dn);
      entry.add("objectClass", "top", "groupOfNames");
      entry.add("cn", roleName);
      for (String user : users) {
        entry.add("member", user);
      }
      service.getAdminSession().add(entry);
      numberOfRolesCreated++;
    }
  }

  /**
   * Add a new partition to the server
   *
   * @param partitionId The partition Id
   * @param partitionDn The partition DN
   * @param dnFactory the DN factory
   * @return The newly added partition
   * @throws Exception If the partition can't be added
   */
  protected Partition addPartition(String partitionId, String partitionDn, DnFactory dnFactory) throws Exception {
    // Create a new partition with the given partition id
    JdbmPartition partition = new JdbmPartition(service.getSchemaManager(), dnFactory);
    partition.setId(partitionId);
    partition.setPartitionPath(new File(service.getInstanceLayout().getPartitionsDirectory(), partitionId).toURI());
    partition.setSuffixDn(new Dn(partitionDn));
    service.addPartition(partition);

    return partition;
  }

  /**
   * Add a new set of index on the given attributes
   *
   * @param partition The partition on which we want to add index
   * @param attrs The list of attributes to index
   */
  protected void addIndex(Partition partition, String... attrs) {
    // Index some attributes on the apache partition
    Set<Index<?, String>> indexedAttributes = new HashSet<>();

    for (String attribute : attrs) {
      indexedAttributes.add(new JdbmIndex<String>(attribute, false));
    }

    ((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);
  }

  public void shutdown() {
    try {
      ldapService.stop();
      service.shutdown();
      if (workingDirectory.exists()) {
        FileUtils.deleteDirectory(workingDirectory);
      }
    } catch (Exception e) {
      LOG.error("exception while shutting down ldap", e);
    } finally {
      ldapService = null;
      service = null;
    }
  }

  protected Properties loadTestProperties() throws IOException {
    Properties properties = new Properties();
    File file = IoUtil.getFile(configFilePath);
    try (FileInputStream propertiesStream = new FileInputStream(file)) {
      properties.load(propertiesStream);
    }
    return properties;
  }

}
