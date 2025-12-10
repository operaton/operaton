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
package org.operaton.bpm.sql.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.change.Change;
import liquibase.change.core.SQLFileChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.ObjectChangeFilter;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.UniqueConstraint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import org.operaton.commons.utils.IoUtil;

import static org.assertj.core.api.Assertions.assertThat;

class SqlScriptTest {

  /*
   * The following unique constraints are present on both databases (manual and
   * by changelog) but are not reliably contained in database snapshots created
   * by Liquibase#diff. They have been manually confirmed and can be ignored in
   * the comparison in case they are missing in either database.
   */
  static final List<String> IGNORED_CONSTRAINTS = Arrays.asList(
      "ACT_UNIQ_VARIABLE",
      "CONSTRAINT_8D1",// used on all but PostgreSQL
      "ACT_HI_PROCINST_PROC_INST_ID__KEY",// used on PostgreSQL
      "ACT_UNIQ_TENANT_MEMB_USER",
      "ACT_UNIQ_TENANT_MEMB_GROUP");

  Properties properties;
  Database database;
  String databaseType;
  String projectVersion;
  Liquibase liquibase;
  DiffGeneratorFactory databaseDiffer;

  @BeforeEach
  void setup() throws Exception {
    InputStream is = getClass().getClassLoader().getResourceAsStream("properties-from-pom.properties");
    properties = new Properties();
    properties.load(is);

    databaseType = properties.getProperty("database.type");
    projectVersion = properties.getProperty("project.version");

    database = getDatabase();
    liquibase = getLiquibase();
    databaseDiffer = DiffGeneratorFactory.getInstance();
    cleanUpDatabaseTables();
  }

  @AfterEach
  void tearDown() throws Exception {
    cleanUpDatabaseTables();
    liquibase.close();
  }

  @Test
  void shouldEqualLiquibaseChangelogAndCreateScripts() throws Exception {
    // given
    executeSqlScript("create", "engine");
    executeSqlScript("create", "identity");
    DatabaseSnapshot snapshotScripts = createCurrentDatabaseSnapshot();
    cleanUpDatabaseTables();

    // when set up with Liquibase changelog
    liquibase.update(new Contexts());

    // then
    DatabaseSnapshot snapshotLiquibase = createCurrentDatabaseSnapshot();
    DiffResult diffResult = databaseDiffer.compare(snapshotScripts, snapshotLiquibase, new CompareControl());
    List<ChangeSet> changeSetsToApply = new DiffToChangeLog(diffResult, new CustomDiffOutputControl())
        .generateChangeSets();

    assertThat(changeSetsToApply)
        .withFailMessage("Liquibase database schema misses changes: %s", getChanges(changeSetsToApply))
        .isEmpty();
  }

  @Test
  @EnabledIf(value="isScriptsFromPreviousVersionPresent", disabledReason = "No scripts from previous version found")
  void shouldEqualOldUpgradedAndNewCreatedViaLiquibase() throws Exception {
    try (Liquibase liquibaseOld = getLiquibase("scripts-old/", getDatabase())) {
      // given
      liquibase.update(new Contexts());
      DatabaseSnapshot snapshotCurrent = createCurrentDatabaseSnapshot();
      cleanUpDatabaseTables();

      // old changelog executed
      liquibaseOld.update(new Contexts());

      // when new changelog executed afterward
      liquibase.update(new Contexts());

      // then
      DatabaseSnapshot snapshotUpgraded = createCurrentDatabaseSnapshot();
      DiffResult diffResult = databaseDiffer.compare(snapshotCurrent, snapshotUpgraded, new CompareControl());
      List<ChangeSet> changeSetsToApply = new DiffToChangeLog(diffResult, new DiffOutputControl()).generateChangeSets();

      assertThat(changeSetsToApply)
          .withFailMessage("Resulting upgraded database misses changes: %s", getChanges(changeSetsToApply))
          .isEmpty();
    }
  }

  @Test
  @EnabledIf(value="isScriptsFromPreviousVersionPresent", disabledReason = "No scripts from previous version found")
  void shouldEqualOldUpgradedAndNewCreatedViaScripts() throws Exception {
    // given
    String currentMajorMinor = properties.getProperty("current.majorminor");
    String oldMajorMinor = properties.getProperty("old.majorminor");

    executeSqlScript("create", "engine");
    executeSqlScript("create", "identity");
    DatabaseSnapshot snapshotCurrent = createCurrentDatabaseSnapshot();

    cleanUpDatabaseTables();

    // old CREATE scripts executed
    executeSqlScript("scripts-old/", "create", "engine_" + oldMajorMinor + ".0");
    executeSqlScript("scripts-old/", "create", "identity_" + oldMajorMinor + ".0");

    // when UPGRADE scripts executed
    executeSqlScript("local-upgrade-test/", "upgrade", "engine_" + oldMajorMinor + "_patch");
    executeSqlScript("local-upgrade-test/", "upgrade", "engine_" + oldMajorMinor + "_to_" + currentMajorMinor);
    executeSqlScript("local-upgrade-test/", "upgrade", "engine_" + currentMajorMinor + "_patch");

    // then
    DatabaseSnapshot snapshotUpgraded = createCurrentDatabaseSnapshot();
    DiffResult diffResult = databaseDiffer.compare(snapshotCurrent, snapshotUpgraded, new CompareControl());
    List<ChangeSet> changeSetsToApply = new DiffToChangeLog(diffResult, new CustomDiffOutputControl())
        .generateChangeSets();

    assertThat(changeSetsToApply)
        .withFailMessage("Resulting upgraded database schema differs: %s", getChanges(changeSetsToApply))
        .isEmpty();
  }

