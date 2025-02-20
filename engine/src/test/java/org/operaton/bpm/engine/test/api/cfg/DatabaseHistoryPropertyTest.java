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
package org.operaton.bpm.engine.test.api.cfg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.HistoryLevelSetupCommand;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.SchemaOperationsProcessEngineBuild;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.junit.After;
import org.junit.Test;

/**
 * @author Christian Lipphardt
 */
public class DatabaseHistoryPropertyTest {


  private static ProcessEngineImpl processEngineImpl;

  // make sure schema is dropped
  @After
  public void cleanup() {
    processEngineImpl.close();
    processEngineImpl = null;
  }

  @Test
  public void schemaCreatedByEngineAndDatabaseSchemaUpdateTrue() {
    processEngineImpl = createProcessEngineImpl("true", true);

    assertHistoryLevel();
  }

  @Test
  public void schemaCreatedByUserAndDatabaseSchemaUpdateTrue() {
    processEngineImpl = createProcessEngineImpl("true", false);
    // simulate manual schema creation by user
    TestHelper.createSchema(processEngineImpl.getProcessEngineConfiguration());

    // let the engine do their schema operations thing
    processEngineImpl.getProcessEngineConfiguration()
    .getCommandExecutorSchemaOperations()
    .execute(new SchemaOperationsProcessEngineBuild());

    processEngineImpl.getProcessEngineConfiguration()
    .getCommandExecutorSchemaOperations()
    .execute(new HistoryLevelSetupCommand());

    assertHistoryLevel();
  }

  @Test
  public void schemaCreatedByUserAndDatabaseSchemaUpdateFalse() {
    processEngineImpl = createProcessEngineImpl("false", false);
    // simulate manual schema creation by user
    TestHelper.createSchema(processEngineImpl.getProcessEngineConfiguration());

    // let the engine do their schema operations thing
    processEngineImpl.getProcessEngineConfiguration()
    .getCommandExecutorSchemaOperations()
    .execute(new SchemaOperationsProcessEngineBuild());

    processEngineImpl.getProcessEngineConfiguration()
    .getCommandExecutorSchemaOperations()
    .execute(new HistoryLevelSetupCommand());

    assertHistoryLevel();
  }

  private void assertHistoryLevel() {
    Map<String, String> properties = processEngineImpl.getManagementService().getProperties();
    String historyLevel = properties.get("historyLevel");
    assertThat(historyLevel).as("historyLevel is null -> not set in database").isNotNull();
    assertThat(Integer.parseInt(historyLevel)).isEqualTo(ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL);
  }


  //----------------------- TEST HELPERS -----------------------

  private static class CreateSchemaProcessEngineImpl extends ProcessEngineImpl {
    public CreateSchemaProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
      super(processEngineConfiguration);
    }

    @Override
    protected void executeSchemaOperations() {
      super.executeSchemaOperations();
    }
  }

  private static class CreateNoSchemaProcessEngineImpl extends ProcessEngineImpl {
    public CreateNoSchemaProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
      super(processEngineConfiguration);
    }

    @Override
    protected void executeSchemaOperations() {
      // nop - do not execute create schema operations
    }
  }

  // allows to return a process engine configuration which doesn't create a schema when it's build.
  private static class CustomStandaloneInMemProcessEngineConfiguration extends StandaloneInMemProcessEngineConfiguration {

    boolean executeSchemaOperations;

    @Override
    public ProcessEngine buildProcessEngine() {
      init();
      if (executeSchemaOperations) {
        return new CreateSchemaProcessEngineImpl(this);
      } else {
        return new CreateNoSchemaProcessEngineImpl(this);
      }
    }

    public ProcessEngineConfigurationImpl setExecuteSchemaOperations(boolean executeSchemaOperations) {
      this.executeSchemaOperations = executeSchemaOperations;
      return this;
    }
  }

  private static ProcessEngineImpl createProcessEngineImpl(String databaseSchemaUpdate, boolean executeSchemaOperations) {
    return (ProcessEngineImpl) new CustomStandaloneInMemProcessEngineConfiguration()
               .setExecuteSchemaOperations(executeSchemaOperations)
               .setProcessEngineName("database-history-test-engine")
               .setDatabaseSchemaUpdate(databaseSchemaUpdate)
               .setHistory(ProcessEngineConfiguration.HISTORY_FULL)
               .setJdbcUrl("jdbc:h2:mem:DatabaseHistoryPropertyTest")
               .buildProcessEngine();
  }

}
