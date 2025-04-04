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

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.SchemaOperationsProcessEngineBuild;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.test.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Smirnov
 *
 */
class DmnDisabledTest {

  protected static ProcessEngineImpl processEngineImpl;

  // make sure schema is dropped
  @AfterEach
  void cleanup() {
    TestHelper.dropSchema(processEngineImpl.getProcessEngineConfiguration());
    processEngineImpl.close();
    processEngineImpl = null;
  }

  @Test
  void disabledDmn() {
    processEngineImpl = createProcessEngineImpl(false);

    // simulate manual schema creation by user
    TestHelper.createSchema(processEngineImpl.getProcessEngineConfiguration());

    // let the engine do their schema operations thing
    processEngineImpl.getProcessEngineConfiguration()
      .getCommandExecutorSchemaOperations()
      .execute(new SchemaOperationsProcessEngineBuild());

    assertThat(processEngineImpl.getRepositoryService().createDecisionDefinitionQuery().count()).isZero();
    assertThat(processEngineImpl.getRepositoryService().createDecisionDefinitionQuery().list()).isEmpty();
    assertThat(processEngineImpl.getRepositoryService().createDecisionRequirementsDefinitionQuery().count()).isZero();
    assertThat(processEngineImpl.getRepositoryService().createDecisionRequirementsDefinitionQuery().list()).isEmpty();

  }

  // allows to return a process engine configuration which doesn't create a schema when it's build.
  protected static class CustomStandaloneInMemProcessEngineConfiguration extends StandaloneInMemProcessEngineConfiguration {

    @Override
    public ProcessEngine buildProcessEngine() {
      init();
      return new CreateNoSchemaProcessEngineImpl(this);
    }
  }

  protected static class CreateNoSchemaProcessEngineImpl extends ProcessEngineImpl {

    public CreateNoSchemaProcessEngineImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
      super(processEngineConfiguration);
    }

    @Override
    protected void executeSchemaOperations() {
      // noop - do not execute create schema operations
    }
  }

  protected static ProcessEngineImpl createProcessEngineImpl(boolean dmnEnabled) {
    StandaloneInMemProcessEngineConfiguration config =
        (StandaloneInMemProcessEngineConfiguration) new CustomStandaloneInMemProcessEngineConfiguration()
               .setProcessEngineName("database-dmn-test-engine")
               .setDatabaseSchemaUpdate("false")
               .setHistory(ProcessEngineConfiguration.HISTORY_FULL)
               .setJdbcUrl("jdbc:h2:mem:DatabaseDmnTest");

    config.setDmnEnabled(dmnEnabled);

    return (ProcessEngineImpl) config.buildProcessEngine();
  }

}
