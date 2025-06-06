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
package org.operaton.bpm.quarkus.engine.test.id;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.db.DbIdGenerator;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.quarkus.engine.extension.QuarkusProcessEngineConfiguration;
import org.operaton.bpm.quarkus.engine.test.helper.ProcessEngineAwareExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigureDbIdGeneratorTest {

  @RegisterExtension
  static final QuarkusUnitTest unitTest = new ProcessEngineAwareExtension()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

  @Inject
  public TaskService taskService;

  @Inject
  protected ProcessEngine processEngine;

  @ApplicationScoped
  static class EngineConfigurer {

    @Produces
    public QuarkusProcessEngineConfiguration engineConfiguration() {
      return (QuarkusProcessEngineConfiguration) new QuarkusProcessEngineConfiguration()
          .setIdGenerator(null);
    }

  }

  @Test
  void shouldConfigureDbIdGenerator() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    String id = taskService.createTaskQuery().singleResult().getId();
    assertThat(Long.parseLong(id)).isPositive();

    ProcessEngineConfigurationImpl engineConfig =
        (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    assertThat(engineConfig.getIdGenerator()).isInstanceOf(DbIdGenerator.class);
  }

}