  void executeSqlScript(String sqlFolder, String sqlScript) throws LiquibaseException {
    executeSqlScript("", sqlFolder, sqlScript + "_" + projectVersion);
  }

  void executeSqlScript(String baseDirectory, String sqlFolder, String sqlScript) throws LiquibaseException {
    String scriptFileName = "%ssql/%s/%s_%s.sql".formatted(baseDirectory, sqlFolder, databaseType, sqlScript);
    InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
    assertThat(resourceAsStream).as("SQL script not found: " + scriptFileName).isNotNull();
    String statements = IoUtil.inputStreamAsString(resourceAsStream);
    SQLFileChange sqlFileChange = new SQLFileChange();
    sqlFileChange.setSql(statements);
    database.execute(sqlFileChange.generateStatements(database), null);
  }

  void cleanUpDatabaseTables() {
    try {
      liquibase.dropAll();
      // dropAll can be incomplete if it takes too long, second attempt should
      // clean up leftovers
      liquibase.dropAll();
    } catch (Exception e) {
      // ignored
    }
  }

  Database getDatabase() throws DatabaseException {
    String databaseUrl = properties.getProperty("database.url");
    String databaseUser = properties.getProperty("database.username");
    String databasePassword = properties.getProperty("database.password");
    String databaseClass = properties.getProperty("database.driver");
    return DatabaseFactory.getInstance().openDatabase(databaseUrl, databaseUser, databasePassword, databaseClass, null,
        null, null, new ClassLoaderResourceAccessor());
  }

  Liquibase getLiquibase() throws URISyntaxException, FileNotFoundException {
    return getLiquibase("", database);
  }

  static Liquibase getLiquibase(String baseDirectory, Database database)
    throws URISyntaxException, FileNotFoundException {
    return new Liquibase("operaton-changelog.xml", getAccessorForChangelogDirectory(baseDirectory), database);
  }

  static DirectoryResourceAccessor getAccessorForChangelogDirectory(String baseDirectory)
    throws URISyntaxException, FileNotFoundException {
    URL resource = SqlScriptTest.class.getClassLoader().getResource(baseDirectory + "sql/liquibase");
    Objects.requireNonNull(resource, "Changelog directory not found");
    URI changelogUri = resource.toURI();
    return new DirectoryResourceAccessor(Path.of(changelogUri));
  }

  DatabaseSnapshot createCurrentDatabaseSnapshot() throws Exception {
    return SnapshotGeneratorFactory.getInstance()
        .createSnapshot(database.getDefaultSchema(), database, new SnapshotControl(database));
  }

  static List<String> getChanges(List<ChangeSet> changeSetsToApply) {
    return changeSetsToApply.stream()
        .flatMap(cs -> cs.getChanges().stream())
        .map(Change::getDescription)
        .toList();
  }

  static class CustomDiffOutputControl extends DiffOutputControl {

    public CustomDiffOutputControl() {
      setObjectChangeFilter(new IgnoreUniqueConstraintsChangeFilter());
    }

    private static class IgnoreUniqueConstraintsChangeFilter implements ObjectChangeFilter {

      @Override
      public boolean includeUnexpected(DatabaseObject object, Database referenceDatabase,
          Database comparisionDatabase) {
        return include(object);
      }

      @Override
      public boolean includeMissing(DatabaseObject object, Database referenceDatabase, Database comparisionDatabase) {
        return include(object);
      }

      @Override
      public boolean includeChanged(DatabaseObject object,
          ObjectDifferences differences,
          Database referenceDatabase,
          Database comparisionDatabase) {
        return include(object);
      }

      @Override
      public boolean include(DatabaseObject object) {
        return !(object instanceof UniqueConstraint) || !IGNORED_CONSTRAINTS.contains(object.getName().toUpperCase());
      }
    }
  }

  static boolean isScriptsFromPreviousVersionPresent() {
    Path scriptsOldDir = Path.of("target/test-classes/scripts-old");
    if (!Files.exists(scriptsOldDir)) {
      return false;
    }

    try (var files = Files.list(scriptsOldDir)) {
      return files.findAny().isPresent();
    } catch (IOException e) {
      return false;
    }
  }
}
