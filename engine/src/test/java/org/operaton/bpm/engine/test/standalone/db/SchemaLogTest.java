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
package org.operaton.bpm.engine.test.standalone.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.db.sql.DbSqlSessionFactory;
import org.operaton.bpm.engine.management.SchemaLogEntry;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.util.TestconfigProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Miklas Boskamp
 *
 */
class SchemaLogTest {

  protected static final String BASE_PATH = "org/operaton/bpm/engine/db";
  protected static final String CREATE_SCRIPT_FOLDER = BASE_PATH + "/create";
  protected static final String UPGRADE_SCRIPT_FOLDER = BASE_PATH + "/upgrade";
  protected static final List<String> SCRIPT_FOLDERS = Arrays.asList(CREATE_SCRIPT_FOLDER, UPGRADE_SCRIPT_FOLDER);
  protected static final String[] DATABASES = DbSqlSessionFactory.SUPPORTED_DATABASES;

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();

  public ProcessEngine processEngine;

  protected Map<String, List<String>> folderContents;

  @BeforeEach
  void init() {
    folderContents = new HashMap<>();
    for (String folder : SCRIPT_FOLDERS) {
      folderContents.put(folder, readFolderContent(folder));
    }
  }

  private List<String> readFolderContent(String path) {
    List<String> files = new ArrayList<>();
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath:" + path + "/*");
      assertThat(resources).isNotEmpty();
      for (Resource res : resources) {
        files.add(res.getFilename());
      }
    } catch (IOException e) {
      throw new AssertionError("unable to load resources from " + path);
    }

    return files;
  }

  public boolean isMinorLevel(String version) {
    // 7.10 -> true, 7.10.1 -> false
    return version.split("[.]").length == 2;
  }

  public boolean isPatchLevel(String version) {
    // 7.10.0 -> true, 7.10.1 -> true, 7.10 -> false
    return version.split("[.]").length == 3;
  }

  @Nested
  class EnsureSqlScript  {

    protected String currentSchemaVersion;
    protected String dataBaseType;

    @BeforeEach
    void init() {
      SchemaLogEntry latestEntry = processEngine.getManagementService().createSchemaLogQuery().orderByTimestamp().desc()
              .listPage(0, 1).get(0);
      currentSchemaVersion = latestEntry.getVersion();

      dataBaseType = processEngine.getProcessEngineConfiguration().getDatabaseType();
    }

    @Test
    void ensureUpgradeScriptsUpdateSchemaLogVersion() {
      List<String> scriptsForDB = new ArrayList<>();
      for (String file : folderContents.get(UPGRADE_SCRIPT_FOLDER)) {
        if (file.startsWith(dataBaseType)) {
          scriptsForDB.add(file);
        }
      }

      if (!scriptsForDB.isEmpty()) {
        assertThat(getLatestTargetVersion(scriptsForDB)).isEqualTo(currentSchemaVersion);
      } else {
        // databases that are newly added have no update scripts yet
        assertThat(getCurrentMinorVersion()).isEqualTo(currentSchemaVersion);
      }
    }

    @Test
    void ensureOnlyScriptsForValidDatabaseTypes() {
      for (String file : folderContents.get(UPGRADE_SCRIPT_FOLDER)) {
        assertThat(file.split("_")[0]).isIn((Object[]) DATABASES);
      }
    }

    protected String getTargetVersionForScript(String file) {
      String targetVersion = file.substring(file.indexOf("to_") + 3).replace(".sql", "");
      if (isMinorLevel(targetVersion)) {
        targetVersion += ".0";
      }
      return targetVersion;
    }

    protected String getLatestTargetVersion(List<String> scriptFiles) {
      String latestVersion = null;
      for (String file : scriptFiles) {
        if (latestVersion == null) {
          latestVersion = getTargetVersionForScript(file);
        } else {
          String targetVersion = getTargetVersionForScript(file);
          if (isLaterVersionThan(targetVersion, latestVersion)) {
            latestVersion = targetVersion;
          }
        }
      }
      return latestVersion;
    }

    protected boolean isLaterVersionThan(String v1, String v2) {
      String[] v1Parts = v1.split("[._]");
      String[] v2Parts = v2.split("[._]");

      int length = Math.max(v1Parts.length, v2Parts.length);
      for (int i = 0; i < length; i++) {
        int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
        int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
        if (v1Part != v2Part) {
          return v1Part > v2Part;
        }
      }
      return false;
    }

    protected String getCurrentMinorVersion() {
      String version = TestconfigProperties.getEngineVersion();
      // remove the patch version, and create a "clean" minor version
      int lastPos = version.lastIndexOf(".");
      version = version.substring(0, lastPos);

      return version + ".0";
    }
  }


  @Nested
  class UpgradeScriptPattern {

    @Test
    void testOnlyValidUpgradeFilePatterns() {
      /*
       * valid patterns:
       * h2_engine_7.2_to_7.3.sql,
       * oracle_engine_7.3_patch_7.3.0_to_7.3.1.sql,
       * postgres_engine_7.3_patch_7.3.2_to_7.3.3_1.sql,
       */
      for (String file : folderContents.get(UPGRADE_SCRIPT_FOLDER)) {
        assertThat(file).endsWith(".sql");
        // get rid of the .sql ending as it makes splitting easier
        file = file.substring(0, file.length() - 4);

        String[] nameParts = file.split("_");
        assertThat(nameParts[0]).isIn((Object[]) DATABASES);
        assertThat(nameParts[1]).isEqualTo("engine");
        String minorVersion = nameParts[2];
        assertThat(isMinorLevel(minorVersion)).isTrue();
        if ("to".equals(nameParts[3])) {
          // minor update
          assertThat(nameParts[4]).isIn(getPossibleNextVersions(minorVersion));

          assertThat(nameParts).hasSize(5);
        } else if ("patch".equals(nameParts[3])) {
          // patch update
          String basePatchVersion = nameParts[4];
          assertThat(isPatchLevel(basePatchVersion)).as("unexpected patch version pattern for file: " + file).isTrue();
          assertThat(minorVersion).isEqualTo(getMinorLevelFromPatchVersion(basePatchVersion));
          assertThat(nameParts[5]).isEqualTo("to");
          assertThat(nameParts[6]).isIn(getPossibleNextVersions(basePatchVersion));

          if (nameParts.length == 8) {
            // check that script version is integer only
            assertThatCode(() -> Integer.parseInt(nameParts[7])).doesNotThrowAnyException();
          } else {
            assertThat(nameParts).hasSize(7);
          }
        } else {
          throw new AssertionError("unexpected pattern for file: " + file);
        }
      }
    }

    private String getMinorLevelFromPatchVersion(String minorVersion) {
      String[] versionParts = minorVersion.split("[.]");
      return StringUtils.join(versionParts, ".", 0, 2);
    }

    private Object[] getPossibleNextVersions(String version) {
      List<String> versions = new ArrayList<>();
      String[] versionParts = version.split("[.]");
      if (isPatchLevel(version)) {
        // next patch version
        versions.add(versionParts[0] + "." + versionParts[1] + "." + (Integer.parseInt(versionParts[2]) + 1));
      } else if (isMinorLevel(version)) {
        // next minor version
        versions.add(versionParts[0] + "." + (Integer.parseInt(versionParts[1]) + 1));
        // next major version
        versions.add((Integer.parseInt(versionParts[0]) + 1) + ".0");
      } else {
        throw new AssertionError("unexpected pattern for version: " + version);
      }
      return versions.toArray(new String[0]);
    }
  }
}

