/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.qa.upgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

public class TestFixture {

  public static final String ENGINE_VERSION = "7.24.0";

  public TestFixture(ProcessEngine processEngine) {
  }

  public static void main(String... args) throws Exception {
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
      .createProcessEngineConfigurationFromResource("operaton.cfg.xml");

    // Run Liquibase in the same classloader as the process engine so that both
    // share the same database instance (critical for jdbc:h2:mem: URLs where each
    // classloader gets an isolated in-memory database).
    String changelogDir = System.getProperty("liquibase.changelog.dir");
    if (changelogDir != null) {
      runLiquibase(
        processEngineConfiguration.getJdbcUrl(),
        processEngineConfiguration.getJdbcUsername(),
        processEngineConfiguration.getJdbcPassword(),
        changelogDir);
    }

    ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

    // register test scenarios
    ScenarioRunner runner = new ScenarioRunner(processEngine, ENGINE_VERSION);

    // example scenario setup
    // runner.setupScenarios(ExampleScenario.class);

    processEngine.close();
  }

  private static void runLiquibase(String url, String username, String password, String changelogDir)
      throws Exception {
    try (Connection conn = DriverManager.getConnection(url, username, password)) {
      Database db = DatabaseFactory.getInstance()
        .findCorrectDatabaseImplementation(new JdbcConnection(conn));
      try {
        new Liquibase("operaton-changelog.xml",
          new FileSystemResourceAccessor(new File(changelogDir)), db)
          .update(new Contexts(), new LabelExpression());
      } finally {
        db.close();
      }
    }
  }
}
