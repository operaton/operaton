/*
 * Copyright 2026 the Operaton contributors.
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
package org.operaton.bpm.quarkus.engine.test.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.health.HealthService;
import org.operaton.bpm.quarkus.engine.extension.HealthServiceProducer;
import org.operaton.bpm.quarkus.engine.extension.QuarkusProcessEngineConfiguration;
import org.operaton.bpm.quarkus.engine.extension.OperatonHealthCheck;

import static org.assertj.core.api.Assertions.assertThat;

class OperatonHealthCheckTest {

  @RegisterExtension
  static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
          .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                  .addClasses(
                          MyConfig.class,
                          OperatonHealthCheck.class,
                          HealthServiceProducer.class
                  ))
          .withConfigurationResource("application.properties");

  @ApplicationScoped
  static class MyConfig {
    @Produces
    public QuarkusProcessEngineConfiguration engineConfig() {
      QuarkusProcessEngineConfiguration config = new QuarkusProcessEngineConfiguration();
      config.setJobExecutorActivate(false);
      return config;
    }
  }

  @Inject
  ProcessEngine processEngine;

  @Inject
  HealthService healthService;

  @Test
  void shouldReportUpWhenEngineIsRunning() {
    assertThat(processEngine).isNotNull();
    assertThat(healthService.check().status()).isEqualTo("UP");
  }

  @Test
  void shouldIncludeDatabaseDetail() {
    var details = healthService.check().details();
    assertThat(details).containsKey("database");
    assertThat(((java.util.Map<?, ?>) details.get("database")).get("connected")).isEqualTo(true);
  }

  @Test
  void shouldExposeHealthServiceAsCdiBean() {
    assertThat(healthService).isNotNull();
  }
}