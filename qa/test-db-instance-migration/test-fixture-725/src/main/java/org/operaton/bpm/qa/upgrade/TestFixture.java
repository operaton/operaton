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

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class TestFixture {

    private static final String SCHEMA_VERSION = "7.25.0";
    private static final Logger LOG = Logger.getLogger("TestFixture-" + SCHEMA_VERSION);

    public TestFixture(ProcessEngine processEngine) {
    }

    public static void main(String... args) throws Exception {
        LOG.info("Starting test fixture for DB schema " + SCHEMA_VERSION);
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("operaton.cfg.xml");

        String changelogDir = System.getProperty("liquibase.changelog.dir");
        Objects.requireNonNull(changelogDir, "System property liquibase.changelog.dir must be set");
        LOG.info("Running Liquibase migration scripts for DB schema " + SCHEMA_VERSION);
        runLiquibase(
                processEngineConfiguration.getJdbcUrl(),
                processEngineConfiguration.getJdbcUsername(),
                processEngineConfiguration.getJdbcPassword(),
                changelogDir);

        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

        // register test scenarios
        ScenarioRunner runner = new ScenarioRunner(processEngine, SCHEMA_VERSION);

        List<Class<?>> scenarios = getScenarios();
        if (scenarios.isEmpty()) {
            LOG.warning("No test scenarios defined.");
        }
        for (Class<?> scenarioClass : scenarios) {
            LOG.info("Running scenario " + scenarioClass.getSimpleName());
            runner.setupScenarios(scenarioClass);
        }

        processEngine.close();
        LOG.info("Finished test fixture for DB schema " + SCHEMA_VERSION);
    }

    private static void runLiquibase(String url, String username, String password, String changelogDir)
            throws SQLException, LiquibaseException {
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            try (Database db = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn))) {
                new Liquibase("operaton-changelog.xml",
                        new FileSystemResourceAccessor(new File(changelogDir)), db)
                        .update(new Contexts(), new LabelExpression());
            }
        }
    }

    private static List<Class<?>> getScenarios() {
        return List.of();
    }
}
