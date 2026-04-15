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
package org.operaton.bpm.quarkus.engine.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import javax.sql.DataSource;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.health.HealthService;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.health.DefaultHealthService;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;

@ApplicationScoped
public class HealthServiceProducer {

  @Produces
  @ApplicationScoped
  public HealthService healthService(DataSource dataSource, ProcessEngine processEngine) {
    JobExecutor jobExecutor = null;
    if (processEngine instanceof ProcessEngineImpl impl) {
      jobExecutor = impl.getProcessEngineConfiguration().getJobExecutor();
    }
    return new DefaultHealthService(dataSource, jobExecutor, null);
  }
}